# Tasks: improve-assistant-prompt

## 1. Novo prompt de sistema e injeção da data atual (RF-10)

- [x] 1.1 Atualizar `src/main/resources/prompts/system-message.st` com a data atual (`{currentDate}`) e as regras do RF-10 (respostas curtas em português, valores em reais, conversão para centavos na tool de registro, categorias disponíveis e pedido de esclarecimento quando faltar valor ou descrição), atualizar `SpringAiVoiceAssistantGateway` para preencher `{currentDate}` com `LocalDate.now(ZoneOffset.UTC)` em `chat(...)` e atualizar `SpringAiVoiceAssistantGatewayTest` verificando o prompt de sistema enviado ao `ChatClient`; verificar que `SpringAiVoiceAssistantGatewayTest` passa

## 2. Verificação final

- [x] 2.1 Rodar `./gradlew test` sem `OPENAI_API_KEY`; verificar `BUILD SUCCESSFUL`, todos os testes verdes e os `*IT` ignorados
