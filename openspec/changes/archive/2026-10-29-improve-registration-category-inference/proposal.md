# Proposal: improve-registration-category-inference

## Why

Mesmo quando um comando de registro informa descrição e valor, a IA pode pedir que a pessoa repita a descrição ou escolha uma categoria, em vez de registrar o gasto. Esse comportamento contraria o RF-10 e o contrato atual, segundo o qual a categoria deve ser inferida pela IA.

## What Changes

- Reforçar as instruções do assistente para considerar como descrição o contexto do gasto informado em linguagem natural.
- Orientar a IA a inferir a categoria sem perguntar à pessoa usuária quando descrição e valor estiverem presentes.
- Determinar `OTHER` como categoria de fallback quando nenhuma categoria específica se aplicar com segurança.
- Preservar o pedido de esclarecimento apenas quando faltar efetivamente o valor ou a descrição.
- Cobrir as novas instruções com testes automatizados, mantendo o RNF-01 e ampliando o RNF-02.

Requisito da proposta inicial refinado por este change: RF-10.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `voice-assistant`: o requisito "Instruções da IA" passa a exigir inferência de categoria sem pergunta adicional, fallback para `OTHER` e reconhecimento da descrição presente no comando.

## Impact

- **Prompt do assistente:** `src/main/resources/prompts/system-message.st`.
- **Testes:** `SpringAiVoiceAssistantGatewayTest` e avaliação ao vivo condicionada à `OPENAI_API_KEY`, sem tornar a suíte padrão dependente do provedor.
- **APIs e ferramentas:** nenhum contrato HTTP, enum ou esquema de Tool Calling será alterado.
- **Dependências:** nenhuma nova dependência.

## Fora do escopo

- Tornar o comportamento do modelo completamente determinístico.
- Criar classificação de categoria em código Java ou alterar as categorias existentes.
- Trocar o modelo ou o provedor de IA.
- Adicionar memória de conversa, moderação de conteúdo ou tratamento específico para descrições sensíveis.
