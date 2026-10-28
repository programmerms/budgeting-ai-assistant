# Tasks: document-api

## 1. Spring REST Docs e Asciidoctor (RNF-03)

- [x] 1.1 Configurar o plugin `org.asciidoctor.jvm.convert`, as dependências `spring-restdocs-asciidoctor` e `spring-restdocs-mockmvc` e as tasks `test` e `asciidoctor` no `build.gradle`; instrumentar `TransactionApiIntegrationTest` para gerar os snippets (`create-transaction`, `get-transactions-by-category`, `get-transactions-summary` e `get-transactions-by-period`) e criar `src/docs/asciidoc/index.adoc`; verificar que `./gradlew asciidoctor` executa os testes e gera `build/docs/asciidoc/index.html` com sucesso

## 2. README final do desafio em português (RF-01, RNF-04)

- [x] 2.1 Reescrever `README.md` em português do Brasil com os seis tópicos exigidos pelo enunciado (o que o projeto faz, como executar, melhorias implementadas com SDD/OpenSpec, tecnologias utilizadas com justificativa do Instancio, como testar e exemplos `curl` com a convenção de centavos/reais, e o que foi aprendido); verificar que todos os comandos e exemplos documentados estão consistentes com a API

## 3. Verificação final

- [x] 3.1 Rodar `./gradlew test asciidoctor` sem `OPENAI_API_KEY`; verificar `BUILD SUCCESSFUL`, todos os testes verdes, os `*IT` ignorados e a documentação gerada
