# Proposal: document-api

## Why

O projeto já possui todas as funcionalidades, validações, ferramentas de IA e testes implementados, mas ainda não gera documentação da API a partir dos testes com Spring REST Docs (RNF-03) e o `README.md` ainda é o arquivo original em inglês do curso, sem os seis tópicos exigidos pelo enunciado do desafio nem a documentação da convenção de centavos na entrada e reais na saída (RF-01, RNF-04). Este é o change 7 (último change) da seção 5 da proposta inicial.

## What Changes

- **Documentação automatizada da API com Spring REST Docs + Asciidoctor (RNF-03):**
  - Configuração do plugin `org.asciidoctor.jvm.convert` e das dependências `spring-restdocs-mockmvc` e `spring-restdocs-asciidoctor` no `build.gradle`, seguindo o módulo `01-spring-web`.
  - Geração dos snippets no `TransactionApiIntegrationTest` cobrindo os endpoints REST (`POST /transactions`, `GET /transactions/{category}`, `GET /transactions/summary` e `GET /transactions?from=...&to=...`).
  - Criação de `src/docs/asciidoc/index.adoc` consolidando os snippets gerados pelo teste em uma documentação HTML completa via `./gradlew asciidoctor`.
- **README final em português (RF-01, RNF-04):**
  - Reescrita de `README.md` em português do Brasil contendo todos os seis itens exigidos pelo enunciado do desafio (o que o projeto faz, como executar, melhorias implementadas com Spec-Driven Development / OpenSpec, tecnologias utilizadas incluindo a justificativa do Instancio nos testes, como testar os fluxos REST/voz/texto com exemplos `curl` e convenção de valores em centavos/reais, e o que foi aprendido).

Requisitos da proposta inicial atendidos: RF-01 (documentação da convenção monetária no README), RNF-03 e RNF-04, mantendo RNF-01 e RNF-05.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma. Este change adiciona apenas geração de documentação a partir dos testes e documentação no `README.md`, sem alterar o comportamento observável da aplicação (`skip_specs: true`).

## Impact

- **Build e dependências:** `build.gradle` ganha o plugin `org.asciidoctor.jvm.convert` (`4.0.5`) e as dependências `spring-restdocs-asciidoctor` e `spring-restdocs-mockmvc`.
- **Testes:** `TransactionApiIntegrationTest` passa a configurar `RestDocumentationExtension` / `@AutoConfigureRestDocs` e documentar os endpoints REST gerando snippets em `build/generated-snippets`.
- **Documentação:** novo `src/docs/asciidoc/index.adoc` e atualização completa de `README.md`.

## Fora do escopo

- Alterar qualquer regra de negócio, endpoint, contrato JSON ou ferramenta da IA.
- Documentar endpoints OpenAI ao vivo que dependam de `OPENAI_API_KEY` durante o `./gradlew test` (para manter RNF-01 intacto).
