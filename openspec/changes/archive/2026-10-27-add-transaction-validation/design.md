# Design: add-transaction-validation

## Context

A motivação está em `proposal.md` (seção Why). O estado atual do código relevante para este change:

- `TransactionId` (`record TransactionId(UUID uuid)`) não verifica se `uuid` é nulo.
- `Transaction` tem dois construtores: o construtor completo gerado por `@AllArgsConstructor` (usado na reconstrução a partir do banco em `TransactionEntity.toDomain()`) e `Transaction(String description, long amount, Category category)` (usado na criação de uma nova transação em `PersistTransactionUseCase`). Nenhum deles valida os parâmetros.
- `TransactionRequest` (`record TransactionRequest(String description, Category category, long amount)`) não possui anotações de Bean Validation, e `build.gradle` ainda não declara `spring-boot-starter-validation`.
- `TransactionController.createTransaction` não usa `@Valid`, e `TransactionController.transcribe` chama `transcriptionModel.transcribe(file.getResource())` sem checar `file.isEmpty()`.
- Não existe `@RestControllerAdvice` no projeto.

## Goals / Non-Goals

**Goals:**
- Garantir que nenhuma `Transaction` ou `TransactionId` exista em memória com estado inválido, protegendo tanto o fluxo REST quanto o fluxo de Tool Calling (`TransactionTools -> PersistTransactionUseCase -> Transaction`).
- Devolver HTTP 400 com mapa `campo -> mensagem` quando `POST /transactions` receber JSON com campos inválidos, e HTTP 400 quando `POST /transactions/ai` receber arquivo vazio.
- Seguir estritamente as ferramentas e padrões do módulo `01-spring-web` e `00-domain-driven-design`.

**Non-Goals:**
- Criar hierarquia de exceções customizadas de validação no domínio (na trilha, invariantes com `Assert` lançam `IllegalArgumentException`).
- Mover a validação ou a chamada de IA de `transcribe` para um caso de uso (isso pertence ao change 5, `extract-voice-command-use-case`).
- Usar anotações de Bean Validation em `PersistTransactionToolRequest` ou `PersistTransactionInput`: o Spring AI não executa `@Valid` automaticamente nos argumentos de `@Tool`, então a proteção real do canal de IA vem das invariantes no domínio.

## Decisions

### 1. Invariantes no domínio com `org.springframework.util.Assert`

`TransactionId` passa a ter construtor compacto validando `Assert.notNull(uuid, "Transaction ID must not be null")`.

Em `Transaction`, substituímos `@AllArgsConstructor` por um construtor explícito `public Transaction(TransactionId id, String description, long amount, Category category)` que valida todos os campos e é chamado pelo construtor secundário `public Transaction(String description, long amount, Category category)`:

- `Assert.notNull(id, "Transaction ID must not be null");`
- `Assert.hasText(description, "Description must not be blank");`
- `Assert.isTrue(amount > 0, "Amount must be greater than zero");`
- `Assert.notNull(category, "Category must not be null");`

- **Por que no construtor completo:** centraliza a validação em um único lugar; tanto uma transação nova quanto uma reconstruída passam pelas mesmas invariantes, e o `TransactionSupport` (que instancia `Transaction` nos testes) continua seguro porque já fixa `description`, `amount` positivo e `category` (apenas garantindo que `id` também seja válido).
- **Por que `Assert` do Spring:** é exatamente como os módulos `00-domain-driven-design` (`Book`, `BookId`), `01-spring-web` (`Task`, `TaskId`) e `02-spring-data` (`Customer`, `CustomerId`) implementam invariantes de domínio.
- **Comportamento no Tool Calling:** quando a IA chama `persist-transaction` com valor `<= 0`, descrição vazia ou categoria nula, `PersistTransactionUseCase` chama `new Transaction(...)`, que lança `IllegalArgumentException` antes de `transactionRepository.save(...)`. O Spring AI captura a exceção da tool e devolve a mensagem de erro ao modelo, sem persistir nada.

### 2. Bean Validation em `TransactionRequest` e `@Valid` no controller

Adicionamos `implementation 'org.springframework.boot:spring-boot-starter-validation'` ao `build.gradle`. Em `TransactionRequest`:

- `@NotBlank String description`
- `@NotNull Category category`
- `@Positive long amount`

E em `TransactionController.createTransaction`: `@RequestBody @Valid TransactionRequest request`.

- **Por que validar no HTTP além do domínio:** no REST, o Bean Validation valida todos os campos de uma vez e produz o `MethodArgumentNotValidException` com o nome exato de cada campo inválido (`description`, `category`, `amount`), atendendo ao RF-03. Já o domínio valida campo a campo e garante que nenhum outro adaptador (como o `TransactionTools` da IA) salve dados inválidos.
- **Alternativa rejeitada:** confiar apenas no `Assert` do domínio também para o REST. Falharia no primeiro campo inválido e não informaria o nome dos campos estruturados num mapa `campo -> mensagem`.
- **Fundamento na trilha:** módulo `01-spring-web` (`build.gradle` com `spring-boot-starter-validation`, `CreateTaskRequest` com `@NotBlank` e `TaskController` com `@RequestBody @Valid`).

### 3. `GlobalExceptionHandler` em `infrastructure/http`

Criamos `dio.budgeting.infrastructure.http.GlobalExceptionHandler` anotado com `@RestControllerAdvice`, contendo dois handlers:

1. `@ExceptionHandler(MethodArgumentNotValidException.class)` com `@ResponseStatus(HttpStatus.BAD_REQUEST)`, percorrendo `ex.getBindingResult().getAllErrors()` e montando um `Map<String, String>` com `((FieldError) error).getField()` e `error.getDefaultMessage()`.
2. `@ExceptionHandler(IllegalArgumentException.class)` com `@ResponseStatus(HttpStatus.BAD_REQUEST)`, retornando `ex.getMessage()` (`String`).

- **Por que incluir `IllegalArgumentException`:** cobre validações imperativas com `Assert` (como arquivo de áudio vazio ou qualquer chamada que chegue ao domínio com argumento inválido), respondendo HTTP 400 com a mensagem clara em vez de HTTP 500.
- **Fundamento na trilha:** idêntico ao `GlobalExceptionHandler` do módulo `01-spring-web`, que usa `@RestControllerAdvice`, `@ExceptionHandler` e `@ResponseStatus` retornando `Map<String, String>` para `MethodArgumentNotValidException` e `String` para exceções de negócio.

### 4. Rejeição de arquivo de áudio vazio com `Assert.isTrue`

No início de `TransactionController.transcribe(@RequestParam("file") MultipartFile file)`, adicionamos:

```java
Assert.isTrue(!file.isEmpty(), "Audio file must not be empty");
```

Quando `file.isEmpty()` é verdadeiro (`0` bytes), `Assert.isTrue` lança `IllegalArgumentException("Audio file must not be empty")` antes de chamar `transcriptionModel`, `chatClient` ou `textToSpeechModel`, e o `GlobalExceptionHandler` devolve HTTP 400 com a mensagem. No change 5 (`extract-voice-command-use-case`), quando a orquestração de voz for extraída para um caso de uso, essa regra acompanhará a extração.

- **Fundamento na trilha:** uso de `org.springframework.util.Assert` (módulo 01) aliado ao `@ExceptionHandler` no `GlobalExceptionHandler` (módulo 01).

### 5. Testes por camada

Seguindo `docs/padrao-de-testes.md`:

| Camada | Classe de teste | Tipo | O que verifica |
|---|---|---|---|
| Domínio | `TransactionIdTest` | Unitário | Criação com UUID válido e `IllegalArgumentException` quando `uuid` é nulo |
| Domínio | `TransactionTest` | Unitário (parametrizado para casos inválidos) | Criação válida e `IllegalArgumentException` para `id` nulo, `description` nula/vazia/em branco, `amount` `0` ou negativo e `category` nula |
| Caso de uso | `PersistTransactionUseCaseTest` | Unitário (`TransactionRepository` mockado) | Quando o input tem dado inválido (ex.: `amount <= 0`), lança `IllegalArgumentException` e `transactionRepository.save` nunca é chamado (`verifyNoInteractions`) |
| HTTP | `GlobalExceptionHandlerTest` | Unitário | `handleValidationExceptions` extrai mapa `campo -> mensagem` e `handleIllegalArgumentException` devolve a mensagem |
| API | `TransactionApiIntegrationTest` | Integração (MockMvc + Testcontainers) | `POST /transactions` com descrição em branco, `amount <= 0` e `category` nula responde 400 indicando os campos e não salva no banco; `POST /transactions/ai` com arquivo vazio (`MockMultipartFile` de 0 bytes) responde 400 |

## Risks / Trade-offs

- [Instancio gerar `Transaction` com `id` nulo se não estiver configurado] → O Instancio instancia records (`TransactionId`) preenchendo seus componentes (`UUID`) automaticamente; os testes de domínio e os testes existentes confirmarão que `defaultTransaction().create()` continua gerando `TransactionId` válido.
- [Validação de arquivo vazio ficar temporariamente no controller] → No change 5 (`extract-voice-command-use-case`) toda a lógica de `transcribe` irá para o caso de uso; manter a checagem de 1 linha aqui resolve o D-06 agora sem antecipar o escopo do change 5.
