# Spec Delta

## MODIFIED Requirements

### Requirement: Instruções da IA
A IA SHALL receber instruções de sistema para responder em português do Brasil, de forma curta e direta, exibir valores em reais e usar a data atual nas consultas por período. Ao registrar, SHALL reconhecer a descrição presente no contexto, inferir a categoria sem solicitá-la, usar `OTHER` quando nenhuma categoria específica se aplicar com segurança e pedir esclarecimento somente quando faltar valor ou descrição.

#### Scenario: Categoria escolhida pela IA
- **WHEN** o comando é "abasteci o carro com 200 reais"
- **THEN** a IA registra a transação na categoria `AUTO` sem que a pessoa diga a categoria

#### Scenario: Data atual disponível nas instruções para consultas por período
- **WHEN** a IA recebe um comando de consulta relativa ao tempo (como "quais foram meus gastos de hoje?" ou "este mês")
- **THEN** as instruções de sistema contêm a data atual em formato ISO (`YYYY-MM-DD`) para que a IA calcule o intervalo `from` e `to`

#### Scenario: Pedido de esclarecimento quando faltar valor ou descrição
- **WHEN** o comando para registrar um gasto omite o valor ou a descrição (como "gastei no mercado" sem informar quanto)
- **THEN** as instruções orientam a IA a pedir esclarecimento em português em vez de inventar dados para chamar a ferramenta de registro

#### Scenario: Descrição e valor reconhecidos em linguagem natural
- **WHEN** o comando é "registrar por favor, contratei um serviço particular e gastei 1000 reais"
- **THEN** a IA considera o contexto informado como descrição, converte o valor para `100000` centavos e chama a ferramenta de registro sem pedir novamente a descrição ou a categoria

#### Scenario: Categoria de fallback para contexto ambíguo
- **WHEN** um comando de registro contém descrição e valor, mas o contexto não corresponde com segurança a uma categoria específica
- **THEN** a IA usa a categoria `OTHER` sem pedir que a pessoa usuária escolha uma categoria
