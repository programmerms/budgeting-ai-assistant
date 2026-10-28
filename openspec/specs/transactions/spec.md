# transactions Specification

## Purpose
Registrar e consultar transações financeiras (gastos) pela API REST, com descrição, valor e categoria. Esta é a linha de base do projeto: descreve o comportamento atual, inclusive os defeitos conhecidos, para que os próximos changes mostrem exatamente o que mudou.

## Requirements

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

### Requirement: Persistência das transações
As transações registradas SHALL ficar salvas em banco de dados relacional e continuar disponíveis para consulta em requisições seguintes, sem separação por usuário.

#### Scenario: Transação disponível na consulta
- **WHEN** uma transação é registrada em `AUTO` e depois é feita a consulta `GET /transactions/AUTO`
- **THEN** a transação registrada aparece na lista

### Requirement: Valor em reais na saída
O valor SHALL ser recebido em centavos, como número inteiro. Na saída, o sistema SHALL devolver o valor em reais, dividido por 100 e com exatamente duas casas decimais. Vale para a resposta REST e para o retorno das ferramentas da IA.

#### Scenario: Valor em centavos convertido para reais
- **WHEN** uma transação é registrada com `amount` igual a `8000` (R$ 80,00)
- **THEN** a resposta mostra `amount` igual a `80.00`

#### Scenario: Valor com centavos
- **WHEN** uma transação é registrada com `amount` igual a `8050`
- **THEN** a resposta mostra `amount` igual a `80.50`

#### Scenario: Menor valor positivo
- **WHEN** uma transação é registrada com `amount` igual a `1`
- **THEN** a resposta mostra `amount` igual a `0.01`

#### Scenario: Valor na consulta
- **WHEN** existe uma transação salva com `amount` igual a `4000` e ela é consultada
- **THEN** a consulta mostra o valor `40.00`

### Requirement: Validação dos dados da transação
O sistema SHALL rejeitar o registro de uma transação quando a descrição estiver nula ou em branco, quando o valor em centavos for menor ou igual a zero ou quando a categoria estiver ausente. A regra SHALL valer tanto para a API REST quanto para a ferramenta de registro chamada pela IA, sem salvar nada no banco de dados.

#### Scenario: Descrição vazia ou em branco
- **WHEN** `POST /transactions` recebe `"description": ""` ou `"description": "   "` com valor e categoria válidos
- **THEN** o sistema responde HTTP 400 indicando o campo `description`
- **AND** nenhuma transação é salva

#### Scenario: Valor zero ou negativo
- **WHEN** `POST /transactions` recebe `"amount": 0` ou `"amount": -500` com descrição e categoria válidas
- **THEN** o sistema responde HTTP 400 indicando o campo `amount`
- **AND** nenhuma transação é salva

#### Scenario: Categoria nula
- **WHEN** `POST /transactions` recebe `"category": null` com descrição e valor válidos
- **THEN** o sistema responde HTTP 400 indicando o campo `category`
- **AND** nenhuma transação é salva

#### Scenario: Dados inválidos via Tool Calling
- **WHEN** a IA chama a ferramenta de registro com descrição em branco, valor menor ou igual a zero ou categoria nula
- **THEN** a chamada falha antes de salvar, nenhuma transação é persistida e o erro é devolvido à IA

### Requirement: Erros de validação com campos e mensagens
Quando uma requisição a `POST /transactions` falhar na validação dos campos, o sistema SHALL responder HTTP 400 com um objeto JSON em que cada chave é o nome do campo inválido (`description`, `amount`, `category`) e o valor é a mensagem de erro correspondente.

#### Scenario: Múltiplos campos inválidos
- **WHEN** `POST /transactions` recebe `"description": ""`, `"amount": 0` e `"category": null`
- **THEN** o sistema responde HTTP 400 com um JSON contendo as chaves `description`, `amount` e `category`, cada uma com sua mensagem de erro
- **AND** nenhuma transação é salva

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
