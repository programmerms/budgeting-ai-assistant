package dio.budgeting.auth.infrastructure.http;

import dio.budgeting.auth.application.PasswordEncoderGateway;
import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountRepository;
import dio.budgeting.auth.domain.UserStatus;
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
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.UUID;

import static dio.budgeting.support.TestConstants.ADMIN_PASSWORD;
import static dio.budgeting.support.TestConstants.ADMIN_USERNAME;
import static dio.budgeting.support.TestConstants.RAW_PASSWORD;
import static dio.budgeting.support.TestConstants.USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-key",
        "app.admin.username=test-admin",
        "app.admin.password=test-admin-password"})
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AdminUserApiIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:9.6")
            .withStartupTimeoutSeconds(300);

    private final WebApplicationContext webApplicationContext;
    private final UserAccountRepository repository;
    private final UserAccountEntityRepository entityRepository;
    private final PasswordEncoderGateway passwordEncoderGateway;

    private MockMvc mockMvc;

    AdminUserApiIntegrationTest(final WebApplicationContext webApplicationContext,
                                final UserAccountRepository repository,
                                final UserAccountEntityRepository entityRepository,
                                final PasswordEncoderGateway passwordEncoderGateway) {
        this.webApplicationContext = webApplicationContext;
        this.repository = repository;
        this.entityRepository = entityRepository;
        this.passwordEncoderGateway = passwordEncoderGateway;
    }

    @BeforeEach
    void setUp(final RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation))
                .build();
        entityRepository.deleteAll();
        repository.save(UserAccount.administrator(ADMIN_USERNAME, passwordEncoderGateway.encode(ADMIN_PASSWORD)));
    }

    @Test
    @DisplayName("Given a pending and an active account When the administrator lists pending accounts Then only the pending one is returned")
    void givenPendingAndActiveAccountWhenTheAdministratorListsPendingAccountsThenOnlyThePendingOneIsReturned()
            throws Exception {
        final var pending = setupStoredAccount(USERNAME, UserStatus.PENDING);
        setupStoredAccount("ativo", UserStatus.ACTIVE);

        final var result = asAdministrator(get("/api/admin/users").param("status", "PENDING"));

        verifyGivenPendingAndActiveAccountWhenTheAdministratorListsPendingAccountsThenOnlyThePendingOneIsReturned(
                result, pending.getId().uuid());
    }

    @Test
    @DisplayName("Given a pending and a rejected account When the administrator lists rejected accounts Then only the rejected one is returned")
    void givenPendingAndRejectedAccountWhenTheAdministratorListsRejectedAccountsThenOnlyTheRejectedOneIsReturned()
            throws Exception {
        setupStoredAccount(USERNAME, UserStatus.PENDING);
        setupStoredAccount("recusado", UserStatus.REJECTED);

        final var result = asAdministrator(get("/api/admin/users").param("status", "REJECTED"));

        verifyGivenPendingAndRejectedAccountWhenTheAdministratorListsRejectedAccountsThenOnlyTheRejectedOneIsReturned(
                result);
    }

    @Test
    @DisplayName("Given no status parameter When the administrator lists accounts Then it responds 400")
    void givenNoStatusParameterWhenTheAdministratorListsAccountsThenItResponds400() throws Exception {
        final var result = asAdministrator(get("/api/admin/users"));

        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Given a pending account When the administrator approves it Then it responds 200 and the account becomes active")
    void givenPendingAccountWhenTheAdministratorApprovesItThenItResponds200AndTheAccountBecomesActive()
            throws Exception {
        final var pending = setupStoredAccount(USERNAME, UserStatus.PENDING);

        final var result = asAdministrator(post("/api/admin/users/{id}/approve", pending.getId().uuid()));

        verifyGivenPendingAccountWhenTheAdministratorApprovesItThenItResponds200AndTheAccountBecomesActive(result);
    }

    @Test
    @DisplayName("Given an approved account When the owner logs in Then it responds 200")
    void givenApprovedAccountWhenTheOwnerLogsInThenItResponds200() throws Exception {
        final var pending = setupStoredAccount(USERNAME, UserStatus.PENDING);
        asAdministrator(post("/api/admin/users/{id}/approve", pending.getId().uuid()))
                .andExpect(status().isOk());

        final var result = login(USERNAME, RAW_PASSWORD);

        result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given a pending account When the administrator rejects it Then it responds 200 and the account becomes rejected")
    void givenPendingAccountWhenTheAdministratorRejectsItThenItResponds200AndTheAccountBecomesRejected()
            throws Exception {
        final var pending = setupStoredAccount(USERNAME, UserStatus.PENDING);

        final var result = asAdministrator(post("/api/admin/users/{id}/reject", pending.getId().uuid()));

        verifyGivenPendingAccountWhenTheAdministratorRejectsItThenItResponds200AndTheAccountBecomesRejected(result);
    }

    @Test
    @DisplayName("Given a rejected account When the owner logs in Then it responds 401")
    void givenRejectedAccountWhenTheOwnerLogsInThenItResponds401() throws Exception {
        final var pending = setupStoredAccount(USERNAME, UserStatus.PENDING);
        asAdministrator(post("/api/admin/users/{id}/reject", pending.getId().uuid()))
                .andExpect(status().isOk());

        final var result = login(USERNAME, RAW_PASSWORD);

        result.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Given an active account When the administrator approves it again Then it responds 409 and the state is unchanged")
    void givenActiveAccountWhenTheAdministratorApprovesItAgainThenItResponds409AndTheStateIsUnchanged()
            throws Exception {
        final var active = setupStoredAccount(USERNAME, UserStatus.ACTIVE);

        final var result = asAdministrator(post("/api/admin/users/{id}/approve", active.getId().uuid()));

        verifyConflictWithUnchangedStatus(result, UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Given an active account When the administrator rejects it Then it responds 409 and the state is unchanged")
    void givenActiveAccountWhenTheAdministratorRejectsItThenItResponds409AndTheStateIsUnchanged() throws Exception {
        final var active = setupStoredAccount(USERNAME, UserStatus.ACTIVE);

        final var result = asAdministrator(post("/api/admin/users/{id}/reject", active.getId().uuid()));

        verifyConflictWithUnchangedStatus(result, UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Given a rejected account When the administrator approves it Then it responds 409 and the state is unchanged")
    void givenRejectedAccountWhenTheAdministratorApprovesItThenItResponds409AndTheStateIsUnchanged() throws Exception {
        final var rejected = setupStoredAccount(USERNAME, UserStatus.REJECTED);

        final var result = asAdministrator(post("/api/admin/users/{id}/approve", rejected.getId().uuid()));

        verifyConflictWithUnchangedStatus(result, UserStatus.REJECTED);
    }

    @Test
    @DisplayName("Given an unknown identifier When the administrator approves it Then it responds 404")
    void givenUnknownIdentifierWhenTheAdministratorApprovesItThenItResponds404() throws Exception {
        final var result = asAdministrator(post("/api/admin/users/{id}/approve", UUID.randomUUID()));

        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Given an unknown identifier When the administrator rejects it Then it responds 404")
    void givenUnknownIdentifierWhenTheAdministratorRejectsItThenItResponds404() throws Exception {
        final var result = asAdministrator(post("/api/admin/users/{id}/reject", UUID.randomUUID()));

        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Given a common user session When the administration endpoints are called Then it responds 403")
    void givenCommonUserSessionWhenTheAdministrationEndpointsAreCalledThenItResponds403() throws Exception {
        final var commonUser = setupStoredAccount("comum", UserStatus.ACTIVE);
        final var session = sessionOf(login("comum", RAW_PASSWORD));

        final var list = mockMvc.perform(get("/api/admin/users").param("status", "PENDING").session(session));
        final var approve = mockMvc.perform(
                post("/api/admin/users/{id}/approve", commonUser.getId().uuid()).session(session));

        verifyGivenCommonUserSessionWhenTheAdministrationEndpointsAreCalledThenItResponds403(list, approve);
    }

    @Test
    @DisplayName("Given no session When the administration endpoints are called Then it responds 401")
    void givenNoSessionWhenTheAdministrationEndpointsAreCalledThenItResponds401() throws Exception {
        final var list = mockMvc.perform(get("/api/admin/users").param("status", "PENDING"));
        final var approve = mockMvc.perform(post("/api/admin/users/{id}/approve", UUID.randomUUID()));

        verifyGivenNoSessionWhenTheAdministrationEndpointsAreCalledThenItResponds401(list, approve);
    }

    private UserAccount setupStoredAccount(final String username, final UserStatus status) {
        final var account = UserAccount.register(username, passwordEncoderGateway.encode(RAW_PASSWORD));

        if (status == UserStatus.ACTIVE) {
            account.approve();
        } else if (status == UserStatus.REJECTED) {
            account.reject();
        }

        return repository.save(account);
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

    private ResultActions asAdministrator(final MockHttpServletRequestBuilder builder) throws Exception {
        final var session = sessionOf(login(ADMIN_USERNAME, ADMIN_PASSWORD));
        return mockMvc.perform(builder.session(session));
    }

    private void verifyGivenPendingAndActiveAccountWhenTheAdministratorListsPendingAccountsThenOnlyThePendingOneIsReturned(
            final ResultActions actual, final UUID expectedId) throws Exception {
        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(expectedId.toString()))
                .andExpect(jsonPath("$[0].username").value(USERNAME))
                .andExpect(jsonPath("$[0].status").value(UserStatus.PENDING.name()))
                .andDo(document("list-pending-users",
                        queryParameters(
                                parameterWithName("status").description(
                                        "Estado dos cadastros a listar: PENDING, ACTIVE ou REJECTED")
                        ),
                        responseFields(
                                fieldWithPath("[].id").description("Identificador único da conta (UUID)"),
                                fieldWithPath("[].username").description("Nome de usuário"),
                                fieldWithPath("[].status").description("Estado atual da conta")
                        )));
    }

    private void verifyGivenPendingAndRejectedAccountWhenTheAdministratorListsRejectedAccountsThenOnlyTheRejectedOneIsReturned(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("recusado"))
                .andExpect(jsonPath("$[0].status").value(UserStatus.REJECTED.name()));
    }

    private void verifyGivenPendingAccountWhenTheAdministratorApprovesItThenItResponds200AndTheAccountBecomesActive(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.status").value(UserStatus.ACTIVE.name()))
                .andDo(document("approve-user",
                        pathParameters(
                                parameterWithName("id").description("Identificador da conta a aprovar")
                        ),
                        responseFields(
                                fieldWithPath("id").description("Identificador único da conta (UUID)"),
                                fieldWithPath("username").description("Nome de usuário"),
                                fieldWithPath("status").description("Estado da conta após a decisão")
                        )));

        assertThat(repository.findByUsername(USERNAME).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    private void verifyGivenPendingAccountWhenTheAdministratorRejectsItThenItResponds200AndTheAccountBecomesRejected(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.status").value(UserStatus.REJECTED.name()))
                .andDo(document("reject-user",
                        pathParameters(
                                parameterWithName("id").description("Identificador da conta a recusar")
                        ),
                        responseFields(
                                fieldWithPath("id").description("Identificador único da conta (UUID)"),
                                fieldWithPath("username").description("Nome de usuário"),
                                fieldWithPath("status").description("Estado da conta após a decisão")
                        )));

        assertThat(repository.findByUsername(USERNAME).orElseThrow().getStatus()).isEqualTo(UserStatus.REJECTED);
    }

    private void verifyConflictWithUnchangedStatus(final ResultActions actual, final UserStatus expected)
            throws Exception {
        actual.andExpect(status().isConflict());
        assertThat(repository.findByUsername(USERNAME).orElseThrow().getStatus()).isEqualTo(expected);
    }

    private void verifyGivenCommonUserSessionWhenTheAdministrationEndpointsAreCalledThenItResponds403(
            final ResultActions list, final ResultActions approve) throws Exception {
        list.andExpect(status().isForbidden());
        approve.andExpect(status().isForbidden());
    }

    private void verifyGivenNoSessionWhenTheAdministrationEndpointsAreCalledThenItResponds401(
            final ResultActions list, final ResultActions approve) throws Exception {
        list.andExpect(status().isUnauthorized());
        approve.andExpect(status().isUnauthorized());
    }
}
