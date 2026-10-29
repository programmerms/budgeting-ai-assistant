# Padrão de testes

Convenção adotada em todos os testes novos do projeto. É o padrão do projeto anterior `dio-proposal-api`, adaptado às camadas da trilha DIO (`domain`, `application`, `infrastructure`), ao Gradle e ao MySQL com Testcontainers.

Os testes `*IT` herdados do curso (`OpenAiChatClientIT`, `ToolCallingIT` etc.) chamam a OpenAI de verdade, ficam como estão e não seguem este padrão.

## 1. Nomes

- `@DisplayName("Given <contexto> When <ação> Then <resultado>")`.
- O método tem a mesma frase em camelCase: `givenContextoWhenAcaoThenResultado()`.
- Classes de teste terminam em `Test` e ficam no mesmo pacote da classe testada.
- Testes de integração terminam em `IntegrationTest` (persistência) ou `ApiIntegrationTest` (HTTP).

## 2. Estrutura do método: dados, setup, ação e verify

Cada método de teste tem **dois helpers privados** com o nome do teste:

| Helper | Papel |
|---|---|
| `setupGiven<NomeDoTeste>(...)` | Prepara o **comportamento dos mocks** (`when().thenReturn()`, `doThrow()`...). Em teste de integração, prepara o estado (ex.: grava um registro antes). |
| `verifyGiven<NomeDoTeste>(...)` | Só **verifica o resultado**: `assertThat(...)` ou `verify(mock, times(1)).metodo(...)`. Recebe o resultado (`actual`) e o esperado (`expected`) por parâmetro. |

Exemplo (caso de uso com repositório mockado):

```java
@Test
@DisplayName("Given a valid input When execute is called Then the transaction is saved and returned in reais")
void givenValidInputWhenExecuteIsCalledThenTransactionIsSavedAndReturnedInReais() {
    final var input = defaultPersistTransactionInput().create();
    final var saved = defaultTransaction().create();
    setupGivenValidInputWhenExecuteIsCalledThenTransactionIsSavedAndReturnedInReais(saved);

    final var result = useCase.execute(input);

    verifyGivenValidInputWhenExecuteIsCalledThenTransactionIsSavedAndReturnedInReais(result, saved);
}
```

Regras:

- O corpo do teste segue a ordem **dados → setup → ação → verify**, separados por linha em branco.
- **Exceções:** capture com `catchThrowable(() -> ...)` na ação e passe o `Throwable` ao `verify`.
- **Sem mocks, sem setup:** se o teste não tem nada a preparar (domínio puro), não se cria `setupGiven...` vazio. O `verifyGiven...` continua obrigatório.
- **Um cenário por teste.** Várias verificações de cenários diferentes viram testes separados.
- Trechos de verificação comuns ficam em helpers com outro nome (ex.: `assertNothingPersisted`), chamados de dentro dos `verifyGiven...`.
- Testes parametrizados (`@ParameterizedTest`) seguem o mesmo padrão: um `verifyGiven...` recebendo o valor esperado.

## 3. `final var` sempre

- Variáveis locais dos testes: `final var`.
- Parâmetros dos helpers: `final` (`final TransactionOutput actual, final Transaction expected`).
- Campos montados em `@BeforeEach` (mocks e classe testada) não são `final`; os demais são.

## 4. Injeção de dependência pelo construtor

Sem `@Autowired`. Em testes de integração:

```java
@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-key",
        "app.admin.username=test-admin",
        "app.admin.password=test-admin-password"})
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class JpaTransactionRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:9.6");

    private final TransactionRepository repository;

    JpaTransactionRepositoryIntegrationTest(final TransactionRepository repository) {
        this.repository = repository;
    }
}
```

O contêiner MySQL usa a mesma imagem do `compose.yml`, seguindo o uso de Testcontainers com `@ServiceConnection` do módulo 00 da trilha.

- **Testcontainers 2 (Spring Boot 4):** a classe é `org.testcontainers.mysql.MySQLContainer`, sem o genérico `<?>`. A antiga `org.testcontainers.containers.MySQLContainer` está obsoleta.
- **Propriedades obrigatórias:** todo `@SpringBootTest` declara três propriedades. `spring.ai.openai.api-key=test-key` cria os beans da OpenAI sem `OPENAI_API_KEY`; `app.admin.username=test-admin` e `app.admin.password=test-admin-password` resolvem os placeholders `${APP_ADMIN_USERNAME}` e `${APP_ADMIN_PASSWORD}` do `application.properties`. Sem elas, o contexto nem sobe.
- **Testes `*IT` do curso:** não usam esse atributo. Como já dependem de `OPENAI_API_KEY` exportada, também exigem `APP_ADMIN_USERNAME` e `APP_ADMIN_PASSWORD` exportadas. Sem a chave da OpenAI eles são ignorados e nada disso importa.
- **Testes de API:** além disso, recebem `@AutoConfigureMockMvc` (`org.springframework.boot.webmvc.test.autoconfigure`, do `spring-boot-starter-webmvc-test`) e o `MockMvc` pelo construtor.
- **Segurança nos testes de API:** quando o `MockMvc` é montado à mão com `MockMvcBuilders.webAppContextSetup(...)`, é obrigatório aplicar `.apply(springSecurity())`. Sem isso o MockMvc não passa pela cadeia de filtros do Spring Security, e um teste que deveria exigir autenticação passa sem ela — falso positivo. Os cenários que precisam de uma pessoa autenticada usam `@WithMockUser(roles = "USER")`; os que verificam 401 e 403 não usam a anotação.
- **Docker:** os testes de integração exigem Docker rodando.

## 5. Dados de teste: classes `Support` + Instancio

Dados de teste **não** são criados por métodos soltos dentro das classes de teste. Ficam em classes de apoio em `src/test/java/dio/budgeting/support/`:

```
support
├── TestConstants          constantes compartilhadas (AMOUNT_IN_CENTS, DESCRIPTION...)
├── domain/                TransactionSupport
├── input/                 PersistTransactionInputSupport
├── request/               TransactionRequestSupport
└── entity/                TransactionEntitySupport
```

Cada `Support`:

- tem construtor privado e métodos estáticos `default<Classe>()`;
- devolve `InstancioApi<T>` (e não o objeto pronto), com os campos que importam **fixos** e o resto aleatório;
- usa `supply(field(...), () -> outroSupport().create())` para objetos aninhados.

```java
public final class TransactionSupport {

    private TransactionSupport() { }

    public static InstancioApi<Transaction> defaultTransaction() {
        return Instancio.of(Transaction.class)
                .set(field(Transaction::getDescription), DESCRIPTION)
                .set(field(Transaction::getAmount), AMOUNT_IN_CENTS)
                .set(field(Transaction::getCategory), Category.GROCERIES);
    }
}
```

No teste:

```java
final var transaction = defaultTransaction().create();                       // padrão
final var pharma = defaultTransaction()
        .set(field(Transaction::getCategory), Category.PHARMA).create();     // sobrescrevendo um campo
```

Quando um objeto novo precisar de dados de teste, **crie ou estenda o `Support`** em vez de montar o objeto no teste.

Campos com regra são sempre fixados no `Support`: valor em centavos (positivo), categoria, descrição e datas. Testes de cálculo e validação (conversão de centavos para reais, invariantes do domínio, totais) usam valores determinísticos, porque o resultado esperado é exato.

**Dependência:** `testImplementation 'org.instancio:instancio-core:5.0.2'` (o Spring Boot não gerencia essa versão). Basta o `instancio-core`: os testes usam a API `Instancio.of(...)` diretamente, sem a extensão JUnit (`instancio-junit`). Não faz parte do conteúdo das aulas; foi adotada por decisão do autor e é só de teste.

## 6. O que testar em cada camada

| Camada | Classe (exemplo) | Tipo de teste | Colaboradores |
|---|---|---|---|
| Domínio | `Transaction`, `TransactionOutput` | Unitário: invariantes e conversão de valores | sem mocks |
| Caso de uso | `PersistTransactionUseCase` | Unitário: orquestração e erros de negócio | `TransactionRepository` **mockado** |
| Caso de uso de IA | caso de uso de comando de voz | Unitário: ordem transcrição → chat → voz | modelos de IA **mockados** |
| Entidade JPA | `TransactionEntity` | Unitário: round-trip `from`/`toDomain` | sem mocks |
| Repositório | `JpaTransactionRepository` | Unitário (`*Test`): delegação e mapeamento | `TransactionEntityRepository` **mockado** |
| Repositório | `JpaTransactionRepository` | Integração (`*IntegrationTest`): round-trip real no MySQL | contexto Spring + Testcontainers |
| Controller | `TransactionController` | Unitário: status e delegação | casos de uso **mockados** |
| API | endpoints REST | Integração com MockMvc (`*ApiIntegrationTest`), gerando os snippets do Spring REST Docs | contexto Spring + Testcontainers; `verifyGiven...` recebe o `ResultActions` |

## 7. Checklist para um teste novo

1. `@DisplayName` em `Given ... When ... Then ...` e método com o mesmo nome em camelCase.
2. Dados vindos de um `Support` (`defaultXxx()`), tudo `final var`.
3. `setupGiven<Nome>` só com mocks/estado; `verifyGiven<Nome>` só com verificações; parâmetros `final`.
4. Sem `@Autowired`; sem `new` de colaborador que deveria ser injetado.
5. `./gradlew test` verde, sem precisar de `OPENAI_API_KEY`.
