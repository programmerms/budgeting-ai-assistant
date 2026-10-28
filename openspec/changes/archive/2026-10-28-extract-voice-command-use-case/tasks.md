# Tasks: extract-voice-command-use-case

## 1. Gateway de IA e implementação em `infrastructure/ai` (D-02, RF-08)

- [x] 1.1 Criar a interface `application/VoiceAssistantGateway` (`transcribe`, `chat`, `synthesize`) e a implementação `infrastructure/ai/SpringAiVoiceAssistantGateway` (com `TranscriptionModel`, `ChatClient` configurado com `system-message.st` em UTF-8 e `TransactionTools`, e `TextToSpeechModel`); escrever `infrastructure/ai/SpringAiVoiceAssistantGatewayTest` cobrindo os três métodos; verificar que `SpringAiVoiceAssistantGatewayTest` passa

## 2. Casos de uso de comando por voz e por texto (D-02, RF-08, RF-09)

- [x] 2.1 Criar `application/ExecuteVoiceCommandUseCase` (validando recurso de áudio não vazio, chamando `transcribe` → log SLF4J → `chat` → `synthesize`) e escrever `application/ExecuteVoiceCommandUseCaseTest` verificando a ordem das chamadas e a rejeição de áudio vazio/nulo; verificar que `ExecuteVoiceCommandUseCaseTest` passa
- [x] 2.2 Criar `application/input/TextCommandInput`, `application/output/TextCommandOutput` e `application/ExecuteTextCommandUseCase` (validando `input` e `message` com `Assert` e delegando a `voiceAssistantGateway.chat`), criar `support/input/TextCommandInputSupport` e escrever `application/ExecuteTextCommandUseCaseTest`; verificar que `ExecuteTextCommandUseCaseTest` passa

## 3. Endpoint `POST /transactions/ai/text` e `TransactionController` enxuto (D-02, RF-08, RF-09, RF-11)

- [x] 3.1 Criar `infrastructure/http/request/TextCommandRequest` (`@NotBlank String message`), `infrastructure/http/response/TextCommandResponse` e `support/request/TextCommandRequestSupport`; refatorar `TransactionController` para injetar apenas os 6 casos de uso e expor `POST /transactions/ai/text`; escrever `infrastructure/http/TransactionControllerTest` cobrindo todos os endpoints com casos de uso mockados e adicionar os cenários de validação de `POST /transactions/ai/text` em `TransactionApiIntegrationTest`; verificar que ambos os testes passam

## 4. Verificação final

- [x] 4.1 Rodar `./gradlew test` sem `OPENAI_API_KEY`; verificar `BUILD SUCCESSFUL`, todos os testes verdes e os `*IT` ignorados
