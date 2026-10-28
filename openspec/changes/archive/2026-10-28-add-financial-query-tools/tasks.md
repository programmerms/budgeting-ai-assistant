# Tasks: add-financial-query-tools

## 1. Resumo de transações por categoria no caso de uso (RF-06)

- [x] 1.1 Criar `application/output/CategorySummaryOutput` e `application/SummarizeTransactionsByCategoryUseCase` (calculando `total` em reais com `BigDecimal.valueOf(totalInCents, 2)` e `count` a partir de `findAllByCategory`, e validando `category` não nula com `Assert.notNull`); escrever `application/SummarizeTransactionsByCategoryUseCaseTest` cobrindo categoria com transações, categoria vazia (`0.00` e `0`) e categoria nula; verificar que `SummarizeTransactionsByCategoryUseCaseTest` passa

## 2. Consulta por período no repositório e no caso de uso (RF-07)

- [x] 2.1 Adicionar `findAllByCreatedAtBetween(Instant from, Instant to)` em `TransactionRepository`, `TransactionEntityRepository` e `JpaTransactionRepository`; atualizar `JpaTransactionRepositoryTest` e `JpaTransactionRepositoryIntegrationTest` para cobrir a busca por período; verificar que ambos os testes passam
- [x] 2.2 Criar `application/input/TransactionsPeriodInput` e `application/ListTransactionsByPeriodUseCase` (validando `input`, `from`, `to` e `!from.isAfter(to)` com `Assert` e convertendo as datas para o início e fim do dia em UTC), criar `support/input/TransactionsPeriodInputSupport` (e constantes de datas em `TestConstants`) e escrever `application/ListTransactionsByPeriodUseCaseTest` cobrindo período válido e entradas inválidas; verificar que `ListTransactionsByPeriodUseCaseTest` passa

## 3. Novas ferramentas no adaptador de IA (`TransactionTools`) (RF-06, RF-07)

- [x] 3.1 Criar `infrastructure/ai/request/TransactionsPeriodToolRequest` (e `support/request/TransactionsPeriodToolRequestSupport` + `TransactionsPeriodToolRequestTest`), adicionar as tools `summarize-transactions-by-category` e `list-transactions-by-period` em `TransactionTools`, e atualizar `TransactionToolsTest` e `TransactionToolsContractTest`; verificar que os três testes passam

## 4. Novos endpoints REST em `TransactionController` (RF-06, RF-07)

- [x] 4.1 Criar `infrastructure/http/response/CategorySummaryResponse` e adicionar os endpoints `GET /transactions/summary?category=...` e `GET /transactions?from=...&to=...` em `TransactionController`; adicionar em `TransactionApiIntegrationTest` os cenários de resumo por categoria (com transações, vazio e inválido) e de consulta por período (dentro/fora do período, vazio e `from > to`); verificar que `TransactionApiIntegrationTest` passa

## 5. Verificação final

- [x] 5.1 Rodar `./gradlew test` sem `OPENAI_API_KEY`; verificar `BUILD SUCCESSFUL`, todos os testes verdes e os `*IT` ignorados
