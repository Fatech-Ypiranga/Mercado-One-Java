# Seguranca

Este documento registra o estado atual de seguranca e as diretrizes para evolucao do Mercado One.

## Estado atual

O backend possui `SecurityConfig` com:

- JWT Bearer para rotas protegidas.
- Cada request autenticado reconsulta o usuario atual no banco para aplicar inativacao e mudanca de perfil sem esperar o token expirar.
- `POST /api/auth/login` publico.
- CSRF desabilitado.
- CORS liberado para o admin web local em `localhost:4200`.
- `GET /actuator/health` publico.
- `GET /api/system/info` publico.
- Rotas de usuarios restritas a `ADMIN`.
- Leitura de catalogo liberada para `ADMIN`, `GERENTE`, `ESTOQUISTA` e `OPERADOR_CAIXA`; escrita restrita a `ADMIN` e `GERENTE`.
- Rotas de estoque restritas a `ADMIN`, `GERENTE` e `ESTOQUISTA`.
- Finalizacao de venda liberada para `ADMIN`, `GERENTE` e `OPERADOR_CAIXA`; consulta de vendas restrita a `ADMIN` e `GERENTE`.
- Ficha completa de clientes restrita a `ADMIN` e `GERENTE`; `OPERADOR_CAIXA` acessa apenas a busca operacional `/api/customers/search`, com clientes ativos e campos resumidos.
- Todas as demais rotas exigindo autenticacao.
- Endpoints iniciais de gestao de usuarios em `/api/access/users` e perfis em `/api/access/roles`.
- Endpoints de conflitos offline restritos a `ADMIN` e `GERENTE`.
- Auditoria inicial em `audit_events` para venda, estoque, usuarios e conflitos offline.

O administrador inicial pode ser criado por seed controlado por variaveis de ambiente. A senha default e apenas para desenvolvimento local.

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
- Tratar autenticacao e autorizacao como fatia propria antes de liberar CRUDs sensiveis.
- Nunca retornar senha ou hash de senha em DTOs de usuario.

## Pontos pendentes

- Definir fluxo de redefinicao administrativa de senha.
- Expandir o modelo de auditoria para consulta administrativa e retencao operacional.
- Definir politica de sessao do PDV para operacao piloto.
