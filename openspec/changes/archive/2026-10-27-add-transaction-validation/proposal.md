# Proposal: add-transaction-validation

## Why

Hoje a aplicação aceita e salva transações com descrição vazia, valor zero ou negativo e categoria nula, tanto pela API REST quanto pelo fluxo de Tool Calling da IA (D-03). Além disso, não existe tratamento padronizado de erros HTTP (D-06): quando a categoria vem nula no `POST /transactions`, a transação é salva no banco e a requisição falha em seguida com erro 500; e quando `POST /transactions/ai` recebe um arquivo de áudio vazio, o sistema tenta enviá-lo ao provedor de transcrição e também responde 500. Este é o change 2 da seção 5 da proposta inicial.

## What Changes

- **Invariantes no domínio (D-03, RF-02):** `Transaction` e `TransactionId` passam a validar seus dados na construção com `org.springframework.util.Assert` (id e categoria não nulos, descrição com texto e valor em centavos maior que zero). Qualquer tentativa de criar uma transação inválida lança `IllegalArgumentException` antes de chegar ao repositório, protegendo tanto o canal REST quanto o Tool Calling da IA.
- **Bean Validation na entrada HTTP (D-03, RF-02):** `TransactionRequest` recebe anotações de validação (`@NotBlank`, `@NotNull`, `@Positive`) e o endpoint `POST /transactions` valida o corpo com `@Valid`. **BREAKING** para clientes que enviavam dados inválidos: requisições com descrição vazia, valor `<= 0` ou categoria nula passam a receber HTTP 400 em vez de 201 (ou 500 no caso de categoria nula), e nada é salvo.
- **Validação de áudio vazio na entrada HTTP (D-06, RF-03):** `POST /transactions/ai` passa a rejeitar arquivo de áudio vazio (`0` bytes) antes de acionar os modelos de IA, respondendo HTTP 400 com mensagem clara.
- **Tratamento global de erros HTTP (D-06, RF-03):** novo `GlobalExceptionHandler` (`@RestControllerAdvice`) que converte `MethodArgumentNotValidException` em HTTP 400 com um mapa `campo -> mensagem` e `IllegalArgumentException` em HTTP 400 com a mensagem do erro.

Defeitos tratados (detalhes em `docs/proposta-inicial.md`, seção 2):

- D-03: nenhuma validação dos dados da transação
- D-06: sem tratamento de erros para entradas inválidas

Requisitos da proposta inicial atendidos: RF-02 e RF-03, além de manter RNF-01 e ampliar RNF-02 com os testes unitários e de integração das validações.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `transactions`: os requisitos que descreviam os defeitos "Ausência de validação no registro (D-03)" e "Categoria ausente gera erro genérico após salvar (D-03, D-06)" são removidos e substituídos por "Validação dos dados da transação" e "Erros de validação com campos e mensagens".
- `voice-assistant`: o requisito "Ferramenta de registro de transação" é atualizado para refletir que a ferramenta aplica as regras de validação do domínio e devolve o erro à IA sem salvar quando os dados são inválidos; o requisito "Falhas sem tratamento (D-06)" é substituído por "Rejeição de arquivo de áudio vazio" (HTTP 400) e "Falha do provedor de IA sem tratamento específico".

## Impact

- **Código de produção:** `Transaction` e `TransactionId` (invariantes com `Assert`), `TransactionRequest` (Bean Validation), `TransactionController` (`@Valid` no `POST /transactions` e checagem de arquivo vazio no `POST /transactions/ai`) e novo `GlobalExceptionHandler` em `infrastructure/http`.
- **API:** `POST /transactions` com campos inválidos responde HTTP 400 com JSON `{"campo": "mensagem"}`; `POST /transactions/ai` com arquivo vazio responde HTTP 400.
- **Dependências:** adição de `org.springframework.boot:spring-boot-starter-validation` no `build.gradle` (mesmo starter do módulo `01-spring-web`).
- **Testes:** novos testes unitários de domínio (`TransactionTest`, `TransactionIdTest`), novos cenários em `PersistTransactionUseCaseTest` e `TransactionApiIntegrationTest`, e teste unitário de `GlobalExceptionHandlerTest`.

## Fora do escopo

- Extrair a orquestração de IA do `TransactionController` para um caso de uso próprio (D-02) e criar o endpoint `POST /transactions/ai/text`: change 5 (`extract-voice-command-use-case`).
- Data de criação (`createdAt`) e novas categorias no enum `Category` (D-07): change 3 (`add-transaction-timestamp-and-categories`).
- Tratamento customizado para indisponibilidade ou falha de autenticação da OpenAI em tempo de execução: permanece o comportamento padrão do Spring Boot, pois a trilha (`01-spring-web` e `05-spring-ai`) trata no `@RestControllerAdvice` apenas erros de validação e de domínio/aplicação.
- Alterar o contrato (JSON Schema) das ferramentas da IA ou os testes `*IT` herdados do curso.
