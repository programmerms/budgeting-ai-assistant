# Design: add-user-accounts-and-authentication

## Context

A motivação está em `proposal.md` (seção Why) e o levantamento completo em `docs/proposta-seguranca-e-multiusuario.md`. O estado atual relevante:

- O código de produção vive em pacotes achatados: `dio.budgeting.domain`, `dio.budgeting.application` e `dio.budgeting.infrastructure`. Há um único contexto, o financeiro.
- Nenhuma dependência de Spring Security no `build.gradle`. Qualquer requisição chega ao `TransactionController` sem identificação.
- `GlobalExceptionHandler` (`infrastructure/http`) trata `MethodArgumentNotValidException` com 400 e mapa `campo → mensagem`, e `IllegalArgumentException` com 400 e corpo em texto.
- `application.properties` já resolve segredo por variável de ambiente: `spring.ai.openai.api-key=${OPENAI_API_KEY}`.
- Todo `@SpringBootTest` do projeto declara `properties = "spring.ai.openai.api-key=test-key"`, conforme `docs/padrao-de-testes.md`.
- O módulo `03-spring-security` da trilha entrega: `User` (entidade JPA implementando `UserDetails`), `UserRole` em `auth/domain`, `JpaUserDetailsService`, `RestUsernamePasswordAuthenticationFilter` lendo JSON em `POST /api/auth/login`, `SecurityConfig` com `csrf` desabilitado, `BCryptPasswordEncoder` e um `CommandLineRunner` que semeia usuários na primeira subida.
- O mesmo módulo separa o contexto `proposal` do contexto `auth`, e o contexto financeiro usa um `OwnerId` próprio em vez de importar a entidade de segurança.

## Goals / Non-Goals

**Goals:**

- Autenticar e autorizar com os mesmos componentes do módulo `03-spring-security`, mudando apenas a camada em que cada um vive quando isso for preciso para respeitar as regras do `AGENTS.md`.
- Manter `domain` e `application` sem importar `org.springframework.security` (RNF-S05).
- Fechar o change com `./gradlew test` verde, sem `OPENAI_API_KEY` e sem credenciais reais.

**Non-Goals:**

- Mover o código financeiro existente para um pacote de contexto próprio (ver decisão 1).
- Qualquer coisa ligada a propriedade e isolamento de dados: é o change `add-transaction-ownership`.

## Decisions

### 1. Contexto `auth` em pacote próprio, com o código financeiro onde está

O código novo vai para `dio.budgeting.auth.{domain,application,infrastructure}`. O código financeiro continua em `dio.budgeting.{domain,application,infrastructure}`.

- **Por quê:** o módulo 03 separa `auth` de `proposal` justamente para sinalizar fronteira de contexto, e o README dele chama isso de *context boundary signal*. Misturar `UserAccount` dentro de `dio.budgeting.domain`, ao lado de `Transaction`, apagaria essa fronteira.
- **Alternativa rejeitada:** mover o financeiro para `dio.budgeting.transactions.*` para ficar simétrico. Renomearia cerca de 33 arquivos de produção e 36 de teste dentro de um change que já é grande, sem mudar nenhum comportamento. Se a simetria incomodar depois, ela cabe num change de refatoração próprio, com `skip_specs: true`, como foi o `move-ai-tools-to-infrastructure`.
- **Fundamento na trilha:** módulo `03-spring-security` (pacotes `auth` e `proposal`).

### 2. Modelo de domínio `UserAccount` separado da entidade JPA

O módulo 03 usa uma classe só: `User` é a entidade JPA e implementa `UserDetails`. Este projeto não vai copiar isso. Teremos:

- `auth/domain/UserAccount`: classe com as invariantes e as transições de estado (`approve()` e `reject()`, que lançam quando o estado não é `PENDING`).
- `auth/domain/UserAccountId`: record com `UUID`, identificador fortemente tipado.
- `auth/domain/UserRole` e `auth/domain/UserStatus`: enums.
- `auth/domain/UserAccountRepository`: contrato no domínio.
- `auth/infrastructure/persistence/entity/UserAccountEntity`: entidade JPA com `from`/`toDomain`.
- `auth/infrastructure/persistence/repository/JpaUserAccountRepository`: implementação do contrato.

- **Por quê:** é exatamente a estrutura que o projeto já tem para `Transaction`/`TransactionEntity`/`TransactionRepository`, e o `AGENTS.md` exige regra de negócio no domínio e Repository Pattern. A regra "só se aprova quem está pendente" é regra de negócio, não de persistência.
- **Alternativa rejeitada:** a classe única do módulo 03. Colocaria a regra de transição dentro de uma entidade JPA e deixaria o domínio sem invariante, contrariando o `AGENTS.md`.
- **Fundamento na trilha:** módulos `00-domain-driven-design` e `02-spring-data` (classe de domínio, entidade e Repository Pattern) e módulo `01-spring-web` (invariantes com `Assert`).

Nome `UserAccount` em vez de `User` para não colidir com `org.springframework.security.core.userdetails.User` na leitura do código de segurança.

### 3. `UserDetails` num adaptador próprio, não no domínio nem na entidade

`auth/infrastructure/security/AuthenticatedUser` é um record que implementa `UserDetails`, construído a partir de um `UserAccount`. O `JpaUserDetailsService` busca o `UserAccount` pelo repositório do domínio e devolve esse adaptador.

- **Por quê:** é o mesmo raciocínio do change `move-ai-tools-to-infrastructure`, registrado na seção 3.3 da proposta inicial: as anotações e interfaces de framework continuam sendo as da trilha, muda só a camada onde ficam. O Spring Security é mais um cliente da aplicação, como o HTTP e o Tool Calling.
- **Benefício para o próximo change:** `AuthenticatedUser` carrega o `UserAccountId`, e é dele que o change seguinte vai tirar o `OwnerId`. Ter um principal estável e próprio evita acoplar o contexto financeiro a uma entidade JPA de segurança.
- **Alternativa rejeitada:** `UserAccountEntity implements UserDetails`, como no módulo 03. Tornaria a entidade JPA o principal da sessão, misturando persistência e segurança numa classe só.

### 4. `PasswordEncoderGateway` na camada `application`

`RegisterUserUseCase` precisa codificar a senha, mas `PasswordEncoder` é do Spring Security e o RNF-S05 proíbe essa dependência na `application`. Criamos `auth/application/PasswordEncoderGateway` com `String encode(String rawPassword)`, implementado por `auth/infrastructure/security/BCryptPasswordEncoderGateway`, que delega ao `PasswordEncoder` do Spring.

- **Por quê:** é o mesmo padrão `Gateway` que o projeto já usa em `VoiceAssistantGateway` (interface em `application`, implementação em `infrastructure/ai`), herdado do módulo `00-domain-driven-design` (`BookSearchGateway` / `OpenLibraryBookSearchGateway`).
- **Alternativa rejeitada:** injetar `PasswordEncoder` direto no caso de uso. Mais curto, mas quebra o RNF-S05 e impede testar o caso de uso sem o Spring Security no classpath.
- A **verificação** da senha no login não precisa de gateway: quem compara é o `DaoAuthenticationProvider` do Spring, dentro da infraestrutura.

### 5. Um caso de uso por capacidade, com `input`/`output`

`RegisterUserUseCase`, `ApproveUserUseCase`, `RejectUserUseCase` e `ListUsersByStatusUseCase`, em `auth/application`, com `input/RegisterUserInput` e `output/UserAccountOutput`. Os controllers (`AuthController` e `AdminUserController`) só adaptam HTTP.

- **Por quê:** regra do `AGENTS.md` e do README raiz da trilha. Aprovar e rejeitar são duas capacidades com resultados diferentes, então são dois casos de uso.
- `UserAccountOutput` nunca expõe a senha, nem em texto puro nem codificada.

### 6. Credenciais do administrador por placeholder de propriedade, com falha na subida

`application.properties` ganha:

```properties
app.admin.username=${APP_ADMIN_USERNAME}
app.admin.password=${APP_ADMIN_PASSWORD}
```

Um `AdminAccountInitializer` (`CommandLineRunner` em `auth/infrastructure/security`) lê essas propriedades e cria a conta `ROLE_ADMIN`/`ACTIVE` quando ainda não existir nenhuma administrativa.

- **Por quê:** o projeto já resolve `OPENAI_API_KEY` exatamente assim. E a exigência de falhar com mensagem clara sai de graça: sem a variável, o Spring não resolve o placeholder e a subida para com `Could not resolve placeholder 'APP_ADMIN_USERNAME'`, que nomeia a variável faltante. Nenhuma validação manual precisa ser escrita.
- **Alternativa rejeitada:** valor padrão no `application.properties` (por exemplo `admin`/`admin`). Seria uma credencial versionada, proibida pelo RNF-S01 e pelo `AGENTS.md`.
- **Fundamento na trilha:** módulo `03-spring-security` (`CommandLineRunner` que semeia usuários na primeira subida).
- **Nos testes:** os `@SpringBootTest` passam a declarar também `app.admin.username=test-admin` e `app.admin.password=test-admin-password`, ao lado do `spring.ai.openai.api-key=test-key` que `docs/padrao-de-testes.md` já exige.

### 7. Autorização declarada no `SecurityFilterChain`, não no controller financeiro

O `SecurityConfig` declara as regras por caminho:

- `POST /api/auth/register` e `POST /api/auth/login`: liberados.
- `/api/admin/**`: `hasRole("ADMIN")`.
- `/transactions/**`: `hasRole("USER")`.
- qualquer outra requisição: autenticada.

- **Por quê:** o `TransactionController` não precisa ser tocado, o que mantém o diff do change concentrado no contexto novo e deixa a política de acesso num lugar só, fácil de auditar.
- O `hasRole("USER")` em `/transactions/**` é o que implementa a separação da seção 4.1 da proposta: o administrador recebe 403 nos endpoints financeiros, porque administrar contas e consultar gastos são responsabilidades diferentes.
- **Fundamento na trilha:** módulo `03-spring-security` (`authorizeHttpRequests` centralizando a proteção).

### 8. Login pelo `RestUsernamePasswordAuthenticationFilter` do módulo 03

Copiamos a abordagem do módulo: um filtro que estende `UsernamePasswordAuthenticationFilter`, lê `{"username", "password"}` do corpo JSON, é registrado com `addFilterAt(..., UsernamePasswordAuthenticationFilter.class)` e tem `setFilterProcessesUrl("/api/auth/login")`. O `SecurityConfig` mantém `securityContext(context -> context.requireExplicitSave(false))`, para o contexto autenticado ser gravado na sessão automaticamente.

- **Por quê:** é literalmente o conteúdo da aula, e entrega o contrato REST que a proposta pede.
- **Alternativa rejeitada:** `formLogin()`. Espera `application/x-www-form-urlencoded`, não JSON.

### 9. Estado da conta mapeado em `isEnabled()`, com resposta de falha uniforme

`AuthenticatedUser.isEnabled()` devolve `status == ACTIVE`. Contas `PENDING` e `REJECTED` fazem o `DaoAuthenticationProvider` lançar `DisabledException` antes mesmo de conferir a senha.

Para cumprir a decisão de resposta indistinguível, o filtro de login recebe um `AuthenticationFailureHandler` que responde 401 com a mesma mensagem genérica para **qualquer** `AuthenticationException`, seja `BadCredentialsException`, `DisabledException` ou usuário inexistente.

- **Por quê:** usar `isEnabled()` deixa o motivo explícito no código, e o handler único garante que o motivo não vaze para fora. Sem isso, um atacante descobriria quais nomes de usuário existem.
- **Alternativa rejeitada:** o `JpaUserDetailsService` buscar só contas `ACTIVE`, transformando tudo em `UsernameNotFoundException`. Daria o mesmo resultado externo, mas esconderia a regra de negócio dentro de uma consulta e deixaria o estado da conta invisível no modelo de segurança.

### 10. 401 e 403 escritos por `AuthenticationEntryPoint` e `AccessDeniedHandler`

`@RestControllerAdvice` não alcança o que acontece na cadeia de filtros, então `GlobalExceptionHandler` não consegue tratar acesso negado. O `SecurityConfig` registra um `AuthenticationEntryPoint` que responde 401 e um `AccessDeniedHandler` que responde 403, ambos com corpo em texto, no mesmo formato que o handler já usa para `IllegalArgumentException`.

### 11. Erros de negócio como exceções de domínio

Em `auth/domain`, três exceções herdando `RuntimeException`, mapeadas no `GlobalExceptionHandler` existente:

| Exceção | Status | Quando |
|---|---|---|
| `UsernameAlreadyTakenException` | 409 | cadastro com nome já existente |
| `UserAccountNotFoundException` | 404 | decisão sobre `id` inexistente |
| `UserAlreadyDecidedException` | 409 | aprovar ou rejeitar conta que não está `PENDING` |

- **Fundamento na trilha:** módulo `01-spring-web` (`TaskNotFoundException` com `@ResponseStatus(NOT_FOUND)` devolvendo `ex.getMessage()`) e módulo `02-spring-data` (`SeatAlreadyReservedException`). Mantemos o corpo em texto por consistência com o que o projeto já faz.

### 12. Normalização do nome de usuário no construtor do domínio

`UserAccount` expõe `static String normalizeUsername(String)` (`trim` seguido de `toLowerCase(Locale.ROOT)`) e aplica essa normalização no próprio construtor. Quem só precisa consultar — `RegisterUserUseCase` ao checar unicidade e `JpaUserDetailsService` ao carregar o usuário — chama o mesmo método estático. A coluna é `unique = true`, o que garante a regra mesmo em concorrência.

- **Por quê:** evita que `Maria` e `maria` virem duas contas. `Locale.ROOT` em vez do locale padrão porque a conversão de maiúsculas depende de idioma (o caso clássico é o `I` sem ponto do turco), e o resultado não pode variar conforme a máquina — é a mesma preocupação que motivou ler o prompt em UTF-8 explícito no change `fix-base-defects`.
- **Por que no construtor e não só no caso de uso:** o cadastro público não é o único caminho que cria conta. O `AdminAccountInitializer` também cria, e com `APP_ADMIN_USERNAME=Admin` a conta seria gravada como `Admin` enquanto o login buscaria `admin`, deixando o administrador sem conseguir autenticar. No construtor, nenhuma origem consegue produzir uma conta fora do padrão.

### 12b. Papel e estado decididos por fábricas do domínio

`UserAccount.register(username, encodedPassword)` cria sempre `ROLE_USER` e `PENDING`; `UserAccount.administrator(username, encodedPassword)` cria `ROLE_ADMIN` e `ACTIVE`. O construtor completo continua existindo para a reconstrução a partir da entidade JPA.

- **Por quê:** o `AGENTS.md` exige regra de negócio no domínio, e "cadastro público nasce comum e pendente" (RF-S04) é regra de negócio. Com a fábrica, não existe caminho de código que crie um cadastro público com outro papel, e o caso de uso não precisa conhecer a regra.
- **Fundamento na trilha:** módulo `01-spring-web`, em que a classe de domínio concentra as invariantes e o caso de uso só orquestra.

### 13. CSRF desabilitado, com o limite documentado

`.csrf(AbstractHttpConfigurer::disable)`, como no módulo 03. O README passa a registrar que a configuração é educacional: autenticação por cookie de sessão sem proteção contra CSRF não deve ser exposta a navegador em produção.

- **Decisão do autor**, tomada na abertura deste change. A seção 9.5 da proposta de segurança já previa a necessidade de documentar o limite caso essa opção fosse escolhida.

### 14. Logout pelo configurador nativo

`.logout(logout -> logout.logoutUrl("/api/auth/logout").logoutSuccessHandler(...))` respondendo 204 e invalidando a sessão. O módulo 03 não mostra logout, mas o `LogoutFilter` é configuração básica do mesmo `SecurityFilterChain` da aula, sem tecnologia nova.

## Risks / Trade-offs

- **Autenticação sem isolamento entre os dois changes** → qualquer usuário `ACTIVE` enxerga as transações de todos até `add-transaction-ownership` ser aplicado. Mitigação: os dois changes são tratados como um ciclo único e o projeto não é publicado nesse meio-termo.
- **CSRF desabilitado** → um site malicioso poderia disparar requisições autenticadas se a API fosse consumida por navegador com cookie. Mitigação: documentar no README e manter o uso em clientes REST (`curl`, Insomnia).
- **Enumeração de usuários por tempo de resposta** → conta `PENDING` falha antes do BCrypt, então responde mais rápido que senha incorreta de conta ativa, apesar do corpo idêntico. Mitigação: nenhuma neste ciclo; o risco é registrado no README como limite conhecido, porque mitigá-lo exigiria comparação em tempo constante, fora do conteúdo da trilha.
- **Toda a suíte de testes existente quebra de uma vez** → os `*ApiIntegrationTest` passam a receber 401 e os `@SpringBootTest` a falhar sem as propriedades do administrador. Mitigação: as tarefas tratam isso como passo explícito, não como efeito colateral a descobrir na primeira execução vermelha.
- **`@WithMockUser` não produz um `AuthenticatedUser`** → funciona neste change, porque nada lê o principal, mas vai falhar no change seguinte, que precisa do `UserAccountId` para montar o `OwnerId`. Mitigação: registrado aqui para que `add-transaction-ownership` já nasça com um suporte de teste que injete um principal real.
- **Desenvolvedor não consegue mais rodar `bootRun` sem configurar duas variáveis novas** → falha imediata e com mensagem clara, mas é atrito novo. Mitigação: README e `AGENTS.md` atualizados com as variáveis obrigatórias.

## Migration Plan

1. `spring.jpa.hibernate.ddl-auto=update` cria a tabela de usuários na primeira subida. Nenhuma tabela existente é alterada e nenhuma transação é perdida.
2. Antes de subir, exportar `APP_ADMIN_USERNAME` e `APP_ADMIN_PASSWORD`; sem elas a aplicação não inicia.
3. O administrador autentica, lista os cadastros pendentes e aprova quem deve usar o sistema.
4. **Rollback:** reverter o commit do change. A tabela de usuários fica órfã no banco de desenvolvimento e pode ser descartada manualmente, sem impacto nas transações.

A decisão sobre as transações já existentes, que ainda não têm proprietário (S-08), pertence ao change `add-transaction-ownership` e não é tocada aqui.
