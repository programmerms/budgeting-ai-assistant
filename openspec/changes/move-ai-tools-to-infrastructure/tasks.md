# Tasks: move-ai-tools-to-infrastructure

## 1. Teste de caracterização (antes de mexer no código)

- [ ] 1.1 Escrever `infrastructure/ai/TransactionToolsContractTest`, que gera as definições com `ToolCallbacks.from(...)` a partir dos dois casos de uso atuais (com o `TransactionRepository` mockado) e confere, para `persist-transaction` e `list-transactions-by-category`, o nome, a descrição e o JSON Schema dos parâmetros (`JSONAssert` em modo estrito), com o JSON esperado copiado da saída atual; verificar que passa com o código atual, sem nenhuma mudança em `src/main`

## 2. Adaptador de Tool Calling

- [ ] 2.1 Criar `infrastructure/ai/request/PersistTransactionToolRequest` (`description`, `amount` do tipo `long`, `category`, com os mesmos `@ToolParam` de hoje) com `toInput()`, o `support/request/PersistTransactionToolRequestSupport` e o `PersistTransactionToolRequestTest` (o `toInput()` mantém os três campos); verificar que o teste passa
- [ ] 2.2 Criar `infrastructure/ai/TransactionTools` (`@Component`) com dois métodos `@Tool` de mesmo nome e descrição dos atuais: o de registro recebe `PersistTransactionToolRequest input` e chama o `PersistTransactionUseCase`; o de consulta recebe `@ToolParam Category category` e chama o `ListTransactionsByCategoryUseCase`; ambos devolvem o `TransactionOutput` do caso de uso; verificar que `./gradlew compileJava` passa
- [ ] 2.3 Escrever `TransactionToolsTest` com os casos de uso mockados: o registro converte o request e chama o caso de uso com o input certo (`ArgumentCaptor`), a consulta repassa a categoria, e os dois devolvem o output do caso de uso; verificar que passa

## 3. Troca do registro e limpeza da camada `application`

- [ ] 3.1 Mudar o `TransactionToolsContractTest` para gerar as definições a partir do `TransactionTools`, sem alterar nenhum valor esperado; verificar que passa
- [ ] 3.2 No `TransactionController`, receber o `TransactionTools` e trocar o `.defaultTools(...)` para registrá-lo no lugar dos casos de uso; remover `@Tool` e `@ToolParam` de `PersistTransactionUseCase`, `ListTransactionsByCategoryUseCase` e `PersistTransactionInput`; verificar que `TransactionToolsContractTest`, `BudgetingApplicationTests` e `TransactionApiIntegrationTest` passam, e confirmar por busca que não há `org.springframework.ai` em `src/main/java/dio/budgeting/application` nem em `src/main/java/dio/budgeting/domain`

## 4. Documentação

- [ ] 4.1 Registrar no `AGENTS.md`, em "Arquitetura e convenções", que as anotações do Spring AI (`@Tool`, `@ToolParam`) ficam só em `infrastructure/ai` e que os casos de uso não dependem do Spring AI; e, no `docs/guia-openspec.md` (seção 5), que uma refatoração sem mudança de comportamento usa `skip_specs: true` no `.openspec.yaml`, com este change como exemplo; verificar que as duas regras aparecem nos arquivos

## 5. Verificação final

- [ ] 5.1 Rodar `./gradlew test` sem `OPENAI_API_KEY` e com o MySQL do Compose parado; verificar `BUILD SUCCESSFUL`, nenhum teste falhando, os testes do change 1 sem alteração e os `*IT` ignorados
