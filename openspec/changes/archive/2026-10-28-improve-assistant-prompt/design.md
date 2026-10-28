# Design: improve-assistant-prompt

## Context

A motivação está em `proposal.md` (seção Why). O estado atual do projeto relevante para este change:

- `src/main/resources/prompts/system-message.st` contém apenas 3 linhas:
  ```text
  Você é um assistente financeiro.
  Sua tarefa é extrair dados de transações e usar as ferramentas disponíveis para manipular transações.
  Ao registrar uma transação, escolha a categoria que melhor se adapta ao contexto.
  ```
- `SpringAiVoiceAssistantGateway` lê `system-message.st` em UTF-8 no construtor e passa a string para `chatClientBuilder.defaultSystem(...)`.
- O requisito RF-10 da proposta inicial pede que o prompt de sistema oriente respostas curtas, em português, com valores em reais, conheça a data atual (para consultas por período) e peça esclarecimento quando faltar valor ou descrição.

## Goals / Non-Goals

**Goals:**
- Evoluir `prompts/system-message.st` para cobrir todos os pontos do RF-10 (idioma pt-BR, respostas curtas adequadas para áudio/texto, valores em reais na resposta vs centavos na entrada da tool de registro, categorias disponíveis, data atual `{currentDate}` e pedido de esclarecimento quando faltar valor ou descrição).
- Preencher `{currentDate}` com a data atual em UTC (`LocalDate.now(ZoneOffset.UTC)`) em `SpringAiVoiceAssistantGateway.chat`, garantindo que mesmo que a aplicação fique rodando por vários dias a data enviada ao modelo seja sempre a do dia da requisição.
- Manter compatibilidade com o requisito existente "Leitura das instruções em UTF-8" (que cita a frase inicial `"Você é um assistente financeiro."`).

**Non-Goals:**
- Adicionar memória de conversa (`ChatMemory`).

## Decisions

### 1. Conteúdo estruturado em `prompts/system-message.st` com placeholder `{currentDate}`

Atualizamos `src/main/resources/prompts/system-message.st` mantendo a frase inicial `"Você é um assistente financeiro."` e acrescentando as regras claras:

```text
Você é um assistente financeiro.
Sua tarefa é ajudar a pessoa usuária a registrar e consultar gastos usando as ferramentas disponíveis.
Data atual (UTC): {currentDate}.

Regras obrigatórias:
- Responda sempre em português do Brasil, de forma curta, direta e natural para ser ouvida em áudio.
- Exiba todos os valores monetários em reais com duas casas decimais (por exemplo, R$ 80,00).
- Ao chamar a ferramenta de registro (persist-transaction), converta o valor informado em reais para centavos (por exemplo, R$ 80,00 = 8000).
- Escolha a categoria que melhor se adapta ao contexto entre: GROCERIES, PHARMA, AUTO, RESTAURANT, SHOPPING, TRANSPORT, HEALTH, LEISURE, OTHER.
- Se a pessoa pedir para registrar um gasto sem informar o valor ou a descrição, não chame a ferramenta de registro e peça esclarecimento de forma curta.
- Para consultas por período (hoje, ontem, esta semana, este mês), use a data atual ({currentDate}) para calcular as datas inicial (from) e final (to) no formato YYYY-MM-DD.
```

- **Por que manter `"Você é um assistente financeiro."` na primeira linha:** preserva a compatibilidade com o requisito e cenário existentes na spec `voice-assistant` ("Leitura das instruções em UTF-8").
- **Fundamento na trilha:** arquivo de template `.st` em `src/main/resources/prompts/system-message.st` lido como `Resource` em UTF-8 do módulo `05-spring-ai`.

### 2. Substituição de `{currentDate}` a cada chamada em `SpringAiVoiceAssistantGateway`

Em `SpringAiVoiceAssistantGateway`, guardamos o template lido em UTF-8 no construtor (`this.systemPromptTemplate = systemPrompt.getContentAsString(StandardCharsets.UTF_8)`) e, em `chat(String userMessage)`, passamos o prompt com a data atual resolvida no momento da chamada (`systemPromptTemplate.replace("{currentDate}", LocalDate.now(ZoneOffset.UTC).toString())`) para o `chatClient`:

```java
var systemMessage = systemPromptTemplate.replace("{currentDate}", LocalDate.now(ZoneOffset.UTC).toString());
return chatClient.prompt()
        .system(systemMessage)
        .user(userMessage)
        .call()
        .content();
```

- **Por que resolver `LocalDate.now(ZoneOffset.UTC)` em `chat(...)` em vez de apenas no construtor:** o bean `@Service` é singleton; se a data fosse fixada no construtor, ficaria congelada no dia em que a aplicação subiu. Resolver em `chat(...)` garante a data certa em qualquer dia e permite verificar facilmente no teste unitário o texto enviado a `.system(...)`.
- **Fundamento na trilha:** API fluente `ChatClient` do módulo `05-spring-ai`.

### 3. Testes unitários

Em `SpringAiVoiceAssistantGatewayTest`:
- Verificamos que `system-message.st` é lido em UTF-8 e que, ao chamar `gateway.chat(USER_MESSAGE)`, o método `.system(...)` do `ChatClient` recebe o prompt contendo `"Você é um assistente financeiro."`, a data atual no formato ISO (`LocalDate.now(ZoneOffset.UTC).toString()`), a orientação de responder em português, valores em reais e pedido de esclarecimento quando faltar valor ou descrição.

## Risks / Trade-offs

- [Comportamento do LLM em produção continua probabilístico] → O teste unitário garante de forma determinística que todas as instruções e a data atual chegam ao `ChatClient` em UTF-8, enquanto a avaliação fim a fim com a OpenAI fica nos testes condicionais à chave (`OPENAI_API_KEY`).
