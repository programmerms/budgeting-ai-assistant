# Proposal: move-ai-tools-to-infrastructure

## Why

Os casos de uso `PersistTransactionUseCase` e `ListTransactionsByCategoryUseCase` e o `PersistTransactionInput` carregam anotações do Spring AI (`@Tool`, `@ToolParam`). Assim, a camada `application` depende de um framework de integração, o que fere a regra de camadas da trilha. Além disso, os dois canais de entrada ficam assimétricos: o HTTP tem um adaptador próprio (`TransactionController`), e o Tool Calling, não. A seção 3.3 da proposta inicial reviu essa decisão: a IA passa a ser tratada como mais um cliente da aplicação. Este é o change 1b da seção 5. Ele vem antes da validação (change 2) e das novas consultas (change 4) para que as tools novas já nasçam no lugar certo.

## What Changes

- Novo adaptador de Tool Calling na camada `infrastructure`, que expõe as duas ferramentas atuais e só chama os casos de uso, no mesmo papel do `TransactionController`.
- Um record de request próprio para a ferramenta de registro, com as anotações `@ToolParam`, convertido para o `PersistTransactionInput`, como o `TransactionRequest` faz no HTTP.
- `@Tool` e `@ToolParam` saem dos casos de uso e do `PersistTransactionInput`. A camada `application` deixa de importar o Spring AI e mantém só o `@Service`, que é injeção de dependência do Spring núcleo.
- O `ChatClient` passa a registrar o adaptador como ferramenta, no lugar dos casos de uso.
- **Sem mudança de comportamento:** o nome, a descrição e os parâmetros das ferramentas que a IA vê ficam idênticos, e o retorno também. A API REST não muda.

Origem: decisão revista da seção 3.3 da proposta inicial. Nenhum requisito RF/RNF ou defeito D é atendido diretamente. Fica preservado o RNF-01 (`./gradlew test` sem `OPENAI_API_KEY`), e o RNF-02 é ampliado com os testes do adaptador.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma. É uma refatoração: os requisitos de `voice-assistant` ("Ferramenta de registro de transação", "Ferramenta de consulta por categoria", "Valor em reais nas ferramentas da IA") continuam valendo como estão. Por isso o change declara `skip_specs: true`.

## Impact

- **Código de produção:** novo pacote `infrastructure/ai`; remoção das anotações em `PersistTransactionUseCase`, `ListTransactionsByCategoryUseCase` e `PersistTransactionInput`; o `TransactionController` passa a receber o adaptador para registrá-lo no `ChatClient`.
- **API REST e contrato das ferramentas:** sem mudança.
- **Dependências:** nenhuma nova.
- **Testes:** os testes do change 1 continuam verdes sem alteração. Entram os testes unitários do adaptador, incluindo um que fixa o contrato das ferramentas (nome, descrição e parâmetros).
- **Documentação:** `AGENTS.md` ganha a regra "anotações do Spring AI só em `infrastructure/ai`".

## Fora do escopo

- Tirar a orquestração de IA (transcrição, chat e voz) do controller (D-02): change 5. O `ChatClient` continua sendo montado no construtor do controller.
- Validação dos dados e tratamento de erros (D-03, D-06): change 2. O cenário "chamada da ferramenta sem valor" continua como está hoje.
- Novas ferramentas (total por categoria, consulta por período): change 4.
- Alterar descrições das ferramentas ou o prompt de sistema: change 6.
- Alterar os testes `*IT` herdados do curso.
