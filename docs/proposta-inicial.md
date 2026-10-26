# Proposta Inicial: Evolução da API de Orçamento com Spring AI

Documento de levantamento de requisitos que serve de entrada para o fluxo Spec-Driven Development (SDD) com OpenSpec. A partir dele serão gerados os artefatos de cada change (`proposal.md`, `specs/`, `design.md`, `tasks.md`).

- **Projeto base:** `05-spring-ai` da trilha [dio-spring-boot-learning-track](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/blob/main/05-spring-ai/README.md) (cópia idêntica ao conteúdo das aulas).
- **Enunciado:** `../desafio.txt`.
- **Status:** levantamento concluído. O andamento de cada change fica registrado em `openspec/` (`openspec list` mostra os changes em andamento; `openspec/changes/archive/`, os concluídos).

---

## 1. Contexto

### 1.1 O desafio

Evoluir uma API de orçamento que processa comandos de voz sobre transações financeiras. O fluxo principal é:

1. Receber um arquivo de áudio do cliente.
2. Transcrever o áudio em texto.
3. Usar IA para entender a intenção do comando.
4. Executar uma função real da aplicação (Tool Calling).
5. Criar ou consultar transações financeiras.
6. Gerar uma resposta final em áudio para a pessoa usuária.

O enunciado sugere as seguintes evoluções: novos tipos de consulta financeira, respostas melhores da IA, novas ferramentas para o Tool Calling, validações antes de salvar, endpoints REST melhores, testes dos principais fluxos e documentação de uso. A entrega é um repositório no GitHub com README explicando o projeto, como executar, a melhoria implementada, as tecnologias, como testar e o que foi aprendido.

### 1.2 Objetivo desta proposta

O projeto será usado num processo de possível contratação. Por isso, a entrega precisa ser **utilizável** (roda, é testável e está documentada) e **100% aderente ao conteúdo ministrado** (arquitetura, padrões e tecnologias da trilha).

### 1.3 Estado atual (observado no código)

| Item | Situação |
|---|---|
| Stack | Java 25 (toolchain Gradle), Spring Boot 4.0.5, Spring AI 2.0.0-M4 (OpenAI), Spring Data JPA, MySQL 9.6 via Docker Compose, Lombok |
| Arquitetura | Camadas `domain`, `application`, `infrastructure`, como no restante da trilha |
| Domínio | `Transaction` (id, description, amount em centavos, category), `TransactionId`, `Category` (`GROCERIES`, `PHARMA`, `AUTO`) e o contrato `TransactionRepository` |
| Casos de uso | `PersistTransactionUseCase` e `ListTransactionsByCategoryUseCase`, ambos expostos como `@Tool` |
| Endpoints | `POST /transactions` (JSON), `GET /transactions/{category}` e `POST /transactions/ai` (multipart, áudio de entrada e MP3 de saída) |
| Modelos de IA | Chat `gpt-4o-mini`, transcrição `whisper-1` (pt) e voz `gpt-4o-mini-tts` |
| Testes | `contextLoads` e cinco testes de integração (`*IT`) que só rodam com `OPENAI_API_KEY` |

---

## 2. Diagnóstico: problemas do projeto base

Estes problemas devem ser resolvidos **antes** de evoluir a aplicação, porque afetam a corretude ou impedem que a entrega seja verificada.

| ID | Problema | Evidência | Impacto |
|---|---|---|---|
| D-01 | Valor em centavos exibido como reais | `TransactionOutput.from` faz `BigDecimal.valueOf(amount).setScale(2)` sem dividir por 100 | `8000` (R$ 80,00) é devolvido como `8000.00`; a IA responde com o valor errado |
| D-02 | Orquestração de IA dentro do controller | `TransactionController.transcribe` chama transcrição, `ChatClient` e TTS diretamente | Viola a regra da trilha "controller chama um caso de uso"; dificulta testes |
| D-03 | Nenhuma validação | Request, input e domínio aceitam descrição vazia, valor zero/negativo e categoria nula | Dados inválidos persistidos tanto via REST quanto via Tool Calling |
| D-04 | Suíte de testes não roda sem infraestrutura externa | `contextLoads` depende de MySQL e de `OPENAI_API_KEY` (placeholder sem valor padrão); não há testes unitários | `./gradlew test` falha numa máquina limpa *(a confirmar na primeira execução)* |
| D-05 | Prompt lido com charset da plataforma | `systemPrompt.getContentAsString(Charset.defaultCharset())` | Risco de quebrar acentos em ambientes que não usam UTF-8 |
| D-06 | Sem tratamento de erros | Não existe `@RestControllerAdvice`; arquivo vazio ou falha do provedor de IA vira erro 500 genérico | API pouco previsível para o cliente |
| D-07 | Transação sem data | `Transaction` não registra quando foi criada | Impede consultas por período e qualquer auditoria básica |

> **Ponto de atenção sobre D-03:** validar apenas o `TransactionRequest` com `@Valid` não basta. No fluxo de IA, o modelo chama `PersistTransactionUseCase` direto como tool, sem passar pelo controller. As regras de negócio precisam estar no domínio (`Transaction`), e a validação HTTP é só uma camada extra.

---

## 3. Requisitos

### 3.1 Requisitos funcionais

| ID | Requisito | Origem |
|---|---|---|
| RF-01 | A API deve representar valores monetários corretamente: entrada em centavos (`long`) e saída em reais com duas casas decimais (`8000` → `80.00`), com a convenção documentada no README | D-01 |
| RF-02 | O sistema deve rejeitar transações com descrição vazia, valor menor ou igual a zero ou categoria ausente, em qualquer canal (REST ou Tool Calling) | D-03, enunciado |
| RF-03 | Erros de validação devem retornar HTTP 400 com os campos e mensagens; recursos/entradas inválidas no fluxo de IA devem retornar erro claro em vez de 500 | D-06 |
| RF-04 | Toda transação deve registrar a data/hora de criação, preenchida automaticamente | D-07, enunciado ("auditoria") |
| RF-05 | O domínio deve oferecer as categorias `GROCERIES`, `PHARMA`, `AUTO`, `RESTAURANT`, `SHOPPING`, `TRANSPORT`, `HEALTH`, `LEISURE` e `OTHER`, mantendo as três existentes | Enunciado |
| RF-06 | Nova consulta: total gasto e quantidade de transações por categoria, disponível como caso de uso, tool e endpoint `GET /transactions/summary?category=...` | Enunciado ("novos tipos de consulta", "novas ferramentas") |
| RF-07 | Nova consulta: transações em um período (data inicial e final), disponível como caso de uso, tool e endpoint `GET /transactions?from=...&to=...` | Enunciado |
| RF-08 | O processamento de comandos (transcrição, IA e voz) deve ficar num caso de uso próprio; o controller apenas adapta HTTP | D-02 |
| RF-09 | Novo endpoint `POST /transactions/ai/text`, que recebe `{"message": "..."}` e devolve `{"message": "..."}` com a resposta da IA, para testar o fluxo de Tool Calling sem áudio | Enunciado ("melhorar os endpoints REST") |
| RF-10 | O prompt de sistema deve orientar respostas curtas, em português, com valores em reais, conhecer a data atual (para consultas por período) e pedir esclarecimento quando faltar valor ou descrição | Enunciado ("melhorar as respostas da IA") |
| RF-11 | Os endpoints existentes (`POST /transactions`, `GET /transactions/{category}`, `POST /transactions/ai`) devem continuar funcionando | Compatibilidade |

### 3.2 Requisitos não funcionais

| ID | Requisito |
|---|---|
| RNF-01 | `./gradlew test` deve passar sem `OPENAI_API_KEY` e sem MySQL rodando manualmente (os testes de IA continuam condicionais à chave) |
| RNF-02 | Todo change inclui testes seguindo `docs/padrao-de-testes.md` (seção 3.4): domínio, casos de uso, mapeamento de entidade, repositório, controller e API, com MySQL real via Testcontainers nos testes de integração |
| RNF-03 | A documentação da API deve ser gerada a partir dos testes (Spring REST Docs) |
| RNF-04 | O README deve estar em português e conter os seis itens exigidos pelo enunciado, além de exemplos de requisição (`curl`) |
| RNF-05 | Nenhuma credencial versionada; a chave da OpenAI vem de variável de ambiente |
| RNF-06 | Arquivos de recurso (prompts) lidos explicitamente em UTF-8 |

### 3.3 Restrições: aderência ao conteúdo ministrado

Toda solução deve usar apenas padrões e tecnologias vistos na trilha:

| Tema | Referência na trilha | Uso nesta proposta |
|---|---|---|
| Camadas DDD, records vs classes, IDs fortemente tipados, Repository Pattern, um caso de uso por capacidade | README raiz, módulo 00 | Toda a estrutura; novos casos de uso e contratos no domínio |
| Invariantes no domínio com `Assert` | 01 (`Task`) | RF-02 |
| Bean Validation (`@Valid`, `@NotBlank`, etc.) e `@RestControllerAdvice` | 01 (`CreateTaskRequest`, `GlobalExceptionHandler`) | RF-02, RF-03 |
| Spring REST Docs + Asciidoctor | 01 (`TaskControllerTest`, `index.adoc`) | RNF-03 |
| Testes unitários com Mockito (`@Mock`, `@InjectMocks`) | 01 (`CreateTaskUseCaseTest`) | RNF-02 |
| `@CreationTimestamp` em entidade JPA | 02 (`Customer`) | RF-04 |
| Testcontainers com `@ServiceConnection` | 00 (`AddBookToCatalogUseCaseTest`) | RNF-01, RNF-02 |
| `ChatClient`, `@Tool`/`@ToolParam`, `TranscriptionModel`, `TextToSpeechModel`, prompt em `.st` | 05 | RF-06 a RF-10 |
| Testes `*IT` condicionais com `@EnabledIfEnvironmentVariable` | 05 | Mantidos |

**Decisão revista (Tool Calling na camada `infrastructure`):** o projeto base coloca anotações do Spring AI (`@Tool`, `@ToolParam`) na camada `application`, nos casos de uso e no `PersistTransactionInput`. É um vazamento de framework. A decisão original era mantê-lo por consistência com o módulo 05. Ela foi revista: a IA passa a ser tratada como mais um cliente da aplicação, igual ao HTTP, com um adaptador `infrastructure/ai/TransactionTools` (e records de request próprios com `@ToolParam`) que chama os casos de uso, no mesmo papel do `TransactionController`.

- **Por quê:** os casos de uso deixam de depender do Spring AI, e os dois canais de entrada (HTTP e Tool Calling) ficam simétricos, seguindo a regra de camadas do README da trilha.
- **Aderência:** as anotações continuam sendo as do módulo 05; muda só a camada onde ficam. Os casos de uso mantêm `@Service`: Spring "núcleo" (injeção de dependência) pode ficar na `application`; integrações específicas (web, JPA, IA) ficam na `infrastructure`.
- **Quando:** change `move-ai-tools-to-infrastructure` (seção 5), logo depois do `fix-base-defects`, para que a validação (change 2) e as tools novas (change 4) já nasçam no adaptador.

**Exceção aprovada:** o padrão de testes (seção 3.4) usa o Instancio para gerar dados de teste. Ele não faz parte do conteúdo ministrado, mas foi adotado por decisão do autor, reaproveitando o padrão do projeto anterior (`dio-proposal-api`). É uma dependência só de teste (`testImplementation`), sem impacto no código de produção, e deve ser justificada no README.

### 3.4 Padrão de testes

Os testes seguem o padrão adotado no projeto anterior `dio-proposal-api`, adaptado às camadas desta trilha. O documento completo está em [`padrao-de-testes.md`](padrao-de-testes.md). Resumo:

- **Nomes:** `@DisplayName("Given ... When ... Then ...")` e o método com a mesma frase em camelCase.
- **Estrutura:** dados → setup → ação → verify, com helpers privados `setupGiven<Nome>` (só mocks/estado) e `verifyGiven<Nome>` (só verificações).
- **Estilo:** `final var` em variáveis locais e parâmetros `final` nos helpers; injeção por construtor com `@TestConstructor`, sem `@Autowired`.
- **Dados:** classes `Support` com Instancio em `src/test/java/dio/budgeting/support/`, com campos que têm regra (valor em centavos, categoria, datas) sempre fixos.
- **Cobertura por camada:** domínio, casos de uso, mapeamento de entidade, repositório (unitário e integração com Testcontainers), controller e API (MockMvc, que também gera os snippets do REST Docs).
- **Testes `*IT` do curso** (que chamam a OpenAI de verdade) ficam como estão: continuam condicionais à chave e fora do padrão, porque são material da aula.

---

## 4. Escopo

### 4.1 Dentro do escopo

Especificação da linha de base, correções D-01 a D-07, requisitos RF-01 a RF-11 e RNF-01 a RNF-06.

### 4.2 Fora do escopo

- Autenticação e transações por usuário (Spring Security, módulo 03): aumenta muito o escopo e o risco. Fica como evolução futura.
- Memória de conversa (`ChatMemory`) e RAG: não fazem parte do conteúdo ministrado.
- Troca de provedor de IA (por exemplo, Gemini): o curso usa OpenAI para chat, transcrição e voz.
- Edição e exclusão de transações.
- Front-end.

---

## 5. Decomposição em changes do OpenSpec

No SDD, cada change deve ser pequeno, coerente e verificável sozinho. Cada change inclui os próprios testes. A ordem abaixo respeita as dependências.

| Ordem | Change (kebab-case) | Conteúdo | Requisitos | Capability afetada |
|---|---|---|---|---|
| 0 | `baseline-specs` | Especificar o comportamento **atual** do projeto base, sem alterar código. Ao arquivar, as specs vão para `openspec/specs/` e viram a linha de base dos próximos changes | — | `transactions`, `voice-assistant` |
| 1 | `fix-base-defects` | Corrigir conversão de centavos, leitura do prompt em UTF-8 e tornar a suíte de testes executável (Testcontainers, chave fictícia em testes, Instancio e classes `Support`) | D-01, D-04, D-05, RF-01, RNF-01, RNF-06 | `transactions` |
| 1b | `move-ai-tools-to-infrastructure` | Mover `@Tool`/`@ToolParam` dos casos de uso para um adaptador em `infrastructure/ai`; casos de uso sem dependência do Spring AI. Refatoração: os testes do change 1 continuam verdes e nenhuma spec muda | Decisão revista da seção 3.3 | (sem mudança de comportamento) |
| 2 | `add-transaction-validation` | Invariantes no domínio, Bean Validation no request e `GlobalExceptionHandler` | D-03, D-06, RF-02, RF-03 | `transactions` |
| 3 | `add-transaction-timestamp-and-categories` | `createdAt` automático e novas categorias | D-07, RF-04, RF-05 | `transactions` |
| 4 | `add-financial-query-tools` | Total por categoria e transações por período (caso de uso, tool e REST) | RF-06, RF-07 | `transactions`, `voice-assistant` |
| 5 | `extract-voice-command-use-case` | Caso de uso de comando, endpoint de texto e controller enxuto | D-02, RF-08, RF-09, RF-11 | `voice-assistant` |
| 6 | `improve-assistant-prompt` | Novo prompt de sistema com data atual e regras de resposta | RF-10 | `voice-assistant` |
| 7 | `document-api` | Spring REST Docs, `index.adoc` e README final do desafio | RNF-03, RNF-04 | (sem mudança de comportamento) |

**Capabilities propostas** (especificações principais em `openspec/specs/`):

- `transactions`: registro e consulta de transações financeiras (REST e regras de domínio).
- `voice-assistant`: comandos por voz e texto, Tool Calling e resposta em áudio.

---

## 6. Critérios de aceite (rascunho para as specs)

Escritos no formato de cenários (WHEN/THEN) para serem levados aos arquivos `spec.md` de cada change.

**Valores monetários (RF-01)**
- WHEN uma transação é criada com `amount = 8000` THEN a resposta mostra o valor `80.00`.

**Validação (RF-02, RF-03)**
- WHEN `POST /transactions` recebe descrição vazia THEN responde 400 indicando o campo `description`.
- WHEN `POST /transactions` recebe `amount <= 0` THEN responde 400 indicando o campo `amount`.
- WHEN o modelo tenta persistir uma transação com valor inválido via tool THEN nada é salvo e o modelo recebe o erro para informar a pessoa usuária.

**Data de criação (RF-04)**
- WHEN uma transação é salva THEN ela tem `createdAt` preenchido automaticamente, e esse valor aparece na resposta.

**Consultas (RF-06, RF-07)**
- WHEN existem transações de R$ 80,00 e R$ 40,00 em `GROCERIES` THEN o total por categoria `GROCERIES` é `120.00`.
- WHEN consulto um período THEN recebo só as transações criadas dentro dele.
- WHEN o comando é "quanto gastei com mercado?" THEN o modelo usa a tool de total por categoria.

**Comandos (RF-08, RF-09, RF-11)**
- WHEN envio texto ao endpoint de comando THEN recebo JSON com a resposta da IA.
- WHEN envio áudio ao `POST /transactions/ai` THEN continuo recebendo um MP3.
- WHEN envio um arquivo de áudio vazio THEN recebo 400.

**Qualidade (RNF-01)**
- WHEN rodo `./gradlew test` sem `OPENAI_API_KEY` THEN a suíte passa e os testes `*IT` são ignorados.

---

## 7. Decisões e questões em aberto

### 7.1 Decididas

| # | Questão | Decisão |
|---|---|---|
| 1 | Formato do valor na API | Entrada em centavos (`long`, evita erro de ponto flutuante) e saída em reais com duas casas; convenção documentada no README |
| 2 | Lista de categorias | `GROCERIES`, `PHARMA`, `AUTO`, `RESTAURANT`, `SHOPPING`, `TRANSPORT`, `HEALTH`, `LEISURE`, `OTHER` |
| 3 | Contrato das consultas REST | Consultas separadas: `GET /transactions/summary?category=GROCERIES` → `{"category", "total", "count"}` e `GET /transactions?from=2026-10-01&to=2026-10-31` → lista do período. `GET /transactions/{category}` mantido. Só *derived queries* do Spring Data. Limitação aceita: não há cruzamento categoria + período |
| 4 | Resposta do endpoint de texto | `POST /transactions/ai/text` com `{"message": "..."}` devolve só a resposta da IA (`{"message": "..."}`). Sem lista de tools chamadas (exigiria controle manual da execução de tools, fora do conteúdo). Transcrição do fluxo de áudio visível nos logs |
| 5 | Specs de linha de base | Sim, pelo change 0 (`baseline-specs`), passando pelo fluxo completo do OpenSpec para ficar registrado no histórico |
| — | Padrão de testes | Seguir o padrão do `dio-proposal-api`, adaptado (seção 3.4) |

### 7.2 Em aberto

Nenhuma questão bloqueante no momento. Novas dúvidas surgidas durante o `design` de cada change serão registradas no próprio change.

---

## 8. Pré-requisitos do ambiente

| Item | Situação atual na máquina | Ação |
|---|---|---|
| JDK 25 | Só há o JDK 21 instalado | Instalar o JDK 25 (por exemplo, via SDKMAN) |
| Docker e Compose | Instalados | Nenhuma |
| `OPENAI_API_KEY` | Não configurada | Exportar a variável antes de rodar a aplicação e os `*IT` |
| OpenSpec CLI | 1.14.0 instalado | Rodar `openspec init` no projeto |
| Repositório | Projeto dentro de `dio-agent/boot-camp/desafios/final` | Criar um repositório próprio no GitHub (fork ou novo) |

---

## 9. Próximos passos no fluxo OpenSpec

1. Criar o repositório do projeto e fazer o primeiro commit com o código base intacto.
2. Rodar `openspec init` e configurar o `openspec/config.yaml`. O `context` deve dizer que os artefatos são escritos em pt-BR e que só podem ser usados padrões e tecnologias da trilha (seção 3.3).
3. Gerar o change 0 (`baseline-specs`) com o fluxo de *propose*, revisar e arquivar, populando `openspec/specs/`.
4. Gerar o change 1 (`fix-base-defects`) usando este documento e `docs/padrao-de-testes.md` como referência.
5. Para cada change: revisar os artefatos e só então aplicar (*apply*), validar e arquivar (*archive*) antes de seguir para o próximo.
