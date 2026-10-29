# Tasks

## 1. Guardrail de escopo no prompt

- [x] 1.1 Atualizar `SpringAiVoiceAssistantGatewayTest` para verificar as capacidades financeiras permitidas, a recusa exata, a proteção contra troca de papel e a permissão de descrição sensível em operação financeira; confirmar que o teste direcionado evidencia a ausência dessas regras antes da implementação
- [x] 1.2 Atualizar `prompts/system-message.st` com o escopo permitido, a recusa padrão e os exemplos contrastantes definidos no design; verificar que `./gradlew test --tests dio.budgeting.infrastructure.ai.SpringAiVoiceAssistantGatewayTest` passa

## 2. Verificação real dos limites

- [x] 2.1 Com uma `OPENAI_API_KEY` válida, iniciar a aplicação e exercitar `POST /transactions/ai/text` com duas perguntas gerais fora do escopo, tentativa de troca de papel, consulta financeira e registro com descrição pessoal; verificar recusa nos três primeiros casos e funcionamento financeiro nos dois últimos

## 3. Verificação final

- [x] 3.1 Rodar `./gradlew test` sem `OPENAI_API_KEY` e verificar `BUILD SUCCESSFUL`, mantendo os testes `*IT` da OpenAI desabilitados
