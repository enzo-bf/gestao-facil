# Modernization Plan: Remaining Security Findings

**Project**: Gestao Facil

## Technical Framework

- **Language**: Java 25
- **Framework**: Spring Boot 3.5.16 / Spring Framework 6.2.x
- **Build Tool**: Maven
- **Database**: PostgreSQL
- **Frontend**: React with Axios
- **Runtime and deployment context**: Docker Compose for development and production

## Overview

This focused plan closes the three remaining findings documented in
`SECURITY_REVIEW.md`: vulnerable Spring MVC dependencies, browser-persisted
bearer JWTs, and per-process authentication rate limits. It also brings the
security report, user-facing documentation, and relevant tests in line with
the implemented and verified outcome.

The work preserves Java 25, existing server-side token revocation, user changes,
and the current workspace (which has no Git repository). It is limited to the
requested findings and their directly affected configuration, tests, and docs.
Execution stops after targeted and relevant full tests and available scans have
run, or after a concrete environment blocker is documented. No implementation
or commit is part of this plan.

## Migration Impact Summary

| Area | Current state | Target outcome | Verification |
|---|---|---|---|
| Spring stack | Boot 3.5.16 / Framework 6.2.x | Compatible Boot 4.x / Framework 7.x | Maven tests/build; Trivy if available |
| Browser session | JWT stored in localStorage | HttpOnly cookie and protected CSRF flow | Auth, cookie, CSRF and browser-flow tests |
| Auth throttling | Per-process limits | Atomic, shared Redis-backed limits | Shared-limit and startup behavior tests |
| Security evidence | Three findings remain | Accurate report, docs and scan evidence | Targeted/full tests and available scans |

## Scope and Constraints

- Retain Java 25 and choose compatible, available patched Spring Boot 4.x and
  Spring Framework 7.x artifacts that remove CVE-2026-47884 and
  CVE-2026-47890. Resolve SpringDoc and any other resulting compatibility
  issues; do not leave known CVEs in place to avoid the major upgrade.
- Replace browser `localStorage` bearer-token authentication with server-set
  HttpOnly JWT cookies. Configure `Secure` appropriately for local development
  and production and use an appropriate `SameSite` policy. Protect all
  state-changing requests against CSRF and provide safe token acquisition for
  Axios. Allow credentialed CORS only for explicit trusted origins.
- Update login, first-user setup auto-login, logout, protected-route behavior,
  session detection, relevant tests, and documentation. Preserve server-side
  token revocation. Never log credentials, bearer tokens, cookies, or CSRF
  tokens, and never persist bearer tokens in browser storage.
- Replace per-process auth rate limits with atomic/shared Redis-backed limits.
  Wire required Redis configuration into development and production Compose,
  environment examples, and production documentation. Redis unavailability
  must cause explicit startup failure or documented fail-closed behavior; no
  silent in-memory fallback is permitted. Do not trust spoofable forwarded-IP
  headers without an explicitly enforced trusted-proxy policy.
- Update `SECURITY_REVIEW.md` with verified outcomes, actual scan evidence and
  remaining risks; add a README link if appropriate. Add or adjust CSRF,
  cookie/session, rate-limit, and authentication-flow tests as feasible.
- Preserve existing user changes. Do not perform work outside the listed
  security findings and their direct verification/documentation needs. Do not
  create a Git repository or commit.

## Validation and Completion

Run the targeted tests for the changed authentication, CSRF, cookie/session,
and rate-limit behavior, then the relevant full test suite and Maven build.
Run Trivy when available and record its actual result. Record unavailable
scans, failed environment setup, or other blockers explicitly rather than
claiming a pass. Update the security review using only observed test and scan
evidence, include residual risks, and stop once the requested tests and
available scans have run or a concrete environment blocker is documented.
