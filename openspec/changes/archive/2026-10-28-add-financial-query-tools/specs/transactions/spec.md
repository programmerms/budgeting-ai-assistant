# Spec Delta: transactions

## ADDED Requirements

### Requirement: Resumo de transações por categoria
O sistema SHALL devolver, em `GET /transactions/summary?category={category}`, o resumo financeiro da categoria informada com status 200, contendo `category`, `total` (soma em reais com duas casas decimais) e `count` (quantidade de transações). A mesma regra de cálculo SHALL valer para a ferramenta de resumo chamada pela IA.

#### Scenario: Categoria com múltiplas transações
- **WHEN** existem transações de `8000` centavos (R$ 80,00) e `4000` centavos (R$ 40,00) em `GROCERIES` e uma de `5000` centavos em `AUTO`, e é feita a consulta `GET /transactions/summary?category=GROCERIES`
- **THEN** o sistema responde HTTP 200 com `category` igual a `GROCERIES`, `total` igual a `120.00` e `count` igual a `2`

#### Scenario: Categoria sem transações no resumo
- **WHEN** não existe nenhuma transação em `PHARMA` e é feita a consulta `GET /transactions/summary?category=PHARMA`
- **THEN** o sistema responde HTTP 200 com `category` igual a `PHARMA`, `total` igual a `0.00` e `count` igual a `0`

#### Scenario: Resumo calculado via Tool Calling
- **WHEN** a IA chama a ferramenta de resumo para a categoria `GROCERIES` contendo transações de R$ 80,00 e R$ 40,00
- **THEN** a ferramenta devolve `category` igual a `GROCERIES`, `total` igual a `120.00` e `count` igual a `2`

#### Scenario: Parâmetro de categoria ausente ou inválido no resumo
- **WHEN** é feita a consulta `GET /transactions/summary` sem `category` ou com `category=INVALID`
- **THEN** o sistema responde HTTP 400

### Requirement: Consulta de transações por período
O sistema SHALL listar, em `GET /transactions?from={from}&to={to}`, todas as transações cuja data de criação (`createdAt` em UTC) esteja dentro do intervalo inclusivo entre o início do dia `from` (`00:00:00Z`) e o fim do dia `to` (`23:59:59.999999999Z`), no formato ISO `YYYY-MM-DD`, com status 200. A mesma regra de filtro e validação de intervalo SHALL valer para a ferramenta de consulta por período chamada pela IA.

#### Scenario: Transações dentro e fora do período
- **WHEN** existem transações criadas em `2026-10-05`, `2026-10-15` e `2026-11-01`, e é feita a consulta `GET /transactions?from=2026-10-01&to=2026-10-31`
- **THEN** o sistema responde HTTP 200 contendo apenas as duas transações de outubro, cada uma com `id`, `category`, `description`, `amount` e `createdAt`

#### Scenario: Período sem transações
- **WHEN** não existem transações no intervalo informado e é feita a consulta `GET /transactions?from=2026-01-01&to=2026-01-31`
- **THEN** o sistema responde HTTP 200 com uma lista vazia

#### Scenario: Data inicial posterior à data final via REST
- **WHEN** é feita a consulta `GET /transactions?from=2026-10-31&to=2026-10-01`
- **THEN** o sistema responde HTTP 400 informando que a data inicial não pode ser posterior à data final

#### Scenario: Data inicial posterior à data final via Tool Calling
- **WHEN** a IA chama a ferramenta de consulta por período com `from` posterior a `to`
- **THEN** a chamada falha na validação de intervalo e devolve o erro à IA

#### Scenario: Parâmetros de período ausentes ou em formato inválido
- **WHEN** é feita a consulta `GET /transactions` sem `from` ou `to`, ou com data em formato inválido
- **THEN** o sistema responde HTTP 400
