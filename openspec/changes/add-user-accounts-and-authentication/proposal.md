# Proposal: add-user-accounts-and-authentication

## Why

A API não identifica quem está usando o sistema: qualquer cliente que alcance a porta 8080 registra e consulta gastos financeiros de todo mundo. Enquanto não existir identidade autenticada, não há de onde tirar um proprietário confiável para as transações, então este change é o pré-requisito do isolamento de dados.

Ele atende os diagnósticos S-01 (não existe autenticação), S-02 (não existe cadastro nem aprovação), S-06 (não existem papéis administrativos) e S-07 (não existe logout) de `docs/proposta-seguranca-e-multiusuario.md`.

## What Changes

- Criar o conceito de conta de usuário, com nome de usuário único, senha codificada com BCrypt, papel (`ROLE_ADMIN` ou `ROLE_USER`) e estado (`PENDING`, `ACTIVE` ou `REJECTED`).
- Criar o administrador inicial na subida da aplicação, a partir de variáveis de ambiente, quando ainda não houver conta administrativa.
- Expor cadastro público em `POST /api/auth/register`, que sempre cria a conta como `ROLE_USER` e `PENDING`, ignorando papel e estado que venham no corpo da requisição.
- Expor login em `POST /api/auth/login` e logout em `POST /api/auth/logout`, com autenticação por sessão HTTP e cookie de sessão.
- Permitir que apenas `ROLE_ADMIN` liste cadastros por estado (`GET /api/admin/users?status=PENDING`), aprove (`POST /api/admin/users/{id}/approve`) e rejeite (`POST /api/admin/users/{id}/reject`).
- **BREAKING** Exigir sessão autenticada com `ROLE_USER` em todos os endpoints financeiros existentes (`/transactions`, `/transactions/{category}`, `/transactions/summary`, `/transactions/ai` e `/transactions/ai/text`). Clientes atuais passam a receber 401 sem login.
- Responder 401 para requisição sem sessão válida e 403 para sessão sem o papel necessário, com corpo JSON previsível.
- Cobrir cadastro, aprovação, autenticação e autorização com testes seguindo `docs/padrao-de-testes.md`.

Requisitos atendidos: RF-S01 a RF-S09, RF-S14, RF-S15, RF-S16, RNF-S01, RNF-S05, RNF-S06, RNF-S07 e, parcialmente, RNF-S02 (papel e estado não controláveis pelo cliente) e RNF-S08 (documentação de cadastro, login, aprovação, cookie e logout).

## Capabilities

### New Capabilities

- `user-access`: contas de usuário, ciclo de aprovação do cadastro, autenticação por sessão e autorização por papel, incluindo a regra de que os endpoints financeiros exigem sessão autenticada.

### Modified Capabilities

Nenhuma.

A exigência de autenticação é uma regra transversal e fica num único requisito de `user-access`, em vez de alterar os vinte e quatro requisitos de `transactions` e `voice-assistant` para repetir a mesma frase. A partir deste change, os cenários daquelas duas specs descrevem requisições já autenticadas. A decisão e o seu limite estão registrados no `design.md`.

## Impact

- **Nova dependência:** `spring-boot-starter-security` (produção) e `spring-security-test` (teste), ambas do módulo `03-spring-security` da trilha.
- **Código novo:** contexto `auth` com entidade de usuário, repositório, `JpaUserDetailsService`, `RestUsernamePasswordAuthenticationFilter`, `SecurityConfig`, casos de uso de cadastro e aprovação, e os controllers de autenticação e administração.
- **Código alterado:** `GlobalExceptionHandler` ganha o tratamento dos erros de cadastro e de transição de estado; `TransactionController` não muda, porque a proteção é declarada no `SecurityFilterChain`.
- **Testes existentes:** `TransactionApiIntegrationTest` e `BudgetingApplicationTests` passam a precisar de contexto de segurança; os testes de API recebem um usuário autenticado.
- **Banco de dados:** nova tabela de usuários. Nenhuma tabela existente é alterada.
- **Documentação:** README e `index.adoc` passam a ensinar cadastro, aprovação, login, uso do cookie de sessão e logout, e a registrar que o CSRF está desabilitado.

## Fora do escopo

Este change entrega autenticação sem isolamento de dados: um usuário `ACTIVE` autenticado continua vendo as transações de todos. Isso é um estado intermediário aceitável apenas porque o change seguinte o fecha. Ficam para `add-transaction-ownership`:

- `OwnerId` obrigatório no domínio financeiro e na persistência (S-03).
- Consultas de transação filtradas pelo proprietário autenticado (S-04, RF-S10, RF-S11).
- Identidade autenticada aplicada às ferramentas chamadas pela IA (S-05, RF-S12, RF-S13).
- Estratégia para as transações já existentes sem proprietário (S-08).

Ficam fora de todo o ciclo, conforme a seção 3.2 da proposta de segurança: JWT e autenticação stateless, OAuth2 e login social, confirmação por e-mail, recuperação e troca de senha, autenticação multifator, token de convite, reativação ou suspensão de contas, exclusão de usuários, interface gráfica de administração e compartilhamento de orçamento entre usuários.
