# Budgeting AI Assistant — API de Orçamento com Spring AI

Projeto final do bootcamp **Java com IA (DIO / Itaú)**, construído a partir do módulo `05-spring-ai` da trilha [dio-spring-boot-learning-track](https://github.com/digitalinnovationone/dio-spring-boot-learning-track) e evoluído passo a passo com **Spec-Driven Development (SDD)** usando [OpenSpec](https://openspec.dev).

---

## 1. O que o projeto faz

Esta aplicação é uma **API inteligente de controle de orçamento pessoal** que permite registrar e consultar gastos tanto por endpoints REST tradicionais quanto por **comandos de voz e de texto** processados por Inteligência Artificial com **Tool Calling**.

No fluxo principal por comando de voz (`POST /transactions/ai`):

1. O cliente envia um arquivo de áudio (ex.: *"Gastei 80 reais no mercado"*).
2. A aplicação transcreve o áudio para texto em português usando o modelo `whisper-1`.
3. O assistente financeiro (`ChatClient` com `gpt-4o-mini` e prompt de sistema contextualizado com a data atual) interpreta a intenção e escolhe qual ferramenta (`@Tool`) acionar.
4. A ferramenta executa o caso de uso real da aplicação (persistindo ou consultando no MySQL).
5. A resposta textual da IA é convertida em áudio MP3 (`gpt-4o-mini-tts`) e devolvida ao cliente.

Para facilitar testes e integrações sem necessidade de gravar áudio, a API também oferece o endpoint `POST /transactions/ai/text`, que executa o mesmo assistente e as mesmas ferramentas recebendo e respondendo JSON (`{"message": "..."}`).

---

## 2. Tecnologias utilizadas

Todas as escolhas seguem os módulos ministrados na trilha **DIO Spring Boot Learning Track**:

| Tecnologia | Papel no projeto | Referência na trilha |
|---|---|---|
| **Java 25** (Toolchain Gradle) | Linguagem base (`record`, pattern matching, APIs modernas) | Módulos 00 a 05 |
| **Spring Boot 4.0.5** | Web MVC, injeção de dependência, validação e configuração | Módulos 00 a 05 |
| **Spring AI 2.0.0-M4 (OpenAI)** | `ChatClient`, Tool Calling (`@Tool`, `@ToolParam`), `TranscriptionModel` (`whisper-1`) e `TextToSpeechModel` (`gpt-4o-mini-tts`) | Módulo `05-spring-ai` |
| **Spring Data JPA + Hibernate** | Persistência relacional, *derived queries* e `@CreationTimestamp` | Módulos `00-domain-driven-design` e `02-spring-data` |
| **MySQL 9.6** (Docker Compose) | Banco de dados relacional | Módulo `05-spring-ai` |
| **Bean Validation (`jakarta.validation`)** | Validação de entrada HTTP (`@Valid`, `@NotBlank`, `@NotNull`, `@Positive`) e `@RestControllerAdvice` | Módulo `01-spring-web` |
| **Testcontainers 2 (`@ServiceConnection`)** | Banco MySQL 9.6 real e efêmero nos testes de integração | Módulo `00-domain-driven-design` |
| **Spring REST Docs + Asciidoctor** | Documentação da API gerada a partir dos testes de integração | Módulo `01-spring-web` |
| **Instancio 5.0.2** *(apenas em teste)* | Geração de objetos de teste nas classes `Support` | **Única exceção à trilha**: dependência exclusivamente de teste (`testImplementation`), sem impacto no código de produção, reaproveitando o padrão de testes do projeto anterior (`dio-proposal-api`, documentado em `docs/padrao-de-testes.md`) |

---

## 3. Melhorias implementadas

Partindo de um diagnóstico inicial do projeto base (`docs/proposta-inicial.md`), a evolução foi dividida em *changes* pequenos, verificáveis e rastreáveis em `openspec/changes/archive/`:

1. **`baseline-specs` (Change 0):** levantamento e especificação formal do comportamento original do projeto base, incluindo seus defeitos (D-01 a D-07).
2. **`fix-base-defects` (Change 1):**
   - **Conversão monetária exata (D-01, RF-01):** correção de `TransactionOutput` e `TransactionResponse` para converter centavos (`long`) em reais (`BigDecimal.valueOf(amount, 2)`), eliminando o erro em que `8000` centavos (R$ 80,00) eram exibidos como `8000.0`.
   - **Leitura do prompt em UTF-8 (D-05, RNF-06):** substituição de `Charset.defaultCharset()` por `StandardCharsets.UTF_8`.
   - **Suíte de testes autossuficiente (D-04, RNF-01):** configuração de Testcontainers com `@ServiceConnection` (MySQL 9.6) e chave fictícia nos testes para que `./gradlew test` passe sem depender de MySQL externo nem de `OPENAI_API_KEY`.
3. **`move-ai-tools-to-infrastructure` (Change 1b):**
   - Remoção das anotações `@Tool` e `@ToolParam` da camada `application` para o adaptador `infrastructure/ai/TransactionTools` (e records de request próprios), tratando a IA como mais um cliente da aplicação (simétrico ao HTTP) e mantendo `domain` e `application` 100% livres de dependência do Spring AI, com contrato garantido por `TransactionToolsContractTest`.
4. **`add-transaction-validation` (Change 2):**
   - **Invariantes no domínio (D-03, RF-02):** validações com `org.springframework.util.Assert` em `Transaction` e `TransactionId`, impedindo dados inválidos tanto via REST quanto via Tool Calling da IA.
   - **Bean Validation e tratamento global de erros (D-06, RF-03):** `@Valid` nos requests HTTP, rejeição de arquivo de áudio vazio (`0` bytes) com HTTP 400 e `GlobalExceptionHandler` (`@RestControllerAdvice`).
5. **`add-transaction-timestamp-and-categories` (Change 3):**
   - **Auditoria com `createdAt` automático (D-07, RF-04):** registro automático do instante de criação (`Instant` com `@CreationTimestamp`) exposto nas respostas REST e nas tools da IA.
   - **Expansão de categorias (RF-05):** ampliação do enum `Category` de 3 para 9 categorias (`GROCERIES`, `PHARMA`, `AUTO`, `RESTAURANT`, `SHOPPING`, `TRANSPORT`, `HEALTH`, `LEISURE`, `OTHER`).
6. **`add-financial-query-tools` (Change 4):**
   - **Resumo por categoria (RF-06):** caso de uso `SummarizeTransactionsByCategoryUseCase`, endpoint `GET /transactions/summary?category=...` e tool `summarize-transactions-by-category` retornando `category`, `total` em reais e `count`.
   - **Consulta por período (RF-07):** *derived query* `findAllByCreatedAtBetween`, caso de uso `ListTransactionsByPeriodUseCase`, endpoint `GET /transactions?from=...&to=...` e tool `list-transactions-by-period`.
7. **`extract-voice-command-use-case` (Change 5):**
   - **Desacoplamento da orquestração de IA (D-02, RF-08):** criação da porta `VoiceAssistantGateway` (`application`), implementada por `SpringAiVoiceAssistantGateway` (`infrastructure/ai`), e extração dos casos de uso `ExecuteVoiceCommandUseCase` e `ExecuteTextCommandUseCase`, deixando o `TransactionController` enxuto e testável em unidade (`TransactionControllerTest`).
   - **Novo endpoint de comando por texto (RF-09):** `POST /transactions/ai/text`.
8. **`improve-assistant-prompt` (Change 6):**
   - **Prompt de sistema aprimorado com data atual (RF-10):** instruções explícitas para respostas curtas em português, valores em reais, conversão para centavos ao registrar, escolha entre as 9 categorias, pedido de esclarecimento quando faltar valor ou descrição e injeção dinâmica da data atual (`{currentDate}`) para consultas por período.
9. **`document-api` (Change 7):**
   - Geração automatizada da documentação com **Spring REST Docs** e **Asciidoctor** (`src/docs/asciidoc/index.adoc`) e consolidação deste `README.md`.
10. **`improve-registration-category-inference`:**
   - Aprimoramento das instruções do assistente para reconhecer descrições em linguagem natural, inferir a categoria sem perguntar à pessoa usuária e usar `OTHER` quando não houver uma categoria específica segura. O assistente só pede esclarecimento quando realmente faltar a descrição ou o valor da despesa.
11. **`restrict-assistant-to-financial-scope`:**
   - Restrição do assistente às capacidades financeiras disponíveis: registrar despesas e consultar gastos. Perguntas sobre outros assuntos e tentativas de alterar o papel do assistente recebem uma recusa curta, enquanto descrições pessoais continuam aceitas quando fazem parte de uma operação financeira válida.

---

## 4. Arquitetura e convenções

```text
src/main/java/dio/budgeting
├── domain                  # Entidade Transaction, TransactionId, Category e contrato TransactionRepository
├── application             # Casos de uso, gateway VoiceAssistantGateway e records de input/output
└── infrastructure
    ├── ai                  # Adaptador TransactionTools (@Tool/@ToolParam) e SpringAiVoiceAssistantGateway
    ├── http                # TransactionController, GlobalExceptionHandler e records de request/response
    └── persistence         # TransactionEntity (@CreationTimestamp), TransactionEntityRepository e JpaTransactionRepository
```

### Convenção de valores monetários (RF-01)

- **Entrada (`amount`):** sempre em **centavos** (`long` inteiro positivo). Exemplo: `8000` representa **R$ 80,00**; `8050` representa **R$ 80,50**. Isso evita problemas de representação em ponto flutuante.
- **Saída (`amount` nos endpoints de transação / `total` no resumo):** sempre em **reais** (`BigDecimal`) com duas casas decimais (`80.00`, `80.50`).

### Categorias disponíveis (`Category`)

`GROCERIES`, `PHARMA`, `AUTO`, `RESTAURANT`, `SHOPPING`, `TRANSPORT`, `HEALTH`, `LEISURE`, `OTHER`.

---

## 5. Como executar a aplicação

### Pré-requisitos

- **JDK 25** (ou gerenciado automaticamente via Toolchain do Gradle)
- **Docker** e **Docker Compose** em execução
- Chave da API da OpenAI (`OPENAI_API_KEY`)

### Subindo a aplicação

```bash
export OPENAI_API_KEY="sua_chave_openai_aqui"
./gradlew bootRun
```

O suporte `spring-boot-docker-compose` sobe automaticamente o contêiner MySQL 9.6 configurado em `compose.yml` quando `bootRun` é executado. A API ficará disponível em `http://localhost:8080`.

---

## 6. Como testar o fluxo principal e usar a API

### 6.1 Executando a suíte de testes automatizados e gerando a documentação

A suíte de testes **não exige `OPENAI_API_KEY`** nem um MySQL iniciado manualmente (os testes de integração sobem um MySQL 9.6 próprio via Testcontainers, exigindo apenas o Docker ativo):

```bash
# Roda todos os testes unitários e de integração (os testes *IT da OpenAI ficam desabilitados sem OPENAI_API_KEY)
./gradlew test

# Roda os testes, gera os snippets do Spring REST Docs e monta o HTML em build/docs/asciidoc/index.html
./gradlew asciidoctor
```

Para rodar também os testes de integração ao vivo com a OpenAI (`*IT` herdados do curso):

```bash
OPENAI_API_KEY="sua_chave_openai_aqui" ./gradlew test
```

### 6.2 Exemplos de requisição (`curl`)

#### 1. Registrar uma transação via REST (`POST /transactions`)

```bash
curl -i -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Compras no mercado",
    "category": "GROCERIES",
    "amount": 8000
  }'
```

Resposta (`201 Created`):

```json
{
  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "category": "GROCERIES",
  "description": "Compras no mercado",
  "amount": 80.00,
  "createdAt": "2026-10-28T15:30:00Z"
}
```

#### 2. Listar transações por categoria (`GET /transactions/{category}`)

```bash
curl -s http://localhost:8080/transactions/GROCERIES
```

#### 3. Consultar resumo (total e quantidade) por categoria (`GET /transactions/summary`)

```bash
curl -s "http://localhost:8080/transactions/summary?category=GROCERIES"
```

Resposta (`200 OK`):

```json
{
  "category": "GROCERIES",
  "total": 80.00,
  "count": 1
}
```

#### 4. Consultar transações por período (`GET /transactions?from=...&to=...`)

```bash
curl -s "http://localhost:8080/transactions?from=2026-10-01&to=2026-10-31"
```

#### 5. Enviar comando em texto para a IA (`POST /transactions/ai/text`)

Permite testar todo o fluxo de interpretação e Tool Calling sem precisar de áudio:

```bash
# Registrar um gasto falando em linguagem natural
curl -s -X POST http://localhost:8080/transactions/ai/text \
  -H "Content-Type: application/json" \
  -d '{"message": "Almocei em um restaurante e gastei 45 reais"}'

# Consultar o total gasto em uma categoria
curl -s -X POST http://localhost:8080/transactions/ai/text \
  -H "Content-Type: application/json" \
  -d '{"message": "Quanto eu já gastei com mercado?"}'

# Consultar gastos do período usando referência relativa de data
curl -s -X POST http://localhost:8080/transactions/ai/text \
  -H "Content-Type: application/json" \
  -d '{"message": "Quais foram meus gastos deste mês?"}'
```

#### 6. Enviar comando de voz para a IA (`POST /transactions/ai`)

```bash
curl -X POST http://localhost:8080/transactions/ai \
  -F "file=@src/test/resources/audio/recording-1.m4a" \
  --output resposta.mp3
```

---

## 7. O que foi aprendido durante o desafio

- **A IA como mais um adaptador de entrada e saída da arquitetura:** em vez de acoplar anotações `@Tool` nos casos de uso ou deixar o `ChatClient` dentro do controller, posicionar o Tool Calling (`TransactionTools`) e os modelos da OpenAI (`SpringAiVoiceAssistantGateway`) na camada `infrastructure` mantém `domain` e `application` limpos, testáveis e independentes de framework.
- **Invariantes no domínio protegem também o Tool Calling:** validar apenas o DTO HTTP com `@Valid` não impede que o modelo de IA passe valores inválidos (como valor zero ou negativo) diretamente para uma tool. Colocar as regras de negócio na entidade de domínio (`Transaction`) garante consistência em qualquer canal de entrada.
- **Testes de contrato para ferramentas de IA:** como o LLM enxerga as ferramentas pelo JSON Schema gerado a partir das assinaturas e anotações Java, um teste automatizado de caracterização (`TransactionToolsContractTest`) evita que refatorações quebrem silenciosamente o que a IA recebe.
- **Evolução incremental com Spec-Driven Development (OpenSpec):** especificar requisitos e cenários (`WHEN`/`THEN`), desenhar a solução técnica e validar cada etapa com testes automatizados (unitários com Mockito e de integração com Testcontainers + MySQL real) trouxe previsibilidade e rastreabilidade do início ao fim do projeto.
