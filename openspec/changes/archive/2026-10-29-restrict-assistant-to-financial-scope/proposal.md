# Proposal: restrict-assistant-to-financial-scope

## Why

O assistente responde perguntas gerais sem relação com finanças, desviando do propósito do projeto. Ele deve se limitar a registrar e consultar gastos, recusando de forma curta qualquer solicitação fora desse escopo.

## What Changes

- Restringir as instruções da IA às capacidades financeiras disponíveis: registrar despesas e consultar gastos por categoria, resumo ou período.
- Orientar uma recusa curta e padronizada, em português, sem responder ao conteúdo de solicitações fora do escopo.
- Recusar tentativas de mudar o papel do assistente, ignorar suas instruções ou obter respostas sobre outros assuntos.
- Preservar o processamento normal de descrições sensíveis quando elas fizerem parte de um registro ou consulta financeira válida.
- Cobrir as regras com testes determinísticos do prompt e verificações reais condicionadas à `OPENAI_API_KEY`.

Este change refina o RF-10 e mantém o RNF-01, ampliando o RNF-02.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `voice-assistant`: o requisito "Instruções da IA" passa a limitar o assistente ao registro e à consulta de gastos, definir a recusa de assuntos externos e preservar despesas válidas com descrição sensível.

## Impact

- **Prompt do assistente:** `src/main/resources/prompts/system-message.st`.
- **Testes:** `SpringAiVoiceAssistantGatewayTest` e verificação manual pelo endpoint de texto com chave real.
- **APIs, ferramentas e domínio:** nenhum contrato HTTP, esquema de Tool Calling, categoria ou regra de persistência será alterado.
- **Dependências:** nenhuma nova dependência.

## Fora do escopo

- Bloquear uma descrição sensível quando ela fizer parte de uma operação financeira válida.
- Adicionar serviço externo de moderação, classificador em Java ou nova dependência.
- Trocar o modelo ou o provedor de IA.
- Garantir matematicamente que um modelo probabilístico nunca descumpra as instruções; o objetivo é reduzir o desvio com regras e exemplos explícitos e verificá-lo nos cenários definidos.
