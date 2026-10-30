# Gestão Fácil

API REST de um SaaS multiusuário para pequenas empresas. Inclui autenticação JWT, confirmação de e-mail, recuperação de senha, workspaces compartilhados, controle granular de permissões e gestão de clientes, ordens de serviço e orçamentos.

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
- `service`: casos de uso de autenticação, contas, permissões, workspaces, auditoria e módulos de negócio.
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

O primeiro cadastro também pode ser feito publicamente pela tela `/register`; a tela de configuração inicial com `INITIAL_SETUP_TOKEN` permanece disponível para o operador. O navegador recebe autenticação em cookie JWT HttpOnly com proteção CSRF; o token não é persistido em armazenamento do navegador. Para produção em VPS, use `docker-compose.prod.yml`, Caddy com HTTPS automático e o arquivo `.env.prod.example` como referência. Procedimentos de deploy em Railway/Render/VPS, variáveis de ambiente e backup/restauração PostgreSQL estão em [docs/production.md](./docs/production.md).

Para habilitar verificação de e-mail, recuperação de senha e convites, configure `RESEND_API_KEY`, `MAIL_FROM` com um remetente verificado no Resend e `APP_PUBLIC_URL` com a URL pública do frontend. Sem essa configuração, essas ações retornam erro explícito e não criam um cadastro que não possa ser confirmado.

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
| `POST` | `/api/v1/auth/register` | Público com CSRF e rate limit | Cria workspace e conta admin, envia confirmação e inicia sessão JWT em cookie HttpOnly. |
| `POST` | `/api/v1/auth/verify-email` | Público com CSRF e rate limit | Confirma e-mail usando token de uso único (24 horas). |
| `POST` | `/api/v1/auth/resend-verification` | Público com CSRF e rate limit | Reenvia a confirmação sem revelar se o e-mail está cadastrado. |
| `POST` | `/api/v1/auth/forgot-password` | Público com CSRF e rate limit | Envia link de redefinição (30 minutos), com resposta que não revela a existência da conta. |
| `POST` | `/api/v1/auth/reset-password` | Público com CSRF e rate limit | Redefine a senha e invalida sessões existentes. |
| `POST` | `/api/v1/auth/activate-invitation` | Público com CSRF e rate limit | Ativa o convite e define a senha inicial (7 dias). |
| `GET` | `/api/v1/auth/session` | Autenticado | Retorna o perfil associado ao cookie JWT atual. |
| `POST` | `/api/v1/auth/logout` | Autenticado com CSRF | Revoga imediatamente o JWT atual e limpa o cookie. |
| `GET` | `/api/v1/profile` | Autenticado | Retorna nome, e-mail e telefone do usuário atual. |
| `PUT` | `/api/v1/profile` | Autenticado com CSRF | Atualiza e-mail/telefone; nome permanece somente leitura e alteração de e-mail renova o cookie de sessão. |
| `GET` | `/api/v1/setup/status` | Público | Informa se a instalação ainda precisa criar a primeira conta. |
| `POST` | `/api/v1/setup/first-user` | Público com chave de bootstrap, uma única vez | Cria a primeira conta apenas enquanto não houver usuários. |
| `POST` | `/api/v1/clients` | `CLIENT_CREATE` | Cadastra um cliente no workspace. |
| `GET` | `/api/v1/clients[/{id}]` | `CLIENT_VIEW` | Lista ou busca clientes do workspace. |
| `PUT` | `/api/v1/clients/{id}` | `CLIENT_UPDATE` | Atualiza um cliente do workspace. |
| `DELETE` | `/api/v1/clients/{id}` | `CLIENT_DELETE` | Exclui um cliente do workspace. |
| `POST` | `/api/v1/service-orders` | `OS_CREATE` | Cria uma ordem de serviço. |
| `GET` | `/api/v1/service-orders[/{id}]` | `OS_VIEW` | Lista ou busca ordens de serviço. |
| `PUT` | `/api/v1/service-orders/{id}` | `OS_UPDATE` | Atualiza descrição, valor e cliente. |
| `PATCH` | `/api/v1/service-orders/{id}/status` | `OS_UPDATE` | Atualiza o status da ordem. |
| `DELETE` | `/api/v1/service-orders/{id}` | `OS_DELETE` | Exclui uma ordem de serviço. |
| `POST` | `/api/v1/budgets` | `BUDGET_CREATE` | Cria orçamento pendente para um cliente. |
| `GET` | `/api/v1/budgets[/{id}]` e `/api/v1/budgets/{id}/pdf` | `BUDGET_VIEW` | Consulta orçamento e baixa PDF. |
| `PUT` | `/api/v1/budgets/{id}` | `BUDGET_UPDATE` | Atualiza os dados do orçamento. |
| `PATCH` | `/api/v1/budgets/{id}/status` | `BUDGET_UPDATE` | Aprova ou rejeita o orçamento. |
| `DELETE` | `/api/v1/budgets/{id}` | `BUDGET_DELETE` | Exclui um orçamento. |
| `GET` | `/api/v1/users` | `USER_MANAGE` | Lista membros do workspace e suas permissões. |
| `POST` | `/api/v1/users` | `USER_MANAGE` | Convida um usuário com permissões individuais. |
| `GET` | `/api/v1/permissions` | `USER_MANAGE` | Lista o catálogo de permissões disponíveis. |
| `PUT` | `/api/v1/users/{id}/permissions` | `USER_MANAGE` | Substitui permissões do usuário. |
| `PATCH` | `/api/v1/users/{id}/status` | `USER_MANAGE` | Ativa ou desativa o usuário. |
| `DELETE` | `/api/v1/users/{id}` | `USER_MANAGE` | Remove um membro do workspace. |
| `GET` | `/api/v1/audit-events` | `USER_MANAGE` | Consulta eventos recentes do workspace. |

O navegador envia o cookie JWT automaticamente; todas as operações de alteração exigem `X-XSRF-TOKEN`, obtido em `GET /api/v1/auth/csrf`. O frontend usa `withCredentials` e CORS aceita somente as origens configuradas. Erros seguem o formato comum com `timestamp`, `status`, `error`, `message`, `path` e `fieldErrors`.

O login aguarda a confirmação da sessão no servidor antes de navegar para as telas protegidas. Consultas repetem apenas falhas transitórias; uma rejeição CSRF identificada recebe atualização do token e, no máximo, uma repetição da operação. Clientes têm validação de nome, telefone brasileiro e dígitos verificadores de CPF/CNPJ no frontend e no backend. Valores de orçamento e ordens usam máscara monetária brasileira no formulário.

### Workspaces, permissões e auditoria

Cada cadastro público cria um workspace e recebe todas as permissões do catálogo. Contas já existentes são migradas para workspaces próprios, preservando seus dados; convites associam novos membros ao workspace do administrador. Clientes e orçamentos são compartilhados entre membros do mesmo workspace e permanecem isolados entre empresas. Não há perfis fixos: a autorização é feita por permissões individuais, validada em cada endpoint e refletida no menu e nas ações da interface. As permissões disponíveis são `CLIENT_VIEW/CREATE/UPDATE/DELETE`, `OS_VIEW/CREATE/UPDATE/DELETE`, `BUDGET_VIEW/CREATE/UPDATE/DELETE`, `DASHBOARD_VIEW`, `SETTINGS_MANAGE` e `USER_MANAGE`. Sem permissão, a API responde `403 Forbidden`; endpoints não mapeados são negados por padrão.

O administrador não pode remover a própria conta nem desativar/remover/retirar a gestão do último administrador ativo e verificado. A auditoria registra mutações em clientes, ordens de serviço, orçamentos e administração de usuários, com ator, ação, recurso, identificador e horário.

Consulte a [revisão de segurança](./SECURITY_REVIEW.md) para os achados verificados, limites dos testes e riscos residuais.

### Exemplos

### Primeiro acesso

Em uma instalação sem usuários, o frontend verifica `GET /api/v1/setup/status` e apresenta a configuração inicial protegida pelo `INITIAL_SETUP_TOKEN`. O cadastro normal também está disponível em `POST /api/v1/auth/register` e na tela `/register`, sem chave de bootstrap; esse endpoint exige CSRF e rate limit e bloqueia e-mails já cadastrados.

```json
{
  "name": "Seu Nome",
  "email": "voce@exemplo.com",
  "phone": "(11) 99999-1234",
  "password": "Senha123"
}
```

O endpoint de cadastro retorna a mesma resposta pública do login e define o cookie HttpOnly. A interface confirma a sessão e direciona o usuário para a confirmação de e-mail; módulos de negócio só liberam acesso após a validação do link.

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

Em produção o health check não revela detalhes dos componentes. A API usa CORS com origens explícitas, headers de segurança, logs JSON e rate limit nos endpoints de autenticação e ações de conta. Veja [docs/production.md](./docs/production.md) para operação, backup e deploy.

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
2. Relatórios avançados e exportação.
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

O painel inclui login, cadastro, verificação de e-mail, recuperação de senha, perfil, gestão de usuários e permissões, dashboard, clientes, ordens de serviço e orçamentos com aprovação/rejeição e download de PDF; o Swagger permanece em `http://localhost:8080/swagger-ui.html`.

Para validar a compilação e as regras de lint:

```powershell
npm run build
npm run lint
```
