# Design

## Context

Ver `proposal.md` — Why. O estado atual relevante para o desenho:

- `Transaction` tem `id`, `description`, `amount`, `category` e `createdAt`, com invariantes no construtor via `Assert` (módulo 01).
- `TransactionRepository` expõe `save`, `findAllByCategory` e `findAllByCreatedAtBetween`, todas globais.
- Os quatro casos de uso financeiros recebem apenas o dado vindo do cliente (`PersistTransactionInput`, `Category`, `TransactionsPeriodInput`).
- `TransactionTools` chama esses mesmos casos de uso a partir da infraestrutura, dentro do ciclo de vida da requisição HTTP.
- O contexto `auth`, entregue no change anterior, autentica por sessão e coloca um `AuthenticatedUser` (que implementa `UserDetails` e carrega um `UserAccountId`) no contexto de segurança.
- `spring.jpa.hibernate.ddl-auto=update` cria e amplia o schema, mas não preenche coluna nova em linha existente.

## Goals / Non-Goals

**Goals:**

- Tornar impossível, pela forma do código, executar uma operação financeira sem proprietário.
- Manter `domain` e `application` livres do Spring Security (RNF-S05), com o proprietário entrando como um dado simples da aplicação.
- Aplicar a mesma regra de isolamento nos três canais (REST, texto e voz) sem duplicar a lógica em cada um.

**Non-Goals:**

- Mudar o contrato HTTP externo. Caminhos, campos e códigos de status continuam iguais.
- Introduzir verificação de autorização por recurso (do tipo "esta transação é sua?"), porque não existe endpoint que busque uma transação por `id`.

## Decisions

### 1. `OwnerId` como conceito do domínio financeiro, separado de `UserAccountId`

Criar `dio.budgeting.domain.OwnerId` como `record OwnerId(UUID uuid)`, irmão de `TransactionId`, e não reaproveitar `UserAccountId` do contexto `auth`.

O módulo 00 ensina IDs fortemente tipados por contexto, e a seção 9.2 da proposta de segurança pede exatamente essa separação: o domínio financeiro precisa saber que um gasto tem dono, mas não precisa saber o que é senha, papel ou sessão. Reutilizar `UserAccountId` criaria uma dependência de `domain` financeiro para o contexto `auth` só para ganhar um `UUID`, e amarraria os dois contextos em qualquer evolução futura.

O valor do `UUID` é o mesmo da conta, o que mantém a tradução trivial na infraestrutura. Alternativa considerada: uma chave estrangeira JPA de `TransactionEntity` para `UserAccountEntity`. Descartada porque acoplaria as duas tabelas e arrastaria o relacionamento para dentro do domínio, sem ganho real — nenhuma consulta precisa navegar da transação para os dados da conta.

### 2. Proprietário como invariante de `Transaction`

`OwnerId` entra como primeiro campo obrigatório do construtor, validado com `Assert.notNull`, exatamente como `TransactionId` hoje. O módulo 01 posiciona as invariantes no domínio justamente porque o Tool Calling chama os casos de uso sem passar pelo controller: uma validação que vivesse só na camada HTTP deixaria o caminho da IA desprotegido.

O construtor de conveniência `Transaction(String, long, Category)`, que hoje gera o `TransactionId`, passa a exigir o proprietário: `Transaction(OwnerId, String, long, Category)`. Assim não sobra nenhuma forma de construir uma transação sem dono.

### 3. Contrato do repositório exige o proprietário em toda operação

`TransactionRepository` passa a declarar:

```java
Transaction save(Transaction transaction);                                      // o dono já vem dentro da transação
List<Transaction> findAllByOwnerIdAndCategory(OwnerId ownerId, Category category);
List<Transaction> findAllByOwnerIdAndCreatedAtBetween(OwnerId ownerId, Instant from, Instant to);
```

Nenhuma variante global sobrevive. A seção 9.3 da proposta é explícita sobre o motivo: um método de leitura sem proprietário pode ser chamado por engano em uma evolução futura e vazar dados, e o compilador é um guarda melhor que a disciplina de quem revisa. O `save` não muda de assinatura porque a transação já carrega o proprietário como invariante (decisão 2), e duplicá-lo no parâmetro abriria espaço para divergência entre os dois valores.

### 4. Proprietário como parâmetro explícito do caso de uso, fora do input do cliente

As assinaturas passam a ser `execute(OwnerId ownerId, PersistTransactionInput input)`, `execute(OwnerId ownerId, Category category)` e `execute(OwnerId ownerId, TransactionsPeriodInput input)`.

O proprietário não entra dentro dos records de input. Esses records nascem de `request.toInput()`, ou seja, de dados que o cliente controla; o proprietário é dado confiável da aplicação. Manter os dois em posições diferentes da assinatura torna a distinção visível para quem lê o código e garante que nenhum campo de proprietário possa ser desserializado de um JSON (RF-S13, RNF-S02).

Alternativa considerada: acrescentar `ownerId` aos records de input e montar com `request.toInput(ownerId)`. Funciona, mas coloca no mesmo objeto um campo que o cliente preenche e outro que ele nunca pode preencher, o que é exatamente a confusão que a regra quer evitar.

### 5. Origem do proprietário: um provedor único na infraestrutura

Criar `dio.budgeting.infrastructure.security.AuthenticatedOwnerProvider`, com um método `currentOwnerId()` que lê o `SecurityContextHolder`, confirma que o principal é um `AuthenticatedUser` e devolve `new OwnerId(user.id().uuid())`. `TransactionController` e `TransactionTools` usam esse provedor.

A seção 9.4 da proposta define que as tools obtêm a identidade do contexto de segurança do servidor, e o módulo 03 usa o `SecurityContextHolder` como fonte do principal autenticado. Concentrar esse acesso em uma classe evita espalhar `SecurityContextHolder` por controller e tools, e dá um ponto único para falhar alto quando não houver identidade: se o principal estiver ausente ou não for um `AuthenticatedUser`, o provedor lança exceção em vez de devolver um proprietário inventado.

Alternativas consideradas:

- `@AuthenticationPrincipal AuthenticatedUser` como parâmetro dos métodos do controller. É idiomático e seria usado no controller, mas não atende as tools, que não são handlers HTTP. Ficariam dois mecanismos para a mesma regra.
- `ToolContext` do Spring AI, passando o proprietário do gateway até a tool. É explícito e não depende de `ThreadLocal`, mas é recurso do Spring AI que a trilha não cobre, e exigiria mudar o `VoiceAssistantGateway` para transportar identidade — justamente a dependência que o change 5 do primeiro ciclo removeu da camada de aplicação.

### 6. Persistência: coluna `owner_id` não nula e indexada

`TransactionEntity` ganha `@Column(name = "owner_id", nullable = false) UUID ownerId`, com índice declarado em `@Table(indexes = @Index(name = "idx_transaction_owner_id", columnList = "owner_id"))`. As *derived queries* de `TransactionEntityRepository` passam a ser `findAllByOwnerIdAndCategory` e `findAllByOwnerIdAndCreatedAtBetween`, no estilo do módulo 02.

O índice existe porque toda consulta do sistema passa a filtrar por `owner_id`; sem ele, cada leitura varre a tabela inteira. A seção 9.2 da proposta já previa a coluna indexada.

### 7. A resposta não expõe o proprietário

`TransactionResponse` e `TransactionOutput` continuam sem campo de proprietário. Quem consulta já sabe quem é, porque é a própria sessão; devolver o identificador só aumentaria a superfície exposta sem servir a nenhum cliente. Isso também mantém intactos os snippets de REST Docs e os testes de contrato das respostas.

### 8. Dados existentes: descartar o volume do banco de desenvolvimento

A primeira tarefa do apply é `docker compose down -v`, derrubando o volume `transaction_data`. É a alternativa 1 da seção 10 da proposta de segurança, recomendada ali mesmo para este projeto educacional.

O motivo é concreto: com `ddl-auto=update`, o Hibernate tentaria adicionar `owner_id NOT NULL` a uma tabela que já tem linhas, e o MySQL recusa. As alternativas seriam atribuir as transações antigas ao administrador — que a própria proposta desaconselha, por misturar administração de contas com propriedade financeira — ou introduzir Flyway, ferramenta fora da trilha. Os dados atuais são do projeto-base e de testes manuais, portanto descartáveis. O administrador é recriado na subida seguinte a partir das variáveis de ambiente.

### 9. Testes passam a injetar um principal real

`@WithMockUser` produz um principal do tipo `User` do Spring Security, que não é um `AuthenticatedUser` e não carrega `UserAccountId`. Todo teste de API financeira passa a usar `SecurityMockMvcRequestPostProcessors.user(authenticatedUser)` com um `AuthenticatedUser` construído por um suporte novo em `src/test/java/dio/budgeting/support`, de modo que o `OwnerId` derivado seja conhecido e verificável.

Esse risco já estava registrado no design do change anterior. Nos testes unitários de caso de uso, o proprietário é só mais um parâmetro, então seguem com Mockito como hoje.

### 10. O teste de contrato das tools passa a afirmar a ausência do proprietário

`TransactionToolsContractTest` ganha uma verificação de que nenhum JSON Schema exposto ao modelo declara parâmetro de proprietário. Sem isso, alguém poderia adicionar um `@ToolParam` de dono no futuro e o isolamento cairia silenciosamente — que é o cenário que a RNF-S04 quer impedir.

## Risks / Trade-offs

- **O `SecurityContextHolder` é um `ThreadLocal`** → se a execução das tools algum dia acontecer em outra thread (streaming ou chamada assíncrona do Spring AI), o contexto não é propagado. Mitigação: hoje a cadeia é síncrona dentro da requisição HTTP; o provedor lança exceção quando não há principal, de modo que a falha seria imediata e visível em vez de silenciosa, e um teste de API cobre o caminho real pelo endpoint.
- **Mudança quebra todos os testes financeiros de uma vez** → assinaturas de domínio, repositório e casos de uso mudam juntas, então a suíte fica vermelha durante boa parte do apply. Mitigação: as tarefas assumem isso explicitamente e nenhum commit é feito com a suíte vermelha; a ordem vai do domínio para fora.
- **Perda definitiva dos dados de desenvolvimento** → não há volta depois do `down -v`. Mitigação: decisão explícita, registrada aqui e na primeira tarefa; os dados são do projeto-base e de testes manuais.
- **Isolamento depende de todas as consultas passarem o proprietário** → uma consulta nova escrita sem ele reabriria o vazamento. Mitigação: o contrato do repositório não oferece operação global, então o erro vira erro de compilação, e os testes de isolamento entre dois usuários cobrem os três canais.
- **Administrador segue sem acesso financeiro** → não existe forma de auditar gastos de ninguém, nem para suporte. Mitigação: é decisão da seção 4.1 da proposta; se auditoria for necessária, entra como change próprio, com requisito explícito.

## Migration Plan

1. `docker compose down -v` para descartar o volume `transaction_data`.
2. Implementar na ordem domínio → persistência → aplicação → infraestrutura → testes, conforme `tasks.md`.
3. Subir a aplicação com `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD` e `OPENAI_API_KEY`: o schema é recriado com `owner_id` e o administrador inicial volta a ser criado pelo `AdminAccountInitializer`.
4. Rollback: reverter os commits do change. O banco de desenvolvimento precisaria ser descartado outra vez, porque a coluna `owner_id` não é removida por `ddl-auto=update`.
