# Proposal: add-transaction-timestamp-and-categories

## Why

Hoje cada transação salva apenas identificador, descrição, valor e categoria, sem registrar quando foi criada (D-07), o que impede auditoria básica e bloqueia a futura consulta de gastos por período (change 4). Além disso, o enum `Category` oferece apenas três categorias (`GROCERIES`, `PHARMA` e `AUTO`), insuficiente para classificar gastos cotidianos como restaurante, compras, transporte, saúde, lazer ou outros (RF-05). Este é o change 3 da seção 5 da proposta inicial.

## What Changes

- **Data/hora de criação automática (D-07, RF-04):** `Transaction`, `TransactionEntity`, `TransactionOutput` e `TransactionResponse` passam a ter o campo `createdAt` (`java.time.Instant`). Na persistência JPA (`TransactionEntity`), `createdAt` é preenchido automaticamente pelo Hibernate com `@CreationTimestamp` em coluna não nula e não atualizável, e devolvido tanto nas respostas REST (`POST /transactions` e `GET /transactions/{category}`) quanto no retorno das ferramentas da IA (`TransactionOutput`).
- **Expansão das categorias (RF-05):** o enum `Category` passa a oferecer `GROCERIES`, `PHARMA`, `AUTO`, `RESTAURANT`, `SHOPPING`, `TRANSPORT`, `HEALTH`, `LEISURE` e `OTHER`, mantendo as três originais e disponibilizando automaticamente todas as nove opções na API REST e no JSON Schema das ferramentas do Spring AI.

Defeito tratado (detalhes em `docs/proposta-inicial.md`, seção 2):

- D-07: transação sem data de criação

Requisitos da proposta inicial atendidos: RF-04 e RF-05, além de manter RNF-01 e ampliar RNF-02 com a atualização dos `Support` e dos testes unitários e de integração.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `transactions`: o requisito de defeito "Transação sem data de criação (D-07)" é removido e substituído por "Data de criação automática da transação"; o requisito "Categorias disponíveis" é atualizado para contemplar as nove categorias (`GROCERIES`, `PHARMA`, `AUTO`, `RESTAURANT`, `SHOPPING`, `TRANSPORT`, `HEALTH`, `LEISURE`, `OTHER`); e os requisitos "Registro de transação via REST" e "Consulta de transações por categoria" passam a incluir `createdAt` nos dados retornados.

## Impact

- **Código de produção:** `Category` (novos valores do enum), `Transaction` (campo `createdAt` e sobrecarga de construtor), `TransactionEntity` (`@CreationTimestamp` e `@Column(nullable = false, updatable = false)` em `createdAt`, mapeamento `from`/`toDomain`), `TransactionOutput` e `TransactionResponse` (inclusão do campo `createdAt`).
- **API e Tools da IA:** respostas de `POST /transactions`, `GET /transactions/{category}` e das ferramentas `persist-transaction` e `list-transactions-by-category` passam a incluir `createdAt` em formato ISO-8601 UTC; o schema das ferramentas passa a listar as nove categorias no `enum`.
- **Banco de dados:** a tabela `transaction_entity` ganha a coluna `created_at` não nula preenchida na inserção.
- **Testes:** atualização de `TestConstants`, `TransactionSupport` e `TransactionEntitySupport` (fixando `CREATED_AT`), atualização do schema esperado em `TransactionToolsContractTest` e novos cenários/asserções em `TransactionTest`, `TransactionOutputTest`, `PersistTransactionUseCaseTest`, `ListTransactionsByCategoryUseCaseTest`, `TransactionEntityTest`, `JpaTransactionRepositoryTest`, `JpaTransactionRepositoryIntegrationTest` e `TransactionApiIntegrationTest`.

## Fora do escopo

- Consultas de total por categoria (`GET /transactions/summary`) e de transações por período (`GET /transactions?from=...&to=...`): change 4 (`add-financial-query-tools`).
- Extrair a orquestração de IA do controller e criar o endpoint `POST /transactions/ai/text`: change 5 (`extract-voice-command-use-case`).
- Atualizar o prompt de sistema da IA para informar a data atual ou regras de resposta: change 6 (`improve-assistant-prompt`).
- Receber `createdAt` no corpo de `POST /transactions` ou nos parâmetros da tool `persist-transaction`: a data de criação é sempre gerada automaticamente pelo sistema no momento da persistência.
