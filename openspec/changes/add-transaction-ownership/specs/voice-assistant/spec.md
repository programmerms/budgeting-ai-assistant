# Spec Delta

## ADDED Requirements

### Requirement: Isolamento do proprietário nas ferramentas da IA
As ferramentas chamadas pela IA SHALL operar somente sobre as transações do usuário autenticado da requisição, obtendo o proprietário do contexto de segurança do servidor. O identificador do proprietário SHALL NOT aparecer no contrato exposto ao modelo nem ser aceito como parâmetro de ferramenta. Nenhuma instrução contida no comando da pessoa usuária SHALL conseguir alterar o proprietário de um registro ou de uma consulta.

#### Scenario: Registro por comando associa ao usuário autenticado
- **WHEN** um usuário autenticado envia um comando que registra um gasto
- **THEN** a transação é salva associada a esse usuário

#### Scenario: Consulta por comando não enxerga transação de outra pessoa
- **WHEN** o usuário A tem uma transação em `GROCERIES` e o usuário B pede, por comando, os gastos dessa categoria
- **THEN** as ferramentas devolvem apenas as transações de B, sem nenhuma de A

#### Scenario: Contrato exposto ao modelo não tem proprietário
- **WHEN** as definições das ferramentas da IA são geradas pelo sistema
- **THEN** nenhuma ferramenta declara parâmetro de proprietário no JSON Schema entregue ao modelo

#### Scenario: Comando tenta escolher outro proprietário
- **WHEN** um usuário autenticado envia um comando pedindo que o gasto seja registrado em nome de outra pessoa
- **THEN** a transação é salva associada ao usuário autenticado, ignorando a instrução

#### Scenario: Mesma regra nos comandos por texto e por voz
- **WHEN** o mesmo comando de consulta é enviado por `POST /transactions/ai` e por `POST /transactions/ai/text`
- **THEN** os dois caminhos aplicam o proprietário autenticado e devolvem apenas os dados dessa pessoa
