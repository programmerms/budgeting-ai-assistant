package dio.budgeting.auth.infrastructure.http;

import dio.budgeting.auth.application.PasswordEncoderGateway;
import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountRepository;
import dio.budgeting.auth.domain.UserStatus;
import dio.budgeting.auth.infrastructure.http.request.RegisterUserRequest;
import dio.budgeting.auth.infrastructure.persistence.repository.UserAccountEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.json.JsonMapper;

import static dio.budgeting.support.TestConstants.RAW_PASSWORD;
import static dio.budgeting.support.TestConstants.USERNAME;
import static dio.budgeting.support.request.RegisterUserRequestSupport.defaultRegisterUserRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-key",
        "app.admin.username=test-admin",
        "app.admin.password=test-admin-password"})
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AuthApiIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:9.6")
            .withStartupTimeoutSeconds(300);

    private final WebApplicationContext webApplicationContext;
    private final JsonMapper jsonMapper;
    private final UserAccountRepository repository;
    private final UserAccountEntityRepository entityRepository;
    private final PasswordEncoderGateway passwordEncoderGateway;
    private final PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    AuthApiIntegrationTest(final WebApplicationContext webApplicationContext,
                           final JsonMapper jsonMapper,
                           final UserAccountRepository repository,
                           final UserAccountEntityRepository entityRepository,
                           final PasswordEncoderGateway passwordEncoderGateway,
                           final PasswordEncoder passwordEncoder) {
        this.webApplicationContext = webApplicationContext;
        this.jsonMapper = jsonMapper;
        this.repository = repository;
        this.entityRepository = entityRepository;
        this.passwordEncoderGateway = passwordEncoderGateway;
        this.passwordEncoder = passwordEncoder;
    }

    @BeforeEach
    void setUp(final RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
        entityRepository.deleteAll();
    }

    @Test
    @DisplayName("Given a valid registration When POST /api/auth/register is called Then it responds 201 with a pending account")
    void givenValidRegistrationWhenPostRegisterIsCalledThenItResponds201WithAPendingAccount() throws Exception {
        final var request = defaultRegisterUserRequest().create();

        final var result = register(request);

        verifyGivenValidRegistrationWhenPostRegisterIsCalledThenItResponds201WithAPendingAccount(result);
    }

    @Test
    @DisplayName("Given a valid registration When POST /api/auth/register is called Then the response body carries no password")
    void givenValidRegistrationWhenPostRegisterIsCalledThenTheResponseBodyCarriesNoPassword() throws Exception {
        final var request = defaultRegisterUserRequest().create();

        final var result = register(request);

        verifyGivenValidRegistrationWhenPostRegisterIsCalledThenTheResponseBodyCarriesNoPassword(result);
    }

    @Test
    @DisplayName("Given a fresh registration When a financial endpoint is called Then it responds 401 because registering does not authenticate")
    void givenFreshRegistrationWhenAFinancialEndpointIsCalledThenItResponds401BecauseRegisteringDoesNotAuthenticate()
            throws Exception {
        final var registration = register(defaultRegisterUserRequest().create());

        final var result = mockMvc.perform(get("/transactions/GROCERIES"));

        verifyGivenFreshRegistrationWhenAFinancialEndpointIsCalledThenItResponds401BecauseRegisteringDoesNotAuthenticate(
                registration, result);
    }

    @Test
    @DisplayName("Given a registration claiming the admin role When POST /api/auth/register is called Then the account is created as a common user")
    void givenRegistrationClaimingAdminRoleWhenPostRegisterIsCalledThenTheAccountIsCreatedAsACommonUser()
            throws Exception {
        final var result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"ana","password":"senha-forte","role":"ROLE_ADMIN"}"""));

        verifyGivenRegistrationClaimingAdminRoleWhenPostRegisterIsCalledThenTheAccountIsCreatedAsACommonUser(result);
    }

    @Test
    @DisplayName("Given a registration claiming the active status When POST /api/auth/register is called Then the account is created as pending")
    void givenRegistrationClaimingActiveStatusWhenPostRegisterIsCalledThenTheAccountIsCreatedAsPending()
            throws Exception {
        final var result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"joao","password":"senha-forte","status":"ACTIVE"}"""));

        verifyGivenRegistrationClaimingActiveStatusWhenPostRegisterIsCalledThenTheAccountIsCreatedAsPending(result);
    }

    @Test
    @DisplayName("Given a taken username When POST /api/auth/register is called Then it responds 409 and creates no account")
    void givenTakenUsernameWhenPostRegisterIsCalledThenItResponds409AndCreatesNoAccount() throws Exception {
        setupStoredAccount(UserStatus.PENDING);

        final var result = register(defaultRegisterUserRequest().create());

        verifyConflictWithoutCreatingAccount(result);
    }

    @Test
    @DisplayName("Given a username taken only in a different case When POST /api/auth/register is called Then it responds 409")
    void givenUsernameTakenOnlyInDifferentCaseWhenPostRegisterIsCalledThenItResponds409() throws Exception {
        setupStoredAccount(UserStatus.PENDING);

        final var result = register(defaultRegisterUserRequest()
                .set(field(RegisterUserRequest::username), "MARIA")
                .create());

        verifyConflictWithoutCreatingAccount(result);
    }

    @Test
    @DisplayName("Given an active account When login uses the username in uppercase Then it responds 200")
    void givenActiveAccountWhenLoginUsesTheUsernameInUppercaseThenItResponds200() throws Exception {
        setupStoredAccount(UserStatus.ACTIVE);

        final var result = login("Maria", RAW_PASSWORD);

        verifyOk(result);
    }

    @Test
    @DisplayName("Given a blank username When POST /api/auth/register is called Then it responds 400 indicating username")
    void givenBlankUsernameWhenPostRegisterIsCalledThenItResponds400IndicatingUsername() throws Exception {
        final var result = register(defaultRegisterUserRequest()
                .set(field(RegisterUserRequest::username), "  ")
                .create());

        verifyBadRequestIndicating(result, "username");
    }

    @Test
    @DisplayName("Given a password shorter than eight characters When POST /api/auth/register is called Then it responds 400 indicating password")
    void givenPasswordShorterThanEightCharactersWhenPostRegisterIsCalledThenItResponds400IndicatingPassword()
            throws Exception {
        final var result = register(defaultRegisterUserRequest()
                .set(field(RegisterUserRequest::password), "curta")
                .create());

        verifyBadRequestIndicating(result, "password");
    }

    @Test
    @DisplayName("Given a password longer than seventy-two characters When POST /api/auth/register is called Then it responds 400 indicating password")
    void givenPasswordLongerThanSeventyTwoCharactersWhenPostRegisterIsCalledThenItResponds400IndicatingPassword()
            throws Exception {
        final var result = register(defaultRegisterUserRequest()
                .set(field(RegisterUserRequest::password), "a".repeat(73))
                .create());

        verifyBadRequestIndicating(result, "password");
    }

    @Test
    @DisplayName("Given a valid registration When the account is persisted Then the stored password is BCrypt encoded")
    void givenValidRegistrationWhenTheAccountIsPersistedThenTheStoredPasswordIsBCryptEncoded() throws Exception {
        register(defaultRegisterUserRequest().create());

        final var stored = repository.findByUsername(USERNAME);

        verifyGivenValidRegistrationWhenTheAccountIsPersistedThenTheStoredPasswordIsBCryptEncoded(
                stored.orElseThrow());
    }

    @Test
    @DisplayName("Given an active account When POST /api/auth/login is called Then it responds 200 and establishes a session")
    void givenActiveAccountWhenPostLoginIsCalledThenItResponds200AndEstablishesASession() throws Exception {
        setupStoredAccount(UserStatus.ACTIVE);

        final var result = login(USERNAME, RAW_PASSWORD);

        verifyGivenActiveAccountWhenPostLoginIsCalledThenItResponds200AndEstablishesASession(result);
    }

    @Test
    @DisplayName("Given an authenticated session When a financial endpoint is called Then it responds 200 instead of 401")
    void givenAuthenticatedSessionWhenAFinancialEndpointIsCalledThenItResponds200InsteadOf401() throws Exception {
        setupStoredAccount(UserStatus.ACTIVE);
        final var session = sessionOf(login(USERNAME, RAW_PASSWORD));

        final var result = mockMvc.perform(get("/transactions/GROCERIES").session(session));

        verifyOk(result);
    }

    @Test
    @DisplayName("Given a pending account When POST /api/auth/login is called with the correct password Then it responds 401")
    void givenPendingAccountWhenPostLoginIsCalledWithTheCorrectPasswordThenItResponds401() throws Exception {
        setupStoredAccount(UserStatus.PENDING);

        final var result = login(USERNAME, RAW_PASSWORD);

        verifyUnauthorized(result);
    }

    @Test
    @DisplayName("Given a rejected account When POST /api/auth/login is called with the correct password Then it responds 401")
    void givenRejectedAccountWhenPostLoginIsCalledWithTheCorrectPasswordThenItResponds401() throws Exception {
        setupStoredAccount(UserStatus.REJECTED);

        final var result = login(USERNAME, RAW_PASSWORD);

        verifyUnauthorized(result);
    }

    @Test
    @DisplayName("Given an unknown username and a wrong password When login is attempted Then both responses are identical")
    void givenUnknownUsernameAndWrongPasswordWhenLoginIsAttemptedThenBothResponsesAreIdentical() throws Exception {
        setupStoredAccount(UserStatus.ACTIVE);

        final var unknownUser = login("nao-existe", RAW_PASSWORD);
        final var wrongPassword = login(USERNAME, "senha-errada");

        verifyResponsesAreIndistinguishable(unknownUser, wrongPassword);
    }

    @Test
    @DisplayName("Given a pending account and a wrong password on an active account When login is attempted Then both responses are identical")
    void givenPendingAccountAndWrongPasswordOnActiveAccountWhenLoginIsAttemptedThenBothResponsesAreIdentical()
            throws Exception {
        setupStoredAccount(UserStatus.ACTIVE);
        repository.save(UserAccount.register("pendente", passwordEncoderGateway.encode(RAW_PASSWORD)));

        final var pending = login("pendente", RAW_PASSWORD);
        final var wrongPassword = login(USERNAME, "senha-errada");

        verifyResponsesAreIndistinguishable(pending, wrongPassword);
    }

    @Test
    @DisplayName("Given an authenticated session When POST /api/auth/logout is called Then it responds 204 and the session no longer works")
    void givenAuthenticatedSessionWhenPostLogoutIsCalledThenItResponds204AndTheSessionNoLongerWorks() throws Exception {
        setupStoredAccount(UserStatus.ACTIVE);
        final var session = sessionOf(login(USERNAME, RAW_PASSWORD));

        final var logout = mockMvc.perform(post("/api/auth/logout").session(session));

        verifyGivenAuthenticatedSessionWhenPostLogoutIsCalledThenItResponds204AndTheSessionNoLongerWorks(
                logout, session);
    }

    private ResultActions register(final RegisterUserRequest request) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));
    }

    private ResultActions login(final String username, final String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}""".formatted(username, password)));
    }

    private MockHttpSession sessionOf(final ResultActions result) {
        return (MockHttpSession) result.andReturn().getRequest().getSession(false);
    }

    private void setupStoredAccount(final UserStatus status) {
        final var account = UserAccount.register(USERNAME, passwordEncoderGateway.encode(RAW_PASSWORD));

        if (status == UserStatus.ACTIVE) {
            account.approve();
        } else if (status == UserStatus.REJECTED) {
            account.reject();
        }

        repository.save(account);
    }

    private void verifyGivenValidRegistrationWhenPostRegisterIsCalledThenItResponds201WithAPendingAccount(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.status").value(UserStatus.PENDING.name()))
                .andDo(document("register-user",
                        requestFields(
                                fieldWithPath("username").description("Nome de usuário, único e sem distinção de maiúsculas"),
                                fieldWithPath("password").description("Senha em texto puro, de 8 a 72 caracteres")
                        ),
                        responseFields(
                                fieldWithPath("id").description("Identificador único da conta (UUID)"),
                                fieldWithPath("username").description("Nome de usuário já normalizado em minúsculas"),
                                fieldWithPath("status").description("Estado da conta; um cadastro novo nasce sempre PENDING")
                        )));
    }

    private void verifyGivenValidRegistrationWhenPostRegisterIsCalledThenTheResponseBodyCarriesNoPassword(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.encodedPassword").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(RAW_PASSWORD))));
    }

    private void verifyGivenFreshRegistrationWhenAFinancialEndpointIsCalledThenItResponds401BecauseRegisteringDoesNotAuthenticate(
            final ResultActions registration, final ResultActions actual) throws Exception {
        registration.andExpect(status().isCreated());
        assertThat(sessionOf(registration)).isNull();

        verifyUnauthorized(actual);
    }

    private void verifyGivenRegistrationClaimingAdminRoleWhenPostRegisterIsCalledThenTheAccountIsCreatedAsACommonUser(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isCreated());
        assertThat(repository.findByUsername("ana").orElseThrow().getRole().name()).isEqualTo("ROLE_USER");
    }

    private void verifyGivenRegistrationClaimingActiveStatusWhenPostRegisterIsCalledThenTheAccountIsCreatedAsPending(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(UserStatus.PENDING.name()));
        assertThat(repository.findByUsername("joao").orElseThrow().getStatus()).isEqualTo(UserStatus.PENDING);
    }

    private void verifyConflictWithoutCreatingAccount(final ResultActions actual) throws Exception {
        actual.andExpect(status().isConflict());
        assertThat(entityRepository.count()).isEqualTo(1);
    }

    private void verifyBadRequestIndicating(final ResultActions actual, final String field) throws Exception {
        actual.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$." + field).isNotEmpty());
        assertThat(entityRepository.count()).isZero();
    }

    private void verifyGivenValidRegistrationWhenTheAccountIsPersistedThenTheStoredPasswordIsBCryptEncoded(
            final UserAccount actual) {
        assertThat(actual.getEncodedPassword()).isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, actual.getEncodedPassword())).isTrue();
    }

    private void verifyGivenActiveAccountWhenPostLoginIsCalledThenItResponds200AndEstablishesASession(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isOk())
                .andDo(document("login", requestFields(
                        fieldWithPath("username").description("Nome de usuário"),
                        fieldWithPath("password").description("Senha em texto puro")
                )));
        assertThat(sessionOf(actual)).isNotNull();
    }

    private void verifyGivenAuthenticatedSessionWhenPostLogoutIsCalledThenItResponds204AndTheSessionNoLongerWorks(
            final ResultActions actual, final MockHttpSession session) throws Exception {
        actual.andExpect(status().isNoContent())
                .andDo(document("logout"));
        assertThat(session.isInvalid()).isTrue();

        mockMvc.perform(get("/transactions/GROCERIES").session(session))
                .andExpect(status().isUnauthorized());
    }

    private void verifyResponsesAreIndistinguishable(final ResultActions first, final ResultActions second)
            throws Exception {
        first.andExpect(status().isUnauthorized());
        second.andExpect(status().isUnauthorized());

        assertThat(first.andReturn().getResponse().getContentAsString())
                .isEqualTo(second.andReturn().getResponse().getContentAsString());
    }

    private void verifyOk(final ResultActions actual) throws Exception {
        actual.andExpect(status().isOk());
    }

    private void verifyUnauthorized(final ResultActions actual) throws Exception {
        actual.andExpect(status().isUnauthorized());
    }
}
