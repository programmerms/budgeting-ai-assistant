# Design: move-ai-tools-to-infrastructure

## Context

A motivação está em `proposal.md` (seção Why). O estado atual que importa para a solução:

- `PersistTransactionUseCase.execute(PersistTransactionInput input)` tem `@Tool(name = "persist-transaction", description = "Persiste uma nova transação financeira")`. Os três campos do `PersistTransactionInput` (`description`, `amount` do tipo `long` e `category`) têm `@ToolParam`.
- `ListTransactionsByCategoryUseCase.execute(Category category)` tem `@Tool(name = "list-transactions-by-category", description = "Lista transações financeiras por categoria")`, e o parâmetro tem `@ToolParam(description = "Categoria de uma transação")`.
- O `TransactionController` registra os dois casos de uso com `.defaultTools(persistTransactionUseCase, listTransactionsByCategoryUseCase)` ao montar o `ChatClient` no construtor.
- O que a IA enxerga de cada ferramenta (a "definição da ferramenta") é gerado pelo Spring AI a partir dessas anotações: o nome, a descrição e um JSON Schema dos parâmetros. Esse schema usa os **nomes dos parâmetros do método** (`input` e `category`) e os nomes dos campos do record. O retorno (`TransactionOutput`) é convertido para JSON e devolvido à IA.
- Os testes do change 1 (casos de uso, saída, persistência e API) não dependem das anotações.

## Goals / Non-Goals

**Goals:**
- Nenhuma classe de `domain` ou `application` importa `org.springframework.ai`.
- A definição das duas ferramentas (nome, descrição e JSON Schema dos parâmetros) e o JSON devolvido à IA ficam idênticos aos de hoje, com um teste automatizado que prove isso.
- O adaptador de IA segue a mesma forma do adaptador HTTP, para servir de modelo às tools do change 4.

**Non-Goals:**
- Mudar a montagem do `ChatClient` ou tirá-lo do controller (change 5).
- Mudar textos das ferramentas, para melhorar como a IA as escolhe (change 6).

## Decisions

### 1. Um adaptador `TransactionTools` em `infrastructure/ai`

Uma classe `infrastructure/ai/TransactionTools`, anotada com `@Component`, com um método `@Tool` para cada ferramenta. Cada método só converte a entrada e chama o caso de uso, sem regra de negócio, exatamente como o `TransactionController` faz no HTTP.

- **Por que uma classe para as duas ferramentas:** é o mesmo agrupamento do `TransactionController`, que atende os dois casos de uso de transação. No change 4, as tools novas de consulta entram nessa mesma classe.
- **Alternativa rejeitada: uma classe de tool por caso de uso.** Gera classes de uma linha e não espelha o lado HTTP.
- **Alternativa rejeitada: manter as anotações nos casos de uso**, como no projeto base do módulo 05. Foi a decisão original, revista na seção 3.3 da proposta inicial: deixa a `application` dependente do Spring AI.
- **Fundamento na trilha:** a regra de camadas do README raiz e do módulo 00 (integrações ficam na `infrastructure` e chamam a `application`); `@Tool` e `@ToolParam` do módulo 05, só que numa camada diferente.

### 2. Record de request próprio para a ferramenta de registro

Entra `infrastructure/ai/request/PersistTransactionToolRequest(description, amount, category)`, com os mesmos `@ToolParam` de hoje e um método `toInput()` que devolve o `PersistTransactionInput`. É o mesmo desenho do `infrastructure/http/request/TransactionRequest.toInput()`. O `PersistTransactionInput` fica como um record simples, sem anotações.

- **Tipos e nomes iguais aos de hoje:** os campos mantêm os nomes `description`, `amount` e `category`, e o `amount` continua `long`. O método do adaptador mantém o nome de parâmetro `input`. Assim o JSON Schema que a IA recebe não muda, e o cenário da spec "Chamada da ferramenta sem valor" continua com o mesmo comportamento. A validação desse caso é assunto do change 2.
- **A ferramenta de consulta não ganha record:** ela recebe só um `Category`, como o `GET /transactions/{category}` recebe um `@PathVariable`. O `@ToolParam` fica direto no parâmetro do método.
- **Alternativa rejeitada: reaproveitar o `PersistTransactionInput` no adaptador** sem as anotações. A IA perderia as descrições dos campos ("Valor do gasto (em centavos)"), que orientam o preenchimento, e isso mudaria o comportamento.
- **Fundamento na trilha:** records como DTOs de entrada com conversão para o input do caso de uso (`CreateTaskRequest` do módulo 01 e `TransactionRequest` do módulo 05).

### 3. Retorno continua sendo o `TransactionOutput`

Os métodos do adaptador devolvem `TransactionOutput` e `List<TransactionOutput>`, que é o mesmo objeto que os casos de uso devolvem hoje à IA.

- **Por que não criar um `TransactionToolResponse`:** o HTTP tem o `TransactionResponse` porque expõe o campo com outro nome (`amount`). Na IA, um record novo só duplicaria o `TransactionOutput` e arriscaria mudar o JSON que o modelo recebe, o que este change promete não fazer. Se um dia o formato para a IA precisar divergir, o record entra nesse change.
- **Fundamento na trilha:** casos de uso com `output` próprio (README raiz, módulo 00).

### 4. O controller registra o adaptador no `ChatClient`

O construtor do `TransactionController` passa a receber o `TransactionTools` e chama `.defaultTools(transactionTools)`. Os casos de uso continuam injetados no controller, porque são usados pelos endpoints REST.

- **Alternativa rejeitada: registrar as tools por um `@Bean` de `ToolCallbackProvider`.** Funciona, mas não aparece no módulo 05 e só faz sentido quando a montagem do `ChatClient` sair do controller (change 5).
- **Fundamento na trilha:** `ChatClient.Builder.defaultTools(...)` do módulo 05.

### 5. Teste de contrato das ferramentas, escrito antes de mover

Como é uma refatoração, a prova de que nada mudou para a IA vem de um **teste de caracterização**: um teste que registra o comportamento atual como resposta esperada, antes de mexer no código.

1. Antes de qualquer mudança, um teste gera as definições a partir dos casos de uso com `ToolCallbacks.from(...)`. É o mesmo mecanismo que o `defaultTools(...)` usa por baixo. O teste confere o nome, a descrição e o JSON Schema de cada ferramenta. O JSON esperado é copiado da saída atual.
2. Depois da mudança, o mesmo teste passa a gerar as definições a partir do `TransactionTools`, com os mesmos valores esperados. Se ficar verde, a IA recebe exatamente as mesmas ferramentas.

- **Comparação do JSON Schema:** com `JSONAssert` em modo estrito. Ele já vem no `spring-boot-starter-test` (é o que o `content().json(...)` do MockMvc usa), então não é dependência nova.
- **Decisão do autor:** o `ToolCallbacks` faz parte do Spring AI do módulo 05 (é o que o `defaultTools(...)` usa por baixo), sem dependência nova. Foi aprovado no lugar da alternativa de confiar só na revisão de código, sem prova automatizada.
- **Fundamento na trilha:** `@Tool` e `defaultTools` do módulo 05; testes unitários sem contexto Spring do módulo 01.

### 6. Testes do adaptador

| Teste | Tipo | Cobre |
|---|---|---|
| `TransactionToolsTest` | unitário, casos de uso mockados | a ferramenta de registro converte o request e chama o `PersistTransactionUseCase`; a ferramenta de consulta chama o `ListTransactionsByCategoryUseCase`; as duas devolvem o `TransactionOutput` do caso de uso |
| `TransactionToolsContractTest` | unitário, sem mocks de comportamento | nome, descrição e JSON Schema das duas ferramentas (decisão 5) |

- Dados de teste num novo `support/request/PersistTransactionToolRequestSupport`, com valor, descrição e categoria fixos (padrão de testes, seção 5).
- **Fundamento na trilha:** Mockito com `@Mock` e `@InjectMocks` do módulo 01 (`CreateTaskUseCaseTest`).

### 7. Regra registrada no `AGENTS.md`

Em "Arquitetura e convenções", entra a regra: "Anotações do Spring AI (`@Tool`, `@ToolParam`) ficam só em `infrastructure/ai`; casos de uso não dependem do Spring AI". Assim os próximos changes, e qualquer agente, seguem o novo padrão sem precisar redescobrir a decisão.

## Risks / Trade-offs

- [Renomear sem querer um parâmetro ou campo muda o JSON Schema, e a IA passa a preencher errado] → o teste de contrato (decisão 5) falha nesse caso.
- [O JSON Schema depende de o compilador guardar os nomes dos parâmetros (`-parameters`)] → o plugin do Spring Boot no Gradle já liga essa opção, e o teste de contrato quebraria se ela sumisse.
- [O teste de contrato fixa o texto exato das descrições] → é intencional: mudar a descrição muda o comportamento da IA e deve ser uma decisão explícita (change 6), atualizando o teste junto.
- [A refatoração não é verificada contra a OpenAI de verdade no `./gradlew test`] → o `ToolCallingIT` do curso não usa estas tools. O teste de contrato cobre o que a IA recebe. Uma conferência manual pelo `POST /transactions/ai` com a chave real é recomendada, mas fica fora do `tasks.md`, porque depende da chave e não é critério de conclusão.
