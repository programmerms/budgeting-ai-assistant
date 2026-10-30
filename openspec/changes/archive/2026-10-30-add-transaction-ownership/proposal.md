# Proposal

## Why

O change `add-user-accounts-and-authentication` fechou a porta de entrada da API, mas não separou os dados: qualquer usuário `ACTIVE` autenticado enxerga e soma as transações de todos os outros. Esse é o estado que a proposta de segurança adverte para não deixar permanente — autenticação sem isolamento (S-03, S-04, S-05). Este change completa o ciclo dando dono a cada transação e filtrando todas as consultas por esse dono, inclusive nas ferramentas chamadas pela IA.

## What Changes

- **BREAKING** Toda transação passa a ter um proprietário obrigatório. O contrato `TransactionRepository` deixa de oferecer qualquer operação global: registrar e consultar exigem o proprietário, para que não exista caminho acidental de vazamento (RF-S10, RNF-S03).
- **BREAKING** As transações já gravadas no banco de desenvolvimento são descartadas junto com o volume do Docker, porque não têm proprietário e a coluna nova é obrigatória (S-08, seção 10 da proposta de segurança).
- `POST /transactions` passa a associar a transação ao usuário autenticado da sessão. O proprietário não é aceito no corpo da requisição nem devolvido na resposta (RF-S13, RNF-S02).
- `GET /transactions/{category}`, `GET /transactions/summary` e `GET /transactions?from=&to=` passam a enxergar apenas as transações do usuário autenticado (RF-S11).
- As quatro ferramentas da IA passam a receber o proprietário do contexto de segurança do servidor, e não do modelo. O identificador continua fora do JSON Schema exposto ao modelo, de modo que nenhuma instrução no prompt consegue trocar de dono (RF-S12, RNF-S04).
- O contrato HTTP externo não muda: mesmos caminhos, mesmos campos, mesmos códigos de status. O que muda é o conjunto de dados que cada resposta enxerga.

## Capabilities

### New Capabilities

Nenhuma. A propriedade é uma regra das capacidades financeiras que já existem, não um novo comportamento do sistema.

### Modified Capabilities

- `transactions`: registro e as três consultas passam a operar apenas sobre as transações do proprietário autenticado; o requisito de persistência deixa de dizer que não há separação por usuário; novos requisitos para a propriedade obrigatória, o isolamento entre usuários e a impossibilidade de forjar o proprietário pela entrada.
- `voice-assistant`: novo requisito de isolamento nas ferramentas da IA, cobrindo a origem do proprietário no servidor, a ausência do identificador no contrato exposto ao modelo e o não compartilhamento de resultados entre usuários diferentes.

## Impact

**Domínio financeiro:** novo `OwnerId` em `dio.budgeting.domain`; `Transaction` ganha o proprietário como invariante; `TransactionRepository` muda a assinatura das três operações.

**Aplicação:** `PersistTransactionUseCase`, `ListTransactionsByCategoryUseCase`, `SummarizeTransactionsByCategoryUseCase` e `ListTransactionsByPeriodUseCase` passam a receber o proprietário como dado confiável da aplicação, separado da entrada vinda do cliente.

**Infraestrutura:** `TransactionEntity` ganha a coluna `owner_id` não nula e indexada; `TransactionEntityRepository` troca as *derived queries* por versões com o proprietário; `TransactionController` e `TransactionTools` passam a resolver o proprietário a partir da sessão autenticada.

**Testes:** os testes financeiros existentes precisam de um principal real, porque `@WithMockUser` não produz um `AuthenticatedUser` do qual o `OwnerId` possa ser extraído; entram testes novos de isolamento entre dois usuários.

**Banco:** o volume `transaction_data` do `compose.yml` é recriado.

**Documentação:** README e `src/docs/asciidoc/index.adoc` passam a explicar que cada pessoa vê apenas os próprios gastos.

### Fora do escopo

- Auditoria global: o administrador continua sem acesso aos dados financeiros de ninguém, como define a seção 4.1 da proposta de segurança.
- Compartilhamento de orçamento, grupos ou contas conjuntas.
- Exclusão de usuários e o destino das transações de uma conta removida.
- Migração versionada de banco (Flyway ou Liquibase), por introduzir ferramenta fora da trilha.
- Transferência de propriedade de uma transação entre usuários.
