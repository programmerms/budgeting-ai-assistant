# Proposal: baseline-specs

## Why

O projeto base (`05-spring-ai`) ainda não tem nenhuma especificação em `openspec/specs/`. Sem uma linha de base, os próximos changes não têm como expressar o que mudou em relação ao comportamento original, e a evolução perde rastreabilidade. Este é o change 0 da seção 5 da proposta inicial e precisa vir antes de qualquer correção.

## What Changes

- Especificar o comportamento **atual** do projeto base, exatamente como o código se comporta hoje, sem alterar nenhuma linha de código de produção ou de teste.
- Registrar nos requisitos os defeitos visíveis de fora do sistema (D-01, D-03, D-05, D-06 e D-07) como comportamento atual, com referência ao ID do diagnóstico. Assim, cada change seguinte corrige o defeito por meio de um requisito `MODIFIED` ou `ADDED`, deixando o antes e o depois visíveis no histórico.
- Executar `./gradlew test` uma vez para confirmar o diagnóstico D-04 (marcado como "a confirmar" na proposta inicial) e registrar o resultado.
- Ao arquivar, as specs passam a existir em `openspec/specs/transactions/` e `openspec/specs/voice-assistant/`.

Defeitos do projeto base (detalhes, evidências e impacto em `docs/proposta-inicial.md`, seção 2):

- D-01: valor em centavos exibido como se fosse em reais
- D-02: orquestração de IA dentro do controller
- D-03: nenhuma validação dos dados da transação
- D-04: suíte de testes não roda sem MySQL e sem chave da OpenAI
- D-05: prompt lido sem garantir UTF-8
- D-06: sem tratamento de erros; falhas viram erro 500 genérico
- D-07: transação sem data de criação

D-02 e D-04 não viram requisito nas specs: o primeiro é organização interna do código e o segundo é sobre a suíte de testes, e nenhum dos dois muda o que o cliente da API vê. D-02 fica registrado no `design.md` deste change e D-04 é confirmado pela tarefa 4.2.

Requisitos da proposta inicial atendidos: nenhum RF/RNF é implementado. O change documenta os diagnósticos D-01 a D-07 e confirma D-04.

## Capabilities

### New Capabilities

- `transactions`: registro e consulta de transações financeiras pela API REST, incluindo categorias, representação de valores e ausência atual de validação e de data de criação.
- `voice-assistant`: processamento de comandos por voz (transcrição, interpretação pela IA com Tool Calling e resposta em áudio) e as ferramentas que a IA pode chamar.

### Modified Capabilities

Nenhuma. Não existem specs anteriores.

## Impact

- **Código:** nenhum arquivo em `src/` é alterado.
- **APIs:** nenhuma mudança. Os endpoints `POST /transactions`, `GET /transactions/{category}` e `POST /transactions/ai` são apenas descritos.
- **Dependências:** nenhuma.
- **Artefatos:** criação de `openspec/changes/baseline-specs/` e, após o archive, de `openspec/specs/`.

## Fora do escopo

- Corrigir qualquer defeito D-01 a D-07. Cada um tem seu change na seção 5 da proposta inicial.
- Criar testes novos ou ajustar os existentes. Isso começa no change 1 (`fix-base-defects`).
- Especificar comportamento futuro (novas categorias, novas consultas, endpoint de texto, novo prompt).
- Detalhes internos de implementação (nomes de classes, bibliotecas). As specs descrevem só o comportamento observável.
