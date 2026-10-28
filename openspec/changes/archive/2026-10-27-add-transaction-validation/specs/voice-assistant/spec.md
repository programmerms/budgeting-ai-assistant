# Spec Delta: voice-assistant

## ADDED Requirements

### Requirement: Rejeição de arquivo de áudio vazio
O sistema SHALL rejeitar requisições a `POST /transactions/ai` cujo arquivo no campo `file` esteja vazio (`0` bytes), respondendo HTTP 400 com mensagem explicativa antes de acionar a transcrição, o chat ou a geração de voz.

#### Scenario: Arquivo de áudio vazio
- **WHEN** o cliente envia `POST /transactions/ai` com um arquivo de 0 bytes no campo `file`
- **THEN** o sistema responde HTTP 400 com uma mensagem informando que o arquivo de áudio não pode estar vazio

### Requirement: Falha do provedor de IA sem tratamento específico
Quando o provedor de IA estiver indisponível ou recusar a chave de acesso durante `POST /transactions/ai`, o sistema SHALL responder HTTP 500 com a estrutura de erro padrão.

#### Scenario: Falha do provedor de IA
- **WHEN** o provedor de IA está indisponível ou recusa a chave de acesso
- **THEN** o sistema responde HTTP 500 com a mensagem de erro padrão

## MODIFIED Requirements

### Requirement: Ferramenta de registro de transação
A IA SHALL ter acesso a uma ferramenta que registra uma transação a partir de descrição, valor em centavos e categoria, e que devolve a transação salva. A categoria SHALL ser escolhida pela IA conforme o contexto do comando. A ferramenta SHALL aplicar as mesmas regras de validação do registro via REST (descrição preenchida, valor em centavos maior que zero e categoria obrigatória), sem salvar quando algum dado for inválido.

#### Scenario: IA registra um gasto
- **WHEN** o comando transcrito é "paguei 40 reais na farmácia"
- **THEN** a IA chama a ferramenta de registro com categoria `PHARMA` e uma transação é salva

#### Scenario: Chamada da ferramenta sem valor
- **WHEN** a IA chama a ferramenta de registro sem informar o valor
- **THEN** a chamada falha antes de salvar, o erro técnico volta para a IA e ela pode tentar chamar a ferramenta de novo

#### Scenario: Chamada da ferramenta com valor menor ou igual a zero ou descrição vazia
- **WHEN** a IA chama a ferramenta de registro com valor menor ou igual a zero ou com descrição em branco
- **THEN** a chamada falha na validação de domínio antes de salvar, nada é persistido e o erro volta para a IA informar a pessoa usuária

## REMOVED Requirements

### Requirement: Falhas sem tratamento (D-06)
**Reason**: O defeito D-06 foi corrigido para entradas inválidas do cliente (arquivo de áudio vazio agora retorna HTTP 400).
**Migration**: Substituído por "Rejeição de arquivo de áudio vazio" (HTTP 400) e "Falha do provedor de IA sem tratamento específico" (mantém HTTP 500 para falhas externas do provedor).
