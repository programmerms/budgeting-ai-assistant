# Design: baseline-specs

## Context

O código em `src/` é uma cópia idêntica do módulo `05-spring-ai` da trilha. Ele tem dois casos de uso expostos como ferramentas da IA (`PersistTransactionUseCase` e `ListTransactionsByCategoryUseCase`), um controller com três endpoints e a orquestração de IA (transcrição, `ChatClient` e voz) dentro do próprio controller (D-02). Como o D-02 não muda o que o cliente da API vê, ele fica registrado só aqui e é corrigido no change 5 (`extract-voice-command-use-case`). A motivação do change está em `proposal.md` (seção Why).

Como o change não altera código, o "como" aqui é **como as specs foram levantadas e como serão conferidas**.

## Goals / Non-Goals

**Goals:**
- Specs fiéis ao comportamento observável de hoje, com cada defeito marcado pelo seu ID (D-01 a D-07).
- Cada requisito com pelo menos um cenário que possa virar teste nos próximos changes.
- Resultado real de `./gradlew test` registrado, confirmando ou refutando D-04.

**Non-Goals:**
- Escrever testes automatizados para os cenários. Eles começam no change 1, quando a suíte passar a rodar sem infraestrutura externa.
- Especificar detalhes internos (classes, bibliotecas, modelos da OpenAI). Eles aparecem neste design, não nas specs.

## Decisions

### 1. As specs descrevem o comportamento real, inclusive os defeitos

Os requisitos com defeito (por exemplo, "Representação do valor na saída (D-01)") descrevem o que o sistema faz hoje, e não o que deveria fazer.

- **Por quê:** o change 1 (`fix-base-defects`) vai trazer esses mesmos requisitos em `## MODIFIED Requirements`, com o comportamento corrigido. O diff entre a spec antiga e a nova mostra a correção de forma explícita, que é o objetivo da decisão 5 da seção 7.1 da proposta inicial.
- **Alternativa considerada:** especificar já o comportamento desejado e tratar os defeitos como "não conformidades". Rejeitada porque a spec principal ficaria mentindo sobre o sistema até o change 1 ser arquivado, e o antes e o depois não ficariam registrados.
- **Fundamento na trilha:** o comportamento descrito é exatamente o do módulo `05-spring-ai`.

### 2. Duas capabilities: `transactions` e `voice-assistant`

- `transactions`: domínio, regras e API REST de transações.
- `voice-assistant`: fluxo de áudio, ferramentas da IA e instruções do modelo.
- **Por quê:** é a divisão da seção 5 da proposta inicial e acompanha a separação de responsabilidades da trilha (README raiz e módulo 00): o registro de transações existe sem IA, e a IA é um canal a mais que reaproveita os mesmos casos de uso.
- **Onde ficam as ferramentas:** na `voice-assistant`, porque descrevem o que a IA pode fazer. As regras que as ferramentas herdam (falta de validação, valor sem conversão) são citadas nas duas specs, porque a regra do `config.yaml` pede cenários para REST e Tool Calling quando a regra vale para ambos.
- **Alternativa considerada:** uma capability só. Rejeitada porque os changes 4 a 6 mexem só no assistente, e uma spec única misturaria assuntos diferentes.

### 3. Como cada comportamento foi levantado

| Comportamento | Origem no código |
|---|---|
| Valor sem divisão por 100 | `TransactionOutput.from` usa `BigDecimal.valueOf(amount).setScale(2)`; o campo é `double`, por isso o JSON mostra `8000.0` |
| Sem validação | `TransactionRequest`, `PersistTransactionInput` e `Transaction` não têm nenhuma checagem |
| Categoria nula gera 500 depois de salvar | `save` grava a entidade (coluna sem restrição de nulo) e só depois `TransactionOutput.from` chama `getCategory().name()`, que lança `NullPointerException` |
| Categoria inválida gera 400 | Conversão de texto para enum falha no corpo JSON e na variável de caminho; o tratamento padrão do Spring responde 400 |
| Falta do campo `file` gera 400 | Tratamento padrão do Spring para parte multipart obrigatória ausente |
| Falhas de IA geram 500 | Não existe `@RestControllerAdvice`; exceções do Spring AI chegam ao tratamento padrão |
| Prompt com charset da plataforma | `systemPrompt.getContentAsString(Charset.defaultCharset())` no construtor do controller |
| Comandos independentes | `ChatClient` montado sem `ChatMemory` |

### 4. Exceção à regra "arquivar com `./gradlew test` verde"

O `AGENTS.md` pede a suíte verde antes do archive. Este change não altera código, e o diagnóstico D-04 prevê que `contextLoads` falha sem MySQL e sem `OPENAI_API_KEY`.

- **Decisão:** rodar `./gradlew test` e registrar o resultado na tarefa. Se falhar por causa de D-04, o change pode ser arquivado mesmo assim, porque corrigir a suíte é responsabilidade do change 1 e mexer nela aqui quebraria a regra "sem alterar código".
- **Se falhar por outro motivo** (por exemplo, erro de compilação), o archive fica bloqueado e o problema é discutido antes de seguir.
- **Alternativa considerada:** antecipar a correção da suíte para este change. Rejeitada porque mistura dois assuntos num change que deveria ser só documentação.

### 5. Cenários que dependem da IA

Cenários como "a IA escolhe a categoria `AUTO`" dependem de um modelo não determinístico. Eles descrevem o comportamento esperado do fluxo e são conferidos manualmente com `OPENAI_API_KEY`, seguindo o mesmo critério dos testes `*IT` do módulo 05, que também chamam a OpenAI de verdade e não fazem parte da suíte obrigatória.

## Risks / Trade-offs

- [Algum cenário levantado pela leitura do código não corresponder ao comportamento real] → as tarefas 2 e 3 conferem cada requisito contra o código e, quando possível, com a aplicação rodando. Divergências são corrigidas na spec antes do archive.
- [Registrar defeitos como requisito pode parecer que o defeito é desejado] → todo requisito com defeito traz o ID (D-xx) no título e a palavra "hoje" ou "NÃO" no texto, deixando claro que é estado atual a ser corrigido.
- [Cenários com áudio e IA não podem ser conferidos sem chave] → ficam marcados como conferência manual opcional; não bloqueiam o archive.
