# Gestão Fácil

API REST de um SaaS para pequenas empresas. Esta versão entrega a base do projeto, autenticação JWT, gestão de clientes e ordens de serviço por conta.

## Stack

- Java 25 LTS, Spring Boot 4.1 e Spring Framework 7
- Spring Security, JWT (JJWT) e BCrypt
- PostgreSQL, Spring Data JPA e Flyway
- Bean Validation, Lombok e OpenAPI/Swagger
- Actuator, Maven, Docker Compose e GitHub Actions

## Arquitetura

O projeto é um monólito modular preparado para crescer:

- `domain`: modelo de negócio sem dependência de persistência.
- `repository`: portas que representam as operações de dados usadas pelo domínio/aplicação.
- `service`: casos de uso de cadastro, autenticação, gestão de clientes e ordens de serviço.
- `infrastructure`: adaptadores de persistência JPA e integração com Spring Security.
- `controller` e `dto`: fronteira HTTP versionada e contratos de entrada/saída.
- `mapper`: conversão de modelos de domínio para respostas da API.
- `config`, `security` e `exception`: configuração transversal, autenticação e erros padronizados.

A entidade JPA fica em `infrastructure.persistence`, separada do modelo de domínio. O esquema é controlado exclusivamente por migrations Flyway; o Hibernate valida o mapeamento sem alterar o banco.

## Executar localmente

### Com Docker (recomendado)

Requer Docker Engine/Desktop com Docker Compose. Crie `.env` a partir de `.env.example` e informe credenciais exclusivas antes de iniciar. Gere `JWT_SECRET` com `openssl rand -base64 32` e `INITIAL_SETUP_TOKEN` com `openssl rand -hex 32`.

```powershell
Copy-Item .env.example .env
docker compose up --build
```

O Compose inicia PostgreSQL, Redis e API; a API aguarda os serviços de dados e executa as migrations ao iniciar. Redis fornece rate limiting compartilhado e permanece somente na rede privada do Compose. A API fica disponível em `http://localhost:8080`.

Para encerrar:

```powershell
docker compose down
```

Os dados permanecem no volume `postgres_data`. Para removê-los intencionalmente, execute `docker compose down -v`.

O primeiro cadastro exige a chave temporária configurada em `INITIAL_SETUP_TOKEN`, enviada no campo de chave da tela inicial. Compartilhe-a com o operador por canal privado e remova-a do ambiente após cadastrar a primeira conta. Se ausente, o cadastro inicial fica desabilitado. O navegador recebe autenticação em cookie JWT HttpOnly com proteção CSRF; o token não é persistido em armazenamento do navegador. Para produção em VPS, use `docker-compose.prod.yml`, Caddy com HTTPS automático e o arquivo `.env.prod.example` como referência. Procedimentos de deploy em Railway/Render/VPS, variáveis de ambiente e backup/restauração PostgreSQL estão em [docs/production.md](./docs/production.md).

### Sem Docker

Requer JDK 25, Maven 3.9+, PostgreSQL e Redis. Crie o banco e configure `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` e `REDIS_HOST`/`REDIS_PORT`; depois defina `JWT_SECRET` como uma chave Base64 com pelo menos 256 bits:

```powershell
$env:DB_PASSWORD = "<senha-forte-do-banco>"
$env:JWT_SECRET = "<chave-base64-com-32-bytes-ou-mais>"
$env:INITIAL_SETUP_TOKEN = "<segredo-aleatorio-de-64-caracteres>"
mvn spring-boot:run
```

O perfil local `dev` é ativado por padrão. Para iniciar em produção, use `SPRING_PROFILES_ACTIVE=prod` e configure obrigatoriamente `JWT_SECRET` e credenciais fortes de banco. Não use a chave ou senha local padrão em ambientes publicados; mantenha os segredos em um cofre de segredos.

## Endpoints

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/v1/auth/csrf` | Público | Emite token CSRF não armazenável para o cliente HTTP. |
| `POST` | `/api/v1/auth/login` | Público com CSRF | Autentica, define cookie JWT HttpOnly e retorna dados públicos do usuário. |
| `GET` | `/api/v1/auth/session` | Autenticado | Retorna o perfil associado ao cookie JWT atual. |
| `POST` | `/api/v1/auth/logout` | Autenticado com CSRF | Revoga imediatamente o JWT atual e limpa o cookie. |
| `GET` | `/api/v1/setup/status` | Público | Informa se a instalação ainda precisa criar a primeira conta. |
| `POST` | `/api/v1/setup/first-user` | Público com chave de bootstrap, uma única vez | Cria a primeira conta apenas enquanto não houver usuários. |
| `POST` | `/api/v1/clients` | Autenticado | Cadastra um cliente na conta atual. |
| `GET` | `/api/v1/clients` | Autenticado | Lista os clientes da conta atual. |
| `GET` | `/api/v1/clients/{id}` | Autenticado | Busca um cliente da conta atual. |
| `PUT` | `/api/v1/clients/{id}` | Autenticado | Atualiza um cliente da conta atual. |
| `DELETE` | `/api/v1/clients/{id}` | Autenticado | Exclui um cliente da conta atual. |
| `POST` | `/api/v1/service-orders` | Autenticado | Cria uma ordem de serviço para um cliente da conta atual. |
| `GET` | `/api/v1/service-orders` | Autenticado | Lista as ordens de serviço da conta atual. |
| `GET` | `/api/v1/service-orders/{id}` | Autenticado | Busca uma ordem de serviço da conta atual. |
| `PUT` | `/api/v1/service-orders/{id}` | Autenticado | Atualiza descrição, valor e cliente. |
| `PATCH` | `/api/v1/service-orders/{id}/status` | Autenticado | Atualiza o status da ordem. |
| `DELETE` | `/api/v1/service-orders/{id}` | Autenticado | Exclui uma ordem de serviço da conta atual. |
| `POST` | `/api/v1/budgets` | Autenticado | Cria orçamento pendente para um cliente. |
| `GET` | `/api/v1/budgets` | Autenticado | Lista orçamentos da conta. |
| `GET` | `/api/v1/budgets/{id}` | Autenticado | Busca um orçamento da conta. |
| `PUT` | `/api/v1/budgets/{id}` | Autenticado | Atualiza os dados do orçamento. |
| `PATCH` | `/api/v1/budgets/{id}/status` | Autenticado | Aprova ou rejeita o orçamento. |
| `DELETE` | `/api/v1/budgets/{id}` | Autenticado | Exclui um orçamento. |
| `GET` | `/api/v1/budgets/{id}/pdf` | Autenticado | Baixa o orçamento em PDF, válido por 30 dias. |

O navegador envia o cookie JWT automaticamente; todas as operações de alteração exigem `X-XSRF-TOKEN`, obtido em `GET /api/v1/auth/csrf`. O frontend usa `withCredentials` e CORS aceita somente as origens configuradas. Erros seguem o formato comum com `timestamp`, `status`, `error`, `message`, `path` e `fieldErrors`.

Consulte a [revisão de segurança](./SECURITY_REVIEW.md) para os achados verificados, limites dos testes e riscos residuais.

### Exemplos

### Primeiro acesso

Em uma instalação sem usuários, o frontend verifica `GET /api/v1/setup/status` e apresenta a criação da primeira conta. Configure `INITIAL_SETUP_TOKEN` com pelo menos 32 caracteres aleatórios e entregue a chave temporária ao operador por canal privado; o frontend a envia somente no cabeçalho `X-Setup-Token`, nunca no corpo nem em armazenamento persistente. O endpoint `POST /api/v1/setup/first-user` aceita o corpo somente uma vez; tentativas posteriores recebem `409 Conflict`. O cadastro inicial é serializado no PostgreSQL para evitar duas primeiras contas em requisições simultâneas. Após cadastrar a conta, remova a chave do ambiente e faça login normalmente. O cadastro público legado `/api/v1/auth/register` não é disponibilizado.

```json
{
  "name": "Seu Nome",
  "email": "voce@exemplo.com",
  "password": "senha-segura-123"
}
```

Quando já existe usuário, a tela exibe o login normal.

Login:

```json
{
  "email": "ana@example.com",
  "password": "senha-segura-123"
}
```

Logout (token JWT obrigatório): `POST /api/v1/auth/logout`. O token atual é revogado no servidor e não poderá ser reutilizado.

Cadastro de cliente (token JWT obrigatório):

```json
{
  "name": "João da Silva",
  "phone": "(11) 99999-0000",
  "email": "joao@example.com",
  "document": "123.456.789-00"
}
```

Criação de ordem de serviço (token JWT obrigatório; status inicial `OPEN`):

```json
{
  "description": "Reparo de equipamento",
  "value": 125.50,
  "clientId": "00000000-0000-0000-0000-000000000000"
}
```

Atualização de status:

```json
{
  "status": "IN_PROGRESS"
}
```

Status disponíveis: `OPEN`, `IN_PROGRESS`, `COMPLETED` e `CANCELLED`. O status é atualizado separadamente pelo endpoint `PATCH`.

Criação de orçamento (token JWT obrigatório; número sequencial e status `PENDING` são gerados pelo servidor):

```json
{
  "clientId": "00000000-0000-0000-0000-000000000000",
  "serviceOrderId": null,
  "description": "Revisão preventiva e troca de peças",
  "value": 480.00
}
```

O orçamento pode ser associado opcionalmente a uma ordem de serviço do mesmo cliente e da mesma conta. O PDF usa uma identidade textual de placeholder "Gestão Fácil", contém os dados do cliente, descrição, valor, emissão e validade de 30 dias. O Dashboard apresenta totais e valor aprovado.

## Swagger e saúde

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health check: `http://localhost:8080/actuator/health`
- Readiness/liveness: `/actuator/health/readiness` e `/actuator/health/liveness`

Em produção o health check não revela detalhes dos componentes. A API usa CORS com origens explícitas, headers de segurança, logs JSON e rate limit para login/cadastro. Veja [docs/production.md](./docs/production.md) para operação, backup e deploy.

Achados, correções, riscos residuais e evidências da auditoria de segurança estão em [SECURITY_REVIEW.md](./SECURITY_REVIEW.md).

## Testes

```powershell
mvn test

cd frontend
npm ci
npm test
npm run lint
npm run build
```

## Roadmap

1. Produtos e serviços.
2. Perfis e permissões, recuperação de senha e refresh token.
3. Pipeline CI/CD e observabilidade ampliada.

Os recursos posteriores podem ser adicionados como módulos independentes, mantendo o versionamento `/api/v1` e os casos de uso isolados.

## Frontend

O painel React + TypeScript está em `frontend/` e consome a API existente. Requer Node.js 20.19+:

```powershell
cd frontend
npm install
npm run dev
```

O Vite fica disponível em `http://localhost:5173` e encaminha `/api` para `http://localhost:8080`. Inicie a API e o PostgreSQL com `docker compose up --build` em outro terminal. Para apontar para outra API, configure `VITE_API_BASE_URL` ou `API_PROXY_TARGET` conforme necessário.

O painel inclui login JWT, dashboard, gestão de clientes, ordens de serviço e orçamentos com aprovação/rejeição e download de PDF; o Swagger permanece em `http://localhost:8080/swagger-ui.html`.

Para validar a compilação e as regras de lint:

```powershell
npm run build
npm run lint
```
