# Segurança

Este documento registra o estado atual de segurança e as diretrizes para evolução do Mercado One.

## Estado atual

O backend possui `SecurityConfig` com:

- JWT Bearer (`Authorization: Bearer`) ou cookie HttpOnly `mercado_one_admin_session`.
- Cada request autenticado reconsulta o usuário atual no banco para aplicar inativação e mudança de perfil sem esperar o token expirar.
- `POST /api/auth/login` público. Sempre devolve `accessToken` no JSON e seta o cookie de sessão do admin.
- `POST /api/auth/logout` autenticado; expira o cookie.
- CSRF desabilitado. Sessão HTTP `STATELESS`.
- CORS em `/api/**` com origens de `MERCADO_ONE_CORS_ALLOWED_ORIGINS` (default `http://localhost:4200,http://127.0.0.1:4200`), métodos `GET`, `POST`, `PUT`, `OPTIONS`, headers `Authorization` e `Content-Type`, `allowCredentials=true`.
- `GET /actuator/health` público. `GET /actuator/info` exige autenticação e não está no CORS de `/api/**`.
- `GET /api/system/info` público.
- Rotas de usuários restritas a `ADMIN`. `PUT /api/access/users/{id}` aceita `password` opcional (redefinição administrativa).
- Leitura de catálogo liberada para `ADMIN`, `GERENTE`, `ESTOQUISTA` e `OPERADOR_CAIXA`; escrita restrita a `ADMIN` e `GERENTE`.
- Rotas de estoque restritas a `ADMIN`, `GERENTE` e `ESTOQUISTA`.
- `POST /api/sales/**` e `POST /api/offline/sales/sync` para `ADMIN`, `GERENTE` e `OPERADOR_CAIXA`; consulta de vendas restrita a `ADMIN` e `GERENTE`.
- Ficha completa de clientes restrita a `ADMIN` e `GERENTE`; `OPERADOR_CAIXA` acessa apenas `/api/customers/search`.
- Endpoints de conflitos offline restritos a `ADMIN` e `GERENTE`.
- Demais rotas exigem autenticação.
- Senhas com BCrypt. Seed de admin só com `MERCADO_ONE_SEED_ADMIN_ENABLED=true`.
- Cookie de sessão `SameSite=Lax` sem `Secure` quando `MERCADO_ONE_COOKIE_SECURE=false` (HTTP local). Com `true`, o cookie sai `Secure` e `SameSite=None` para o admin em outra origem HTTPS; o logout exige uma origem autorizada.
- Auditoria inicial em `audit_events` para venda, estoque, usuários e conflitos offline. Sem API de leitura.

O admin web guarda metadados de sessão em `sessionStorage` (`mercado-one-admin-session-state`) e envia o cookie via `withCredentials`. O PDV guarda o JWT em memória (`bearerToken`) até o processo encerrar.

`OPERADOR_CAIXA` autentica no admin web, mas não há rota de shell para esse perfil.

## Requisitos do MVP

- Autenticação obrigatória.
- Senhas armazenadas de forma protegida.
- Autorização por perfil.
- Bloqueio de usuários inativos.
- Validação de entradas no frontend e backend.
- Auditoria básica de operações críticas.

## Perfis iniciais

- `ADMIN`
- `GERENTE`
- `OPERADOR_CAIXA`
- `ESTOQUISTA`

Os nomes técnicos atuais ficam em `UserRole`. A documentação de domínio usa os nomes em português para linguagem do produto.

## Diretrizes

- Não hardcodar credenciais reais.
- Não reutilizar senha default de desenvolvimento fora do ambiente local.
- Não expor stack trace ou detalhes internos no envelope de erro.
- Validar toda entrada em bordas HTTP e em regras de negócio.
- Registrar o usuário responsável em operações críticas.
- Nunca retornar senha ou hash de senha em DTOs de usuário.

## Pontos pendentes

- Expandir o modelo de auditoria para consulta administrativa, retenção e eventos de catálogo.
- Definir política de sessão do PDV para operação piloto (hoje o token vive só no processo).
