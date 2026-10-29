package dio.budgeting.auth.infrastructure.security;

import dio.budgeting.infrastructure.persistence.repository.TransactionEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.json.JsonMapper;

import static dio.budgeting.support.request.TransactionRequestSupport.defaultTransactionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-key",
        "app.admin.username=test-admin",
        "app.admin.password=test-admin-password"})
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class SecurityConfigApiIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:9.6")
            .withStartupTimeoutSeconds(300);

    private final WebApplicationContext webApplicationContext;
    private final JsonMapper jsonMapper;
    private final TransactionEntityRepository transactionEntityRepository;

    private MockMvc mockMvc;

    SecurityConfigApiIntegrationTest(final WebApplicationContext webApplicationContext,
                                     final JsonMapper jsonMapper,
                                     final TransactionEntityRepository transactionEntityRepository) {
        this.webApplicationContext = webApplicationContext;
        this.jsonMapper = jsonMapper;
        this.transactionEntityRepository = transactionEntityRepository;
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        transactionEntityRepository.deleteAll();
    }

    @Test
    @DisplayName("Given no session When POST /transactions is called Then it responds 401 and saves nothing")
    void givenNoSessionWhenPostTransactionsIsCalledThenItResponds401AndSavesNothing() throws Exception {
        final var request = defaultTransactionRequest().create();

        final var result = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        verifyGivenNoSessionWhenPostTransactionsIsCalledThenItResponds401AndSavesNothing(result);
    }

    @Test
    @DisplayName("Given no session When the category query is called Then it responds 401")
    void givenNoSessionWhenTheCategoryQueryIsCalledThenItResponds401() throws Exception {
        final var result = mockMvc.perform(get("/transactions/GROCERIES"));

        verifyUnauthorized(result);
    }

    @Test
    @DisplayName("Given no session When the summary query is called Then it responds 401")
    void givenNoSessionWhenTheSummaryQueryIsCalledThenItResponds401() throws Exception {
        final var result = mockMvc.perform(get("/transactions/summary").param("category", "GROCERIES"));

        verifyUnauthorized(result);
    }

    @Test
    @DisplayName("Given no session When the period query is called Then it responds 401")
    void givenNoSessionWhenThePeriodQueryIsCalledThenItResponds401() throws Exception {
        final var result = mockMvc.perform(get("/transactions")
                .param("from", "2026-10-01")
                .param("to", "2026-10-31"));

        verifyUnauthorized(result);
    }

    @Test
    @DisplayName("Given no session When POST /transactions/ai is called Then it responds 401 without reaching the AI provider")
    void givenNoSessionWhenPostTransactionsAiIsCalledThenItResponds401WithoutReachingTheAiProvider() throws Exception {
        final var audio = new MockMultipartFile("file", "command.mp3", "audio/mpeg", new byte[]{1, 2, 3, 4});

        final var result = mockMvc.perform(multipart("/transactions/ai").file(audio));

        verifyUnauthorized(result);
    }

    @Test
    @DisplayName("Given no session When POST /transactions/ai/text is called Then it responds 401 without reaching the AI provider")
    void givenNoSessionWhenPostTransactionsAiTextIsCalledThenItResponds401WithoutReachingTheAiProvider()
            throws Exception {
        final var result = mockMvc.perform(post("/transactions/ai/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Gastei 80 reais no mercado\"}"));

        verifyUnauthorized(result);
    }

    @Test
    @DisplayName("Given no session When the administrative listing is called Then it responds 401")
    void givenNoSessionWhenTheAdministrativeListingIsCalledThenItResponds401() throws Exception {
        final var result = mockMvc.perform(get("/api/admin/users").param("status", "PENDING"));

        verifyUnauthorized(result);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Given a session with the user role When POST /transactions is called Then it responds 201")
    void givenSessionWithUserRoleWhenPostTransactionsIsCalledThenItResponds201() throws Exception {
        final var request = defaultTransactionRequest().create();

        final var result = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        verifyGivenSessionWithUserRoleWhenPostTransactionsIsCalledThenItResponds201(result);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Given an administrator session When the category query is called Then it responds 403")
    void givenAdministratorSessionWhenTheCategoryQueryIsCalledThenItResponds403() throws Exception {
        final var result = mockMvc.perform(get("/transactions/GROCERIES"));

        verifyForbidden(result);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Given an administrator session When POST /transactions is called Then it responds 403 and saves nothing")
    void givenAdministratorSessionWhenPostTransactionsIsCalledThenItResponds403AndSavesNothing() throws Exception {
        final var request = defaultTransactionRequest().create();

        final var result = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        verifyGivenAdministratorSessionWhenPostTransactionsIsCalledThenItResponds403AndSavesNothing(result);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Given a session with the user role When the administrative listing is called Then it responds 403")
    void givenSessionWithUserRoleWhenTheAdministrativeListingIsCalledThenItResponds403() throws Exception {
        final var result = mockMvc.perform(get("/api/admin/users").param("status", "PENDING"));

        verifyForbidden(result);
    }

    private void verifyGivenNoSessionWhenPostTransactionsIsCalledThenItResponds401AndSavesNothing(
            final ResultActions actual) throws Exception {
        verifyUnauthorized(actual);
        assertNothingPersisted();
    }

    private void verifyGivenSessionWithUserRoleWhenPostTransactionsIsCalledThenItResponds201(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isCreated());
        assertThat(transactionEntityRepository.count()).isEqualTo(1);
    }

    private void verifyGivenAdministratorSessionWhenPostTransactionsIsCalledThenItResponds403AndSavesNothing(
            final ResultActions actual) throws Exception {
        verifyForbidden(actual);
        assertNothingPersisted();
    }

    private void verifyUnauthorized(final ResultActions actual) throws Exception {
        actual.andExpect(status().isUnauthorized());
    }

    private void verifyForbidden(final ResultActions actual) throws Exception {
        actual.andExpect(status().isForbidden());
    }

    private void assertNothingPersisted() {
        assertThat(transactionEntityRepository.count()).isZero();
    }
}
