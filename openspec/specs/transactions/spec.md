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
O sistema SHALL registrar uma transação por `POST /transactions`, recebendo JSON com `description`, `amount` (inteiro, em centavos) e `category`. A transação SHALL ser associada ao usuário autenticado da sessão. A resposta SHALL ter status 201 e conter `id`, `category`, `description`, `amount` e `createdAt`. O `id` SHALL ser um UUID gerado pelo próprio sistema.

#### Scenario: Registro com dados completos
- **WHEN** um usuário autenticado chama `POST /transactions` com `{"description": "Mercado", "amount": 8000, "category": "GROCERIES"}`
- **THEN** o sistema responde HTTP 201 com um `id` em formato UUID, `description` igual a `Mercado`, `category` igual a `GROCERIES`, `amount` igual a `80.00` e `createdAt` preenchido

#### Scenario: Identificadores diferentes
- **WHEN** duas transações idênticas são registradas em sequência
- **THEN** cada uma recebe um `id` diferente

### Requirement: Consulta de transações por categoria
O sistema SHALL listar, por `GET /transactions/{category}`, as transações do usuário autenticado na categoria informada, com status 200. Cada item SHALL conter `id`, `category`, `description`, `amount` e `createdAt`. Não há paginação, filtro por período nem ordenação definida.

#### Scenario: Categoria com transações
- **WHEN** o usuário autenticado tem duas transações em `GROCERIES` e uma em `AUTO`, e é feita a consulta `GET /transactions/GROCERIES`
- **THEN** o sistema responde HTTP 200 com uma lista de exatamente duas transações, ambas da categoria `GROCERIES` e com `createdAt` preenchido

#### Scenario: Categoria sem transações
- **WHEN** o usuário autenticado não tem nenhuma transação em `PHARMA` e é feita a consulta `GET /transactions/PHARMA`
- **THEN** o sistema responde HTTP 200 com uma lista vazia

#### Scenario: Categoria inválida na consulta
- **WHEN** é feita a consulta `GET /transactions/INVALID`
- **THEN** o sistema responde HTTP 400

### Requirement: Persistência das transações
As transações registradas SHALL ficar salvas em banco de dados relacional, com o proprietário gravado junto, e continuar disponíveis em consultas seguintes do mesmo proprietário.

#### Scenario: Transação disponível na consulta
- **WHEN** um usuário autenticado registra uma transação em `AUTO` e depois faz a consulta `GET /transactions/AUTO`
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

### Requirement: Resumo de transações por categoria
O sistema SHALL devolver, em `GET /transactions/summary?category={category}`, o resumo financeiro da categoria informada, considerando somente as transações do usuário autenticado, com status 200, contendo `category`, `total` (soma em reais com duas casas decimais) e `count` (quantidade de transações). A mesma regra de cálculo e de proprietário SHALL valer para a ferramenta de resumo chamada pela IA.

#### Scenario: Categoria com múltiplas transações
- **WHEN** o usuário autenticado tem transações de `8000` centavos (R$ 80,00) e `4000` centavos (R$ 40,00) em `GROCERIES` e uma de `5000` centavos em `AUTO`, e é feita a consulta `GET /transactions/summary?category=GROCERIES`
- **THEN** o sistema responde HTTP 200 com `category` igual a `GROCERIES`, `total` igual a `120.00` e `count` igual a `2`

#### Scenario: Categoria sem transações no resumo
- **WHEN** o usuário autenticado não tem nenhuma transação em `PHARMA` e é feita a consulta `GET /transactions/summary?category=PHARMA`
- **THEN** o sistema responde HTTP 200 com `category` igual a `PHARMA`, `total` igual a `0.00` e `count` igual a `0`

#### Scenario: Resumo calculado via Tool Calling
- **WHEN** a IA chama a ferramenta de resumo para a categoria `GROCERIES` contendo transações de R$ 80,00 e R$ 40,00 do usuário autenticado
- **THEN** a ferramenta devolve `category` igual a `GROCERIES`, `total` igual a `120.00` e `count` igual a `2`

#### Scenario: Parâmetro de categoria ausente ou inválido no resumo
- **WHEN** é feita a consulta `GET /transactions/summary` sem `category` ou com `category=INVALID`
- **THEN** o sistema responde HTTP 400

### Requirement: Consulta de transações por período
O sistema SHALL listar, em `GET /transactions?from={from}&to={to}`, as transações do usuário autenticado cuja data de criação (`createdAt` em UTC) esteja dentro do intervalo inclusivo entre o início do dia `from` (`00:00:00Z`) e o fim do dia `to` (`23:59:59.999999999Z`), no formato ISO `YYYY-MM-DD`, com status 200. A mesma regra de filtro, de proprietário e de validação de intervalo SHALL valer para a ferramenta de consulta por período chamada pela IA.

#### Scenario: Transações dentro e fora do período
- **WHEN** o usuário autenticado tem transações criadas em `2026-10-05`, `2026-10-15` e `2026-11-01`, e é feita a consulta `GET /transactions?from=2026-10-01&to=2026-10-31`
- **THEN** o sistema responde HTTP 200 contendo apenas as duas transações de outubro, cada uma com `id`, `category`, `description`, `amount` e `createdAt`

#### Scenario: Período sem transações
- **WHEN** o usuário autenticado não tem transações no intervalo informado e é feita a consulta `GET /transactions?from=2026-01-01&to=2026-01-31`
- **THEN** o sistema responde HTTP 200 com uma lista vazia

#### Scenario: Data inicial posterior à data final via REST
- **WHEN** é feita a consulta `GET /transactions?from=2026-10-31&to=2026-10-01`
- **THEN** o sistema responde HTTP 400 informando que a data inicial não pode ser posterior à data final

#### Scenario: Data inicial posterior à data final via Tool Calling
- **WHEN** a IA chama a ferramenta de consulta por período com `from` posterior a `to`
- **THEN** a chamada falha na validação de intervalo e devolve o erro à IA

#### Scenario: Parâmetros de período ausentes ou em formato inválido
- **WHEN** é feita a consulta `GET /transactions` sem `from` ou `to`, ou com data em formato inválido
- **THEN** o sistema responde HTTP 400

### Requirement: Propriedade obrigatória da transação
Toda transação SHALL ter um proprietário obrigatório, correspondente ao usuário autenticado que a registrou. O sistema SHALL recusar a criação de uma transação sem proprietário. O registro e as consultas de transações SHALL sempre informar o proprietário, e SHALL NOT existir nenhuma operação de leitura global, sem proprietário, no contrato de persistência.

#### Scenario: Transação registrada guarda o proprietário
- **WHEN** um usuário autenticado registra uma transação
- **THEN** a transação persistida fica associada ao identificador desse usuário

#### Scenario: Transação sem proprietário é recusada
- **WHEN** uma transação é criada sem proprietário
- **THEN** a criação falha na validação de domínio e nada é persistido

### Requirement: Isolamento das transações entre usuários
O sistema SHALL devolver, em todas as consultas financeiras, somente as transações do usuário autenticado. Transações de outras pessoas SHALL NOT aparecer em listagens, SHALL NOT entrar em somas e SHALL NOT ser contadas, mesmo quando compartilharem categoria, valor ou período.

#### Scenario: Consulta por categoria não mostra transação de outra pessoa
- **WHEN** o usuário A tem uma transação em `GROCERIES`, o usuário B não tem nenhuma, e B consulta `GET /transactions/GROCERIES`
- **THEN** o sistema responde HTTP 200 com uma lista vazia

#### Scenario: Resumo não soma valores de outra pessoa
- **WHEN** o usuário A tem uma transação de `8000` centavos em `GROCERIES`, o usuário B tem uma de `4000` centavos na mesma categoria, e B consulta `GET /transactions/summary?category=GROCERIES`
- **THEN** o sistema responde HTTP 200 com `total` igual a `40.00` e `count` igual a `1`

#### Scenario: Consulta por período não inclui transação de outra pessoa
- **WHEN** os usuários A e B têm, cada um, uma transação criada no mesmo dia, e B consulta `GET /transactions` com um período que cobre esse dia
- **THEN** o sistema responde HTTP 200 com exatamente uma transação, a do próprio B

#### Scenario: Cada pessoa enxerga o próprio registro
- **WHEN** os usuários A e B registram transações com a mesma descrição, valor e categoria, e cada um consulta essa categoria
- **THEN** cada consulta devolve exatamente uma transação, com o `id` daquela registrada pelo próprio usuário

### Requirement: Proprietário não controlável pela entrada
O proprietário SHALL ser determinado exclusivamente pela sessão autenticada. Campos de proprietário enviados no corpo da requisição, em parâmetros de consulta ou em qualquer outro dado de entrada SHALL ser ignorados. A resposta SHALL NOT expor o identificador do proprietário.

#### Scenario: Proprietário enviado no corpo é ignorado
- **WHEN** um usuário autenticado chama `POST /transactions` com um campo de proprietário apontando para outra pessoa
- **THEN** a transação é criada associada ao usuário autenticado, e não ao identificador enviado

#### Scenario: Resposta não expõe o proprietário
- **WHEN** um usuário autenticado registra ou consulta transações
- **THEN** nenhuma resposta contém o identificador do proprietário
