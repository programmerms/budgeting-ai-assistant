# Spec Delta

## Purpose

Contas de usuário da API de orçamento: cadastro público, aprovação pelo administrador, autenticação por sessão HTTP e autorização por papel, incluindo a regra de que as funcionalidades financeiras só atendem pessoas autenticadas.

## ADDED Requirements

### Requirement: Cadastro público de usuário
O sistema SHALL aceitar solicitações de cadastro sem autenticação em `POST /api/auth/register`, recebendo JSON com `username` e `password`. A resposta SHALL ter status 201 e conter `id` (UUID gerado pelo sistema), `username` e `status`. A resposta SHALL NOT conter a senha, em texto puro ou codificada. O cadastro SHALL NOT iniciar uma sessão.

#### Scenario: Cadastro aceito
- **WHEN** `POST /api/auth/register` recebe `{"username": "maria", "password": "senha-forte"}` e não existe conta com esse nome
- **THEN** o sistema responde HTTP 201 com `id` em formato UUID, `username` igual a `maria` e `status` igual a `PENDING`

#### Scenario: Resposta do cadastro não expõe a senha
- **WHEN** `POST /api/auth/register` recebe um cadastro válido
- **THEN** o corpo da resposta não contém nenhum campo de senha

#### Scenario: Cadastro não autentica
- **WHEN** `POST /api/auth/register` recebe um cadastro válido
- **THEN** a resposta não cria sessão nem devolve cookie de sessão, e uma requisição seguinte a um endpoint financeiro responde HTTP 401

### Requirement: Papel e estado definidos pelo sistema
O sistema SHALL criar toda conta vinda do cadastro público com o papel `ROLE_USER` e o estado `PENDING`, independentemente do conteúdo da requisição. Campos de papel ou estado enviados pelo cliente SHALL ser ignorados.

#### Scenario: Cliente tenta escolher o papel de administrador
- **WHEN** `POST /api/auth/register` recebe `{"username": "ana", "password": "senha-forte", "role": "ROLE_ADMIN"}`
- **THEN** a conta é criada com o papel `ROLE_USER`, e depois de aprovada ela recebe HTTP 403 ao tentar listar cadastros pendentes

#### Scenario: Cliente tenta escolher o estado ativo
- **WHEN** `POST /api/auth/register` recebe `{"username": "joao", "password": "senha-forte", "status": "ACTIVE"}`
- **THEN** a conta é criada com o estado `PENDING` e a resposta mostra `status` igual a `PENDING`

### Requirement: Nome de usuário único e insensível a maiúsculas
O sistema SHALL tratar o nome de usuário como insensível a maiúsculas e minúsculas, normalizando-o para minúsculas ao cadastrar e ao autenticar. Dois cadastros que só diferem por maiúsculas SHALL ser considerados o mesmo nome. Uma tentativa de cadastro com nome já existente SHALL responder HTTP 409 com uma mensagem de erro, sem criar conta.

#### Scenario: Nome de usuário duplicado
- **WHEN** já existe a conta `maria` e `POST /api/auth/register` recebe `{"username": "maria", "password": "outra-senha"}`
- **THEN** o sistema responde HTTP 409 com uma mensagem de erro e nenhuma conta nova é criada

#### Scenario: Nome duplicado apenas por maiúsculas
- **WHEN** já existe a conta `maria` e `POST /api/auth/register` recebe `{"username": "MARIA", "password": "outra-senha"}`
- **THEN** o sistema responde HTTP 409 e nenhuma conta nova é criada

#### Scenario: Autenticação com o nome em maiúsculas
- **WHEN** a conta `maria` está `ACTIVE` e `POST /api/auth/login` recebe `{"username": "Maria", "password": "senha-forte"}`
- **THEN** a autenticação é aceita e o sistema responde HTTP 200

### Requirement: Validação dos dados do cadastro
O sistema SHALL rejeitar o cadastro quando `username` estiver ausente ou em branco, ou quando `password` estiver ausente, em branco, tiver menos de 8 caracteres ou mais de 72 caracteres. A resposta SHALL ter status 400 com um objeto JSON em que cada chave é o nome do campo inválido e o valor é a mensagem correspondente, sem criar conta.

#### Scenario: Nome de usuário em branco
- **WHEN** `POST /api/auth/register` recebe `{"username": "  ", "password": "senha-forte"}`
- **THEN** o sistema responde HTTP 400 indicando o campo `username` e nenhuma conta é criada

#### Scenario: Senha curta demais
- **WHEN** `POST /api/auth/register` recebe `{"username": "maria", "password": "curta"}`
- **THEN** o sistema responde HTTP 400 indicando o campo `password` e nenhuma conta é criada

#### Scenario: Senha longa demais
- **WHEN** `POST /api/auth/register` recebe uma senha com 73 caracteres
- **THEN** o sistema responde HTTP 400 indicando o campo `password` e nenhuma conta é criada

### Requirement: Senha armazenada apenas codificada
O sistema SHALL codificar a senha com BCrypt antes de persistir a conta. A senha em texto puro SHALL NOT ser gravada no banco de dados nem registrada em log.

#### Scenario: Senha persistida é diferente do texto original
- **WHEN** a conta `maria` é cadastrada com a senha `senha-forte`
- **THEN** o valor da senha guardado no banco é diferente de `senha-forte` e é reconhecido como válido ao verificar `senha-forte` com BCrypt

### Requirement: Administrador inicial vindo do ambiente
O sistema SHALL criar, na subida da aplicação, uma conta `ROLE_ADMIN` e `ACTIVE` a partir das variáveis de ambiente `APP_ADMIN_USERNAME` e `APP_ADMIN_PASSWORD`, somente quando ainda não existir nenhuma conta administrativa. Quando essas variáveis não estiverem definidas, a aplicação SHALL falhar na subida com uma mensagem que identifique as variáveis faltantes.

#### Scenario: Administrador criado na primeira subida
- **WHEN** a aplicação sobe com `APP_ADMIN_USERNAME` e `APP_ADMIN_PASSWORD` definidos e não existe conta administrativa
- **THEN** existe uma conta com esse nome, papel `ROLE_ADMIN` e estado `ACTIVE`, capaz de autenticar com a senha informada

#### Scenario: Administrador não é duplicado
- **WHEN** a aplicação sobe duas vezes seguidas com as mesmas variáveis
- **THEN** continua existindo exatamente uma conta administrativa

#### Scenario: Credenciais do administrador ausentes
- **WHEN** a aplicação sobe sem `APP_ADMIN_USERNAME` ou sem `APP_ADMIN_PASSWORD`
- **THEN** a subida falha com uma mensagem que identifica a variável de ambiente faltante

### Requirement: Autenticação por sessão HTTP
O sistema SHALL autenticar em `POST /api/auth/login`, recebendo JSON com `username` e `password`. Em caso de sucesso, a resposta SHALL ter status 200 e estabelecer uma sessão HTTP, devolvendo o cookie de sessão. Requisições seguintes que apresentarem esse cookie SHALL ser tratadas como autenticadas.

#### Scenario: Login de usuário ativo
- **WHEN** a conta `maria` está `ACTIVE` e `POST /api/auth/login` recebe as credenciais corretas
- **THEN** o sistema responde HTTP 200 e devolve um cookie de sessão

#### Scenario: Sessão reaproveitada na requisição seguinte
- **WHEN** um cliente autentica com sucesso e envia o cookie de sessão em `GET /transactions/GROCERIES`
- **THEN** o sistema responde HTTP 200 em vez de 401

### Requirement: Somente usuários ativos autenticam
O sistema SHALL recusar a autenticação de contas nos estados `PENDING` e `REJECTED`, mesmo quando a senha estiver correta. Nenhuma sessão SHALL ser criada nesses casos.

#### Scenario: Usuário pendente não autentica
- **WHEN** a conta `maria` está `PENDING` e `POST /api/auth/login` recebe as credenciais corretas
- **THEN** o sistema responde HTTP 401 e não cria sessão

#### Scenario: Usuário rejeitado não autentica
- **WHEN** a conta `joao` está `REJECTED` e `POST /api/auth/login` recebe as credenciais corretas
- **THEN** o sistema responde HTTP 401 e não cria sessão

### Requirement: Falha de login com resposta indistinguível
O sistema SHALL responder HTTP 401 com a mesma mensagem genérica para qualquer falha de autenticação, seja conta inexistente, senha incorreta, conta `PENDING` ou conta `REJECTED`. A resposta SHALL NOT permitir descobrir se um nome de usuário existe nem em que estado a conta está.

#### Scenario: Conta inexistente e senha incorreta produzem a mesma resposta
- **WHEN** `POST /api/auth/login` é chamado uma vez com um nome de usuário inexistente e outra vez com um nome existente e senha incorreta
- **THEN** as duas respostas têm status 401 e corpo idêntico

#### Scenario: Conta pendente produz a mesma resposta de senha incorreta
- **WHEN** `POST /api/auth/login` é chamado com as credenciais corretas de uma conta `PENDING` e, em outra tentativa, com a senha incorreta de uma conta `ACTIVE`
- **THEN** as duas respostas têm status 401 e corpo idêntico

### Requirement: Encerramento da sessão
O sistema SHALL encerrar a sessão autenticada em `POST /api/auth/logout`, respondendo HTTP 204. Depois do logout, o cookie anterior SHALL NOT dar acesso a endpoints protegidos.

#### Scenario: Logout invalida a sessão
- **WHEN** um cliente autenticado chama `POST /api/auth/logout` e depois usa o mesmo cookie em `GET /transactions/GROCERIES`
- **THEN** o logout responde HTTP 204 e a requisição seguinte responde HTTP 401

### Requirement: Listagem de cadastros por estado pelo administrador
O sistema SHALL listar, em `GET /api/admin/users?status={status}`, as contas no estado informado, com status 200. Cada item SHALL conter `id`, `username` e `status`, e SHALL NOT conter a senha. O parâmetro `status` SHALL ser obrigatório e aceitar apenas `PENDING`, `ACTIVE` e `REJECTED`.

#### Scenario: Administrador lista cadastros pendentes
- **WHEN** existem duas contas `PENDING` e uma `ACTIVE`, e um administrador autenticado chama `GET /api/admin/users?status=PENDING`
- **THEN** o sistema responde HTTP 200 com exatamente as duas contas `PENDING`, sem nenhum campo de senha

#### Scenario: Estado desconhecido na consulta
- **WHEN** um administrador autenticado chama `GET /api/admin/users?status=INVALID`
- **THEN** o sistema responde HTTP 400

#### Scenario: Parâmetro de estado ausente
- **WHEN** um administrador autenticado chama `GET /api/admin/users` sem o parâmetro `status`
- **THEN** o sistema responde HTTP 400

### Requirement: Decisão do administrador sobre cadastro pendente
O sistema SHALL permitir que o administrador aprove um cadastro em `POST /api/admin/users/{id}/approve`, levando-o a `ACTIVE`, e o rejeite em `POST /api/admin/users/{id}/reject`, levando-o a `REJECTED`. A resposta SHALL ter status 200 e conter `id`, `username` e o novo `status`.

#### Scenario: Cadastro aprovado
- **WHEN** a conta `maria` está `PENDING` e um administrador autenticado chama `POST /api/admin/users/{id}/approve`
- **THEN** o sistema responde HTTP 200 com `status` igual a `ACTIVE`, e `maria` passa a conseguir autenticar

#### Scenario: Cadastro rejeitado
- **WHEN** a conta `joao` está `PENDING` e um administrador autenticado chama `POST /api/admin/users/{id}/reject`
- **THEN** o sistema responde HTTP 200 com `status` igual a `REJECTED`, e `joao` continua sem conseguir autenticar

#### Scenario: Conta inexistente
- **WHEN** um administrador autenticado chama `POST /api/admin/users/{id}/approve` com um `id` que não corresponde a nenhuma conta
- **THEN** o sistema responde HTTP 404

### Requirement: Transição de estado apenas a partir de pendente
O sistema SHALL aceitar aprovação e rejeição somente de contas no estado `PENDING`. Uma tentativa de decidir sobre conta já `ACTIVE` ou `REJECTED` SHALL responder HTTP 409 com uma mensagem de erro, sem alterar o estado.

#### Scenario: Aprovar conta já ativa
- **WHEN** a conta `maria` está `ACTIVE` e um administrador autenticado chama `POST /api/admin/users/{id}/approve`
- **THEN** o sistema responde HTTP 409 e a conta continua `ACTIVE`

#### Scenario: Aprovar conta rejeitada
- **WHEN** a conta `joao` está `REJECTED` e um administrador autenticado chama `POST /api/admin/users/{id}/approve`
- **THEN** o sistema responde HTTP 409 e a conta continua `REJECTED`

### Requirement: Administração restrita ao papel de administrador
O sistema SHALL permitir listar, aprovar e rejeitar cadastros somente a sessões com o papel `ROLE_ADMIN`. Uma requisição sem sessão SHALL receber HTTP 401 e uma sessão sem esse papel SHALL receber HTTP 403, em ambos os casos sem alterar nenhum dado.

#### Scenario: Usuário comum tenta listar cadastros
- **WHEN** um usuário `ROLE_USER` autenticado chama `GET /api/admin/users?status=PENDING`
- **THEN** o sistema responde HTTP 403

#### Scenario: Usuário comum tenta aprovar um cadastro
- **WHEN** um usuário `ROLE_USER` autenticado chama `POST /api/admin/users/{id}/approve` para uma conta `PENDING`
- **THEN** o sistema responde HTTP 403 e a conta continua `PENDING`

#### Scenario: Requisição administrativa sem sessão
- **WHEN** `GET /api/admin/users?status=PENDING` é chamado sem cookie de sessão
- **THEN** o sistema responde HTTP 401

### Requirement: Endpoints financeiros exigem sessão autenticada
O sistema SHALL exigir sessão autenticada com o papel `ROLE_USER` em todos os endpoints financeiros: `POST /transactions`, `GET /transactions`, `GET /transactions/{category}`, `GET /transactions/summary`, `POST /transactions/ai` e `POST /transactions/ai/text`. Requisição sem sessão SHALL receber HTTP 401 e sessão sem o papel SHALL receber HTTP 403, sem executar a operação.

#### Scenario: Registro de transação sem sessão
- **WHEN** `POST /transactions` é chamado com um corpo válido e sem cookie de sessão
- **THEN** o sistema responde HTTP 401 e nenhuma transação é salva

#### Scenario: Consultas financeiras sem sessão
- **WHEN** `GET /transactions/GROCERIES`, `GET /transactions/summary?category=GROCERIES` ou `GET /transactions?from=2026-10-01&to=2026-10-31` são chamados sem cookie de sessão
- **THEN** cada um responde HTTP 401

#### Scenario: Comandos de IA sem sessão
- **WHEN** `POST /transactions/ai` ou `POST /transactions/ai/text` são chamados sem cookie de sessão
- **THEN** cada um responde HTTP 401, sem transcrever áudio, sem chamar o modelo e sem executar nenhuma ferramenta

#### Scenario: Usuário ativo acessa os endpoints financeiros
- **WHEN** um usuário `ROLE_USER` autenticado chama `POST /transactions` com um corpo válido
- **THEN** o sistema responde HTTP 201, como antes deste change

### Requirement: Administração separada dos dados financeiros
O sistema SHALL NOT conceder ao papel `ROLE_ADMIN` acesso aos endpoints financeiros. Administrar contas e consultar gastos SHALL ser responsabilidades distintas.

#### Scenario: Administrador tenta consultar gastos
- **WHEN** um administrador autenticado chama `GET /transactions/GROCERIES`
- **THEN** o sistema responde HTTP 403

#### Scenario: Administrador tenta registrar uma transação
- **WHEN** um administrador autenticado chama `POST /transactions` com um corpo válido
- **THEN** o sistema responde HTTP 403 e nenhuma transação é salva
