# Proposal: fix-base-defects

## Why

As specs de linha de base confirmaram três defeitos que bloqueiam qualquer evolução: o valor em centavos aparece como se fosse em reais (D-01), o prompt da IA é lido sem garantir UTF-8 (D-05) e `./gradlew test` falha numa máquina limpa (D-04). Sem uma suíte que roda, nenhum change seguinte consegue provar que funciona. Este é o change 1 da seção 5 da proposta inicial.

## What Changes

- **D-01:** a saída passa a mostrar o valor em reais, com duas casas decimais (`8000` vira `80.00`), na API REST e no retorno das ferramentas da IA. A entrada continua em centavos. **BREAKING** para quem consome a API: o campo `amount` da resposta muda de `8000.0` para `80.00`.
- **D-05:** o arquivo de instruções da IA passa a ser lido explicitamente em UTF-8.
- **D-04:** `./gradlew test` passa a rodar sem `OPENAI_API_KEY` e sem MySQL iniciado à mão. O banco de testes sobe com Testcontainers, e os testes usam uma chave fictícia. Os testes `*IT` continuam ignorados sem a chave real.
- Base de testes do padrão `docs/padrao-de-testes.md`: Instancio (exceção aprovada, só em testes), classes `Support` e os primeiros testes por camada, cobrindo o comportamento corrigido.

Defeitos tratados (detalhes em `docs/proposta-inicial.md`, seção 2):

- D-01: valor em centavos exibido como se fosse em reais
- D-04: suíte de testes não roda sem MySQL e sem chave da OpenAI
- D-05: prompt lido sem garantir UTF-8

Requisitos da proposta inicial atendidos: RF-01, RNF-01 e RNF-06. Atende em parte o RNF-02: este change cria a base de testes, e cada change seguinte acrescenta os testes do que mudar.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Em cada caso, o requisito que descrevia o defeito é removido e um novo requisito, com nome que descreve o comportamento correto, entra no lugar:

- `transactions`: "Representação do valor na saída (D-01)" é substituído por "Valor em reais na saída".
- `voice-assistant`: "Valor informado pela IA (D-01)" é substituído por "Valor em reais nas ferramentas da IA", e "Leitura das instruções com a codificação da plataforma (D-05)" é substituído por "Leitura das instruções em UTF-8".

## Impact

- **Código de produção:** `TransactionOutput` e `TransactionResponse` (tipo e cálculo do valor) e a leitura do prompt em `TransactionController`.
- **API:** o formato do `amount` nas respostas de `POST /transactions` e `GET /transactions/{category}` muda (veja BREAKING acima). A entrada não muda.
- **Dependências (só de teste):** Testcontainers com MySQL, integração do Spring Boot com Testcontainers e Instancio.
- **Testes:** `BudgetingApplicationTests.contextLoads` passa a subir com Testcontainers e chave fictícia; novos testes em `src/test/java/dio/budgeting/`.
- **Requisito de ambiente:** Docker rodando para executar `./gradlew test`, como no módulo 00 da trilha.

## Fora do escopo

- Validação dos dados e tratamento de erros (D-03, D-06): change 2 (`add-transaction-validation`).
- Data de criação e novas categorias (D-07): change 3.
- Tirar a orquestração de IA do controller (D-02) e o teste unitário do controller: change 5. Hoje o construtor do controller monta o `ChatClient`, o que torna o teste unitário artificial; a API REST é coberta por teste de integração neste change.
- Spring REST Docs: change 7. Os testes de API deste change usam MockMvc, mas ainda não geram snippets.
- Alterar os testes `*IT` herdados do curso.
- Limite de tempo para as chamadas de IA, observado na conferência do change 0.
