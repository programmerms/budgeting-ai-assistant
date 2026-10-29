# Design: improve-registration-category-inference

## Context

Veja `proposal.md` para a motivação. Hoje o prompt manda escolher a categoria mais adequada e pedir esclarecimento quando faltar valor ou descrição, mas não declara que a categoria nunca precisa ser fornecida pela pessoa nem define um fallback. Diante de uma descrição ambígua, o modelo pode interpretar a categoria como dado ausente e perguntar também por informações que já constam no comando.

O fluxo usa o mesmo `SpringAiVoiceAssistantGateway` e o mesmo prompt para comandos de voz e texto. O comportamento do provedor é probabilístico, enquanto `./gradlew test` precisa continuar independente de `OPENAI_API_KEY`.

## Goals / Non-Goals

**Goals:**
- Tornar explícita a diferença entre dados obrigatórios fornecidos pela pessoa (descrição e valor) e dado inferido pela IA (categoria).
- Definir `OTHER` como fallback para evitar perguntas de categoria.
- Verificar deterministicamente que as novas regras chegam ao `ChatClient`.

**Non-Goals:**
- Implementar um classificador determinístico em Java.
- Garantir que toda execução futura de um modelo externo produza a mesma chamada de ferramenta.
- Alterar o contrato da ferramenta, o enum `Category` ou os endpoints HTTP.

## Decisions

### 1. Reforçar as regras no template de sistema existente

O arquivo `prompts/system-message.st` será ajustado para declarar:

1. A descrição pode ser uma frase em linguagem natural que identifique o contexto do gasto; não precisa ser um rótulo curto.
2. Quando descrição e valor estiverem presentes, a IA deve chamar `persist-transaction` sem pedir confirmação da descrição ou da categoria.
3. A categoria é sempre inferida pela IA e nunca solicitada à pessoa usuária.
4. Quando nenhuma categoria específica se aplicar com segurança, a IA deve usar `OTHER`.
5. O pedido de esclarecimento permanece restrito à ausência real de valor ou descrição.
6. Um exemplo completo do cenário reportado demonstra a chamada esperada da ferramenta, incluindo descrição preservada, conversão para centavos e categoria `OTHER`.

Manter a decisão no prompt preserva a responsabilidade do modelo de interpretar linguagem natural e usar ferramentas. Esse é o padrão de prompt `.st`, `ChatClient` e Tool Calling ensinado no módulo `05-spring-ai`.

**Alternativa rejeitada — classificador em Java:** exigiria duplicar interpretação de linguagem natural em regras frágeis ou introduzir tecnologia não ensinada na trilha. Também misturaria a adaptação de IA com regras que não são invariantes do domínio, contrariando a separação de camadas do módulo `00-domain-driven-design`.

### 2. Usar `OTHER` como fallback em vez de pedir a categoria

`OTHER` já faz parte das nove categorias aceitas e existe justamente para contextos sem correspondência específica. O fallback evita bloquear um registro válido sem alterar o enum ou o esquema da ferramenta.

Essa decisão reutiliza o enum e o Tool Calling do módulo `05-spring-ai` e mantém a persistência por categoria como `@Enumerated(EnumType.STRING)`, padrão do módulo `02-spring-data`.

**Alternativa rejeitada — escolher sempre `LEISURE` em descrições ambíguas:** produziria classificação arbitrária para contextos que podem não representar lazer. `OTHER` é a opção conservadora prevista pelo domínio.

### 3. Testar o contrato do prompt de forma determinística

`SpringAiVoiceAssistantGatewayTest` continuará capturando a mensagem enviada a `.system(...)` e passará a verificar as orientações de: reconhecer descrição em linguagem natural, não perguntar categoria e usar `OTHER` como fallback. O teste seguirá o padrão `Given/When/Then`, helpers `setupGiven...` e `verifyGiven...` definido em `docs/padrao-de-testes.md`.

O fundamento é o teste unitário com Mockito ensinado no módulo `01-spring-web`, aplicado ao `ChatClient` do módulo `05-spring-ai`. A suíte padrão não fará chamada real à OpenAI, preservando o RNF-01.

Uma verificação manual do cenário reportado poderá ser feita pelo endpoint de texto com uma chave real. Ela não será uma asserção obrigatória da suíte porque respostas de LLM não são determinísticas.

## Risks / Trade-offs

- [O modelo ainda pode descumprir o prompt em alguma execução] → Usar instruções explícitas e fallback, validar manualmente o cenário e registrar que prompt engineering reduz, mas não elimina, variabilidade.
- [Descrições sensíveis podem receber classificação conservadora] → Preservar o texto informado como descrição e usar `OTHER` quando não houver correspondência segura.
- [Uma regra muito rígida pode registrar comandos realmente incompletos] → Manter explícita a obrigação de pedir esclarecimento quando valor ou descrição estiverem ausentes.

## Migration Plan

Não há migração de dados ou esquema. A implantação exige apenas publicar o novo recurso de prompt e reiniciar a aplicação. O rollback consiste em restaurar o template e o teste anteriores.
