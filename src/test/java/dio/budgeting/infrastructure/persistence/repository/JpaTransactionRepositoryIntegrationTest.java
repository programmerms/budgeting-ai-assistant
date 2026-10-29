package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.Instant;
import java.util.List;

import static dio.budgeting.support.domain.TransactionSupport.defaultTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-key",
        "app.admin.username=test-admin",
        "app.admin.password=test-admin-password"})
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class JpaTransactionRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:9.6")
            .withStartupTimeoutSeconds(300);

    private final TransactionRepository repository;
    private final TransactionEntityRepository entityRepository;
    private final JdbcTemplate jdbcTemplate;

    JpaTransactionRepositoryIntegrationTest(final TransactionRepository repository,
                                            final TransactionEntityRepository entityRepository,
                                            final JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.entityRepository = entityRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void cleanDatabase() {
        entityRepository.deleteAll();
    }

    @Test
    @DisplayName("Given transactions of two categories When findAllByCategory is called Then only the transactions of that category are returned intact with createdAt populated")
    void givenTransactionsOfTwoCategoriesWhenFindAllByCategoryIsCalledThenOnlyTheTransactionsOfThatCategoryAreReturnedIntactWithCreatedAtPopulated() {
        final var groceries = defaultTransaction()
                .set(field(Transaction::getCategory), Category.GROCERIES)
                .set(field(Transaction::getCreatedAt), null)
                .create();
        final var pharma = defaultTransaction()
                .set(field(Transaction::getCategory), Category.PHARMA)
                .set(field(Transaction::getCreatedAt), null)
                .create();
        setupGivenTransactionsOfTwoCategoriesWhenFindAllByCategoryIsCalledThenOnlyTheTransactionsOfThatCategoryAreReturnedIntactWithCreatedAtPopulated(
                groceries, pharma);

        final var result = repository.findAllByCategory(Category.GROCERIES);

        verifyGivenTransactionsOfTwoCategoriesWhenFindAllByCategoryIsCalledThenOnlyTheTransactionsOfThatCategoryAreReturnedIntactWithCreatedAtPopulated(
                result, groceries);
    }

    @Test
    @DisplayName("Given transactions inside and outside a period When findAllByCreatedAtBetween is called Then only the transactions inside the period are returned")
    void givenTransactionsInsideAndOutsidePeriodWhenFindAllByCreatedAtBetweenIsCalledThenOnlyTransactionsInsidePeriodAreReturned() {
        final var inside = defaultTransaction()
                .set(field(Transaction::getCreatedAt), null)
                .create();
        final var outside = defaultTransaction()
                .set(field(Transaction::getCreatedAt), null)
                .create();
        final var from = Instant.parse("2026-10-01T00:00:00Z");
        final var to = Instant.parse("2026-10-31T23:59:59Z");
        setupGivenTransactionsInsideAndOutsidePeriodWhenFindAllByCreatedAtBetweenIsCalledThenOnlyTransactionsInsidePeriodAreReturned(
                inside, outside);

        final var result = repository.findAllByCreatedAtBetween(from, to);

        verifyGivenTransactionsInsideAndOutsidePeriodWhenFindAllByCreatedAtBetweenIsCalledThenOnlyTransactionsInsidePeriodAreReturned(
                result, inside);
    }

    private void setupGivenTransactionsOfTwoCategoriesWhenFindAllByCategoryIsCalledThenOnlyTheTransactionsOfThatCategoryAreReturnedIntactWithCreatedAtPopulated(
            final Transaction groceries, final Transaction pharma) {
        repository.save(groceries);
        repository.save(pharma);
    }

    private void verifyGivenTransactionsOfTwoCategoriesWhenFindAllByCategoryIsCalledThenOnlyTheTransactionsOfThatCategoryAreReturnedIntactWithCreatedAtPopulated(
            final List<Transaction> actual, final Transaction expected) {
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt")
                .containsExactly(expected);
        assertThat(actual.getFirst().getCreatedAt()).isNotNull();
    }

    private void setupGivenTransactionsInsideAndOutsidePeriodWhenFindAllByCreatedAtBetweenIsCalledThenOnlyTransactionsInsidePeriodAreReturned(
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

    private void verifyGivenTransactionsInsideAndOutsidePeriodWhenFindAllByCreatedAtBetweenIsCalledThenOnlyTransactionsInsidePeriodAreReturned(
            final List<Transaction> actual, final Transaction expected) {
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt")
                .containsExactly(expected);
        assertThat(actual.getFirst().getCreatedAt())
                .isBetween(Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"));
    }
}
