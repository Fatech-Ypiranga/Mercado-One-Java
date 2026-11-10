# Segurança

Este documento registra o estado atual de segurança e as diretrizes para evolução do Mercado One.

## Estado atual

O backend possui `SecurityConfig` com:

- JWT Bearer (`Authorization: Bearer`) ou cookie HttpOnly `mercado_one_admin_session`.
- Cada request autenticado reconsulta o usuário no banco. Usuário inativo é recusado, e token emitido antes da última alteração do usuário (senha, perfil, status) deixa de valer (401).
- `POST /api/auth/login` público. Sempre devolve `accessToken` no JSON e seta o cookie de sessão do admin.
- `POST /api/auth/logout` autenticado; expira o cookie.
- CSRF do Spring desabilitado. Sessão HTTP `STATELESS`. No lugar, `JwtAuthenticationFilter` recusa com 403 a escrita (métodos diferentes de `GET`, `HEAD`, `OPTIONS` e `TRACE`) autenticada por cookie quando a origem não é confiável. A origem vem de `Origin` ou, na falta dele, de `Referer`, e é confiável se estiver em `MERCADO_ONE_CORS_ALLOWED_ORIGINS` ou for o próprio host da API. Sem `Origin` e sem `Referer`, a escrita só passa com `MERCADO_ONE_COOKIE_SECURE=false`. Requisições com `Authorization: Bearer` (PDV) não passam por essa regra.
- CORS em `/api/**` com origens de `MERCADO_ONE_CORS_ALLOWED_ORIGINS` (default `http://localhost:4200,http://127.0.0.1:4200`), métodos `GET`, `POST`, `PUT`, `OPTIONS`, headers `Authorization` e `Content-Type`, `allowCredentials=true`.
- `GET /actuator/health` público. `GET /actuator/info` exige autenticação e não está no CORS de `/api/**`.
- `GET /api/system/info` público.
- Rotas de usuários restritas a `ADMIN`. Redefinição administrativa de senha por `POST /api/access/users/{id}/password` ou pelo `password` opcional em `PUT /api/access/users/{id}`.
- Qualquer usuário autenticado troca a própria senha em `POST /api/auth/password`, informando a senha atual. Senhas exigem de 6 a 120 caracteres.
- Consulta de auditoria (`/api/audit/**`) restrita a `ADMIN`.
- O administrador não consegue desativar a própria conta nem trocar o próprio perfil.
- Leitura de catálogo liberada para `ADMIN`, `GERENTE`, `ESTOQUISTA` e `OPERADOR_CAIXA`; escrita restrita a `ADMIN` e `GERENTE`.
- Rotas de estoque restritas a `ADMIN`, `GERENTE` e `ESTOQUISTA`.
- `POST /api/sales`, `POST /api/offline/sales/sync` e `GET /api/offline/sales/status` para `ADMIN`, `GERENTE` e `OPERADOR_CAIXA`. Cancelamento de venda e relatórios gerais restritos a `ADMIN` e `GERENTE`; `GET /api/sales/mine` devolve só as vendas do próprio usuário.
- Desconto limitado por perfil: `ADMIN` e `GERENTE` sem limite além do total bruto; `OPERADOR_CAIXA` até `mercado-one.sales.operator-max-discount-percent` (padrão 0); `ESTOQUISTA` sem desconto.
- Ficha completa de clientes restrita a `ADMIN` e `GERENTE`; `OPERADOR_CAIXA` acessa apenas `/api/customers/search` e o cadastro rápido `POST /api/customers/quick`.
- Endpoints de conflitos offline restritos a `ADMIN` e `GERENTE`.
- Demais rotas exigem autenticação.
- Senhas com BCrypt. Seed de admin só com `MERCADO_ONE_SEED_ADMIN_ENABLED=true`. Com `MERCADO_ONE_COOKIE_SECURE=true` (ambiente remoto), a API não sobe se a senha do seed for `admin123`, `admin` ou `123456`.
- Cookie de sessão `SameSite=Lax` sem `Secure` quando `MERCADO_ONE_COOKIE_SECURE=false` (HTTP local). Com `true`, o cookie sai `Secure` e `SameSite=None` para o admin em outra origem HTTPS; o logout exige uma origem autorizada.
- Auditoria em `audit_events` para venda (confirmação e cancelamento), estoque (entrada, nota de entrada, ajuste, baixa e devolução), produto (criação e alteração), usuários (criação, alteração, troca e redefinição de senha) e venda offline (conflito, aceite e rejeição). Cadastro de cliente não gera evento. Consulta em `GET /api/audit/events`, só para `ADMIN`.

O admin web guarda metadados de sessão em `sessionStorage` (`mercado-one-admin-session-state`) e envia o cookie via `withCredentials`. O PDV guarda o JWT em memória (`bearerToken`) até o processo encerrar.

`OPERADOR_CAIXA` usa o PDV. No admin web acessa apenas "Meu caixa" e "Minha conta".

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

- Auditoria de cadastro de clientes, categorias e fornecedores, e política de retenção dos eventos.
- Limite de tentativas de login e política de senha mais forte (hoje o mínimo é 6 caracteres).
- Definir política de sessão do PDV para operação piloto (hoje o token vive só no processo).
