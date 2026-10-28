# Tasks: add-transaction-timestamp-and-categories

## 1. Expansão das categorias (RF-05)

- [x] 1.1 Adicionar `RESTAURANT`, `SHOPPING`, `TRANSPORT`, `HEALTH`, `LEISURE` e `OTHER` ao enum `Category` e atualizar os JSON Schemas esperados em `TransactionToolsContractTest` com as nove categorias; verificar que `TransactionToolsContractTest` passa

## 2. Campo `createdAt` no domínio e na camada de aplicação (D-07, RF-04)

- [x] 2.1 Adicionar `createdAt` (`java.time.Instant`) em `Transaction` (com construtor completo recebendo `createdAt` e sobrecargas delegando com `null`), adicionar `CREATED_AT` em `TestConstants` e fixá-lo em `TransactionSupport`, e atualizar `TransactionTest` cobrindo a criação com e sem `createdAt`; verificar que `TransactionTest` passa
- [x] 2.2 Adicionar `createdAt` (`Instant`) em `TransactionOutput` e `TransactionResponse`, atualizar `TransactionOutputTest`, `PersistTransactionUseCaseTest` e `ListTransactionsByCategoryUseCaseTest` conferindo o mapeamento de `createdAt`; verificar que os três testes passam

## 3. Persistência automática de `createdAt` e testes de integração (D-07, RF-04, RF-05)

- [x] 3.1 Adicionar `createdAt` (`Instant`) anotado com `@CreationTimestamp` e `@Column(nullable = false, updatable = false)` em `TransactionEntity` (atualizando `from` e `toDomain`), fixar `CREATED_AT` em `TransactionEntitySupport` e atualizar `TransactionEntityTest` e `JpaTransactionRepositoryTest`; verificar que ambos os testes unitários passam
- [x] 3.2 Atualizar `JpaTransactionRepositoryIntegrationTest` e `TransactionApiIntegrationTest` para verificar o preenchimento automático de `createdAt` no MySQL e na resposta JSON (`POST /transactions` e `GET /transactions/{category}`) e incluir cenário de registro e consulta com nova categoria (`RESTAURANT`); verificar que ambos os testes de integração passam

## 4. Verificação final

- [x] 4.1 Rodar `./gradlew test` sem `OPENAI_API_KEY`; verificar `BUILD SUCCESSFUL`, todos os testes verdes e os `*IT` ignorados
