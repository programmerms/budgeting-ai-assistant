# Design: add-transaction-timestamp-and-categories

## Context

A motivação está em `proposal.md` (seção Why). O estado atual do código relevante para este change:

- `Category` (`domain/Category.java`) possui apenas três constantes: `GROCERIES`, `PHARMA` e `AUTO`.
- `Transaction` (`domain/Transaction.java`) possui `id`, `description`, `amount` e `category`, sem campo de data/hora.
- `TransactionEntity` (`infrastructure/persistence/entity/TransactionEntity.java`) mapeia apenas `id`, `description`, `amount` e `category`.
- `TransactionOutput` (`application/output/TransactionOutput.java`) e `TransactionResponse` (`infrastructure/http/response/TransactionResponse.java`) não expõem `createdAt`.
- `TransactionToolsContractTest` valida estritamente o JSON Schema gerado pelo Spring AI para `persist-transaction` e `list-transactions-by-category`, que hoje lista apenas `["GROCERIES", "PHARMA", "AUTO"]`.

## Goals / Non-Goals

**Goals:**
- Registrar automaticamente o instante de criação (`createdAt`) na persistência JPA e propagá-lo para o domínio, para o output da camada `application` (usado também pelas tools da IA) e para o response HTTP.
- Expandir o enum `Category` com as seis novas categorias previstas no RF-05 sem quebrar o funcionamento das três categorias existentes.
- Manter 100% de aderência aos padrões dos módulos `00-domain-driven-design`, `02-spring-data` e `05-spring-ai` da trilha.

**Non-Goals:**
- Permitir que o cliente HTTP ou a IA envie `createdAt` na criação da transação (`TransactionRequest`, `PersistTransactionInput` e `PersistTransactionToolRequest` continuam com apenas `description`, `amount` e `category`).
- Implementar consultas por período (`from`/`to`) ou soma por categoria (ficam para o change 4, `add-financial-query-tools`).

## Decisions

### 1. Tipo `java.time.Instant` para `createdAt` em todas as camadas

O campo `createdAt` usa `java.time.Instant` em `Transaction`, `TransactionEntity`, `TransactionOutput` e `TransactionResponse`.

- **Por que `Instant`:** representa um ponto exato na linha do tempo em UTC, é serializado nativamente pelo Jackson no padrão ISO-8601 (`2026-10-01T12:00:00Z`) e funciona diretamente com `@CreationTimestamp` do Hibernate.
- **Alternativa rejeitada:** `LocalDateTime`. Não carrega informação de fuso horário, o que pode gerar ambiguidade dependendo do timezone da JVM ou do banco.
- **Fundamento na trilha:** módulos `00-domain-driven-design` (`BookEntity.createdOn`) e `02-spring-data` (`Customer.createdOn`, `Address.createdOn`, `Event.createdOn`, `SeatLock.createdAt` e `EventOutput.date`), que usam exclusivamente `java.time.Instant`.

### 2. Preenchimento automático com `@CreationTimestamp` em `TransactionEntity`

Em `TransactionEntity`, adicionamos:

```java
@CreationTimestamp
@Column(nullable = false, updatable = false)
private Instant createdAt;
```

- **Fluxo de criação e retorno:**
  1. `PersistTransactionUseCase` instancia `new Transaction(input.description(), input.amount(), input.category())`, em que `createdAt` ainda é `null` (pois a transação ainda não foi persistida).
  2. `JpaTransactionRepository.save` converte o domínio com `TransactionEntity.from(transaction)` e chama `transactionEntityRepository.save(entity)`.
  3. O Hibernate preenche `entity.createdAt` automaticamente no `INSERT` graças ao `@CreationTimestamp` e devolve a entidade gerenciada com o timestamp preenchido.
  4. `.toDomain()` reconstrói `Transaction` passando `this.createdAt`, que segue para `TransactionOutput.from(...)` e `TransactionResponse.from(...)`.
- **Por que não exigir `createdAt` não nulo no construtor de `Transaction`:** antes de passar pelo repositório, uma nova transação criada pelo caso de uso ainda não foi persistida, e quem gera o timestamp oficial de auditoria é a camada de persistência (`@CreationTimestamp`). Exigir `Assert.notNull(createdAt, ...)` no domínio conflitaria com a geração automática no banco.
- **Construtores em `Transaction`:**
  - `public Transaction(TransactionId id, String description, long amount, Category category, Instant createdAt)`: construtor principal, usado na reconstrução a partir do banco (`TransactionEntity.toDomain()`), mantendo as invariantes de `id`, `description`, `amount` e `category`.
  - `public Transaction(TransactionId id, String description, long amount, Category category)`: delega para `this(id, description, amount, category, null)`.
  - `public Transaction(String description, long amount, Category category)`: delega para `this(new TransactionId(), description, amount, category, null)`.
- **Fundamento na trilha:** módulo `02-spring-data` (`Customer` e `Event`, que usam `@CreationTimestamp` com `@Column(nullable = false, updatable = false)` em campo `Instant`) e seção 3.3 da proposta inicial.

### 3. Expansão do enum `Category` (RF-05)

`Category` passa a declarar:

```java
public enum Category {
    GROCERIES,
    PHARMA,
    AUTO,
    RESTAURANT,
    SHOPPING,
    TRANSPORT,
    HEALTH,
    LEISURE,
    OTHER,
}
```

- **Impacto automático no REST e no Spring AI:** como `TransactionRequest`, `TransactionController.readTransactions`, `PersistTransactionToolRequest` e `TransactionTools.listTransactionsByCategory` já tipam a categoria como `Category`, o Jackson (no REST) e o gerador de JSON Schema do Spring AI (nas tools) passam a aceitar automaticamente as nove categorias sem código extra.
- **Persistência JPA:** `TransactionEntity` já usa `@Enumerated(EnumType.STRING)`, portanto novos nomes de enum são gravados como texto sem quebrar registros antigos.
- **Fundamento na trilha:** módulo `05-spring-ai` (`Category` como `enum` mapeado com `@Enumerated(EnumType.STRING)` e exposto em `@ToolParam`).

### 4. Atualização dos `Support` e dos testes por camada

Seguindo `docs/padrao-de-testes.md` (seção 5: "Campos com regra são sempre fixados no `Support`: valor em centavos, categoria, descrição e datas"):

- `TestConstants`: ganha `public static final Instant CREATED_AT = Instant.parse("2026-10-01T12:00:00Z");`.
- `TransactionSupport` e `TransactionEntitySupport`: passam a fixar `.set(field(...::getCreatedAt), CREATED_AT)`.
- `JpaTransactionRepositoryIntegrationTest`: como o MySQL + `@CreationTimestamp` atribuem o instante real da inserção no banco (sobrescrevendo o `createdAt` em memória e truncando na precisão do MySQL), o teste de integração verifica os demais campos contra a transação esperada (`ignoringFields("createdAt")`) e confirma que `createdAt` foi preenchido automaticamente pelo banco (`isNotNull()`).

| Camada | Classe de teste | O que verifica |
|---|---|---|
| Domínio | `TransactionTest` | Construtores preservam `createdAt` quando informado e deixam `null` antes da persistência quando omitido |
| Saída (application) | `TransactionOutputTest` | `TransactionOutput.from` mapeia `createdAt` do domínio |
| Caso de uso | `PersistTransactionUseCaseTest`, `ListTransactionsByCategoryUseCaseTest` | `TransactionOutput` retornado inclui o `createdAt` da transação salva/listada |
| Entidade JPA | `TransactionEntityTest` | Round-trip `from`/`toDomain` preserva `createdAt` |
| Repositório | `JpaTransactionRepositoryTest` | Mapeamento unitário de `createdAt` na entidade e no domínio |
| Repositório (integração) | `JpaTransactionRepositoryIntegrationTest` | No MySQL real, `save` e `findAllByCategory` preenchem e retornam `createdAt` não nulo |
| Adaptador IA | `TransactionToolsContractTest` | JSON Schema das duas tools contém as 9 categorias na propriedade `enum` |
| API (integração) | `TransactionApiIntegrationTest` | `POST /transactions` e `GET /transactions/{category}` retornam `createdAt` preenchido e aceitam as novas categorias (ex.: `RESTAURANT`) |

## Risks / Trade-offs

- [Precisão de nanosegundos da JVM vs microssegundos do MySQL em `Instant`] → Em testes de integração com MySQL real, comparar um `Instant` gerado antes do `save` com o lido do banco falharia porque o `@CreationTimestamp` gera o instante no momento do `INSERT` e o MySQL armazena 6 casas decimais (`DATETIME(6)`). Nos testes de integração verificamos que `createdAt` é preenchido (`isNotNull()`), enquanto nos testes unitários usamos a constante determinística `CREATED_AT`.
- [Consultas por período no change 4 precisarão de transações em datas distintas no banco] → Como `@CreationTimestamp` tem `updatable = false` apenas no mapeamento JPA de update ou preenche no insert, no change 4 avaliaremos a melhor estratégia de setup de dados de teste para períodos distintos sem comprometer o preenchimento automático exigido pelo RF-04.
