# Spec Delta: voice-assistant

## MODIFIED Requirements

### Requirement: Instruções da IA
A IA SHALL receber instruções de sistema que a definem como assistente financeiro e orientam a responder sempre em português do Brasil, de forma curta e direta, com valores monetários expressos em reais (ex.: R$ 80,00). As instruções SHALL informar a data atual para consultas por período, orientar a escolha da categoria mais adequada entre as disponíveis e exigir pedido de esclarecimento quando faltar valor ou descrição antes de registrar um gasto.

#### Scenario: Categoria escolhida pela IA
- **WHEN** o comando é "abasteci o carro com 200 reais"
- **THEN** a IA registra a transação na categoria `AUTO` sem que a pessoa diga a categoria

#### Scenario: Data atual disponível nas instruções para consultas por período
- **WHEN** a IA recebe um comando de consulta relativa ao tempo (como "quais foram meus gastos de hoje?" ou "este mês")
- **THEN** as instruções de sistema contêm a data atual em formato ISO (`YYYY-MM-DD`) para que a IA calcule o intervalo `from` e `to`

#### Scenario: Pedido de esclarecimento quando faltar valor ou descrição
- **WHEN** o comando para registrar um gasto omite o valor ou a descrição (como "gastei no mercado" sem informar quanto)
- **THEN** as instruções orientam a IA a pedir esclarecimento em português em vez de inventar dados para chamar a ferramenta de registro
