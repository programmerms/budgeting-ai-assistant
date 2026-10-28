# Proposal: extract-voice-command-use-case

## Why

Hoje o `TransactionController` monta o `ChatClient` no construtor e orquestra diretamente a transcrição de áudio, o chat com Tool Calling e a síntese de voz (D-02), violando a regra de camadas da trilha ("o controller só adapta HTTP e chama um caso de uso") e impedindo o teste unitário do controller. Além disso, para testar comandos da IA hoje é obrigatório enviar áudio em `POST /transactions/ai`, faltando um endpoint em texto (`POST /transactions/ai/text`) que permita exercitar o fluxo de Tool Calling com JSON simples (RF-09). Este é o change 5 da seção 5 da proposta inicial.

## What Changes

- **Extração dos casos de uso de comando (D-02, RF-08, RF-11):** a orquestração de comandos por voz e por texto sai do `TransactionController` para os casos de uso `ExecuteVoiceCommandUseCase` e `ExecuteTextCommandUseCase` em `application`, que conversam com a infraestrutura de IA por meio de um gateway na camada `application` (`VoiceAssistantGateway`, seguindo o padrão `BookSearchGateway` do módulo `00-domain-driven-design`) implementado em `infrastructure/ai`, mantendo `application` sem dependência do Spring AI.
- **Novo endpoint de comando em texto (RF-09):** `POST /transactions/ai/text`, que recebe JSON `{"message": "..."}` validado com `@NotBlank` e devolve HTTP 200 com `{"message": "..."}` contendo a resposta da IA após acionar as tools necessárias (decisão 4 da seção 7.1 da proposta inicial).
- **Controller enxuto e testável em unidade (D-02, RNF-02):** `TransactionController` passa a depender exclusivamente dos casos de uso da aplicação, sem injetar `TranscriptionModel`, `ChatClient` ou `TextToSpeechModel`, viabilizando o teste unitário `TransactionControllerTest`.

Defeito tratado (detalhes em `docs/proposta-inicial.md`, seção 2):

- D-02: orquestração de IA dentro do controller

Requisitos da proposta inicial atendidos: RF-08, RF-09 e RF-11, além de manter RF-03, RNF-01, RNF-06 e ampliar RNF-02 com os testes unitários dos casos de uso de comando, do gateway de IA, do controller e de integração do novo endpoint.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `voice-assistant`: adição do requisito "Comando em texto com resposta em texto" (`POST /transactions/ai/text` recebendo `{"message": "..."}`, validando mensagem não vazia com HTTP 400 e devolvendo `{"message": "..."}` com a resposta da IA).

## Impact

- **Código de produção:**
  - `application`: gateway `VoiceAssistantGateway` (com métodos `transcribe`, `chat` e `synthesize`), `ExecuteVoiceCommandUseCase` (fluxo áudio → transcrição → chat → síntese de voz, incluindo validação de áudio não vazio e log da transcrição conforme decisão 4 da seção 7.1) e `ExecuteTextCommandUseCase` (fluxo mensagem → chat → resposta, com `TextCommandInput` e `TextCommandOutput`).
  - `infrastructure/ai`: `SpringAiVoiceAssistantGateway` implementando `VoiceAssistantGateway` com `TranscriptionModel`, `ChatClient` (lendo `prompts/system-message.st` em UTF-8 e registrando `TransactionTools`) e `TextToSpeechModel`.
  - `infrastructure/http`: `TransactionController` enxuto (delegando `POST /transactions/ai` e `POST /transactions/ai/text` aos casos de uso), mais `TextCommandRequest` e `TextCommandResponse`.
- **Testes:** novos testes unitários `ExecuteVoiceCommandUseCaseTest`, `ExecuteTextCommandUseCaseTest`, `SpringAiVoiceAssistantGatewayTest`, `TransactionControllerTest` (cobrindo todos os endpoints com casos de uso mockados) e novos cenários em `TransactionApiIntegrationTest` para `POST /transactions/ai/text`.

## Fora do escopo

- Alterar o conteúdo do prompt `prompts/system-message.st` (idioma, tamanho da resposta, data atual ou pedido de esclarecimento): change 6 (`improve-assistant-prompt`).
- Retornar a lista de tools acionadas na resposta de `POST /transactions/ai/text` (decisão 4 da seção 7.1 da proposta inicial: retorna apenas `{"message": "..."}`).
- Documentação com Spring REST Docs e README final: change 7 (`document-api`).
