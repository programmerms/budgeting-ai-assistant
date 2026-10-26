# Tasks: baseline-specs

Este change não altera código. As tarefas conferem se as specs descrevem o comportamento real. Qualquer divergência encontrada é corrigida **na spec**, nunca no código.

## 1. Conferência das specs contra o código

- [x] 1.1 Ler `specs/transactions/spec.md` e, para cada requisito, localizar o trecho de código correspondente (tabela da decisão 3 do `design.md`); verificar que não sobrou nenhum requisito sem origem no código
- [x] 1.2 Ler `specs/voice-assistant/spec.md` e fazer a mesma conferência, com foco em `TransactionController`, nos dois casos de uso, em `system-message.st` e em `application.properties`; verificar que não sobrou nenhum requisito sem origem no código

## 2. Conferência com a aplicação rodando (endpoints REST)

- [x] 2.1 Subir a aplicação com uma chave fictícia (`OPENAI_API_KEY=fake ./gradlew bootRun`) e verificar que ela inicia e que o MySQL sobe pelo Docker Compose
- [x] 2.2 Com `curl`, registrar `{"description": "Mercado", "amount": 8000, "category": "GROCERIES"}` e verificar status 201, `id` em UUID, `amount` igual a `8000.0` e ausência de campo de data (requisitos de registro, D-01 e D-07)
- [x] 2.3 Registrar uma transação com `"description": ""` e outra com `"amount": -500` e verificar status 201 nas duas (D-03)
- [x] 2.4 Registrar uma transação com `"category": null` e verificar status 500; depois consultar o banco (`SELECT * FROM transaction_entity WHERE category IS NULL`) e verificar que a linha foi salva (D-03, D-06)
- [x] 2.5 Registrar com `"category": "RESTAURANT"` e consultar `GET /transactions/RESTAURANT`; verificar status 400 nos dois casos
- [x] 2.6 Consultar `GET /transactions/GROCERIES` e `GET /transactions/PHARMA` e verificar, respectivamente, a lista só com transações de `GROCERIES` e a lista vazia (se não houver dados de `PHARMA`)

## 3. Conferência do fluxo de voz

- [x] 3.1 Com a chave fictícia, chamar `POST /transactions/ai` sem o campo `file` e verificar status 400
- [x] 3.2 Com a chave fictícia, enviar um arquivo de 0 bytes no campo `file` e verificar status 500 (D-06)
- [x] 3.3 (Opcional, exige `OPENAI_API_KEY` real) Enviar `src/test/resources/audio/recording-1.m4a` e verificar que a resposta é um MP3 anexado como `audio.mp3`, que a transação foi salva e qual valor a IA falou (efeito do D-01)
  - Resposta: HTTP 200, `Content-Type: audio/mp3`, `Content-Disposition: attachment; filename="audio.mp3"`, MP3 válido de cerca de 70 KB.
  - Transação salva: `PHARMA`, "Compra de três itens na farmácia", `amount = 8000` (R$ 80,00).
  - Fala da IA (transcrita do MP3): "Transação de R$ 80,00 referente à compra de três itens na farmácia." O modelo compensou o D-01 sozinho nesta execução, o que não é garantido.
  - A primeira chamada da IA à ferramenta de registro veio sem `amount` e falhou (`Cannot map null into type long`); o erro voltou ao modelo, que chamou de novo com o valor e salvou.
  - A requisição levou cerca de 5 minutos, quase todos esperando a geração de voz da OpenAI (uma chamada direta à mesma API levou 24 s para uma frase curta). A aplicação não define limite de tempo para as chamadas de IA; fica como ponto de atenção para o tratamento de erros (change 2) e para o caso de uso de comando (change 5).
  - Primeira tentativa, antes da recarga de créditos: HTTP 429 `insufficient_quota` da OpenAI, que a API devolveu como 500 genérico, confirmando o cenário "Falha do provedor de IA" (D-06).

## 4. Ajustes e validação dos artefatos

- [x] 4.1 Corrigir nas specs qualquer divergência encontrada nos grupos 1 a 3 e verificar que `openspec validate baseline-specs --strict` passa sem erros
  - Grupos 2 e 3.1–3.2: nenhuma divergência; todos os cenários se comportaram como descrito, inclusive a categoria nula (HTTP 500 com a linha salva no banco com `category = NULL`) e o arquivo de áudio vazio (HTTP 500).
  - Tarefa 3.3: dois ajustes em `specs/voice-assistant/spec.md`. Novo cenário "Chamada da ferramenta sem valor" e texto do requisito "Valor informado pela IA (D-01)" reescrito para dizer que o valor falado depende da interpretação do modelo.
- [x] 4.2 Rodar `./gradlew test` sem `OPENAI_API_KEY` e sem MySQL rodando, registrar aqui o resultado (testes que passaram, falharam e foram ignorados) e verificar se ele confirma o D-04; aplicar a regra da decisão 4 do `design.md` para decidir se o change pode ser arquivado
  - Resultado: `BUILD FAILED`, 5 testes executados, 1 falhou e 4 foram ignorados (os `*IT`, por falta da chave; o parametrizado de transcrição também foi ignorado).
  - Falha: `BudgetingApplicationTests.contextLoads`, com `Failed to determine a suitable driver class`. O Docker Compose está como `developmentOnly` e não sobe nos testes, e não há banco configurado para eles.
  - Conclusão: D-04 confirmado. Pela decisão 4 do `design.md`, a falha vem do D-04 e o change pode ser arquivado; a correção fica no change 1 (`fix-base-defects`).
