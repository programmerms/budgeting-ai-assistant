# Proposal: add-financial-query-tools

## Why

Hoje a aplicação oferece apenas uma consulta (`GET /transactions/{category}` e a tool `list-transactions-by-category`), que devolve a lista bruta de transações de uma categoria. Quando a pessoa pergunta "quanto gastei com mercado?", o modelo precisa somar os itens manualmente; e quando pergunta sobre gastos em um intervalo de datas, não há nenhuma consulta por período disponível. Este é o change 4 da seção 5 da proposta inicial.

## What Changes

- **Resumo de gastos por categoria (RF-06):** novo caso de uso `SummarizeTransactionsByCategoryUseCase` que retorna a categoria, o total gasto em reais com duas casas decimais (`BigDecimal`) e a quantidade de transações (`long`), exposto na API REST em `GET /transactions/summary?category={category}` (respondendo `{"category", "total", "count"}`) e no adaptador de IA (`TransactionTools`) como a ferramenta `summarize-transactions-by-category`.
- **Consulta de transações por período (RF-07):** novo método `findAllByCreatedAtBetween(Instant from, Instant to)` no contrato `TransactionRepository` (implementado com *derived query* do Spring Data JPA em `TransactionEntityRepository`), novo caso de uso `ListTransactionsByPeriodUseCase` validando o intervalo (`from` e `to` obrigatórios e `from <= to`), endpoint `GET /transactions?from=YYYY-MM-DD&to=YYYY-MM-DD` e ferramenta `list-transactions-by-period` em `TransactionTools`.
- **Tratamento de parâmetros obrigatórios/inválidos na query string (RF-03):** `GlobalExceptionHandler` passa a tratar parâmetros de query ausentes (`MissingServletRequestParameterException`) ou com formato inválido (`MethodArgumentTypeMismatchException`, como categoria ou data inválida) de forma consistente com HTTP 400.

Requisitos da proposta inicial atendidos: RF-06 e RF-07 (além de manter RF-01, RF-03, RF-11, RNF-01 e ampliar RNF-02 com os testes de todas as camadas).

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `transactions`: adição dos requisitos "Resumo de transações por categoria" (`GET /transactions/summary?category=...`) e "Consulta de transações por período" (`GET /transactions?from=...&to=...`), cobrindo cálculo de total em reais, contagem, filtro inclusivo por datas e erros HTTP 400 para parâmetros ausentes ou intervalo inválido (`from` posterior a `to`).
- `voice-assistant`: adição dos requisitos "Ferramenta de resumo por categoria" e "Ferramenta de consulta por período", permitindo que a IA consulte o total/quantidade de uma categoria e liste transações criadas dentro de um intervalo de datas.

## Impact

- **Código de produção:**
  - `domain`: `TransactionRepository` ganha `findAllByCreatedAtBetween(Instant from, Instant to)`.
  - `application`: novos casos de uso `SummarizeTransactionsByCategoryUseCase` (com `CategorySummaryOutput`) e `ListTransactionsByPeriodUseCase` (com `TransactionsPeriodInput`).
  - `infrastructure/persistence`: `TransactionEntityRepository` e `JpaTransactionRepository` implementam `findAllByCreatedAtBetween`.
  - `infrastructure/ai`: `TransactionTools` ganha as tools `summarize-transactions-by-category` e `list-transactions-by-period` (com `TransactionsPeriodToolRequest`).
  - `infrastructure/http`: `TransactionController` ganha `GET /transactions/summary` (com `CategorySummaryResponse`) e `GET /transactions` por período.
- **Testes:** novos `Support` e testes unitários e de integração cobrindo casos de uso, repositório, `TransactionTools` (incluindo contrato JSON Schema) e endpoints REST.

## Fora do escopo

- Cruzamento de filtro por categoria e período na mesma consulta (decisão 3 da seção 7.1 da proposta inicial: consultas separadas usando apenas *derived queries* do Spring Data).
- Extrair a orquestração de IA do `TransactionController` para um caso de uso próprio e criar o endpoint `POST /transactions/ai/text`: change 5 (`extract-voice-command-use-case`).
- Atualizar o prompt de sistema (`system-message.st`) com a data atual e instruções detalhadas de resposta: change 6 (`improve-assistant-prompt`).
