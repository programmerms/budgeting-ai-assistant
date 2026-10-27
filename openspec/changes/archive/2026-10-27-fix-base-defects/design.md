# Design: fix-base-defects

## Context

A motivação está em `proposal.md` (seção Why). O estado atual que importa para a solução:

- `TransactionOutput.from` monta o valor com `BigDecimal.valueOf(amount).setScale(2)` e guarda o resultado num campo `double`. `TransactionResponse` repete o `double`. Por isso a API mostra `8000.0`.
- O construtor de `TransactionController` lê o prompt com `Charset.defaultCharset()`.
- `BudgetingApplicationTests.contextLoads` sobe o contexto completo. No teste não há banco, porque o Docker Compose está como `developmentOnly`, e `spring.ai.openai.api-key=${OPENAI_API_KEY}` não tem valor padrão. A conferência do change 0 confirmou a falha: `Failed to determine a suitable driver class`.
- Não existe nenhum teste unitário nem dependência de teste além do `spring-boot-starter-test`.

## Goals / Non-Goals

**Goals:**
- Valor em reais, exato e com duas casas decimais, sem erro de ponto flutuante.
- `./gradlew test` verde numa máquina com Docker, sem `OPENAI_API_KEY` e sem MySQL iniciado à mão.
- Base do padrão de testes pronta (dependências, `Support`, um exemplo por camada) para os próximos changes só acrescentarem casos.

**Non-Goals:**
- Criar um objeto de valor `Money` no domínio. Resolveria o mesmo problema com mais código, e a regra de conversão cabe no mapeamento de saída que já existe.
- Teste unitário do controller: fica para o change 5, quando o controller deixar de montar o `ChatClient` no construtor.
- Otimizar o tempo da suíte (contêiner compartilhado entre classes).

## Decisions

### 1. Conversão com `BigDecimal.valueOf(amount, 2)` e campo `BigDecimal` na saída

`BigDecimal.valueOf(8000, 2)` cria o número `80.00`: o primeiro argumento é o valor sem vírgula, e o segundo diz quantas casas decimais ele tem. A conta é exata, porque nunca passa por `double`. O tipo do campo de valor em `TransactionOutput` e `TransactionResponse` muda de `double` para `BigDecimal`, e o JSON passa a mostrar `80.00`, e não `80.0`.

- **Alternativa rejeitada:** `amount / 100.0` com `double`. Números como `0.1` não têm representação exata em `double`, e o JSON perderia o zero final (`80.0`). Contraria o RF-01, "duas casas decimais".
- **Onde fica:** no `TransactionOutput.from`, camada `application`, que já é o ponto único de conversão para REST e para as ferramentas da IA. Corrigir ali conserta os dois canais de uma vez, que é o requisito em ambas as specs.
- **Fundamento na trilha:** `BigDecimal` para dinheiro e records como DTOs de saída já estão no projeto base do módulo 05; records para DTOs vêm do README raiz e do módulo 00.

### 2. Prompt lido com `StandardCharsets.UTF_8`

Troca de `Charset.defaultCharset()` por `StandardCharsets.UTF_8` na leitura do `system-message.st`.

- **Contexto honesto:** desde o Java 18 (JEP 400), a codificação padrão da JVM já é UTF-8 em qualquer sistema operacional, salvo se alguém iniciar a aplicação com `-Dfile.encoding=COMPAT`. No Java 25 o risco real é pequeno. A mudança deixa o contrato explícito (RNF-06), em vez de depender de um padrão da JVM.
- **Sem teste automatizado:** reproduzir o defeito exigiria subir uma JVM com outra codificação. A verificação é a revisão do código e o `contextLoads`, que lê o arquivo real.
- **Fundamento na trilha:** leitura do prompt `.st` como `Resource` do módulo 05; só muda o charset.

### 3. Banco de testes com Testcontainers e `@ServiceConnection`

Os testes de integração sobem um MySQL `mysql:9.6`, a mesma imagem do `compose.yml`, num contêiner Docker com `@Testcontainers`, `@Container` e `@ServiceConnection` num campo `static` da classe de teste. O Spring Boot lê do contêiner a URL, o usuário e a senha, sem nenhuma propriedade escrita à mão.

- **Alternativa rejeitada:** H2 em memória. É outro banco, com outro dialeto SQL, e não faz parte da trilha. Um teste que passa no H2 pode falhar no MySQL.
- **Alternativa adiada:** um único contêiner compartilhado por `@TestConfiguration`. É mais rápido, mas foge do exemplo do módulo 00 e do `docs/padrao-de-testes.md`. Com poucas classes de integração, o ganho não compensa agora.
- **Versão:** o Spring Boot 4 traz o Testcontainers 2, que mudou nomes de artefatos (`testcontainers-mysql`, `testcontainers-junit-jupiter`) e de classe (`org.testcontainers.mysql.MySQLContainer`, sem o genérico `<?>`). Os nomes exatos são confirmados pela compilação na tarefa 1.1. Se diferirem do exemplo em `docs/padrao-de-testes.md`, o documento é atualizado.
- **Fundamento na trilha:** módulo 00 (`AddBookToCatalogUseCaseTest`), Testcontainers com `@ServiceConnection`.

### 4. Chave fictícia pelo atributo `properties` do `@SpringBootTest`

Os testes que sobem o contexto declaram `@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")`. Isso basta para os beans da OpenAI serem criados; nenhum teste deste change chama a OpenAI.

- **Alternativa rejeitada:** valor padrão no `application.properties` principal (`${OPENAI_API_KEY:fake}`). A aplicação subiria em produção sem chave e só falharia na primeira chamada à IA, escondendo o erro de configuração.
- **Alternativa rejeitada:** `src/test/resources/application.properties`. Ele esconderia o arquivo principal inteiro nos testes, e todas as outras propriedades teriam de ser copiadas.
- **Alternativa considerada:** perfil `test` com `application-test.properties` e `@ActiveProfiles`. Funciona, mas introduz o conceito de perfis só para uma linha. O atributo `properties` usa a mesma anotação `@SpringBootTest` que a trilha já usa, e a chave fictícia fica visível no próprio teste.
- **Os `*IT` não mudam:** eles não usam o atributo, continuam lendo `${OPENAI_API_KEY}` e seguem ignorados por `@EnabledIfEnvironmentVariable` quando a variável não existe.
- **Fundamento na trilha:** `@SpringBootTest` dos testes do módulo 05.

### 5. `contextLoads` adaptado, não removido

`BudgetingApplicationTests` não é `*IT`, então não está protegido pela regra de "testes do curso ficam como estão". Ele ganha o contêiner MySQL e a chave fictícia e continua sendo o teste que garante que a aplicação inteira sobe.

### 6. Instancio e classes `Support`

Entra `testImplementation 'org.instancio:instancio-core:5.0.2'`. O Spring Boot não gerencia essa versão; a 5.0.2 foi escolhida pelo autor durante o apply, no lugar da "versão estável mais recente" prevista inicialmente. Só o `instancio-core`: os `Support` usam `Instancio.of(...)` diretamente, sem a extensão JUnit. As classes `Support` seguem a seção 5 do padrão: `TestConstants`, `TransactionSupport`, `PersistTransactionInputSupport`, `TransactionRequestSupport` e `TransactionEntitySupport`. Valor (positivo, em centavos), descrição e categoria são sempre fixos; o `id` pode ser aleatório.

- **Fundamento:** exceção aprovada pelo autor (proposta inicial, seção 3.3). É só dependência de teste.

### 7. Testes deste change, por camada

| Camada | Teste | Tipo | Cobre |
|---|---|---|---|
| Saída (application) | `TransactionOutputTest` | unitário, parametrizado | `8000 → 80.00`, `8050 → 80.50`, `1 → 0.01` |
| Caso de uso | `PersistTransactionUseCaseTest`, `ListTransactionsByCategoryUseCaseTest` | unitário, repositório mockado | delegação ao repositório e valor em reais |
| Entidade JPA | `TransactionEntityTest` | unitário | round-trip `from` / `toDomain` |
| Repositório | `JpaTransactionRepositoryTest` | unitário, `TransactionEntityRepository` mockado | delegação e mapeamento |
| Repositório | `JpaTransactionRepositoryIntegrationTest` | integração, MySQL real | salvar e buscar por categoria |
| API | `TransactionApiIntegrationTest` | integração com MockMvc, MySQL real | `POST` e `GET` devolvendo `80.00` e `40.00` |
| Aplicação | `BudgetingApplicationTests` | integração | o contexto inteiro sobe |

- **Fundamento na trilha:** Mockito com `@Mock` / `@InjectMocks` e MockMvc do módulo 01 (`CreateTaskUseCaseTest`, `TaskControllerTest`).
- **MockMvc no Spring Boot 4:** o suporte a testes web foi separado em módulos próprios. Se `@AutoConfigureMockMvc` não estiver disponível só com o `spring-boot-starter-test`, entra o starter de teste do Web MVC, confirmado na tarefa 1.1.

## Risks / Trade-offs

- [Mudança no formato do `amount` (`8000.0` vira `80.00`) quebra quem já consome a API] → o único consumidor hoje é a própria IA, que passa a receber o valor certo. A mudança está marcada como **BREAKING** no proposal e vai para o README no change 7.
- [Os testes passam a exigir Docker] → é o mesmo requisito do `bootRun` e do módulo 00. Fica registrado no `AGENTS.md` e, no change 7, no README.
- [Suíte mais lenta: cada classe de integração sobe o seu MySQL] → aceito por enquanto (decisão 3). Se passar a incomodar, o contêiner compartilhado vira um change próprio.
- [Testcontainers no ambiente do agente: acesso ao socket do Docker] → os testes rodam fora do sandbox, como o `bootRun` do change 0.
- [Instancio preencher campos com valores que quebram regras] → os campos com regra são sempre fixados no `Support` (padrão, seção 5).
