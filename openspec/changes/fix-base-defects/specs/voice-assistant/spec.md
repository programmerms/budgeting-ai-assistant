# Spec Delta: voice-assistant

## ADDED Requirements

### Requirement: Valor em reais nas ferramentas da IA
As ferramentas SHALL devolver à IA o valor em reais, com duas casas decimais, e não em centavos. Assim, a IA recebe o valor correto e não precisa converter nada para informá-lo.

#### Scenario: Confirmação com valor em reais
- **WHEN** a IA registra um gasto de R$ 80,00 (salvo como `8000` centavos)
- **THEN** a ferramenta devolve o valor `80.00`, e a resposta da IA informa R$ 80,00

### Requirement: Leitura das instruções em UTF-8
O arquivo de instruções da IA SHALL ser lido explicitamente em UTF-8, independentemente da codificação padrão do sistema operacional, para que os acentos cheguem intactos à IA.

#### Scenario: Ambiente que não usa UTF-8
- **WHEN** a aplicação roda num ambiente cuja codificação padrão não é UTF-8
- **THEN** as instruções chegam à IA com os acentos corretos, por exemplo "Você é um assistente financeiro."

## REMOVED Requirements

### Requirement: Valor informado pela IA (D-01)
**Reason**: O defeito D-01 foi corrigido. As ferramentas deixam de devolver o valor em centavos.
**Migration**: Substituído pelo requisito "Valor em reais nas ferramentas da IA". Nenhuma ação é necessária por parte de quem usa a API.

### Requirement: Leitura das instruções com a codificação da plataforma (D-05)
**Reason**: O defeito D-05 foi corrigido. O arquivo de instruções passa a ser lido explicitamente em UTF-8.
**Migration**: Substituído pelo requisito "Leitura das instruções em UTF-8". Nenhuma ação é necessária por parte de quem usa a API.
