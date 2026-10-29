# Proposta de Evolução: Segurança e Acesso Multiusuário

Documento de levantamento para um novo ciclo de evolução da API de orçamento. Esta proposta complementa, sem substituir, a [`proposta-inicial.md`](proposta-inicial.md).

- **Base funcional:** projeto concluído após os changes registrados em `openspec/changes/archive/`.
- **Referência principal da trilha:** módulo `03-spring-security` da `dio-spring-boot-learning-track`.
- **Status:** proposta documental; nenhuma implementação ou especificação OpenSpec foi iniciada.

---

## 1. Contexto

### 1.1 Situação atual

A aplicação registra e consulta transações financeiras por REST, texto e voz, mas não identifica quem está utilizando o sistema. Todas as transações pertencem a um conjunto global de dados e qualquer cliente com acesso à API pode registrar e consultar gastos.

Esse comportamento era aceitável no projeto-base do módulo `05-spring-ai`, cujo objetivo era demonstrar transcrição, ChatClient, Tool Calling e geração de voz. Depois das correções e evoluções do primeiro ciclo, a ausência de identidade e propriedade passou a ser a principal lacuna funcional e de segurança.

### 1.2 Objetivo

Evoluir a aplicação para um ambiente multiusuário em que:

1. uma pessoa possa solicitar seu cadastro;
2. um administrador autorize ou rejeite o acesso;
3. somente usuários ativos e autenticados acessem as funcionalidades financeiras;
4. cada usuário registre e consulte exclusivamente suas próprias transações;
5. o isolamento também seja respeitado nas ferramentas chamadas pela IA.

### 1.3 Por que esta evolução é relevante

Autenticar uma pessoa sem associar os dados a ela não é suficiente. A solução precisa combinar:

- **autenticação:** confirmar quem está acessando;
- **autorização:** definir quais operações essa pessoa pode executar;
- **propriedade:** garantir que cada transação pertença a um usuário;
- **isolamento:** impedir acesso, inclusive indireto pela IA, aos dados de outra pessoa.

---

## 2. Diagnóstico

| ID | Problema atual | Impacto |
|---|---|---|
| S-01 | Não existe autenticação | Qualquer cliente pode acessar todos os endpoints |
| S-02 | Não existe cadastro nem ciclo de aprovação | Não há controle sobre quem pode utilizar a aplicação |
| S-03 | `Transaction` não possui proprietário | Os gastos não podem ser separados por pessoa |
| S-04 | Consultas do repositório são globais | Um usuário receberia transações de todos os demais |
| S-05 | As tools da IA não conhecem a identidade autenticada | O Tool Calling poderia registrar ou consultar dados sem isolamento |
| S-06 | Não existem papéis administrativos | Ninguém pode aprovar ou rejeitar novos acessos |
| S-07 | Não existe encerramento explícito da sessão | Um cliente não possui uma operação documentada de logout |
| S-08 | O banco atual pode conter transações sem proprietário | A inclusão de propriedade obrigatória exige uma estratégia para os dados existentes |

---

## 3. Escopo proposto

### 3.1 Dentro do escopo

- Administrador inicial criado automaticamente.
- Cadastro público com nome de usuário e senha.
- Estados `PENDING`, `ACTIVE` e `REJECTED`.
- Aprovação e rejeição de cadastros pelo administrador.
- Autenticação REST baseada em sessão HTTP.
- Senhas armazenadas com BCrypt.
- Papéis `ROLE_ADMIN` e `ROLE_USER`.
- Proteção dos endpoints financeiros.
- Propriedade obrigatória das transações.
- Consultas filtradas pelo proprietário autenticado.
- Isolamento nas tools executadas pela IA.
- Logout.
- Tratamento consistente de respostas `401`, `403` e erros de cadastro.
- Testes unitários, de integração, de API e de isolamento.
- Atualização da documentação de uso e segurança.

### 3.2 Fora do escopo deste ciclo

- JWT ou outro modelo de autenticação stateless.
- Login social ou OAuth2.
- Confirmação por e-mail.
- Recuperação ou troca de senha.
- Autenticação multifator.
- Token de convite.
- Expiração e revogação de convites.
- Interface gráfica para administração.
- Compartilhamento de orçamento entre usuários.
- Grupos, famílias ou contas conjuntas.
- Exclusão de usuários e anonimização de dados.

Tokens de convite permanecem como evolução futura. Quando implementados, deverão ter valor aleatório forte, validade definida, uso único, revogação e armazenamento seguro do hash em vez do token em texto puro.

---

## 4. Modelo de acesso

### 4.1 Papéis

| Papel | Capacidades |
|---|---|
| `ROLE_ADMIN` | Entrar, listar cadastros por estado, aprovar e rejeitar usuários |
| `ROLE_USER` | Entrar, registrar despesas e consultar somente os próprios gastos por REST, texto ou voz |

O administrador não recebe acesso automático aos dados financeiros dos usuários. Administração de contas e acesso a informações financeiras são responsabilidades diferentes. Caso seja necessária auditoria global no futuro, ela deverá ser proposta explicitamente.

### 4.2 Estados do usuário

| Estado | Significado | Pode autenticar? |
|---|---|---|
| `PENDING` | Cadastro realizado e aguardando decisão | Não |
| `ACTIVE` | Cadastro aprovado | Sim |
| `REJECTED` | Cadastro recusado | Não |

Transições permitidas neste ciclo:

```text
PENDING ──aprovar──> ACTIVE
PENDING ──rejeitar─> REJECTED
```

Reativação, suspensão e mudança de uma decisão anterior ficam fora do escopo inicial.

### 4.3 Administrador inicial

Um único administrador inicial será criado no início da aplicação quando ainda não existir uma conta administrativa.

- Nome de usuário e senha virão de variáveis de ambiente.
- A senha será codificada com BCrypt antes de ser persistida.
- Nenhuma credencial real será incluída no repositório.
- A aplicação deverá falhar com mensagem clara quando as credenciais obrigatórias do administrador não estiverem configuradas no ambiente adequado.

Nomes propostos para as variáveis:

```text
APP_ADMIN_USERNAME
APP_ADMIN_PASSWORD
```

Valores de teste serão definidos apenas no contexto automatizado de testes.

---

## 5. Fluxos funcionais

### 5.1 Cadastro público

1. A pessoa envia nome de usuário e senha.
2. A aplicação valida os campos e verifica a unicidade do nome.
3. A senha é codificada com BCrypt.
4. A conta é persistida como `ROLE_USER` e `PENDING`.
5. A resposta informa que o cadastro aguarda aprovação, sem iniciar uma sessão.

O cliente nunca poderá escolher o próprio papel ou estado no payload.

### 5.2 Aprovação

1. O administrador autentica-se.
2. Consulta os usuários pendentes.
3. Aprova ou rejeita um cadastro.
4. A aplicação valida que o usuário está em `PENDING`.
5. O estado muda para `ACTIVE` ou `REJECTED`.

Somente `ROLE_ADMIN` pode executar essas operações.

### 5.3 Login e logout

O login seguirá o módulo `03-spring-security`:

- credenciais enviadas em JSON;
- autenticação por nome de usuário e senha;
- contexto salvo em sessão HTTP;
- cookie de sessão devolvido ao cliente;
- endpoints seguintes utilizam essa sessão.

Usuários `PENDING` ou `REJECTED` não poderão iniciar sessão, mesmo com senha correta. O logout encerrará a sessão atual.

### 5.4 Operação financeira

1. O Spring Security identifica o usuário da sessão.
2. A camada HTTP ou o adaptador de IA converte a identidade autenticada em `OwnerId`.
3. O caso de uso recebe o proprietário como dado confiável da aplicação.
4. O repositório persiste ou consulta usando esse proprietário.
5. A resposta contém somente dados pertencentes ao usuário autenticado.

---

## 6. Requisitos funcionais

| ID | Requisito |
|---|---|
| RF-S01 | A API deve permitir cadastro público com nome de usuário e senha |
| RF-S02 | O nome de usuário deve ser obrigatório e único |
| RF-S03 | A senha deve ser obrigatória, validada e armazenada somente após codificação com BCrypt |
| RF-S04 | Todo cadastro público deve ser criado com papel `ROLE_USER` e estado `PENDING`, independentemente do payload recebido |
| RF-S05 | Um administrador inicial deve ser criado a partir de credenciais fornecidas pelo ambiente |
| RF-S06 | A API deve permitir login REST por sessão HTTP |
| RF-S07 | Somente usuários `ACTIVE` devem conseguir autenticar |
| RF-S08 | Somente `ROLE_ADMIN` deve listar, aprovar ou rejeitar cadastros |
| RF-S09 | Todos os endpoints financeiros devem exigir autenticação |
| RF-S10 | Toda transação deve possuir um `OwnerId` obrigatório obtido da identidade autenticada |
| RF-S11 | Registro, listagem por categoria, resumo e consulta por período devem operar somente sobre as transações do proprietário autenticado |
| RF-S12 | Os comandos por texto e voz devem respeitar o mesmo proprietário e isolamento dos endpoints REST |
| RF-S13 | O identificador do proprietário não deve ser aceito no JSON, multipart ou parâmetros fornecidos à IA |
| RF-S14 | A API deve permitir o encerramento da sessão autenticada |
| RF-S15 | Tentativas sem autenticação devem receber HTTP 401 e operações sem permissão devem receber HTTP 403 |
| RF-S16 | Nome de usuário duplicado e transições inválidas de estado devem produzir respostas de erro claras e previsíveis |

---

## 7. Requisitos não funcionais

| ID | Requisito |
|---|---|
| RNF-S01 | Nenhuma senha ou credencial real deve ser versionada ou registrada em logs |
| RNF-S02 | O cliente nunca deve conseguir definir papel, estado ou proprietário por dados de entrada |
| RNF-S03 | Todas as consultas ao repositório de transações devem incluir o proprietário |
| RNF-S04 | O isolamento deve ser aplicado no servidor, sem depender das instruções dadas ao modelo de IA |
| RNF-S05 | Casos de uso e domínio devem permanecer independentes do Spring Security |
| RNF-S06 | A implementação deve usar os padrões do módulo `03-spring-security`: sessão, `SecurityFilterChain`, BCrypt, principal autenticado e autorização por papel |
| RNF-S07 | A suíte deve continuar executável sem `OPENAI_API_KEY`; testes ao vivo da OpenAI permanecem condicionais |
| RNF-S08 | A documentação da API deve incluir cadastro, login, aprovação, cookies de sessão, logout e respostas de segurança |

---

## 8. Contratos HTTP propostos

Os contratos abaixo são preliminares e deverão ser formalizados nas specs do OpenSpec antes da implementação.

| Método e caminho | Acesso | Objetivo |
|---|---|---|
| `POST /api/auth/register` | Público | Solicitar cadastro |
| `POST /api/auth/login` | Público | Autenticar e criar sessão |
| `POST /api/auth/logout` | Autenticado | Encerrar sessão |
| `GET /api/admin/users?status=PENDING` | `ROLE_ADMIN` | Listar cadastros por estado |
| `POST /api/admin/users/{id}/approve` | `ROLE_ADMIN` | Aprovar cadastro pendente |
| `POST /api/admin/users/{id}/reject` | `ROLE_ADMIN` | Rejeitar cadastro pendente |

Todos os endpoints atuais em `/transactions` passarão a exigir `ROLE_USER` e operarão exclusivamente sobre os dados do principal autenticado.

### 8.1 Exemplo de cadastro

```json
{
  "username": "maria",
  "password": "senha-forte"
}
```

Resposta conceitual:

```json
{
  "id": "uuid",
  "username": "maria",
  "status": "PENDING"
}
```

O retorno nunca deve expor a senha codificada.

---

## 9. Arquitetura proposta

### 9.1 Contexto de autenticação

Será criado um contexto `auth`, inspirado no módulo `03-spring-security`, contendo:

- integração do usuário persistido com `UserDetails`;
- repositório de usuários;
- serviço de carregamento do usuário;
- filtro de login REST;
- configuração do `SecurityFilterChain`;
- codificador BCrypt;
- endpoints e casos de uso de cadastro e aprovação.

### 9.2 Propriedade no domínio financeiro

O contexto financeiro utilizará `OwnerId` como identificador fortemente tipado, sem depender diretamente da entidade de segurança `User`.

`Transaction` passará a possuir um `OwnerId` obrigatório. O mesmo valor será persistido em `TransactionEntity` por uma coluna `owner_id` não nula e indexada.

Essa separação segue o módulo 03: o contexto financeiro conhece o conceito de proprietário, mas não precisa conhecer detalhes de autenticação, senha, papel ou sessão.

### 9.3 Casos de uso e repositório

Os casos de uso financeiros receberão o `OwnerId` junto à operação. O contrato `TransactionRepository` e todas as implementações deverão exigir esse identificador em registros e consultas.

Não será mantida uma variante global de consulta sem proprietário, porque ela poderia ser utilizada acidentalmente e causar vazamento de dados.

### 9.4 Identidade nas tools da IA

O modelo de IA não receberá `ownerId` como `@ToolParam` e nunca decidirá a quem pertence uma operação.

`TransactionTools`, por estar na infraestrutura, obterá a identidade autenticada do contexto de segurança e enviará o `OwnerId` ao caso de uso. Assim:

- a descrição da tool exposta ao modelo continua exclusivamente financeira;
- o identificador não aparece no JSON Schema da tool;
- uma tentativa de prompt injection não consegue trocar o proprietário;
- REST, texto e voz aplicam a mesma regra de isolamento.

### 9.5 Sessão e CSRF

Para aderência ao módulo 03 e uso atual por clientes REST como Insomnia e `curl`, o primeiro ciclo usará sessão HTTP. A configuração exata de CSRF deverá ser decidida no design do change:

- desabilitar CSRF reproduz o exemplo educacional da trilha e simplifica clientes REST;
- aplicações futuras executadas em navegador com autenticação por cookie exigem proteção contra CSRF.

Se CSRF for desabilitado, essa limitação deverá ser documentada explicitamente e a configuração não deverá ser apresentada como pronta para produção em navegador.

---

## 10. Estratégia para dados existentes

A aplicação utiliza atualmente `spring.jpa.hibernate.ddl-auto=update`, e as transações existentes não possuem proprietário. Adicionar diretamente uma coluna `owner_id` obrigatória pode falhar ou deixar dados sem identidade válida.

Alternativas a decidir no design:

1. **Recriar o banco de desenvolvimento:** solução recomendada para este projeto educacional, desde que os dados atuais sejam descartáveis.
2. **Atribuir dados existentes ao administrador:** simples, mas mistura administração de contas com propriedade financeira e não é a opção recomendada.
3. **Criar uma migração explícita:** mais apropriada para um ambiente com dados reais, porém introduziria uma ferramenta de migração ainda não utilizada na trilha.

A implementação não deve apagar dados automaticamente sem uma decisão explícita e documentada.

---

## 11. Estratégia de testes

### 11.1 Autenticação e cadastro

- cadastro válido cria usuário `PENDING`;
- senha persistida não corresponde ao texto original e é validável pelo BCrypt;
- nome duplicado é rejeitado;
- payload não consegue definir `ROLE_ADMIN` ou `ACTIVE`;
- usuário pendente ou rejeitado não autentica;
- usuário ativo autentica e recebe sessão;
- logout invalida a sessão.

### 11.2 Autorização administrativa

- usuário comum não lista nem aprova cadastros;
- administrador lista pendentes;
- administrador aprova ou rejeita;
- transição fora de `PENDING` é rejeitada.

### 11.3 Isolamento financeiro

- requisição sem sessão recebe `401`;
- usuário A registra uma transação associada a A;
- usuário B não recebe a transação de A por categoria;
- resumo de B não soma valores de A;
- consulta por período de B não inclui valores de A;
- usuário não consegue forjar `ownerId` no payload.

### 11.4 Isolamento no Tool Calling

- tool de registro associa a transação ao principal autenticado;
- tools de consulta usam o mesmo principal;
- parâmetro de proprietário não aparece no contrato exposto ao modelo;
- comandos de texto e voz de usuários diferentes não compartilham resultados.

Os testes seguirão `docs/padrao-de-testes.md`, com testes unitários e integração real via Testcontainers. Testes de segurança HTTP usarão suporte do Spring Security para MockMvc, conforme definido no futuro design.

---

## 12. Aderência à trilha

| Tema | Referência | Aplicação proposta |
|---|---|---|
| Spring Security e `SecurityFilterChain` | Módulo 03 | Proteção dos endpoints e regras de acesso |
| Login REST com sessão | Módulo 03 | `POST /api/auth/login` e cookie de sessão |
| `UserDetails` e carregamento por JPA | Módulo 03 | Autenticação dos usuários persistidos |
| BCrypt | Módulo 03 | Armazenamento seguro das senhas |
| Papéis e autorização | Módulo 03 | Separação entre `ROLE_ADMIN` e `ROLE_USER` |
| Principal autenticado | Módulo 03 | Origem confiável do proprietário |
| `OwnerId` no domínio | Módulos 00 e 03 | Isolamento sem acoplar o domínio financeiro ao Spring Security |
| Repository Pattern e casos de uso | Módulo 00 | Operações filtradas por proprietário |
| Bean Validation e tratamento de erros | Módulo 01 | Validação do cadastro e respostas HTTP |
| Spring Data JPA | Módulo 02 | Persistência de usuários e propriedade |
| Tool Calling | Módulo 05 | Identidade aplicada internamente às tools |

---

## 13. Direção para o futuro ciclo OpenSpec

Nenhum change foi criado por este documento. Quando o ciclo for autorizado, a primeira atividade será transformar esta proposta em requisitos e cenários verificáveis.

Por se tratar de uma fronteira de segurança transversal, a implementação deve evitar um estado final em que exista autenticação sem isolamento. A sugestão inicial é um único change coerente:

`add-secure-multi-user-access`

O change poderá ser executado em tarefas e commits internos menores:

1. modelo de usuário, estados, papéis e bootstrap do administrador;
2. cadastro, login, logout e aprovação;
3. `OwnerId` no domínio e persistência;
4. consultas financeiras filtradas;
5. identidade autenticada nas tools;
6. testes de autenticação, autorização e isolamento;
7. documentação REST e README.

A decisão sobre dados existentes e a configuração de CSRF deverá ser fechada no `design.md` antes do apply.

---

## 14. Critérios de conclusão do ciclo

O novo ciclo será considerado concluído quando:

1. um cadastro público começar como `PENDING`;
2. somente o administrador puder aprová-lo;
3. somente usuários `ACTIVE` puderem autenticar;
4. todos os endpoints financeiros exigirem sessão;
5. dois usuários nunca enxergarem ou alterarem transações um do outro;
6. o mesmo isolamento existir nas tools da IA;
7. nenhuma senha, credencial ou proprietário for controlável pelo modelo ou pelo payload;
8. a suíte completa estiver verde sem chave real da OpenAI;
9. a documentação ensinar cadastro, aprovação, login, uso do cookie e logout;
10. os riscos e limites educacionais da configuração estiverem documentados.

---

## 15. Questões que deverão ser fechadas antes da implementação

1. Os dados atuais de desenvolvimento podem ser descartados para adicionar a propriedade obrigatória?
2. Qual política mínima de tamanho e composição será aplicada às senhas?
3. A primeira versão desabilitará CSRF como o módulo 03 ou incluirá proteção compatível com os clientes utilizados?
4. O nome de usuário será sensível a maiúsculas e minúsculas?
5. Quais mensagens serão devolvidas para usuário pendente e rejeitado sem expor informações desnecessárias?
