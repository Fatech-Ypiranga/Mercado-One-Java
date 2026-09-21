# Seguranca

Este documento registra o estado atual de seguranca e as diretrizes para evolucao do Mercado One.

## Estado atual

O backend possui `SecurityConfig` com:

- JWT Bearer (`Authorization: Bearer`) ou cookie HttpOnly `mercado_one_admin_session`.
- Cada request autenticado reconsulta o usuario atual no banco para aplicar inativacao e mudanca de perfil sem esperar o token expirar.
- `POST /api/auth/login` publico. Sempre devolve `accessToken` no JSON e seta o cookie de sessao do admin.
- `POST /api/auth/logout` autenticado; expira o cookie.
- CSRF desabilitado. Sessao HTTP `STATELESS`.
- CORS em `/api/**` para `http://localhost:4200` e `http://127.0.0.1:4200`, metodos `GET`, `POST`, `PUT`, `OPTIONS`, headers `Authorization` e `Content-Type`, `allowCredentials=true`.
- `GET /actuator/health` publico. `GET /actuator/info` exige autenticacao e nao esta no CORS de `/api/**`.
- `GET /api/system/info` publico.
- Rotas de usuarios restritas a `ADMIN`. `PUT /api/access/users/{id}` aceita `password` opcional (redefinicao administrativa).
- Leitura de catalogo liberada para `ADMIN`, `GERENTE`, `ESTOQUISTA` e `OPERADOR_CAIXA`; escrita restrita a `ADMIN` e `GERENTE`.
- Rotas de estoque restritas a `ADMIN`, `GERENTE` e `ESTOQUISTA`.
- `POST /api/sales/**` e `POST /api/offline/sales/sync` para `ADMIN`, `GERENTE` e `OPERADOR_CAIXA`; consulta de vendas restrita a `ADMIN` e `GERENTE`.
- Ficha completa de clientes restrita a `ADMIN` e `GERENTE`; `OPERADOR_CAIXA` acessa apenas `/api/customers/search`.
- Endpoints de conflitos offline restritos a `ADMIN` e `GERENTE`.
- Demais rotas exigem autenticacao.
- Senhas com BCrypt. Seed de admin so com `MERCADO_ONE_SEED_ADMIN_ENABLED=true`.
- Auditoria inicial em `audit_events` para venda, estoque, usuarios e conflitos offline. Sem API de leitura.

O admin web guarda metadados de sessao em `sessionStorage` (`mercado-one-admin-session-state`) e envia o cookie via `withCredentials`. O PDV guarda o JWT em memoria (`bearerToken`) ate o processo encerrar.

`OPERADOR_CAIXA` autentica no admin web, mas nao ha rota de shell para esse perfil.

## Requisitos do MVP

- Autenticacao obrigatoria.
- Senhas armazenadas de forma protegida.
- Autorizacao por perfil.
- Bloqueio de usuarios inativos.
- Validacao de entradas no frontend e backend.
- Auditoria basica de operacoes criticas.

## Perfis iniciais

- `ADMIN`
- `GERENTE`
- `OPERADOR_CAIXA`
- `ESTOQUISTA`

Os nomes tecnicos atuais ficam em `UserRole`. A documentacao de dominio usa os nomes em portugues para linguagem do produto.

## Diretrizes

- Nao hardcodar credenciais reais.
- Nao reutilizar senha default de desenvolvimento fora do ambiente local.
- Nao expor stack trace ou detalhes internos no envelope de erro.
- Validar toda entrada em bordas HTTP e em regras de negocio.
- Registrar usuario responsavel em operacoes criticas.
- Nunca retornar senha ou hash de senha em DTOs de usuario.

## Pontos pendentes

- Cookie sem flag `Secure` (adequado a HTTP local; insuficiente para HTTPS de piloto).
- Expandir o modelo de auditoria para consulta administrativa, retencao e eventos de catalogo.
- Definir politica de sessao do PDV para operacao piloto (hoje o token vive so no processo).
- CORS e origens fixas de desenvolvimento; nao ha configuracao de origem de producao.
