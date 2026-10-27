package dio.budgeting.infrastructure.http;

import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.infrastructure.http.request.TransactionRequest;
import dio.budgeting.infrastructure.persistence.repository.TransactionEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.json.JsonMapper;

import static dio.budgeting.support.TestConstants.CATEGORY;
import static dio.budgeting.support.domain.TransactionSupport.defaultTransaction;
import static dio.budgeting.support.request.TransactionRequestSupport.defaultTransactionRequest;
import static org.hamcrest.Matchers.containsString;
import static org.instancio.Select.field;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
@AutoConfigureMockMvc
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class TransactionApiIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:9.6");

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;
    private final TransactionRepository repository;
    private final TransactionEntityRepository entityRepository;

    TransactionApiIntegrationTest(final MockMvc mockMvc,
                                  final JsonMapper jsonMapper,
                                  final TransactionRepository repository,
                                  final TransactionEntityRepository entityRepository) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
        this.repository = repository;
        this.entityRepository = entityRepository;
    }

    @BeforeEach
    void cleanDatabase() {
        entityRepository.deleteAll();
    }

    @Test
    @DisplayName("Given an amount of 8000 cents When POST /transactions is called Then it responds 201 with amount 80.00")
    void givenAmountOf8000CentsWhenPostTransactionsIsCalledThenItResponds201WithAmount8000() throws Exception {
        final var request = defaultTransactionRequest()
                .set(field(TransactionRequest::amount), 8000L)
                .create();

        final var result = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        verifyGivenAmountOf8000CentsWhenPostTransactionsIsCalledThenItResponds201WithAmount8000(result, request);
    }

    @Test
    @DisplayName("Given a stored transaction of 4000 cents When GET /transactions/{category} is called Then it shows amount 40.00")
    void givenStoredTransactionOf4000CentsWhenGetTransactionsByCategoryIsCalledThenItShowsAmount4000() throws Exception {
        final var stored = defaultTransaction()
                .set(field(Transaction::getAmount), 4000L)
                .create();
        setupGivenStoredTransactionOf4000CentsWhenGetTransactionsByCategoryIsCalledThenItShowsAmount4000(stored);

        final var result = mockMvc.perform(get("/transactions/{category}", CATEGORY));

        verifyGivenStoredTransactionOf4000CentsWhenGetTransactionsByCategoryIsCalledThenItShowsAmount4000(result, stored);
    }

    private void verifyGivenAmountOf8000CentsWhenPostTransactionsIsCalledThenItResponds201WithAmount8000(
            final ResultActions actual, final TransactionRequest request) throws Exception {
        actual.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.description").value(request.description()))
                .andExpect(jsonPath("$.category").value(request.category().name()))
                .andExpect(content().string(containsString("\"amount\":80.00")));
    }

    private void setupGivenStoredTransactionOf4000CentsWhenGetTransactionsByCategoryIsCalledThenItShowsAmount4000(
            final Transaction stored) {
        repository.save(stored);
    }

    private void verifyGivenStoredTransactionOf4000CentsWhenGetTransactionsByCategoryIsCalledThenItShowsAmount4000(
            final ResultActions actual, final Transaction expected) throws Exception {
        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(expected.getId().uuid().toString()))
                .andExpect(content().string(containsString("\"amount\":40.00")));
    }
}
