# Tasks: add-transaction-validation

## 1. Dependência de validação

- [x] 1.1 Adicionar `implementation 'org.springframework.boot:spring-boot-starter-validation'` ao `build.gradle`; verificar que `./gradlew compileJava` passa

## 2. Invariantes no domínio e no caso de uso (D-03, RF-02)

- [x] 2.1 Adicionar construtor compacto em `TransactionId` validando `Assert.notNull(uuid, "Transaction ID must not be null")` e escrever `domain/TransactionIdTest` cobrindo UUID válido e `uuid` nulo; verificar que `TransactionIdTest` passa
- [x] 2.2 Em `Transaction`, substituir `@AllArgsConstructor` por um construtor explícito `(TransactionId id, String description, long amount, Category category)` com `Assert.notNull(id, ...)`, `Assert.hasText(description, ...)`, `Assert.isTrue(amount > 0, ...)` e `Assert.notNull(category, ...)`, delegado pelo construtor `(String description, long amount, Category category)`; escrever `domain/TransactionTest` cobrindo criação válida e os cenários inválidos; verificar que `TransactionTest` passa
- [x] 2.3 Adicionar em `PersistTransactionUseCaseTest` os cenários em que o input contém dados inválidos (descrição em branco, valor menor ou igual a zero e categoria nula), verificando que `IllegalArgumentException` é lançada e que `transactionRepository.save` nunca é chamado; verificar que `PersistTransactionUseCaseTest` passa

## 3. Bean Validation, tratamento de erros HTTP e áudio vazio (D-03, D-06, RF-02, RF-03)

- [x] 3.1 Anotar `TransactionRequest` com `@NotBlank` em `description`, `@NotNull` em `category` e `@Positive` em `amount`; adicionar `@Valid` no parâmetro de `TransactionController.createTransaction` e `Assert.isTrue(!file.isEmpty(), "Audio file must not be empty")` no início de `TransactionController.transcribe`; verificar que `./gradlew compileJava` passa
- [x] 3.2 Criar `infrastructure/http/GlobalExceptionHandler` (`@RestControllerAdvice`) tratando `MethodArgumentNotValidException` (HTTP 400 com `Map<String, String>` de campo para mensagem) e `IllegalArgumentException` (HTTP 400 com `ex.getMessage()`); escrever `infrastructure/http/GlobalExceptionHandlerTest` cobrindo os dois handlers; verificar que `GlobalExceptionHandlerTest` passa
- [x] 3.3 Adicionar em `TransactionApiIntegrationTest` os cenários HTTP 400 para `POST /transactions` (descrição vazia/em branco, `amount <= 0`, `category` nula e múltiplos campos inválidos no mesmo JSON, garantindo que nada é salvo no banco) e para `POST /transactions/ai` com arquivo de áudio vazio (`0` bytes); verificar que `TransactionApiIntegrationTest` passa

## 4. Verificação final

- [x] 4.1 Rodar `./gradlew test` sem `OPENAI_API_KEY` e com o MySQL do Compose parado; verificar `BUILD SUCCESSFUL`, todos os testes verdes e os `*IT` ignorados
