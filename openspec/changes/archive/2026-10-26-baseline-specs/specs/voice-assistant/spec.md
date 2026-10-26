# Spec Delta: voice-assistant

## Purpose

Permitir que a pessoa usuária registre e consulte gastos falando: o sistema transcreve o áudio, usa IA com Tool Calling para executar a ação certa e responde em áudio. Esta é a linha de base do projeto e descreve o comportamento atual, inclusive os defeitos conhecidos.

## ADDED Requirements

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
A IA SHALL ter acesso a uma ferramenta que registra uma transação a partir de descrição, valor em centavos e categoria, e que devolve a transação salva. A categoria SHALL ser escolhida pela IA conforme o contexto do comando. A ferramenta aplica as mesmas regras do registro via REST, ou seja, hoje não há validação (D-03).

#### Scenario: IA registra um gasto
- **WHEN** o comando transcrito é "paguei 40 reais na farmácia"
- **THEN** a IA chama a ferramenta de registro com categoria `PHARMA` e uma transação é salva

#### Scenario: Chamada da ferramenta sem valor
- **WHEN** a IA chama a ferramenta de registro sem informar o valor
- **THEN** a chamada falha antes de salvar, o erro técnico volta para a IA e ela pode tentar chamar a ferramenta de novo

### Requirement: Ferramenta de consulta por categoria
A IA SHALL ter acesso a uma ferramenta que lista as transações de uma categoria, devolvendo descrição, categoria e valor de cada uma.

#### Scenario: IA consulta gastos de uma categoria
- **WHEN** o comando transcrito é "quais foram meus gastos com carro?"
- **THEN** a IA chama a ferramenta de consulta com a categoria `AUTO` e responde com base nas transações devolvidas

### Requirement: Valor informado pela IA (D-01)
As ferramentas devolvem à IA o valor sem conversão de centavos para reais. O valor falado SHALL depender da interpretação do modelo: ele pode compensar a conversão sozinho ou informar um valor 100 vezes maior que o real. Esse é o efeito do defeito D-01 no fluxo de voz.

#### Scenario: Confirmação com valor sem conversão
- **WHEN** a IA registra um gasto de R$ 80,00 (salvo como `8000` centavos)
- **THEN** a ferramenta devolve o valor `8000.0`, e a resposta da IA pode citar "R$ 80,00" ou "8000 reais", conforme o modelo interprete o valor

### Requirement: Instruções da IA
A IA SHALL receber instruções fixas que a definem como assistente financeiro, encarregado de extrair os dados da transação, usar as ferramentas disponíveis e escolher a categoria mais adequada. As instruções hoje NÃO definem idioma, tamanho ou formato da resposta, nem informam a data atual.

#### Scenario: Categoria escolhida pela IA
- **WHEN** o comando é "abasteci o carro com 200 reais"
- **THEN** a IA registra a transação na categoria `AUTO` sem que a pessoa diga a categoria

### Requirement: Leitura das instruções com a codificação da plataforma (D-05)
O arquivo de instruções da IA SHALL ser lido hoje com a codificação padrão do sistema operacional, e não explicitamente em UTF-8. Esse é o defeito D-05.

#### Scenario: Ambiente que não usa UTF-8
- **WHEN** a aplicação roda num ambiente cuja codificação padrão não é UTF-8
- **THEN** os acentos das instruções podem chegar corrompidos à IA

### Requirement: Comandos independentes
Cada comando SHALL ser processado de forma independente: o sistema não guarda o histórico da conversa entre requisições.

#### Scenario: Comando que depende do anterior
- **WHEN** a pessoa envia "gastei 50 reais" e, numa segunda requisição, "foi no mercado"
- **THEN** a segunda requisição é interpretada sem conhecer a primeira

### Requirement: Falhas sem tratamento (D-06)
Falhas na transcrição, na IA ou na geração de voz, incluindo o envio de um arquivo de áudio vazio, SHALL resultar hoje em erro genérico HTTP 500, sem mensagem que explique o problema. Esse é o defeito D-06.

#### Scenario: Arquivo de áudio vazio
- **WHEN** o cliente envia `POST /transactions/ai` com um arquivo de 0 bytes no campo `file`
- **THEN** o sistema responde HTTP 500 com a mensagem de erro padrão

#### Scenario: Falha do provedor de IA
- **WHEN** o provedor de IA está indisponível ou recusa a chave de acesso
- **THEN** o sistema responde HTTP 500 com a mensagem de erro padrão
