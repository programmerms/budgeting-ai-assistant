# Spec Delta: transactions

## ADDED Requirements

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

## REMOVED Requirements

### Requirement: Ausência de validação no registro (D-03)
**Reason**: O defeito D-03 foi corrigido. O domínio e a entrada HTTP passam a validar descrição, valor e categoria antes de persistir.
**Migration**: Substituído pelo requisito "Validação dos dados da transação". Requisições com dados inválidos deixam de retornar 201 e passam a retornar 400.

### Requirement: Categoria ausente gera erro genérico após salvar (D-03, D-06)
**Reason**: Os defeitos D-03 e D-06 foram corrigidos. A categoria nula passa a ser barrada antes da persistência e tratada como erro 400.
**Migration**: Substituído pelos requisitos "Validação dos dados da transação" e "Erros de validação com campos e mensagens".
