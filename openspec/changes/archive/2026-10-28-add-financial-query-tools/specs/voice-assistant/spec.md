# Spec Delta: voice-assistant

## ADDED Requirements

### Requirement: Ferramenta de resumo por categoria
A IA SHALL ter acesso a uma ferramenta que recebe uma categoria e devolve o resumo financeiro correspondente, contendo o nome da categoria, o total gasto em reais com duas casas decimais (`0.00` quando não houver transações) e a quantidade de transações.

#### Scenario: IA consulta o total gasto em uma categoria
- **WHEN** o comando da pessoa usuária pergunta "quanto gastei com mercado?"
- **THEN** a IA chama a ferramenta de resumo com a categoria `GROCERIES` e responde com base no total em reais e na quantidade devolvidos

### Requirement: Ferramenta de consulta por período
A IA SHALL ter acesso a uma ferramenta que recebe uma data inicial (`from`) e uma data final (`to`) no formato `YYYY-MM-DD` e devolve a lista de transações criadas dentro desse intervalo inclusivo (em UTC), com `id`, `description`, `category`, `value` em reais e `createdAt`. A ferramenta SHALL rejeitar intervalos em que a data inicial seja posterior à data final.

#### Scenario: IA consulta gastos de um período
- **WHEN** o comando da pessoa usuária pede os gastos de um período (como "quais foram meus gastos em outubro de 2026?")
- **THEN** a IA chama a ferramenta de consulta por período informando `from` e `to` e responde com base nas transações devolvidas

#### Scenario: Chamada da ferramenta de período com intervalo inválido
- **WHEN** a IA chama a ferramenta de consulta por período com `from` posterior a `to`
- **THEN** a chamada falha com erro de validação e o erro volta para a IA
