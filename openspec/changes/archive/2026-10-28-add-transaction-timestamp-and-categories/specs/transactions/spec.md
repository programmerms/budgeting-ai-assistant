# Spec Delta: transactions

## ADDED Requirements

### Requirement: Data de criação automática da transação
O sistema SHALL preencher automaticamente a data e hora de criação (`createdAt`, em UTC no formato ISO-8601) no momento em que a transação é salva no banco de dados, sem exigir esse campo na entrada. O valor de `createdAt` SHALL estar presente na resposta de registro, na listagem por categoria e no retorno das ferramentas chamadas pela IA.

#### Scenario: Data preenchida automaticamente no registro via REST
- **WHEN** `POST /transactions` recebe uma transação válida sem informar data de criação
- **THEN** o sistema responde HTTP 201 com o campo `createdAt` preenchido automaticamente com o instante da criação

#### Scenario: Data preservada na consulta por categoria
- **WHEN** uma transação salva é consultada por `GET /transactions/{category}`
- **THEN** o item retornado contém o mesmo `createdAt` registrado no momento da criação

#### Scenario: Data devolvida nas ferramentas da IA
- **WHEN** a IA registra ou lista transações por meio das ferramentas disponíveis
- **THEN** cada transação retornada à IA contém o campo `createdAt` preenchido

## MODIFIED Requirements

### Requirement: Categorias disponíveis
O sistema SHALL aceitar as categorias `GROCERIES`, `PHARMA`, `AUTO`, `RESTAURANT`, `SHOPPING`, `TRANSPORT`, `HEALTH`, `LEISURE` e `OTHER`, tanto no registro quanto na consulta de transações via REST e nas ferramentas disponibilizadas à IA.

#### Scenario: Categoria conhecida
- **WHEN** uma transação é registrada com a categoria `PHARMA`
- **THEN** a transação é salva e a resposta mostra a categoria `PHARMA`

#### Scenario: Nova categoria no registro e na consulta
- **WHEN** uma transação é registrada com uma das novas categorias (como `RESTAURANT`, `SHOPPING`, `TRANSPORT`, `HEALTH`, `LEISURE` ou `OTHER`) e em seguida consultada em `GET /transactions/{category}`
- **THEN** a transação é salva com status 201 e retornada na listagem da categoria correspondente com status 200

#### Scenario: Categorias disponíveis para Tool Calling da IA
- **WHEN** as definições das ferramentas da IA são geradas pelo sistema
- **THEN** o esquema de parâmetros aceita todas as nove categorias (`GROCERIES`, `PHARMA`, `AUTO`, `RESTAURANT`, `SHOPPING`, `TRANSPORT`, `HEALTH`, `LEISURE` e `OTHER`)

#### Scenario: Categoria desconhecida no registro
- **WHEN** `POST /transactions` recebe `"category": "INVALID"`
- **THEN** o sistema responde HTTP 400 e nada é salvo

### Requirement: Registro de transação via REST
O sistema SHALL registrar uma transação por `POST /transactions`, recebendo JSON com `description`, `amount` (inteiro, em centavos) e `category`. A resposta SHALL ter status 201 e conter `id`, `category`, `description`, `amount` e `createdAt`. O `id` SHALL ser um UUID gerado pelo próprio sistema.

#### Scenario: Registro com dados completos
- **WHEN** `POST /transactions` recebe `{"description": "Mercado", "amount": 8000, "category": "GROCERIES"}`
- **THEN** o sistema responde HTTP 201 com um `id` em formato UUID, `description` igual a `Mercado`, `category` igual a `GROCERIES`, `amount` igual a `80.00` e `createdAt` preenchido

#### Scenario: Identificadores diferentes
- **WHEN** duas transações idênticas são registradas em sequência
- **THEN** cada uma recebe um `id` diferente

### Requirement: Consulta de transações por categoria
O sistema SHALL listar, por `GET /transactions/{category}`, todas as transações da categoria informada, com status 200. Cada item SHALL conter `id`, `category`, `description`, `amount` e `createdAt`. Não há paginação, filtro por período nem ordenação definida.

#### Scenario: Categoria com transações
- **WHEN** existem duas transações em `GROCERIES` e uma em `AUTO`, e é feita a consulta `GET /transactions/GROCERIES`
- **THEN** o sistema responde HTTP 200 com uma lista de exatamente duas transações, ambas da categoria `GROCERIES` e com `createdAt` preenchido

#### Scenario: Categoria sem transações
- **WHEN** não existe nenhuma transação em `PHARMA` e é feita a consulta `GET /transactions/PHARMA`
- **THEN** o sistema responde HTTP 200 com uma lista vazia

#### Scenario: Categoria inválida na consulta
- **WHEN** é feita a consulta `GET /transactions/INVALID`
- **THEN** o sistema responde HTTP 400

## REMOVED Requirements

### Requirement: Transação sem data de criação (D-07)
**Reason**: O defeito D-07 foi corrigido. Toda transação passa a registrar automaticamente a data e hora em que foi criada.
**Migration**: Substituído pelo requisito "Data de criação automática da transação". As respostas de criação e listagem passam a incluir o campo `createdAt`.
