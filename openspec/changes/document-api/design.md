# Design: document-api

## Context

A motivação está em `proposal.md` (seção Why). O estado atual do projeto relevante para este change:

- `build.gradle` ainda não declara o plugin `org.asciidoctor.jvm.convert` nem as dependências `spring-restdocs-mockmvc` e `spring-restdocs-asciidoctor`.
- `TransactionApiIntegrationTest` já testa todos os endpoints REST (`POST /transactions`, `GET /transactions/{category}`, `GET /transactions/summary` e `GET /transactions?from=...&to=...`) com `MockMvc` e MySQL via Testcontainers.
- O módulo `01-spring-web` da trilha (`build.gradle`, `TaskControllerTest` e `src/docs/asciidoc/index.adoc`) demonstra a configuração exata de Spring REST Docs + Asciidoctor.
- `README.md` ainda está em inglês com o texto original do curso.

## Goals / Non-Goals

**Goals:**
- Configurar Spring REST Docs + Asciidoctor exatamente como no módulo `01-spring-web` e gerar os snippets a partir dos testes de integração existentes em `TransactionApiIntegrationTest` (RNF-03).
- Produzir `src/docs/asciidoc/index.adoc` e validar que `./gradlew asciidoctor` gera a documentação HTML sem erros.
- Escrever o `README.md` final em português do Brasil cobrindo os seis itens do enunciado, os exemplos `curl` e a convenção monetária de centavos na entrada e reais na saída (RF-01, RNF-04).

**Non-Goals:**
- Adicionar OpenAPI/Swagger/Springdoc (fora do conteúdo ministrado na trilha; a trilha usa Spring REST Docs).
- Alterar código de produção (`src/main`).

## Decisions

### 1. Configuração do Spring REST Docs e Asciidoctor no `build.gradle` (RNF-03)

Replicamos a configuração do `01-spring-web/build.gradle`:

- Plugin: `id 'org.asciidoctor.jvm.convert' version '4.0.5'`
- `ext { snippetsDir = file('build/generated-snippets') }`
- `configurations { asciidoctorExtensions }`
- `asciidoctorj { version = "3.0.0" }`
- Dependências:
  - `asciidoctorExtensions 'org.springframework.restdocs:spring-restdocs-asciidoctor'`
  - `testImplementation 'org.springframework.restdocs:spring-restdocs-mockmvc'`
- Tasks:
  - `test { outputs.dir snippetsDir }`
  - `asciidoctor { configurations "asciidoctorExtensions"; inputs.dir snippetsDir; dependsOn test }`

- **Fundamento na trilha:** idêntico ao `build.gradle` do módulo `01-spring-web`.

### 2. Geração de snippets em `TransactionApiIntegrationTest`

Em `TransactionApiIntegrationTest`, adicionamos `@AutoConfigureRestDocs` (do módulo `spring-boot-starter-webmvc-test` / Spring REST Docs, compatível com `@AutoConfigureMockMvc` já usado na classe, ou `RestDocumentationExtension` como no `TaskControllerTest`) e adicionamos `.andDo(document(...))` nos testes de sucesso de cada endpoint nos helpers `verifyGiven...`:

- `create-transaction` (`POST /transactions`): `requestFields` (`description`, `category`, `amount`) e `responseFields` (`id`, `category`, `description`, `amount`, `createdAt`).
- `get-transactions-by-category` (`GET /transactions/{category}`): `pathParameters` (`category`) e `responseFields` (`[].id`, `[].category`, `[].description`, `[].amount`, `[].createdAt`).
- `get-transactions-summary` (`GET /transactions/summary?category=...`): `queryParameters` (`category`) e `responseFields` (`category`, `total`, `count`).
- `get-transactions-by-period` (`GET /transactions?from=...&to=...`): `queryParameters` (`from`, `to`) e `responseFields` (`[].id`, `[].category`, `[].description`, `[].amount`, `[].createdAt`).

- **Fundamento na trilha:** módulo `01-spring-web` (`TaskControllerTest`) e seção 6 de `docs/padrao-de-testes.md` ("API: Integração com MockMvc (`*ApiIntegrationTest`), gerando os snippets do Spring REST Docs").

### 3. Estrutura de `src/docs/asciidoc/index.adoc` e `README.md` (RF-01, RNF-03, RNF-04)

- **`src/docs/asciidoc/index.adoc`**: segue a estrutura do `01-spring-web/src/docs/asciidoc/index.adoc` (Introdução, Modelo de Dados e Categorias, seções para cada endpoint incluindo `curl-request.adoc`, `http-response.adoc`, `request-fields.adoc`/`path-parameters.adoc`/`query-parameters.adoc` e `response-fields.adoc`, e tabela de Códigos de Status).
- **`README.md`**: escrito em português do Brasil, estruturado com:
  1. **O que o projeto faz** (visão geral, fluxo de voz e de texto com Tool Calling).
  2. **Tecnologias utilizadas** (Java 25, Spring Boot 4, Spring AI 2 com OpenAI, Spring Data JPA, MySQL, Testcontainers, Spring REST Docs e justificativa explícita do Instancio como única exceção de teste).
  3. **Melhorias implementadas** (resumo do diagnóstico D-01 a D-07 e dos changes evoluídos com Spec-Driven Development / OpenSpec).
  4. **Convenção de valores monetários e categorias** (entrada em centavos `long`, saída em reais `BigDecimal` com duas casas decimais, e as 9 categorias).
  5. **Como executar a aplicação** (variável `OPENAI_API_KEY`, Docker Compose automático no `./gradlew bootRun`).
  6. **Como testar e usar a API** (`./gradlew test` sem necessidade de `OPENAI_API_KEY`, `./gradlew asciidoctor` e exemplos completos de `curl` para todos os endpoints REST, comando de texto e comando de voz).
  7. **O que foi aprendido durante o desafio** (integração de IA respeitando camadas DDD/Clean Architecture, simetria entre adaptadores HTTP e Tool Calling, invariantes no domínio protegendo chamadas da IA, e evolução guiada por especificações com OpenSpec).

## Risks / Trade-offs

- [Incompatibilidade de `@AutoConfigureRestDocs` no Spring Boot 4] → Caso o pacote de `@AutoConfigureRestDocs` tenha mudado no Spring Boot 4, usamos `@ExtendWith(RestDocumentationExtension.class)` + `MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(documentationConfiguration(restDocumentation)).build()`, que é exatamente a forma usada em `TaskControllerTest` do módulo `01-spring-web`.
