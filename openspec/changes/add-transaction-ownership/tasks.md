# Tasks

> Nota sobre a suíte: as assinaturas de domínio, repositório e casos de uso mudam juntas, então a suíte fica vermelha da tarefa 2.1 até a 5.3, quando os últimos chamadores são atualizados. Nenhum commit é feito com a suíte vermelha; os grupos 2 a 5 formam um commit único.

## 1. Descarte dos dados de desenvolvimento

- [ ] 1.1 Rodar `docker compose down -v` na raiz do projeto para descartar o volume `transaction_data`; verificar com `docker volume ls` que o volume não existe mais

## 2. Proprietário no domínio

- [ ] 2.1 Criar `domain/OwnerId` como `record OwnerId(UUID uuid)` com `Assert.notNull` e construtor sem argumentos que gera um UUID, no mesmo formato de `TransactionId`; escrever `OwnerIdTest` cobrindo criação válida, rejeição de `null` e unicidade entre instâncias; verificar que `OwnerIdTest` passa
- [ ] 2.2 Adicionar `OwnerId` obrigatório como primeiro campo de `Transaction`, validado com `Assert.notNull`, ajustando os três construtores para exigi-lo; atualizar `TransactionTest` com os cenários "Transação registrada guarda o proprietário" e "Transação sem proprietário é recusada" do requisito "Propriedade obrigatória da transação"; verificar que `TransactionTest` passa
- [ ] 2.3 Atualizar `TransactionRepository` para `findAllByOwnerIdAndCategory(OwnerId, Category)` e `findAllByOwnerIdAndCreatedAtBetween(OwnerId, Instant, Instant)`, sem nenhuma operação global de leitura; verificar com `./gradlew compileJava` que nenhuma implementação do contrato ficou para trás
- [ ] 2.4 Atualizar `support/domain/TransactionSupport` e `TestConstants` com um `OwnerId` de teste conhecido, para que os testes seguintes possam afirmar a quem a transação pertence; verificar que `OwnerIdTest` e `TransactionTest` continuam passando

## 3. Propriedade na persistência

- [ ] 3.1 Adicionar a `TransactionEntity` a coluna `owner_id` não nula e o índice `idx_transaction_owner_id` em `@Table`, mapeando o proprietário em `from` e `toDomain`; atualizar `TransactionEntityTest` para cobrir a ida e a volta do proprietário; verificar que `TransactionEntityTest` passa
- [ ] 3.2 Trocar as *derived queries* de `TransactionEntityRepository` por `findAllByOwnerIdAndCategory` e `findAllByOwnerIdAndCreatedAtBetween`, e atualizar `JpaTransactionRepository` para o novo contrato; atualizar `JpaTransactionRepositoryTest`; verificar que `JpaTransactionRepositoryTest` passa
- [ ] 3.3 Atualizar `JpaTransactionRepositoryIntegrationTest` para gravar transações de dois proprietários diferentes e verificar, contra o MySQL do Testcontainers, que cada consulta devolve apenas as do proprietário informado; verificar que o teste passa

## 4. Proprietário nos casos de uso

- [ ] 4.1 Mudar `PersistTransactionUseCase` para `execute(OwnerId, PersistTransactionInput)`, construindo a transação com o proprietário recebido; atualizar `PersistTransactionUseCaseTest` cobrindo a associação ao proprietário e a rejeição de proprietário nulo; verificar que o teste passa
- [ ] 4.2 Mudar `ListTransactionsByCategoryUseCase` e `SummarizeTransactionsByCategoryUseCase` para `execute(OwnerId, Category)`, delegando às consultas filtradas; atualizar os dois testes unitários correspondentes; verificar que passam
- [ ] 4.3 Mudar `ListTransactionsByPeriodUseCase` para `execute(OwnerId, TransactionsPeriodInput)`, preservando a validação de intervalo existente; atualizar `ListTransactionsByPeriodUseCaseTest`; verificar que passa

## 5. Identidade autenticada na infraestrutura

- [ ] 5.1 Criar `infrastructure/security/AuthenticatedOwnerProvider` com `currentOwnerId()` lendo o `SecurityContextHolder`, devolvendo `OwnerId` a partir do `AuthenticatedUser` e lançando exceção quando não houver principal autenticado ou ele for de outro tipo; escrever `AuthenticatedOwnerProviderTest` cobrindo principal válido, contexto vazio e principal de tipo inesperado; verificar que o teste passa
- [ ] 5.2 Injetar o provedor em `TransactionController` e repassar o `OwnerId` aos quatro casos de uso financeiros; atualizar `TransactionControllerTest`; verificar que passa
- [ ] 5.3 Injetar o provedor em `TransactionTools` e repassar o `OwnerId` nas quatro ferramentas, sem adicionar nenhum `@ToolParam` de proprietário; atualizar `TransactionToolsTest`; verificar que passa
- [ ] 5.4 Acrescentar a `TransactionToolsContractTest` a verificação do cenário "Contrato exposto ao modelo não tem proprietário", afirmando que nenhum JSON Schema das ferramentas declara parâmetro de proprietário; verificar que o teste passa e que `./gradlew test` compila a suíte inteira de novo

## 6. Isolamento ponta a ponta na API

- [ ] 6.1 Criar em `src/test/java/dio/budgeting/support` um suporte que construa um `AuthenticatedUser` com `UserAccountId` conhecido, para uso com `SecurityMockMvcRequestPostProcessors.user(...)`; atualizar `TransactionApiIntegrationTest` para autenticar com esse principal em vez de `@WithMockUser`; verificar que o teste passa com o mesmo comportamento de antes
- [ ] 6.2 Acrescentar a `TransactionApiIntegrationTest` os cenários do requisito "Isolamento das transações entre usuários", cobrindo consulta por categoria, resumo e consulta por período com dois usuários distintos; verificar que o teste passa
- [ ] 6.3 Acrescentar a `TransactionApiIntegrationTest` os cenários do requisito "Proprietário não controlável pela entrada", enviando um campo de proprietário no corpo e conferindo que ele é ignorado e que nenhuma resposta expõe o proprietário; verificar que o teste passa

## 7. Documentação

- [ ] 7.1 Atualizar `src/docs/asciidoc/index.adoc` registrando que cada endpoint financeiro opera apenas sobre os gastos da pessoa autenticada e que o proprietário não é aceito nem devolvido; verificar que `./gradlew asciidoctor` gera `build/docs/asciidoc/index.html` com a seção atualizada
- [ ] 7.2 Atualizar o `README.md` com o isolamento por usuário, incluindo a nota de que o administrador não acessa dados financeiros, e registrar o descarte do banco de desenvolvimento nas instruções de execução; verificar lendo o README que o fluxo descrito bate com os endpoints implementados

## 8. Verificação final

- [ ] 8.1 Rodar `./gradlew test` sem `OPENAI_API_KEY`; verificar `BUILD SUCCESSFUL` com os testes `*IT` ignorados
- [ ] 8.2 Subir a aplicação e percorrer o fluxo com `curl`: cadastrar e aprovar dois usuários, cada um registrar uma transação, e confirmar que a consulta por categoria, o resumo, a consulta por período e um comando em texto de cada um devolvem apenas os próprios dados
