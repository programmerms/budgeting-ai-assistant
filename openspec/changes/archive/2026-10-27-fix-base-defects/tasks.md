# Tasks: fix-base-defects

## 1. Dependências de teste

- [x] 1.1 Adicionar ao `build.gradle` as dependências de teste: `spring-boot-testcontainers`, Testcontainers JUnit Jupiter e MySQL (artefatos do Testcontainers 2), `instancio-core` na versão escolhida pelo autor (5.0.2, ver design, decisão 6) e, se `@AutoConfigureMockMvc` exigir, o starter de teste do Web MVC do Spring Boot 4; verificar que `./gradlew compileTestJava` passa

## 2. Suíte executável sem infraestrutura externa (D-04)

- [x] 2.1 Adaptar `BudgetingApplicationTests` com `@Testcontainers`, um MySQL `mysql:9.6` em campo `static` com `@Container` e `@ServiceConnection` e `@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")`; verificar que `contextLoads` passa sem `OPENAI_API_KEY` e sem o MySQL do Compose rodando
- [x] 2.2 Criar `support/TestConstants` e os `Support` de `Transaction`, `PersistTransactionInput`, `TransactionRequest` e `TransactionEntity` (seção 5 do padrão), com valor, descrição e categoria fixos; verificar que compilam e são usados pelos testes dos grupos 3 a 5

## 3. Valor em reais (D-01)

- [x] 3.1 Escrever `TransactionOutputTest` parametrizado para `8000 → 80.00`, `8050 → 80.50` e `1 → 0.01`, comparando `BigDecimal` com escala 2; verificar que ele falha com o código atual
- [x] 3.2 Alterar `TransactionOutput` para usar `BigDecimal.valueOf(amount, 2)` num campo `BigDecimal`, e `TransactionResponse` para repassar o `BigDecimal`; verificar que `TransactionOutputTest` passa
- [x] 3.3 Escrever `PersistTransactionUseCaseTest` e `ListTransactionsByCategoryUseCaseTest` com `TransactionRepository` mockado, verificando a chamada ao repositório e o valor devolvido em reais; verificar que passam

## 4. Persistência

- [x] 4.1 Escrever `TransactionEntityTest` (round-trip `from` → `toDomain` mantém id, descrição, valor e categoria) e `JpaTransactionRepositoryTest` (delegação ao `TransactionEntityRepository` mockado e mapeamento para o domínio); verificar que passam
- [x] 4.2 Escrever `JpaTransactionRepositoryIntegrationTest` com MySQL do Testcontainers: salvar transações de duas categorias e buscar por uma delas devolve só as dela, com os dados intactos; verificar que passa

## 5. API REST

- [x] 5.1 Escrever `TransactionApiIntegrationTest` com MockMvc e MySQL do Testcontainers: `POST /transactions` com `amount = 8000` responde 201 com `amount = 80.00`, e `GET /transactions/{category}` mostra `40.00` para uma transação de `4000`; verificar que passam

## 6. Prompt em UTF-8 (D-05)

- [x] 6.1 Trocar `Charset.defaultCharset()` por `StandardCharsets.UTF_8` na leitura do prompt em `TransactionController`; verificar com `contextLoads` e `TransactionApiIntegrationTest` verdes e confirmar por busca que não sobrou `Charset.defaultCharset()` em `src/main`

## 7. Documentação

- [x] 7.1 Atualizar o exemplo de Testcontainers em `docs/padrao-de-testes.md` se a API do Testcontainers 2 diferir do exemplo atual, e registrar no `AGENTS.md` que `./gradlew test` exige Docker rodando; verificar que o exemplo do documento é igual ao código dos testes

## 8. Verificação final

- [x] 8.1 Rodar `./gradlew test` sem `OPENAI_API_KEY` e com o MySQL do Compose parado; verificar `BUILD SUCCESSFUL`, nenhum teste falhando e os `*IT` ignorados
