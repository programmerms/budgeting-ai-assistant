# voice-assistant Specification

## Purpose
Permitir que a pessoa usuária registre e consulte gastos falando: o sistema transcreve o áudio, usa IA com Tool Calling para executar a ação certa e responde em áudio. Esta é a linha de base do projeto e descreve o comportamento atual, inclusive os defeitos conhecidos.

## Requirements

### Requirement: Comando de voz com resposta em áudio
O sistema SHALL aceitar, em `POST /transactions/ai`, uma requisição multipart com o arquivo de áudio no campo `file`. O áudio SHALL ser transcrito para texto, interpretado pela IA e respondido com um áudio MP3, devolvido com status 200 como anexo de nome `audio.mp3`.

#### Scenario: Comando de registro por voz
- **WHEN** o cliente envia um áudio dizendo "gastei 80 reais no mercado"
- **THEN** o sistema responde HTTP 200 com conteúdo `audio/mp3` e cabeçalho `Content-Disposition` de anexo com nome `audio.mp3`
- **AND** uma transação de categoria `GROCERIES` é salva

#### Scenario: Requisição sem o campo de arquivo
- **WHEN** o cliente chama `POST /transactions/ai` sem o campo `file`
- **THEN** o sistema responde HTTP 400

### Requirement: Transcrição em português
A transcrição SHALL ser feita em português, orientada a frases de gastos financeiros (valor em reais, ação como "gastei" ou "paguei" e um local ou estabelecimento).

#### Scenario: Valor reconhecido na transcrição
- **WHEN** o áudio contém a frase "gastei 80 reais na farmácia"
- **THEN** o texto transcrito contém "80 reais"

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

### Requirement: Ferramenta de consulta por categoria
A IA SHALL ter acesso a uma ferramenta que lista as transações de uma categoria, devolvendo descrição, categoria e valor de cada uma.

#### Scenario: IA consulta gastos de uma categoria
- **WHEN** o comando transcrito é "quais foram meus gastos com carro?"
- **THEN** a IA chama a ferramenta de consulta com a categoria `AUTO` e responde com base nas transações devolvidas

### Requirement: Instruções da IA
A IA SHALL receber instruções fixas que a definem como assistente financeiro, encarregado de extrair os dados da transação, usar as ferramentas disponíveis e escolher a categoria mais adequada. As instruções hoje NÃO definem idioma, tamanho ou formato da resposta, nem informam a data atual.

#### Scenario: Categoria escolhida pela IA
- **WHEN** o comando é "abasteci o carro com 200 reais"
- **THEN** a IA registra a transação na categoria `AUTO` sem que a pessoa diga a categoria

### Requirement: Comandos independentes
Cada comando SHALL ser processado de forma independente: o sistema não guarda o histórico da conversa entre requisições.

#### Scenario: Comando que depende do anterior
- **WHEN** a pessoa envia "gastei 50 reais" e, numa segunda requisição, "foi no mercado"
- **THEN** a segunda requisição é interpretada sem conhecer a primeira

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
