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
| **Spring Security** | Autenticação por sessão HTTP (`SecurityFilterChain`, filtro REST de login, `UserDetailsService` próprio) e autorização por papel, com senhas em BCrypt | Módulo `03-spring-security` |
| **Bean Validation (`jakarta.validation`)** | Validação de entrada HTTP (`@Valid`, `@NotBlank`, `@NotNull`, `@Positive`) e `@RestControllerAdvice` | Módulo `01-spring-web` |
| **Testcontainers 2 (`@ServiceConnection`)** | Banco MySQL 9.6 real e efêmero nos testes de integração | Módulo `00-domain-driven-design` |
| **Spring REST Docs + Asciidoctor** | Documentação da API gerada a partir dos testes de integração | Módulo `01-spring-web` |
| **Instancio 5.0.2** *(apenas em teste)* | Geração de objetos de teste nas classes `Support` | **Única exceção à trilha**: dependência exclusivamente de teste (`testImplementation`), sem impacto no código de produção, reaproveitando o padrão de testes do projeto anterior (`dio-proposal-api`, documentado em `docs/padrao-de-testes.md`) |

---

## 3. Melhorias implementadas

Partindo de um diagnóstico inicial do projeto base (`docs/proposta-inicial.md`), a evolução foi dividida em *changes* pequenos, verificáveis e rastreáveis em `openspec/changes/archive/`:

Para entender a organização entre specs atuais, changes ativos e histórico arquivado, consulte o [`Guia rápido do OpenSpec`](docs/guia-openspec.md).

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
   - **Consulta por período (RF-07):** *derived query* `findAllByOwnerIdAndCreatedAtBetween`, caso de uso `ListTransactionsByPeriodUseCase`, endpoint `GET /transactions?from=...&to=...` e tool `list-transactions-by-period`.
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
12. **`add-user-accounts-and-authentication`:**
   - **Contas, papéis e aprovação:** cadastro público com estado `PENDING`, aprovação ou recusa por um administrador e papéis `ROLE_USER` e `ROLE_ADMIN` definidos sempre pelo sistema, nunca pelos dados do cliente.
   - **Autenticação por sessão:** login e logout REST com cookie `JSESSIONID`, senhas em BCrypt, administrador inicial vindo de variáveis de ambiente e endpoints financeiros passando a exigir sessão autenticada.
13. **`add-transaction-ownership`:**
   - **Propriedade obrigatória:** toda transação guarda o identificador da pessoa autenticada; o contrato do repositório não oferece consultas globais.
   - **Isolamento em todos os canais:** REST, comandos de texto e voz registram, listam e somam apenas os gastos da sessão atual. O proprietário vem do servidor, não pode ser escolhido pelo cliente ou pelo modelo e não é exposto nas respostas.

---

## 4. Arquitetura e convenções

```text
src/main/java/dio/budgeting
├── domain                  # Transaction, TransactionId, OwnerId, Category e contrato TransactionRepository
├── application             # Casos de uso, gateway VoiceAssistantGateway e records de input/output
├── infrastructure
│   ├── ai                  # Adaptador TransactionTools (@Tool/@ToolParam) e SpringAiVoiceAssistantGateway
│   ├── http                # TransactionController, GlobalExceptionHandler e records de request/response
│   ├── security            # AuthenticatedOwnerProvider traduz a sessão para OwnerId
│   └── persistence         # TransactionEntity (@CreationTimestamp), TransactionEntityRepository e JpaTransactionRepository
└── auth                    # Contexto de contas e acesso, com as mesmas três camadas
    ├── domain              # UserAccount, UserAccountId, UserRole, UserStatus e contrato UserAccountRepository
    ├── application         # RegisterUserUseCase, ApproveUserUseCase, RejectUserUseCase, ListUsersByStatusUseCase e PasswordEncoderGateway
    └── infrastructure
        ├── http            # AuthController, AdminUserController e records de request/response
        ├── persistence     # UserAccountEntity, UserAccountEntityRepository e JpaUserAccountRepository
        └── security        # SecurityConfig, JpaUserDetailsService, AuthenticatedUser e AdminAccountInitializer
```

### Convenção de valores monetários (RF-01)

- **Entrada (`amount`):** sempre em **centavos** (`long` inteiro positivo). Exemplo: `8000` representa **R$ 80,00**; `8050` representa **R$ 80,50**. Isso evita problemas de representação em ponto flutuante.
- **Saída (`amount` nos endpoints de transação / `total` no resumo):** sempre em **reais** (`BigDecimal`) com duas casas decimais (`80.00`, `80.50`).

### Categorias disponíveis (`Category`)

`GROCERIES`, `PHARMA`, `AUTO`, `RESTAURANT`, `SHOPPING`, `TRANSPORT`, `HEALTH`, `LEISURE`, `OTHER`.

### Controle de acesso

A API é multiusuário: todos os endpoints de `/transactions` exigem uma sessão autenticada com o papel `ROLE_USER`, e os endpoints de `/api/admin` exigem `ROLE_ADMIN`. Apenas o cadastro (`POST /api/auth/register`) e o login (`POST /api/auth/login`) são públicos.

O ciclo de vida de uma conta é **cadastro → aprovação → login**: o cadastro nasce no estado `PENDING` e não autentica; um administrador aprova (`ACTIVE`) ou recusa (`REJECTED`); só uma conta `ACTIVE` consegue entrar. O papel e o estado são sempre definidos pelo sistema, nunca pelos dados enviados pelo cliente.

A autenticação é por **sessão HTTP** com cookie `JSESSIONID`, seguindo o módulo `03-spring-security` da trilha. As senhas são gravadas apenas codificadas com BCrypt.

Cada transação pertence à pessoa autenticada que a registrou. Todas as consultas e ferramentas da IA filtram por esse proprietário; o identificador não é aceito nas entradas nem devolvido nas respostas. O administrador pode aprovar e recusar cadastros, mas **não acessa dados financeiros** de nenhuma pessoa.

#### Limites conhecidos

Esta configuração é **educacional** e não está pronta para uso em navegador em produção:

- **CSRF desabilitado.** Como no módulo `03-spring-security` da trilha, a proteção contra *Cross-Site Request Forgery* está desligada para simplificar o consumo por `curl` e por clientes de API. Em um front-end de navegador com sessão por cookie, isso precisaria ser reativado.
- **Enumeração de usuários pelo cadastro.** O `POST /api/auth/register` responde `409` quando o nome já está em uso, revelando em uma única requisição quais nomes existem. É uma troca consciente por usabilidade: sem essa resposta, a pessoa não saberia que precisa escolher outro nome. Esconder a informação exigiria um cadastro com confirmação por e-mail, fora do escopo do projeto.
- **Resíduo de tempo no login.** Toda falha de login devolve o mesmo `401` genérico, mas uma conta `PENDING` falha antes de o BCrypt rodar e responde um pouco mais rápido que uma senha errada em conta ativa. O caso de usuário inexistente não tem esse problema: o Spring Security já roda o BCrypt contra um hash falso para igualar o tempo. Esse resíduo não é tratado porque revelaria apenas o que o `409` do cadastro acima já entrega de graça.

---

## 5. Como executar a aplicação

### Pré-requisitos

- **JDK 25** instalado
- **Docker** e **Docker Compose** em execução
- Chave da API da OpenAI (`OPENAI_API_KEY`)

### Variáveis de ambiente obrigatórias

| Variável | Papel |
|---|---|
| `OPENAI_API_KEY` | Chave da API da OpenAI usada pelo assistente |
| `APP_ADMIN_USERNAME` | Nome de usuário do administrador inicial |
| `APP_ADMIN_PASSWORD` | Senha em texto puro do administrador inicial, codificada com BCrypt antes de ser gravada |

Nenhuma credencial é versionada: as três variáveis são lidas do ambiente e a aplicação **não sobe** se alguma faltar, falhando na inicialização com o nome da variável ausente.

Na primeira inicialização, a aplicação cria o administrador com essas credenciais. As variáveis continuam obrigatórias nas inicializações seguintes, mas, se já existir uma conta com o papel `ROLE_ADMIN`, o inicializador não cria nem altera a conta existente. Portanto, trocar a senha depois exige alterar a conta no banco, não a variável.

### Subindo a aplicação

Ao atualizar uma instalação criada antes do isolamento por usuário, descarte uma vez o banco de desenvolvimento antigo. As transações anteriores não têm proprietário e o comando abaixo remove o volume e seus dados de forma definitiva:

```bash
docker compose down -v
```

Depois, configure as variáveis e suba a aplicação:

```bash
export OPENAI_API_KEY="sua_chave_openai_aqui"
export APP_ADMIN_USERNAME="admin"
export APP_ADMIN_PASSWORD="uma_senha_forte_aqui"
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
OPENAI_API_KEY="sua_chave_openai_aqui" \
APP_ADMIN_USERNAME="admin-de-teste" \
APP_ADMIN_PASSWORD="senha-de-teste-forte" \
./gradlew test
```

### 6.2 Collection do Insomnia

O arquivo [`docs/insomnia/budgeting-ai-assistant-insomnia.yaml`](docs/insomnia/budgeting-ai-assistant-insomnia.yaml), no formato Insomnia v5.1, pode ser importado diretamente no Insomnia em **Import → File**. A collection contém 13 requisições de autenticação, administração, transações e comandos de IA, organizadas em quatro pastas.

Depois de importar, abra o **Base Environment** e preencha `admin_password` e `user_password`. Nenhuma senha real é armazenada no arquivo. Ajuste também `admin_username`, `user_username`, datas, categoria e demais valores quando necessário.

Fluxo recomendado:

1. `Cadastrar usuário`; copie o `id` da resposta para `user_id` no ambiente.
2. `Login — Administrador`; o Insomnia grava automaticamente o `JSESSIONID`.
3. `Aprovar usuário`.
4. `Login — Usuário`; a sessão administrativa é substituída pela sessão da pessoa usuária.
5. Execute as requisições das pastas `Transações` e `Comandos de IA`.

Para voltar a uma rota administrativa, execute novamente `Login — Administrador`. Para o comando de voz, informe em `audio_file` o caminho absoluto de um arquivo de áudio.

### 6.3 Autenticação (`curl`)

Os exemplos usam `-c cookies.txt` para **gravar** o cookie de sessão e `-b cookies.txt` para **reenviá-lo** nas requisições seguintes.

#### 1. Cadastrar um usuário (`POST /api/auth/register`)

```bash
curl -i -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "maria", "password": "senha-forte"}'
```

Resposta (`201 Created`) — a conta nasce pendente e ainda **não** consegue entrar:

```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "username": "maria",
  "status": "PENDING"
}
```

#### 2. Entrar como administrador (`POST /api/auth/login`)

```bash
curl -i -c admin-cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "uma_senha_forte_aqui"}'
```

#### 3. Listar e aprovar cadastros pendentes (`/api/admin/users`)

```bash
# Lista as contas pendentes
curl -s -b admin-cookies.txt "http://localhost:8080/api/admin/users?status=PENDING"

# Aprova um cadastro (use o id devolvido no cadastro ou na listagem)
curl -i -b admin-cookies.txt -X POST \
  http://localhost:8080/api/admin/users/7c9e6679-7425-40de-944b-e07fc1f90ae7/approve

# Para recusar, troque /approve por /reject
```

#### 4. Entrar como o usuário aprovado (`POST /api/auth/login`)

```bash
curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "maria", "password": "senha-forte"}'
```

#### 5. Sair (`POST /api/auth/logout`)

```bash
curl -i -b cookies.txt -X POST http://localhost:8080/api/auth/logout
```

Responde `204 No Content` e invalida a sessão: reutilizar o mesmo cookie em um endpoint protegido passa a devolver `401`.

### 6.4 Exemplos de requisição (`curl`)

Todos os endpoints abaixo exigem a sessão obtida no passo 4 acima; sem o cookie, a resposta é `401`. Cada pessoa registra e consulta somente os próprios gastos, inclusive nos comandos de IA. Não envie proprietário nas requisições: ele é obtido da sessão e nunca aparece nas respostas.

#### 1. Registrar uma transação via REST (`POST /transactions`)

```bash
curl -i -b cookies.txt -X POST http://localhost:8080/transactions \
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
curl -s -b cookies.txt http://localhost:8080/transactions/GROCERIES
```

#### 3. Consultar resumo (total e quantidade) por categoria (`GET /transactions/summary`)

```bash
curl -s -b cookies.txt "http://localhost:8080/transactions/summary?category=GROCERIES"
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
curl -s -b cookies.txt "http://localhost:8080/transactions?from=2026-10-01&to=2026-10-31"
```

#### 5. Enviar comando em texto para a IA (`POST /transactions/ai/text`)

Permite testar todo o fluxo de interpretação e Tool Calling sem precisar de áudio:

```bash
# Registrar um gasto falando em linguagem natural
curl -s -b cookies.txt -X POST http://localhost:8080/transactions/ai/text \
  -H "Content-Type: application/json" \
  -d '{"message": "Almocei em um restaurante e gastei 45 reais"}'

# Consultar o total gasto em uma categoria
curl -s -b cookies.txt -X POST http://localhost:8080/transactions/ai/text \
  -H "Content-Type: application/json" \
  -d '{"message": "Quanto eu já gastei com mercado?"}'

# Consultar gastos do período usando referência relativa de data
curl -s -b cookies.txt -X POST http://localhost:8080/transactions/ai/text \
  -H "Content-Type: application/json" \
  -d '{"message": "Quais foram meus gastos deste mês?"}'
```

#### 6. Enviar comando de voz para a IA (`POST /transactions/ai`)

```bash
curl -b cookies.txt -X POST http://localhost:8080/transactions/ai \
  -F "file=@src/test/resources/audio/recording-1.m4a" \
  --output resposta.mp3
```

---

## 7. O que foi aprendido durante o desafio

- **A IA como mais um adaptador de entrada e saída da arquitetura:** em vez de acoplar anotações `@Tool` nos casos de uso ou deixar o `ChatClient` dentro do controller, posicionar o Tool Calling (`TransactionTools`) e os modelos da OpenAI (`SpringAiVoiceAssistantGateway`) na camada `infrastructure` mantém `domain` e `application` limpos, testáveis e independentes de framework.
- **Invariantes no domínio protegem também o Tool Calling:** validar apenas o DTO HTTP com `@Valid` não impede que o modelo de IA passe valores inválidos (como valor zero ou negativo) diretamente para uma tool. Colocar as regras de negócio na entidade de domínio (`Transaction`) garante consistência em qualquer canal de entrada.
- **Testes de contrato para ferramentas de IA:** como o LLM enxerga as ferramentas pelo JSON Schema gerado a partir das assinaturas e anotações Java, um teste automatizado de caracterização (`TransactionToolsContractTest`) evita que refatorações quebrem silenciosamente o que a IA recebe.
- **Evolução incremental com Spec-Driven Development (OpenSpec):** especificar requisitos e cenários (`WHEN`/`THEN`), desenhar a solução técnica e validar cada etapa com testes automatizados (unitários com Mockito e de integração com Testcontainers + MySQL real) trouxe previsibilidade e rastreabilidade do início ao fim do projeto.
