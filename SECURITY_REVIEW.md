# Revisão de Segurança — Gestão Fácil

**Data:** 29/10/2026  
**Escopo:** achados remanescentes de Spring MVC, sessão de navegador e limitação distribuída de autenticação.  
**Resultado:** os três achados foram corrigidos. As imagens backend e frontend finais não apresentaram vulnerabilidades no scan Trivy registrado abaixo. O sistema ainda depende da disponibilidade de Redis para autenticação.

## Resumo executivo

O projeto mantém Java 25 e foi atualizado para Spring Boot 4.1.2 / Spring Framework 7.0.10. A autenticação do navegador não retorna nem persiste JWT em `localStorage`: o servidor define cookie JWT `HttpOnly`, configurável para `Secure` e `SameSite`. CSRF está habilitado em todos os métodos de alteração; Axios obtém o token por `GET /api/v1/auth/csrf`, envia `X-XSRF-TOKEN` e renova o token após login. CORS permite credenciais apenas para origens explícitas.

O limitador local foi substituído por contador Redis atômico compartilhado entre instâncias. Se Redis falhar, login e primeiro cadastro falham fechados com HTTP 503; não há fallback em memória. Os endereços encaminhados não são confiados para criar chaves de limite.

## Achados remediados

| # | Severidade original | Estado atual | Evidência |
|---|---|---|---|
| 1 | 🔴 CRITICAL | ✅ Corrigido | `CVE-2026-47884` e `CVE-2026-47890` estavam em `spring-webmvc` 6.2.x. Atualizados Boot para 4.1.2 e Framework para 7.0.10; Trivy 0.76.1 não encontrou vulnerabilidades no backend final, incluindo o JAR Spring. |
| 2 | 🟡 MEDIUM | ✅ Corrigido | Removida a autenticação JWT do armazenamento local e do corpo de login. Smoke test HTTP confirmou login, cookie `HttpOnly`, resposta sem `accessToken`, acesso à sessão e revogação efetiva após logout. CSRF ausente foi rejeitado com 403. |
| 3 | 🟡 MEDIUM | ✅ Corrigido | Rate limit usa script Lua com `INCR`/expiração atômicos no Redis compartilhado. Teste cobre duas instâncias usando o armazenamento compartilhado e indisponibilidade Redis (503 sem liberar a requisição). Compose inicia Redis privado e aguarda healthcheck. |

## Controles e configuração

### Sessão e CSRF

- O cookie `GF-AUTH` é `HttpOnly`, limitado ao caminho `/api`, com expiração alinhada ao JWT e `SameSite` configurável. Em produção o Compose configura `Secure=true`; `SameSite=None` exige `Secure`.
- No perfil dev, `Secure=false` e `SameSite=Lax` permitem HTTP local. Isso não deve ser usado em produção.
- O token CSRF fica somente em memória no cliente. `GET /api/v1/auth/csrf` o obtém de forma segura; operações POST/PUT/PATCH/DELETE enviam `X-XSRF-TOKEN`.
- Login, setup inicial e logout são protegidos por CSRF. CORS usa origens explícitas e `allowCredentials=true`; curingas são recusados.
- O endpoint de sessão consulta o usuário associado ao cookie. Logout mantém a revogação persistida no servidor e remove o cookie.
- O frontend não grava credenciais; na inicialização, remove as antigas chaves de JWT/perfil do `localStorage`. Não usa headers bearer.

### Rate limiting

- Login e criação do primeiro usuário usam Redis com chave por endereço do peer e janela expirada de 60 segundos.
- Nenhum header `Forwarded`/`X-Forwarded-For` é usado. `server.forward-headers-strategy=none` impede que esses headers controlem o endereço; atrás de um proxy único, os clientes podem compartilhar uma chave. Não habilite confiança nesses headers sem política explícita de proxy confiável.
- Redis indisponível produz 503 e bloqueia a tentativa; Redis é obrigatório para autenticação. Compose de produção requer senha e não publica a porta Redis.
- Compose inclui uma instância Redis, sem alta disponibilidade configurada. A disponibilidade e o failover desse serviço permanecem responsabilidades operacionais.

### Outros controles já presentes

- O primeiro cadastro continua exigindo segredo temporário de bootstrap e é limitado a uma conta.
- BCrypt usa custo 12; JWT usa assinatura, emissor, expiração e `jti`; revogações continuam no PostgreSQL.
- Endpoints de negócio mantêm autorização por proprietário. Actuator expõe apenas health agregado.
- Frontend e backend usam imagens sem root na camada runtime; somente Caddy publica portas no Compose de produção.

## Testes, builds e scans

Executados em **29/10/2026**:

| Validação | Resultado |
|---|---|
| Testes direcionados (`AuthRateLimitFilterTest`, `JwtAuthenticationFilterTest`, `AuthCookieFactoryTest`, `SecurityAuthorizationTest`) | 11 passaram; 0 falhas/erros |
| `mvn clean test`, Java 25.0.3, Boot 4.1.2 / Framework 7.0.10 | 52 testes em 13 classes; 0 falhas/erros |
| `mvn clean test-compile`, Java 25.0.3 | Sucesso |
| Frontend Vitest | 8 testes em 2 arquivos; 0 falhas |
| Frontend `npm run build` | Sucesso; Vite removeu avisos de anotação Rollup em comentários de Zod |
| `docker compose config --quiet` (dev e prod) | Sucesso |
| Smoke test com PostgreSQL/Redis/API reais em Compose isolado | Serviços healthy; CSRF ausente → 403; login → 200; cookie HttpOnly; sem token no corpo; sessão → 200; logout → 204; sessão após logout → 401 |
| Trivy 0.76.1, dependências frontend (`package-lock.json`), todas as severidades | 0 vulnerabilidades |
| Trivy 0.76.1, imagem backend `gfauditsecurity-app`, todas as severidades | 0 vulnerabilidades |
| Trivy 0.76.1, imagem frontend `gestao-facil-frontend-security-validation`, todas as severidades | 0 vulnerabilidades |

Uma tentativa adicional de `trivy fs` no workspace não gerou relatório: o analisador Maven recebeu HTTP 429 do Maven Central ao buscar `spring-data-bom:2026.0.2`. Os scans das imagens finais completaram com sucesso; não se afirma que o scan do filesystem tenha passado. `npm audit` e OWASP Dependency-Check não foram executados nesta remediação.

## Riscos residuais e operação

1. Redis é uma dependência obrigatória: sua indisponibilidade interrompe login e bootstrap com 503. Planeje monitoramento, backup/configuração e alta disponibilidade do serviço conforme o ambiente.
2. Atrás de proxy, o endereço de peer pode ser comum a vários usuários. Configure um limite compatível com esse tráfego; somente habilite IP encaminhado após impor uma política de proxy confiável que sobrescreva os headers.
3. Configure `JWT_COOKIE_SECURE=true`, `JWT_COOKIE_SAME_SITE` apropriado e origens CORS exatas em todo ambiente publicado. O smoke test foi local e verificou `Secure=false` apenas no perfil de desenvolvimento; a configuração de produção foi validada por Compose e pelas propriedades/código.
4. O teste de limite compartilhado usa armazenamento Redis simulado para controlar as duas instâncias; não foi executado teste de concorrência multi-instância contra Redis em produção.
5. A análise completa de dependências em filesystem permanece pendente de uma execução sem bloqueio HTTP 429 do Maven Central; os scans Trivy das duas imagens finais não reportaram vulnerabilidades.

Nenhuma operação Git foi executada e nenhuma alteração foi commitada.
