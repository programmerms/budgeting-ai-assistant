package dio.budgeting.infrastructure.http;

import dio.budgeting.domain.Category;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

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
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:9.6")
            .withStartupTimeoutSeconds(300);

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;
    private final TransactionRepository repository;
    private final TransactionEntityRepository entityRepository;
    private final JdbcTemplate jdbcTemplate;

    TransactionApiIntegrationTest(final MockMvc mockMvc,
                                  final JsonMapper jsonMapper,
                                  final TransactionRepository repository,
                                  final TransactionEntityRepository entityRepository,
                                  final JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
        this.repository = repository;
        this.entityRepository = entityRepository;
        this.jdbcTemplate = jdbcTemplate;
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

    @Test
    @DisplayName("Given a transaction with a new category When POST and GET /transactions/{category} are called Then it responds 201 and 200 with the category and createdAt")
    void givenTransactionWithNewCategoryWhenPostAndGetTransactionsByCategoryAreCalledThenItResponds201And200WithCategoryAndCreatedAt() throws Exception {
        final var request = defaultTransactionRequest()
                .set(field(TransactionRequest::category), Category.RESTAURANT)
                .create();

        final var postResult = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));
        final var getResult = mockMvc.perform(get("/transactions/{category}", Category.RESTAURANT));

        verifyGivenTransactionWithNewCategoryWhenPostAndGetTransactionsByCategoryAreCalledThenItResponds201And200WithCategoryAndCreatedAt(
                postResult, getResult, request);
    }

    @Test
    @DisplayName("Given stored transactions in a category When GET /transactions/summary is called Then it responds 200 with total and count")
    void givenStoredTransactionsInCategoryWhenGetTransactionsSummaryIsCalledThenItResponds200WithTotalAndCount() throws Exception {
        final var first = defaultTransaction()
                .set(field(Transaction::getCategory), Category.GROCERIES)
                .set(field(Transaction::getAmount), 8000L)
                .create();
        final var second = defaultTransaction()
                .set(field(Transaction::getCategory), Category.GROCERIES)
                .set(field(Transaction::getAmount), 4000L)
                .create();
        final var otherCategory = defaultTransaction()
                .set(field(Transaction::getCategory), Category.AUTO)
                .set(field(Transaction::getAmount), 5000L)
                .create();
        setupGivenStoredTransactionsInCategoryWhenGetTransactionsSummaryIsCalledThenItResponds200WithTotalAndCount(
                first, second, otherCategory);

        final var result = mockMvc.perform(get("/transactions/summary")
                .param("category", Category.GROCERIES.name()));

        verifyGivenStoredTransactionsInCategoryWhenGetTransactionsSummaryIsCalledThenItResponds200WithTotalAndCount(result);
    }

    @Test
    @DisplayName("Given no transactions in a category When GET /transactions/summary is called Then it responds 200 with total 0.00 and count 0")
    void givenNoTransactionsInCategoryWhenGetTransactionsSummaryIsCalledThenItResponds200WithTotalZeroAndCountZero() throws Exception {
        final var result = mockMvc.perform(get("/transactions/summary")
                .param("category", Category.PHARMA.name()));

        verifyGivenNoTransactionsInCategoryWhenGetTransactionsSummaryIsCalledThenItResponds200WithTotalZeroAndCountZero(result);
    }

    @Test
    @DisplayName("Given an invalid category When GET /transactions/summary is called Then it responds 400")
    void givenInvalidCategoryWhenGetTransactionsSummaryIsCalledThenItResponds400() throws Exception {
        final var result = mockMvc.perform(get("/transactions/summary")
                .param("category", "INVALID"));

        verifyGivenInvalidCategoryWhenGetTransactionsSummaryIsCalledThenItResponds400(result);
    }

    @Test
    @DisplayName("Given transactions inside and outside a period When GET /transactions is called with from and to Then it responds 200 with only the transactions inside")
    void givenTransactionsInsideAndOutsidePeriodWhenGetTransactionsByPeriodIsCalledThenItResponds200WithOnlyTransactionsInside() throws Exception {
        final var inside = defaultTransaction()
                .set(field(Transaction::getAmount), 8000L)
                .create();
        final var outside = defaultTransaction()
                .set(field(Transaction::getAmount), 4000L)
                .create();
        setupGivenTransactionsInsideAndOutsidePeriodWhenGetTransactionsByPeriodIsCalledThenItResponds200WithOnlyTransactionsInside(
                inside, outside);

        final var result = mockMvc.perform(get("/transactions")
                .param("from", "2026-10-01")
                .param("to", "2026-10-31"));

        verifyGivenTransactionsInsideAndOutsidePeriodWhenGetTransactionsByPeriodIsCalledThenItResponds200WithOnlyTransactionsInside(
                result, inside);
    }

    @Test
    @DisplayName("Given a start date after the end date When GET /transactions is called Then it responds 400 with an error message")
    void givenStartDateAfterEndDateWhenGetTransactionsByPeriodIsCalledThenItResponds400WithErrorMessage() throws Exception {
        final var result = mockMvc.perform(get("/transactions")
                .param("from", "2026-10-31")
                .param("to", "2026-10-01"));

        verifyGivenStartDateAfterEndDateWhenGetTransactionsByPeriodIsCalledThenItResponds400WithErrorMessage(result);
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
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
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
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
                .andExpect(content().string(containsString("\"amount\":40.00")));
    }

    private void verifyGivenTransactionWithNewCategoryWhenPostAndGetTransactionsByCategoryAreCalledThenItResponds201And200WithCategoryAndCreatedAt(
            final ResultActions postActual, final ResultActions getActual, final TransactionRequest request) throws Exception {
        postActual.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.category").value(Category.RESTAURANT.name()))
                .andExpect(jsonPath("$.description").value(request.description()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        getActual.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value(Category.RESTAURANT.name()))
                .andExpect(jsonPath("$[0].description").value(request.description()))
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty());
    }

    private void setupGivenStoredTransactionsInCategoryWhenGetTransactionsSummaryIsCalledThenItResponds200WithTotalAndCount(
            final Transaction first, final Transaction second, final Transaction otherCategory) {
        repository.save(first);
        repository.save(second);
        repository.save(otherCategory);
    }

    private void verifyGivenStoredTransactionsInCategoryWhenGetTransactionsSummaryIsCalledThenItResponds200WithTotalAndCount(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value(Category.GROCERIES.name()))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(content().string(containsString("\"total\":120.00")));
    }

    private void verifyGivenNoTransactionsInCategoryWhenGetTransactionsSummaryIsCalledThenItResponds200WithTotalZeroAndCountZero(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value(Category.PHARMA.name()))
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(content().string(containsString("\"total\":0.00")));
    }

    private void verifyGivenInvalidCategoryWhenGetTransactionsSummaryIsCalledThenItResponds400(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isBadRequest());
    }

    private void setupGivenTransactionsInsideAndOutsidePeriodWhenGetTransactionsByPeriodIsCalledThenItResponds200WithOnlyTransactionsInside(
            final Transaction inside, final Transaction outside) {
        repository.save(inside);
        repository.save(outside);
        jdbcTemplate.update(
                "UPDATE transaction_entity SET created_at = ? WHERE id = UUID_TO_BIN(?)",
                Instant.parse("2026-10-15T12:00:00Z"),
                inside.getId().uuid().toString());
        jdbcTemplate.update(
                "UPDATE transaction_entity SET created_at = ? WHERE id = UUID_TO_BIN(?)",
                Instant.parse("2026-11-05T12:00:00Z"),
                outside.getId().uuid().toString());
    }

    private void verifyGivenTransactionsInsideAndOutsidePeriodWhenGetTransactionsByPeriodIsCalledThenItResponds200WithOnlyTransactionsInside(
            final ResultActions actual, final Transaction expected) throws Exception {
        actual.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(expected.getId().uuid().toString()))
                .andExpect(jsonPath("$[0].createdAt").value(containsString("2026-10-15T")))
                .andExpect(content().string(containsString("\"amount\":80.00")));
    }

    private void verifyGivenStartDateAfterEndDateWhenGetTransactionsByPeriodIsCalledThenItResponds400WithErrorMessage(
            final ResultActions actual) throws Exception {
        actual.andExpect(status().isBadRequest())
                .andExpect(content().string("Start date must not be after end date"));
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
