# Spec Delta: transactions

## ADDED Requirements

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

## REMOVED Requirements

### Requirement: Representação do valor na saída (D-01)
**Reason**: O defeito D-01 foi corrigido. O valor deixa de ser exibido em centavos como se fosse em reais.
**Migration**: Substituído pelo requisito "Valor em reais na saída". O campo `amount` das respostas muda de `8000.0` para `80.00`; a entrada continua em centavos.
