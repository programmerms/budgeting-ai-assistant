# Design: extract-voice-command-use-case

## Context

A motivação está em `proposal.md` (seção Why). O estado atual do código relevante para este change:

- `TransactionController` (`infrastructure/http/TransactionController.java`) recebe `TranscriptionModel`, `Resource systemPrompt`, `ChatClient.Builder`, `TransactionTools` e `TextToSpeechModel` no construtor, monta o `ChatClient` ali mesmo e executa transcrição → chat → voz dentro do método `transcribe(MultipartFile)`.
- A validação de arquivo vazio (`Assert.isTrue(!file.isEmpty(), "Audio file must not be empty")`) foi adicionada temporariamente no controller no change 2 (`add-transaction-validation`), com nota explícita no design daquele change de que acompanharia a extração para o caso de uso no change 5.
- Nenhuma classe em `domain` ou `application` depende de `org.springframework.ai` (regra estabelecida no change 1b e no `AGENTS.md`).
- No módulo `00-domain-driven-design` da trilha (`BookSearchGateway` em `application` implementado por `OpenLibraryBookSearchGateway` em `infrastructure/integration`), integrações externas consumidas pela camada `application` são abstraídas por uma interface `Gateway` na própria camada `application`.

## Goals / Non-Goals

**Goals:**
- Extrair a orquestração de comandos de voz e de texto para casos de uso dedicados em `application` (`ExecuteVoiceCommandUseCase` e `ExecuteTextCommandUseCase`), mantendo `application` sem importar `org.springframework.ai` (D-02, RF-08).
- Expor `POST /transactions/ai/text` recebendo `{"message": "..."}` e devolvendo `{"message": "..."}` (RF-09), preservando intacto o comportamento de `POST /transactions/ai` (RF-11).
- Deixar `TransactionController` dependendo apenas dos casos de uso e coberto por `TransactionControllerTest` (RNF-02).

**Non-Goals:**
- Alterar o texto de `prompts/system-message.st` (fica para o change 6, `improve-assistant-prompt`).
- Incluir metadados de tools chamadas no retorno de `POST /transactions/ai/text` (decisão 4 da seção 7.1 da proposta inicial).

## Decisions

### 1. Gateway `VoiceAssistantGateway` na camada `application` e implementação `SpringAiVoiceAssistantGateway` em `infrastructure/ai`

Seguindo exatamente o padrão `BookSearchGateway` (`application`) / `OpenLibraryBookSearchGateway` (`infrastructure`) do módulo `00-domain-driven-design`, criamos `dio.budgeting.application.VoiceAssistantGateway`:

```java
public interface VoiceAssistantGateway {
    String transcribe(Resource audioResource);

    String chat(String userMessage);

    byte[] synthesize(String text);
}
```

E a implementação `dio.budgeting.infrastructure.ai.SpringAiVoiceAssistantGateway` anotada com `@Service`:

- Recebe `TranscriptionModel`, `@Value("classpath:prompts/system-message.st") Resource systemPrompt`, `ChatClient.Builder`, `TransactionTools` e `TextToSpeechModel` no construtor.
- Monta o `ChatClient` com `.defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))` e `.defaultTools(transactionTools)`.
- Implementa `transcribe(Resource)` delegando a `transcriptionModel.transcribe(audioResource)`, `chat(String)` delegando a `chatClient.prompt().user(userMessage).call().content()` e `synthesize(String)` delegando a `textToSpeechModel.call(text)`.

- **Por que um `VoiceAssistantGateway` em `application`:**
  1. Mantém `application` 100% livre de `org.springframework.ai` (`Resource` é do `spring-core`, já permitido na `application` como `@Service` e `Assert`).
  2. Permite testar `ExecuteVoiceCommandUseCase` e `ExecuteTextCommandUseCase` em unidade mockando o gateway, e testar `SpringAiVoiceAssistantGateway` em unidade mockando `TranscriptionModel`, `ChatClient` e `TextToSpeechModel`.
- **Alternativa rejeitada:** injetar `TranscriptionModel`, `ChatClient` e `TextToSpeechModel` diretamente nos casos de uso em `application`. Violaria a regra do `AGENTS.md` ("Os casos de uso não dependem do Spring AI").
- **Fundamento na trilha:** módulo `00-domain-driven-design` (`BookSearchGateway` em `application` e `OpenLibraryBookSearchGateway` em `infrastructure`, inclusive com `LoggerFactory.getLogger(...)`) e módulo `05-spring-ai` (`TranscriptionModel`, `ChatClient.Builder`, `TextToSpeechModel`).

### 2. Dois casos de uso dedicados: `ExecuteVoiceCommandUseCase` e `ExecuteTextCommandUseCase`

Seguindo a regra da trilha "um caso de uso por capacidade, com `input`/`output`":

1. **`ExecuteVoiceCommandUseCase`:**
   - Recebe `Resource audioResource` (ou valida que não é nulo e `audioResource.contentLength() > 0` / `!isEmpty`, lançando `IllegalArgumentException("Audio file must not be empty")`).
   - Chama `var userMessage = voiceAssistantGateway.transcribe(audioResource);`, registra a transcrição no log (`LOGGER.info("Transcribed voice command: {}", userMessage)`, atendendo à decisão 4 da seção 7.1 da proposta inicial), chama `var assistantReply = voiceAssistantGateway.chat(userMessage);` e retorna `voiceAssistantGateway.synthesize(assistantReply)` (`byte[]`).
2. **`ExecuteTextCommandUseCase`:**
   - Entrada `TextCommandInput(String message)` e saída `TextCommandOutput(String message)`.
   - Valida `Assert.notNull(input, "Text command input must not be null")` e `Assert.hasText(input.message(), "Message must not be blank")`.
   - Chama `var reply = voiceAssistantGateway.chat(input.message())` e retorna `new TextCommandOutput(reply)`.

- **Por que dois casos de uso separados (voz e texto) em vez de um só com flags:** cada um representa uma capacidade clara com tipos de entrada e saída próprios (`Resource -> byte[]` vs `TextCommandInput -> TextCommandOutput`), seguindo o padrão de `PersistTransactionUseCase`, `ListTransactionsByCategoryUseCase`, `SummarizeTransactionsByCategoryUseCase` e `ListTransactionsByPeriodUseCase`.
- **Fundamento na trilha:** módulos `00-domain-driven-design` e `01-spring-web` (casos de uso `@Service` com `execute`, invariantes com `Assert` e `Logger` SLF4J).

### 3. Endpoint `POST /transactions/ai/text` e `TransactionController` enxuto (RF-08, RF-09, RF-11)

Em `infrastructure/http`:

- `request/TextCommandRequest(@NotBlank String message)` com método `toInput()` retornando `new TextCommandInput(message)`.
- `response/TextCommandResponse(String message)` com `static TextCommandResponse from(TextCommandOutput output)`.
- Em `TransactionController`:
  - Construtor recebe apenas os 6 casos de uso (`PersistTransactionUseCase`, `ListTransactionsByCategoryUseCase`, `SummarizeTransactionsByCategoryUseCase`, `ListTransactionsByPeriodUseCase`, `ExecuteVoiceCommandUseCase`, `ExecuteTextCommandUseCase`).
  - `@PostMapping(value = "/ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "audio/mp3")` valida `Assert.isTrue(!file.isEmpty(), "Audio file must not be empty")`, chama `byte[] audio = executeVoiceCommandUseCase.execute(file.getResource())` e monta o `ResponseEntity<Resource>` com `Content-Disposition: attachment; filename="audio.mp3"`.
  - `@PostMapping("/ai/text")` recebe `@RequestBody @Valid TextCommandRequest request`, chama `executeTextCommandUseCase.execute(request.toInput())` e devolve `TextCommandResponse.from(output)`.

- **Fundamento na trilha:** módulo `01-spring-web` (`TaskController` com `@RequestBody @Valid` e DTOs de request/response em `record`) e módulo `05-spring-ai` (`POST /transactions/ai`).

### 4. Testes por camada

| Camada | Classe de teste | O que verifica |
|---|---|---|
| Caso de uso | `ExecuteVoiceCommandUseCaseTest` | Ordem `transcribe -> chat -> synthesize` no `VoiceAssistantGateway` e rejeição de recurso nulo/vazio |
| Caso de uso | `ExecuteTextCommandUseCaseTest` | Delegação a `voiceAssistantGateway.chat(message)` e `IllegalArgumentException` para input nulo ou mensagem em branco |
| Infraestrutura IA | `SpringAiVoiceAssistantGatewayTest` | Delegação a `TranscriptionModel`, `ChatClient` e `TextToSpeechModel` e leitura do prompt em UTF-8 |
| Controller | `TransactionControllerTest` | Teste unitário dos 6 endpoints com todos os casos de uso mockados |
| API (integração) | `TransactionApiIntegrationTest` | `POST /transactions/ai/text` com mensagem em branco/nula retorna HTTP 400 indicando o campo `message`; com `VoiceAssistantGateway` ou chat acionado retorna 200 |

## Risks / Trade-offs

- [`POST /transactions/ai/text` em `TransactionApiIntegrationTest` sem `OPENAI_API_KEY`] → Nos testes de integração sem chave real da OpenAI, testamos a validação HTTP 400 de `POST /transactions/ai/text` no `TransactionApiIntegrationTest` e o fluxo de sucesso do endpoint no `TransactionControllerTest` (com caso de uso mockado), mantendo `./gradlew test` 100% independente de `OPENAI_API_KEY` (RNF-01).
