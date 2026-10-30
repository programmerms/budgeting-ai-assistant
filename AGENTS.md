# AGENTS.md

Instruções para agentes de IA que trabalham neste repositório. Leia este arquivo inteiro antes de qualquer ação.

## O projeto

API de orçamento que registra e consulta gastos por comando de voz: recebe um áudio, transcreve, usa IA com Tool Calling para executar casos de uso reais e responde em áudio. É o projeto final do bootcamp DIO, construído a partir do módulo `05-spring-ai` da trilha [dio-spring-boot-learning-track](https://github.com/digitalinnovationone/dio-spring-boot-learning-track) e evoluído com Spec-Driven Development (OpenSpec).

O projeto faz parte de um processo de aprendizagem: a entrega precisa ser utilizável, testada, documentada e 100% aderente ao conteúdo ministrado na trilha.

Documentos de referência (leia antes de planejar ou implementar):

- `docs/proposta-inicial.md`: diagnóstico do projeto base (D-01 a D-07), requisitos (RF/RNF), restrições de tecnologia (seção 3.3), escopo, ordem dos changes (seção 5) e decisões tomadas (seção 7).
- `docs/padrao-de-testes.md`: padrão obrigatório para todos os testes novos.
- `openspec/config.yaml`: contexto e regras aplicados a todos os artefatos do OpenSpec.

## Stack e comandos

Java 25, Spring Boot 4, Spring AI 2 (OpenAI), Spring Security, Spring Data JPA, MySQL (Docker Compose em desenvolvimento), Lombok, Gradle.

```bash
export OPENAI_API_KEY="..."      # obrigatória para bootRun e para os testes *IT
export APP_ADMIN_USERNAME="..."  # obrigatória para bootRun: administrador inicial
export APP_ADMIN_PASSWORD="..."  # obrigatória para bootRun: senha do administrador inicial
./gradlew bootRun                # sobe a API; o Docker Compose inicia o MySQL
./gradlew test                   # roda a suíte de testes (exige Docker rodando; não precisa de variável nenhuma)
```

`APP_ADMIN_USERNAME` e `APP_ADMIN_PASSWORD` não têm valor padrão: sem elas a aplicação falha na inicialização apontando a variável ausente. Nos testes, as três propriedades são declaradas em `@SpringBootTest(properties = ...)`, como descrito em `docs/padrao-de-testes.md`.

Os testes de integração sobem um MySQL próprio com Testcontainers, por isso o Docker precisa estar rodando. Sem `OPENAI_API_KEY`, os testes `*IT` são ignorados.

## Arquitetura e convenções

- Camadas da trilha: `domain` (modelo, invariantes, contratos), `application` (um caso de uso por capacidade, com `input`/`output`), `infrastructure` (HTTP, JPA e integrações).
- O contexto de contas e acesso vive em `dio.budgeting.auth`, com as mesmas três camadas. O domínio e os casos de uso de `auth` não dependem do Spring Security: o framework só aparece em `auth/infrastructure/security`.
- O controller só adapta HTTP e chama um caso de uso. A orquestração de IA não fica no controller.
- Regras de negócio ficam no domínio, porque o fluxo de Tool Calling chama os casos de uso sem passar pelo controller.
- A IA é mais um cliente da aplicação: as anotações do Spring AI (`@Tool`, `@ToolParam`) ficam só no adaptador `infrastructure/ai` (`TransactionTools` e records em `infrastructure/ai/request`), que só converte a entrada e chama os casos de uso, como o controller faz no HTTP. Os casos de uso não dependem do Spring AI.
- Use `record` para value objects e DTOs, `class` para entidades, IDs fortemente tipados (`TransactionId`) e o Repository Pattern (contrato no domínio, implementação na infraestrutura).
- Valores monetários: entrada em centavos (`long`), saída em reais com duas casas decimais.
- Use apenas padrões e tecnologias ensinados na trilha. Única exceção aprovada: Instancio, somente em testes. Qualquer outra dependência nova precisa de aprovação do autor.
- Os testes `*IT` herdados do curso chamam a OpenAI de verdade, são condicionais a `OPENAI_API_KEY` e ficam como estão.

## Fluxo de trabalho: Spec-Driven Development com OpenSpec

Nenhuma alteração de comportamento acontece fora de um change do OpenSpec.

1. **Propose** (`/opsx-propose`): gera `proposal.md`, `specs/`, `design.md` e `tasks.md`. Planejamento apenas; não edite código.
2. **Revisão:** pare e aguarde o autor revisar e aprovar os artefatos.
3. **Apply** (`/opsx-apply`): implementação das tarefas, no modo de trabalho descrito abaixo.
4. **Revisão:** pare de novo, com a suíte verde, e aguarde a aprovação do autor.
5. **Archive** (`/opsx-archive`): arquiva o change e atualiza `openspec/specs/`.

Como todo o estado do planejamento vive em arquivo, e não na conversa, qualquer harness ou modelo pode assumir o trabalho a partir daqui: basta ler este arquivo e os artefatos do change em andamento.

Siga a ordem dos changes da seção 5 da proposta inicial (`baseline-specs` primeiro). Antes de começar, rode `openspec list` para ver o que está em andamento.

### Sessão e modelo por fase

O planejamento exige mais julgamento que a implementação, e a implementação consome mais tokens que o planejamento. Por isso as duas fases não precisam rodar no mesmo modelo nem na mesma conversa.

- **Propose:** modelo mais capaz. É aqui que as decisões difíceis e as alternativas descartadas são definidas.
- **Apply:** sessão nova com um modelo intermediário. A sessão nova economiza mais que a troca de modelo, porque o histórico inteiro é reenviado a cada turno e o implementador não precisa do transcript do planejamento.
- **Lembre o autor na fronteira.** Ao terminar um propose, ofereça o prompt pronto para abrir a sessão de apply. Ao terminar um apply, avise que o archive pode seguir na mesma sessão, por ser curto.
- Quanto mais preciso o `tasks.md` — arquivo, assinatura, teste e comando de verificação em cada tarefa —, menos julgamento o apply exige. É isso que torna o modelo mais simples viável; invista no plano em vez de economizar depois.

Esta divisão está em avaliação: o primeiro experimento é o apply de `add-transaction-ownership`. Se o resultado não se sustentar, revise esta seção com o que foi observado.

## Modo de trabalho

O autor está aprendendo e precisa conseguir explicar cada decisão numa entrevista. É esse o objetivo que o modo de trabalho serve.

**Combinado em vigor:** o agente implementa e o autor revisa.

- O agente escreve o código explicando cada decisão enquanto avança. O autor revisa.
- Justifique toda escolha citando o módulo da trilha que a fundamenta. Quando a decisão não vier da trilha, diga isso e explique o critério usado.
- **Pare para a revisão do autor depois de cada propose e depois de cada apply, antes do archive.** Não emende uma fase na outra.
- Registre no `design.md` as alternativas consideradas e o motivo de cada descarte; é esse texto que sustenta a explicação em uma entrevista.
- Faça uma pergunta por vez e confirme o entendimento antes de avançar.
- Comunique-se em português do Brasil, de forma direta e sem jargão desnecessário.

Este combinado substitui o modo original, em que o autor escrevia o código e o agente apenas orientava. Se o autor pedir para voltar a esse formato, ele volta a valer.

## Commits e segurança

- Commits no padrão Conventional Commits, em inglês e no imperativo: `feat(transactions): add category summary endpoint`, `fix: convert cents to reais in output`, `docs: ...`, `test: ...`, `chore: ...`.
- Um change do OpenSpec por vez; commits pequenos e coerentes com as tarefas.
- Nunca versione credenciais. A chave da OpenAI vem de variável de ambiente.
