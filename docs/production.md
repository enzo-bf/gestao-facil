# Production operations

## Runtime configuration

Use the `prod` Spring profile in published environments. It requires explicit database and Redis credentials, `JWT_SECRET`, and `CORS_ALLOWED_ORIGINS`. It emits structured JSON logs and deliberately does not trust forwarded client-IP headers for rate-limit keys. Never publish development secrets.

| Variable | Required | Purpose |
|---|---:|---|
| `SPRING_PROFILES_ACTIVE` | Yes | Set to `prod`. |
| `SERVER_PORT` | Platform-dependent | HTTP port; map Railway/Render's `PORT`. |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Yes | PostgreSQL endpoint and database. |
| `DB_USERNAME`, `DB_PASSWORD` | Yes | PostgreSQL credentials. |
| `REDIS_HOST`, `REDIS_PORT` | Yes | Shared Redis endpoint used for atomic authentication rate limits. |
| `REDIS_PASSWORD` | Yes in production | Redis authentication secret; keep Redis private to the application network. |
| `JWT_SECRET` | Yes | Base64-encoded random key of at least 256 bits. Generate with `openssl rand -base64 32`. |
| `JWT_COOKIE_SECURE` | Yes in production | Enable `Secure` on the HttpOnly authentication cookie; production Compose sets `true`. |
| `JWT_COOKIE_SAME_SITE` | Yes in production | `None` for separately hosted frontend origins (requires Secure); use `Lax` when deployed same-site. |
| `JWT_ISSUER` | No | JWT issuer; defaults to `gestao-facil`. |
| `JWT_EXPIRATION_SECONDS` | No | JWT lifetime; defaults to 1800 seconds. |
| `INITIAL_SETUP_TOKEN` | Only during initial setup | At least 32 random characters; sent as `X-Setup-Token` to authorize the first account. Remove it after the first account is created. |
| `CORS_ALLOWED_ORIGINS` | Yes in prod | Comma-separated exact frontend origins, including scheme and port; do not use `*`. |
| `AUTH_RATE_LIMIT_PER_MINUTE` | No | POST login/first-account setup limit per Redis key; defaults to 20 per 60-second window. |
| `VITE_API_BASE_URL` | Frontend build-time | API base URL; keep empty for same-origin VPS deployment, otherwise use the public API origin. |

The authentication limiter atomically increments a Redis key shared by application replicas, expires each key after 60 seconds, and responds with HTTP 429 plus `Retry-After`. It protects login and first-account setup, not general API traffic. If Redis is unavailable, the filter fails closed with HTTP 503; there is no in-memory fallback. In Docker Compose, Redis is private to the application network and production Redis requires a password. The application uses the socket peer address for rate limiting and ignores forwarded-IP headers. Consequently requests through a single reverse proxy may share one rate-limit key; do not enable forwarded-header processing unless a separate trusted-proxy policy is enforced. Set `INITIAL_SETUP_TOKEN` to a random secret of at least 32 characters only while provisioning the first account. Provide it to the operator through a separate private channel, then remove it from the deployment environment and restart the service. When unset or too short, first-account creation returns service unavailable.

`GET /actuator/health` is public and returns only aggregate UP/DOWN status. Readiness and liveness probes are available at `/actuator/health/readiness` and `/actuator/health/liveness`. No other Actuator endpoints are exposed. Spring Security adds CSP, content-type, frame, referrer, permissions, and HSTS headers; terminate HTTPS at the production proxy. Browser authentication uses an HttpOnly JWT cookie; state-changing API requests require a CSRF token obtained from `GET /api/v1/auth/csrf` and sent in `X-XSRF-TOKEN`. Credentialed CORS accepts only explicitly configured origins.

Login and first-user setup set the HttpOnly `GF-AUTH` cookie. The cookie is Secure in production and has the configured SameSite policy. The frontend keeps only the non-secret profile in memory and never stores bearer JWTs in browser storage. Logout revokes the current JWT in PostgreSQL immediately and clears the cookie; revoked token identifiers are retained only until expiration. If logout cannot reach the server, the client clears in-memory profile state and reports that server-side revocation could not be confirmed.

## Docker Compose on a Linux VPS

1. Install Docker Engine and the Compose plugin. Point the domain's A/AAAA records to the host and allow inbound TCP 80/443 (and UDP 443 for HTTP/3).
2. Clone the project, create `.env` from `.env.prod.example`, and provide unique DB/Redis credentials, a freshly generated JWT secret, and the exact HTTPS frontend origin. Keep `.env` out of source control.
3. Start the stack:

   ```sh
   docker compose -f docker-compose.prod.yml config
   docker compose -f docker-compose.prod.yml up --build -d
   docker compose -f docker-compose.prod.yml ps
   ```

   Caddy obtains and renews TLS certificates for `DOMAIN`; only Caddy publishes ports. PostgreSQL and password-protected Redis are private to the Compose network; database data is stored in the named `postgres_data` volume, and Flyway applies migrations on API startup. The frontend is served by unprivileged Nginx and proxies `/api/` to the backend on the private network.
4. Verify `https://$DOMAIN/` and `https://$DOMAIN/actuator/health`. Review logs with `docker compose -f docker-compose.prod.yml logs --tail=100 app`.
5. To deploy an update, pull the reviewed revision and run `docker compose -f docker-compose.prod.yml up --build -d`. Back up first when the release changes data structures. Never use `down -v` on production.

### PostgreSQL backups and recovery

The named Docker volume provides persistence across container recreation, but is not a backup. Create compressed, checksummed logical backups on the host:

```sh
BACKUP_DIR=/var/backups/gestao-facil \
BACKUP_RETENTION_DAYS=14 \
bash scripts/backup-postgres.sh
```

Schedule the command with a host cron/systemd timer (for example nightly), monitor its exit status, and copy successful `.dump` and `.sha256` files to encrypted off-site/object storage with restricted access. Keep retention appropriate to the business's recovery-point objective and periodically test restore in a separate database/server.

Example restore to a fresh or intentionally replaced database (review the target carefully first):

```sh
docker compose -f docker-compose.prod.yml exec -T postgres \
  sh -c 'pg_restore --clean --if-exists --no-owner --username="$POSTGRES_USER" --dbname="$POSTGRES_DB"' \
  < /path/to/gestao-facil-backup.dump
```

Restoration overwrites conflicting database objects. Stop API writers, validate the backup checksum, restore to a non-production target first when possible, and verify application health before resuming traffic.

## Railway

Deploy PostgreSQL and a Redis-compatible service in Railway, then deploy the backend from the repository root using the existing `Dockerfile`. Add a public domain to the backend (for `/actuator/health`) and set:

- `SPRING_PROFILES_ACTIVE=prod`, `SERVER_PORT=${{PORT}}`.
- Map the PostgreSQL service's private host, port, database, username, and password to `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`.
- Map the Redis service's private host, port, and password to `REDIS_HOST`, `REDIS_PORT`, and `REDIS_PASSWORD`; keep Redis private and reachable from the backend.
- Set `JWT_SECRET` from a secret variable generated with `openssl rand -base64 32`; set `CORS_ALLOWED_ORIGINS` to the exact public frontend origin.
- Set `JWT_COOKIE_SECURE=true` and choose `JWT_COOKIE_SAME_SITE` according to the frontend/API deployment relationship (`None` requires Secure).
- Set a unique `INITIAL_SETUP_TOKEN` secret while provisioning, give it to the initial operator through a separate private channel, and remove it after the first account is created.
- Set Railway's health check path to `/actuator/health`.

Deploy the frontend as a separate Docker service with its root directory set to `frontend/` and the `frontend/Dockerfile`. Set its build variable `VITE_API_BASE_URL` to the backend's public origin, attach a domain, and add that exact origin to the backend CORS list. Vite embeds this value at image build time; rebuild after changing it. Use Railway's managed database backup/export facility and copy backups to independent storage; do not treat service volumes as backups.

## Render

Create managed PostgreSQL and Redis-compatible services, plus a backend Web Service using the root `Dockerfile`. Map Render's **internal** database hostname and credentials to the `DB_*` variables and the private Redis endpoint/secret to `REDIS_HOST`, `REDIS_PORT`, and `REDIS_PASSWORD`. Set `SPRING_PROFILES_ACTIVE=prod`, `SERVER_PORT=${PORT}`, `JWT_SECRET`, `JWT_COOKIE_SECURE=true`, an appropriate `JWT_COOKIE_SAME_SITE`, and `CORS_ALLOWED_ORIGINS`, then configure health check path `/actuator/health`.

During first-account provisioning, also set a unique `INITIAL_SETUP_TOKEN` secret and share it with the operator using a separate private channel. Remove the variable after the account is created.

Create the frontend as a Static Site rooted at `frontend/`, with build command `npm ci && npm run build`, publish directory `dist`, and build-time `VITE_API_BASE_URL` set to the API's HTTPS origin. Configure the backend CORS allowlist to the exact static-site/custom-domain origin. If using a custom domain, use that canonical HTTPS origin on both sides.

Enable the database provider's automated backups/PITR plan suitable for the deployment, and regularly export a copy to storage outside Render. Test a restore independently; a successful health check does not prove backup recoverability.

## GitHub Actions

`.github/workflows/ci.yml` runs on pushes to `main`/`master` and pull requests. It uses Java 25 and Node 22, runs Maven `verify` (including unit tests), frontend ESLint and production build, then uploads the backend JAR and frontend `dist` as 14-day workflow artifacts. Deployments remain an explicit release action; CI does not publish images or deploy unreviewed changes.
