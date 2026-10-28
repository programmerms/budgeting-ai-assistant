# Spec Delta: voice-assistant

## ADDED Requirements

### Requirement: Comando em texto com resposta em texto
O sistema SHALL aceitar, em `POST /transactions/ai/text`, uma requisição JSON com o campo `message` contendo o comando em linguagem natural. O comando SHALL ser processado pelo mesmo assistente financeiro e conjunto de ferramentas do fluxo de voz, respondendo HTTP 200 com um JSON `{"message": "..."}` contendo a resposta textual da IA.

#### Scenario: Comando de registro ou consulta em texto
- **WHEN** `POST /transactions/ai/text` recebe `{"message": "gastei 80 reais no mercado"}`
- **THEN** o sistema processa o comando com o assistente financeiro e responde HTTP 200 com `{"message": "..."}` contendo a resposta da IA

#### Scenario: Mensagem vazia ou em branco no comando em texto
- **WHEN** `POST /transactions/ai/text` recebe `{"message": ""}`, `{"message": "   "}` ou `{"message": null}`
- **THEN** o sistema responde HTTP 400 indicando o campo `message` antes de acionar a IA
