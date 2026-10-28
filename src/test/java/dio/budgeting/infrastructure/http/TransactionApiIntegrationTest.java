package dio.budgeting.infrastructure.http;

import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.infrastructure.http.request.TransactionRequest;
import dio.budgeting.infrastructure.persistence.repository.TransactionEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.instancio.Select.field;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("Given a blank description When POST /transactions is called Then it responds 400 indicating description and saves nothing")
    void givenBlankDescriptionWhenPostTransactionsIsCalledThenItResponds400IndicatingDescriptionAndSavesNothing(
            final String invalidDescription) throws Exception {
        final var request = defaultTransactionRequest()
                .set(field(TransactionRequest::description), invalidDescription)
                .create();

        final var result = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        verifyGivenBlankDescriptionWhenPostTransactionsIsCalledThenItResponds400IndicatingDescriptionAndSavesNothing(result);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -500L})
    @DisplayName("Given a non-positive amount When POST /transactions is called Then it responds 400 indicating amount and saves nothing")
    void givenNonPositiveAmountWhenPostTransactionsIsCalledThenItResponds400IndicatingAmountAndSavesNothing(
            final long invalidAmount) throws Exception {
        final var request = defaultTransactionRequest()
                .set(field(TransactionRequest::amount), invalidAmount)
                .create();

        final var result = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        verifyGivenNonPositiveAmountWhenPostTransactionsIsCalledThenItResponds400IndicatingAmountAndSavesNothing(result);
    }

    @Test
    @DisplayName("Given a null category When POST /transactions is called Then it responds 400 indicating category and saves nothing")
    void givenNullCategoryWhenPostTransactionsIsCalledThenItResponds400IndicatingCategoryAndSavesNothing() throws Exception {
        final var request = defaultTransactionRequest()
                .set(field(TransactionRequest::category), null)
                .create();

        final var result = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        verifyGivenNullCategoryWhenPostTransactionsIsCalledThenItResponds400IndicatingCategoryAndSavesNothing(result);
    }

    @Test
    @DisplayName("Given multiple invalid fields When POST /transactions is called Then it responds 400 with all invalid fields and saves nothing")
    void givenMultipleInvalidFieldsWhenPostTransactionsIsCalledThenItResponds400WithAllInvalidFieldsAndSavesNothing() throws Exception {
        final var request = new TransactionRequest("", null, 0L);

        final var result = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        verifyGivenMultipleInvalidFieldsWhenPostTransactionsIsCalledThenItResponds400WithAllInvalidFieldsAndSavesNothing(result);
    }

    @Test
    @DisplayName("Given an empty audio file When POST /transactions/ai is called Then it responds 400 with an error message")
    void givenEmptyAudioFileWhenPostTransactionsAiIsCalledThenItResponds400WithErrorMessage() throws Exception {
        final var emptyFile = new MockMultipartFile("file", "empty.m4a", "audio/m4a", new byte[0]);

        final var result = mockMvc.perform(multipart("/transactions/ai").file(emptyFile));

        verifyGivenEmptyAudioFileWhenPostTransactionsAiIsCalledThenItResponds400WithErrorMessage(result);
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

    private void verifyGivenBlankDescriptionWhenPostTransactionsIsCalledThenItResponds400IndicatingDescriptionAndSavesNothing(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.description").isNotEmpty());
        assertNothingPersisted();
    }

    private void verifyGivenNonPositiveAmountWhenPostTransactionsIsCalledThenItResponds400IndicatingAmountAndSavesNothing(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount").isNotEmpty());
        assertNothingPersisted();
    }

    private void verifyGivenNullCategoryWhenPostTransactionsIsCalledThenItResponds400IndicatingCategoryAndSavesNothing(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.category").isNotEmpty());
        assertNothingPersisted();
    }

    private void verifyGivenMultipleInvalidFieldsWhenPostTransactionsIsCalledThenItResponds400WithAllInvalidFieldsAndSavesNothing(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.amount").isNotEmpty())
                .andExpect(jsonPath("$.category").isNotEmpty());
        assertNothingPersisted();
    }

    private void verifyGivenEmptyAudioFileWhenPostTransactionsAiIsCalledThenItResponds400WithErrorMessage(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isBadRequest())
                .andExpect(content().string("Audio file must not be empty"));
    }

    private void assertNothingPersisted() {
        assertThat(entityRepository.count()).isZero();
    }
}
