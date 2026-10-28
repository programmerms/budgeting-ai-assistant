# Proposal: improve-assistant-prompt

## Why

Hoje o arquivo `prompts/system-message.st` tem apenas três frases genéricas: não orienta o modelo a responder de forma curta e em português com valores formatados em reais, não informa a data atual (impedindo que a IA resolva comandos como "quanto gastei hoje?" ou "meus gastos deste mês" ao chamar a ferramenta de período) e não instrui a pedir esclarecimento quando faltar valor ou descrição. Este é o change 6 da seção 5 da proposta inicial.

## What Changes

- **Novo prompt de sistema orientando o assistente financeiro (RF-10):** `src/main/resources/prompts/system-message.st` passa a instruir explicitamente:
  1. Responder sempre em português do Brasil, de forma curta, direta e natural para leitura em voz.
  2. Informar todos os valores monetários em reais (ex.: `R$ 80,00`), lembrando que na ferramenta de registro o valor de entrada é em centavos (`R$ 80,00` → `8000`).
  3. Escolher a categoria mais adequada entre as nove disponíveis (`GROCERIES`, `PHARMA`, `AUTO`, `RESTAURANT`, `SHOPPING`, `TRANSPORT`, `HEALTH`, `LEISURE`, `OTHER`).
  4. Pedir esclarecimento à pessoa usuária, sem chamar a ferramenta de registro, quando faltar o valor ou a descrição do gasto.
  5. Usar a data atual fornecida no contexto (`{currentDate}`) para resolver referências relativas de tempo ("hoje", "ontem", "este mês") nas consultas por período.
- **Injeção da data atual no prompt de sistema em cada chamada (RF-10):** `SpringAiVoiceAssistantGateway` passa a informar a data atual em UTC (`LocalDate.now(ZoneOffset.UTC)`) ao preencher o template do sistema na chamada do `ChatClient`, permitindo que consultas relativas de data funcionem sem precisar que a pessoa diga o ano/mês por extenso.

Requisitos da proposta inicial atendidos: RF-10, mantendo RNF-01, RNF-06 e ampliando RNF-02 com os testes do prompt e da injeção da data atual no `SpringAiVoiceAssistantGatewayTest`.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `voice-assistant`: o requisito "Instruções da IA" é atualizado para refletir que o assistente recebe a data atual e instruções explícitas de idioma (português), respostas curtas, valores em reais e pedido de esclarecimento quando faltar valor ou descrição.

## Impact

- **Código de produção:** `src/main/resources/prompts/system-message.st` e `SpringAiVoiceAssistantGateway` (passando a data atual ao sistema do `ChatClient`).
- **Testes:** atualização de `SpringAiVoiceAssistantGatewayTest` verificando o conteúdo do prompt renderizado (incluindo a data atual em formato ISO `YYYY-MM-DD`, regras de idioma, valores em reais e pedido de esclarecimento).

## Fora do escopo

- Memória de conversa entre requisições (`ChatMemory`): fora do escopo da proposta inicial (seção 4.2).
- Documentação da API com Spring REST Docs e README final do desafio: change 7 (`document-api`).
