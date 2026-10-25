# Spec Delta: transactions

## Purpose

Registrar e consultar transações financeiras (gastos) pela API REST, com descrição, valor e categoria. Esta é a linha de base do projeto: descreve o comportamento atual, inclusive os defeitos conhecidos, para que os próximos changes mostrem exatamente o que mudou.

## ADDED Requirements

### Requirement: Categorias disponíveis
O sistema SHALL aceitar apenas as categorias `GROCERIES`, `PHARMA` e `AUTO`, tanto no registro quanto na consulta de transações.

#### Scenario: Categoria conhecida
- **WHEN** uma transação é registrada com a categoria `PHARMA`
- **THEN** a transação é salva e a resposta mostra a categoria `PHARMA`

#### Scenario: Categoria desconhecida no registro
- **WHEN** `POST /transactions` recebe `"category": "RESTAURANT"`
- **THEN** o sistema responde HTTP 400 e nada é salvo

### Requirement: Registro de transação via REST
O sistema SHALL registrar uma transação por `POST /transactions`, recebendo JSON com `description`, `amount` (inteiro, em centavos) e `category`. A resposta SHALL ter status 201 e conter `id`, `category`, `description` e `amount`. O `id` SHALL ser um UUID gerado pelo próprio sistema.

#### Scenario: Registro com dados completos
- **WHEN** `POST /transactions` recebe `{"description": "Mercado", "amount": 8000, "category": "GROCERIES"}`
- **THEN** o sistema responde HTTP 201 com um `id` em formato UUID, `description` igual a `Mercado` e `category` igual a `GROCERIES`

#### Scenario: Identificadores diferentes
- **WHEN** duas transações idênticas são registradas em sequência
- **THEN** cada uma recebe um `id` diferente

### Requirement: Representação do valor na saída (D-01)
O valor SHALL ser recebido em centavos. Na saída, o sistema devolve hoje o mesmo número recebido, com duas casas decimais, **sem dividir por 100**. Esse é o defeito D-01: o valor em centavos aparece como se fosse em reais. Vale para a resposta REST e para o retorno das ferramentas da IA.

#### Scenario: Valor em centavos exibido sem conversão
- **WHEN** uma transação é registrada com `amount` igual a `8000` (R$ 80,00)
- **THEN** a resposta mostra `amount` igual a `8000.0`, e não `80.00`

#### Scenario: Valor na consulta
- **WHEN** existe uma transação salva com `amount` igual a `4000` e ela é consultada
- **THEN** a consulta mostra o valor `4000.0`

### Requirement: Ausência de validação no registro (D-03)
O sistema hoje NÃO valida os dados de uma transação. Descrição vazia e valor zero ou negativo SHALL ser aceitos e salvos, tanto pela API REST quanto pela ferramenta de registro chamada pela IA. Esse é o defeito D-03.

#### Scenario: Descrição vazia
- **WHEN** `POST /transactions` recebe `"description": ""` com valor e categoria válidos
- **THEN** o sistema responde HTTP 201 e a transação é salva com descrição vazia

#### Scenario: Valor zero ou negativo
- **WHEN** `POST /transactions` recebe `"amount": 0` ou `"amount": -500`
- **THEN** o sistema responde HTTP 201 e a transação é salva com esse valor

#### Scenario: Valor inválido via Tool Calling
- **WHEN** a IA chama a ferramenta de registro com valor negativo
- **THEN** a transação é salva sem nenhuma verificação

### Requirement: Categoria ausente gera erro genérico após salvar (D-03, D-06)
Quando a categoria vem nula, o sistema SHALL salvar a transação sem categoria e, em seguida, falhar ao montar a resposta, devolvendo um erro genérico HTTP 500 sem indicar o campo com problema.

#### Scenario: Categoria nula
- **WHEN** `POST /transactions` recebe `"category": null` com descrição e valor preenchidos
- **THEN** o sistema responde HTTP 500 com a mensagem de erro padrão
- **AND** a transação fica salva no banco sem categoria

### Requirement: Consulta de transações por categoria
O sistema SHALL listar, por `GET /transactions/{category}`, todas as transações da categoria informada, com status 200. Cada item SHALL conter `id`, `category`, `description` e `amount`. Não há paginação, filtro por período nem ordenação definida.

#### Scenario: Categoria com transações
- **WHEN** existem duas transações em `GROCERIES` e uma em `AUTO`, e é feita a consulta `GET /transactions/GROCERIES`
- **THEN** o sistema responde HTTP 200 com uma lista de exatamente duas transações, ambas da categoria `GROCERIES`

#### Scenario: Categoria sem transações
- **WHEN** não existe nenhuma transação em `PHARMA` e é feita a consulta `GET /transactions/PHARMA`
- **THEN** o sistema responde HTTP 200 com uma lista vazia

#### Scenario: Categoria inválida na consulta
- **WHEN** é feita a consulta `GET /transactions/RESTAURANT`
- **THEN** o sistema responde HTTP 400

### Requirement: Transação sem data de criação (D-07)
A transação hoje NÃO registra quando foi criada. A resposta SHALL conter apenas `id`, `category`, `description` e `amount`, e não existe consulta por período. Esse é o defeito D-07.

#### Scenario: Resposta sem data
- **WHEN** uma transação é registrada
- **THEN** a resposta não contém nenhum campo de data ou hora

### Requirement: Persistência das transações
As transações registradas SHALL ficar salvas em banco de dados relacional e continuar disponíveis para consulta em requisições seguintes, sem separação por usuário.

#### Scenario: Transação disponível na consulta
- **WHEN** uma transação é registrada em `AUTO` e depois é feita a consulta `GET /transactions/AUTO`
- **THEN** a transação registrada aparece na lista
