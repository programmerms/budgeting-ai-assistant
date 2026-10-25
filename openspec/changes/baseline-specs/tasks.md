# Tasks: baseline-specs

Este change não altera código. As tarefas conferem se as specs descrevem o comportamento real. Qualquer divergência encontrada é corrigida **na spec**, nunca no código.

## 1. Conferência das specs contra o código

- [ ] 1.1 Ler `specs/transactions/spec.md` e, para cada requisito, localizar o trecho de código correspondente (tabela da decisão 3 do `design.md`); verificar que não sobrou nenhum requisito sem origem no código
- [ ] 1.2 Ler `specs/voice-assistant/spec.md` e fazer a mesma conferência, com foco em `TransactionController`, nos dois casos de uso, em `system-message.st` e em `application.properties`; verificar que não sobrou nenhum requisito sem origem no código

## 2. Conferência com a aplicação rodando (endpoints REST)

- [ ] 2.1 Subir a aplicação com uma chave fictícia (`OPENAI_API_KEY=fake ./gradlew bootRun`) e verificar que ela inicia e que o MySQL sobe pelo Docker Compose
- [ ] 2.2 Com `curl`, registrar `{"description": "Mercado", "amount": 8000, "category": "GROCERIES"}` e verificar status 201, `id` em UUID, `amount` igual a `8000.0` e ausência de campo de data (requisitos de registro, D-01 e D-07)
- [ ] 2.3 Registrar uma transação com `"description": ""` e outra com `"amount": -500` e verificar status 201 nas duas (D-03)
- [ ] 2.4 Registrar uma transação com `"category": null` e verificar status 500; depois consultar o banco (`SELECT * FROM transaction_entity WHERE category IS NULL`) e verificar que a linha foi salva (D-03, D-06)
- [ ] 2.5 Registrar com `"category": "RESTAURANT"` e consultar `GET /transactions/RESTAURANT`; verificar status 400 nos dois casos
- [ ] 2.6 Consultar `GET /transactions/GROCERIES` e `GET /transactions/PHARMA` e verificar, respectivamente, a lista só com transações de `GROCERIES` e a lista vazia (se não houver dados de `PHARMA`)

## 3. Conferência do fluxo de voz

- [ ] 3.1 Com a chave fictícia, chamar `POST /transactions/ai` sem o campo `file` e verificar status 400
- [ ] 3.2 Com a chave fictícia, enviar um arquivo de 0 bytes no campo `file` e verificar status 500 (D-06)
- [ ] 3.3 (Opcional, exige `OPENAI_API_KEY` real) Enviar `src/test/resources/audio/recording-1.m4a` e verificar que a resposta é um MP3 anexado como `audio.mp3`, que a transação foi salva e qual valor a IA falou (efeito do D-01)

## 4. Ajustes e validação dos artefatos

- [ ] 4.1 Corrigir nas specs qualquer divergência encontrada nos grupos 1 a 3 e verificar que `openspec validate baseline-specs --strict` passa sem erros
- [ ] 4.2 Rodar `./gradlew test` sem `OPENAI_API_KEY` e sem MySQL rodando, registrar aqui o resultado (testes que passaram, falharam e foram ignorados) e verificar se ele confirma o D-04; aplicar a regra da decisão 4 do `design.md` para decidir se o change pode ser arquivado
