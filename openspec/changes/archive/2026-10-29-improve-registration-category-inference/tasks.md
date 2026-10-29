# Tasks

## 1. Instruções de registro e teste do prompt

- [x] 1.1 Atualizar `SpringAiVoiceAssistantGatewayTest` para verificar que o prompt enviado ao `ChatClient` reconhece descrição em linguagem natural, proíbe pedir categoria e define `OTHER` como fallback; confirmar que `./gradlew test --tests dio.budgeting.infrastructure.ai.SpringAiVoiceAssistantGatewayTest` evidencia a ausência dessas regras antes da implementação
- [x] 1.2 Atualizar `prompts/system-message.st` com as regras de inferência definidas no design e verificar que `./gradlew test --tests dio.budgeting.infrastructure.ai.SpringAiVoiceAssistantGatewayTest` passa
- [x] 1.3 Adicionar ao prompt uma regra para preservar descrições coloquiais ou sensíveis e um exemplo completo do cenário reportado; verificar o conteúdo em `SpringAiVoiceAssistantGatewayTest` e confirmar pelo `POST /transactions/ai/text`, com chave real, que a ferramenta registra `100000` centavos em `OTHER` sem pedir esclarecimento

## 2. Verificação final

- [x] 2.1 Rodar `./gradlew test` sem `OPENAI_API_KEY` e verificar `BUILD SUCCESSFUL`, mantendo os testes `*IT` da OpenAI desabilitados
