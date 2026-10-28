# Design: add-financial-query-tools

## Context

A motivação está em `proposal.md` (seção Why). O estado atual do projeto relevante para este change:

- `Transaction` e `TransactionEntity` já possuem `createdAt` (`java.time.Instant`) e as nove categorias no enum `Category` (change 3).
- `TransactionRepository` possui apenas `save(Transaction)` e `findAllByCategory(Category)`.
- `TransactionTools` (`infrastructure/ai`) expõe duas ferramentas (`persist-transaction` e `list-transactions-by-category`) e tem seu contrato JSON Schema verificado em `TransactionToolsContractTest`.
- `TransactionController` expõe `POST /transactions`, `GET /transactions/{category}` e `POST /transactions/ai`.

## Goals / Non-Goals

**Goals:**
- Implementar a consulta de resumo por categoria (`category`, `total` em reais com 2 casas decimais e `count`) como caso de uso, tool da IA e endpoint `GET /transactions/summary?category=...` (RF-06).
- Implementar a consulta de transações por período (`from` e `to` inclusivos) como método de repositório (*derived query*), caso de uso, tool da IA e endpoint `GET /transactions?from=...&to=...` (RF-07).
- Manter separação estrita de camadas: regras e validações na camada `application`/`domain`, anotações `@Tool`/`@ToolParam` apenas em `infrastructure/ai`, e adaptação HTTP no controller.

**Non-Goals:**
- Filtro combinado de categoria + período (decisão 3 da seção 7.1 da proposta inicial).
- Queries JPQL/SQL customizadas (`@Query`): a decisão 3 da proposta inicial determina usar apenas *derived queries* do Spring Data JPA e agregação em memória no caso de uso.

## Decisions

### 1. Resumo por categoria calculado no caso de uso a partir de `findAllByCategory` (RF-06)

Criamos `SummarizeTransactionsByCategoryUseCase` em `application`, recebendo `Category` (validado com `Assert.notNull(category, "Category must not be null")`) e devolvendo `CategorySummaryOutput( String category, BigDecimal total, long count)`:

```java
var transactions = transactionRepository.findAllByCategory(category);
long totalInCents = transactions.stream().mapToLong(Transaction::getAmount).sum();
return new CategorySummaryOutput(
        category.name(),
        BigDecimal.valueOf(totalInCents, 2),
        transactions.size());
```

- **Por que somar em centavos (`long`) e converter com `BigDecimal.valueOf(totalInCents, 2)`:** mantém a mesma regra exata do change 1 (`TransactionOutput.from`). Quando não há transações na categoria, `totalInCents` é `0L` e `BigDecimal.valueOf(0L, 2)` produz `0.00` com `count = 0`.
- **Por que reaproveitar `findAllByCategory` em vez de `@Query` com `SUM`/`COUNT`:** respeita a restrição da proposta inicial (seção 7.1, decisão 3: "Só *derived queries* do Spring Data") e mantém a lógica de negócio testável em unidade sem banco.
- **No HTTP (`infrastructure/http`):** `CategorySummaryResponse(String category, BigDecimal total, long count)` com `CategorySummaryResponse.from(CategorySummaryOutput)` e endpoint `@GetMapping("/summary")` em `TransactionController` recebendo `@RequestParam Category category`.
- **Na IA (`infrastructure/ai`):** método `@Tool(name = "summarize-transactions-by-category", description = "Resume o total gasto e a quantidade de transações de uma categoria")` em `TransactionTools`, recebendo `@ToolParam(description = "Categoria de uma transação") Category category` e devolvendo `CategorySummaryOutput`.
- **Fundamento na trilha:** um caso de uso por capacidade com `output` em record (README raiz e módulo `00-domain-driven-design`); `@Tool`/`@ToolParam` do módulo `05-spring-ai`.

### 2. Consulta por período com *derived query* `findAllByCreatedAtBetween` e `LocalDate` na entrada (RF-07)

1. **Repositório (`domain` e `infrastructure/persistence`):**
   - `TransactionRepository` declara `List<Transaction> findAllByCreatedAtBetween(Instant from, Instant to);`.
   - `TransactionEntityRepository` declara a *derived query* `List<TransactionEntity> findAllByCreatedAtBetween(Instant from, Instant to);`.
   - `JpaTransactionRepository` delega para `transactionEntityRepository.findAllByCreatedAtBetween(from, to)` e mapeia cada `TransactionEntity` com `toDomain()`.
2. **Entrada e validação no caso de uso (`application`):**
   - `TransactionsPeriodInput(LocalDate from, LocalDate to)`.
   - `ListTransactionsByPeriodUseCase.execute(TransactionsPeriodInput input)` valida com `org.springframework.util.Assert`:
     - `Assert.notNull(input, "Period input must not be null");`
     - `Assert.notNull(input.from(), "Start date must not be null");`
     - `Assert.notNull(input.to(), "End date must not be null");`
     - `Assert.isTrue(!input.from().isAfter(input.to()), "Start date must not be after end date");`
   - Converte o intervalo de datas para `Instant` cobrindo os dias inteiros em UTC:
     - `Instant fromInstant = input.from().atStartOfDay(ZoneOffset.UTC).toInstant();`
     - `Instant toInstant = input.to().atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();`
   - Chama `transactionRepository.findAllByCreatedAtBetween(fromInstant, toInstant)` e mapeia para `List<TransactionOutput>`.
3. **No HTTP (`infrastructure/http`):**
   - Em `TransactionController`: `@GetMapping` em `/transactions` recebendo `@RequestParam LocalDate from` e `@RequestParam LocalDate to` (o Spring Web faz o parse automático de `YYYY-MM-DD` para `LocalDate`), chamando `listTransactionsByPeriodUseCase.execute(new TransactionsPeriodInput(from, to))` e convertendo para `List<TransactionResponse>`.
   - Quando `from > to`, o `Assert.isTrue` do caso de uso lança `IllegalArgumentException`, que o `GlobalExceptionHandler` já converte em HTTP 400 com a mensagem `"Start date must not be after end date"`.
4. **Na IA (`infrastructure/ai`):**
   - Record `TransactionsPeriodToolRequest(@ToolParam(description = "Data inicial no formato YYYY-MM-DD") LocalDate from, @ToolParam(description = "Data final no formato YYYY-MM-DD") LocalDate to)` com método `toInput()`.
   - Em `TransactionTools`: `@Tool(name = "list-transactions-by-period", description = "Lista transações financeiras realizadas em um período de datas") public List<TransactionOutput> listTransactionsByPeriod(TransactionsPeriodToolRequest input)`.
- **Por que `LocalDate` na borda e `Instant` no repositório:** quem consome a API REST (`?from=2026-10-01&to=2026-10-31`) ou a tool da IA pensa em datas de calendário (`YYYY-MM-DD`), enquanto o banco armazena timestamps em UTC (`Instant`). Converter `from` para `00:00:00Z` e `to` para `23:59:59.999999999Z` no caso de uso garante que transações criadas em qualquer horário do dia final sejam incluídas.
- **Fundamento na trilha:** *derived queries* do módulo `02-spring-data` (`CustomerEntityRepository.findByFirstNameStartingWithIgnoreCase`), invariantes com `Assert` do módulo `01-spring-web` e `@Tool`/`@ToolParam` do módulo `05-spring-ai`.

### 3. Testes de integração de período com datas distintas no MySQL

Como `TransactionEntity.createdAt` usa `@CreationTimestamp` (que preenche a data atual no `INSERT`), para testar em `JpaTransactionRepositoryIntegrationTest` e `TransactionApiIntegrationTest` que `findAllByCreatedAtBetween` filtra corretamente transações de datas diferentes no MySQL real, usamos `JdbcTemplate` nos helpers `setupGiven...` dos testes de integração para ajustar a coluna `created_at` de registros específicos após o `save` (por exemplo, movendo uma transação para fora do período consultado).

- **Por que `JdbcTemplate` no setup do teste:** preserva `@CreationTimestamp` com `@Column(nullable = false, updatable = false)` intacto no código de produção (garantindo que a aplicação nunca sobrescreva `createdAt` via JPA), ao mesmo tempo em que permite preparar cenários determinísticos com datas passadas/futuras no MySQL real do Testcontainers.

### 4. Testes por camada

| Camada | Classe de teste | O que verifica |
|---|---|---|
| Caso de uso | `SummarizeTransactionsByCategoryUseCaseTest` | Soma em reais e contagem para categoria com transações, retorno `0.00` e `0` para categoria vazia, e `IllegalArgumentException` para categoria nula |
| Caso de uso | `ListTransactionsByPeriodUseCaseTest` | Conversão de `LocalDate` para início/fim do dia em UTC, delegação a `findAllByCreatedAtBetween` e `IllegalArgumentException` quando `from > to` ou datas nulas |
| Repositório (unitário) | `JpaTransactionRepositoryTest` | Delegação de `findAllByCreatedAtBetween` ao `TransactionEntityRepository` e mapeamento para `Transaction` |
| Repositório (integração) | `JpaTransactionRepositoryIntegrationTest` | No MySQL real, `findAllByCreatedAtBetween` retorna apenas as transações cujo `created_at` está dentro do intervalo |
| Adaptador IA | `TransactionToolsTest`, `TransactionToolsContractTest`, `TransactionsPeriodToolRequestTest` | Delegação das duas novas tools aos casos de uso e verificação estrita do JSON Schema das 4 tools |
| API (integração) | `TransactionApiIntegrationTest` | `GET /transactions/summary?category=...` (com transações, vazio e inválido) e `GET /transactions?from=...&to=...` (filtrando período e retornando 400 quando `from > to`) |

## Risks / Trade-offs

- [Conflito de rotas entre `GET /transactions/{category}`, `GET /transactions/summary` e `GET /transactions?from=...&to=...`] → No Spring MVC, rotas literais (`/transactions/summary`) têm precedência sobre variáveis de template (`/transactions/{category}`), e `/transactions` (sem segmento extra) não colide com nenhuma das duas. Os testes em `TransactionApiIntegrationTest` exercitam as três rotas juntas.
