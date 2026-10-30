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
import static dio.budgeting.support.TestConstants.OWNER_ID;
import static dio.budgeting.support.TestConstants.SECOND_OWNER_ID;
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
    @DisplayName("Given transactions of two owners When findAllByOwnerIdAndCategory is called Then only the owner transaction is returned")
    void givenTransactionsOfTwoOwnersWhenFindAllByOwnerIdAndCategoryIsCalledThenOnlyTheOwnerTransactionIsReturned() {
        final var groceries = defaultTransaction()
                .set(field(Transaction::getOwnerId), OWNER_ID)
                .set(field(Transaction::getCategory), Category.GROCERIES)
                .set(field(Transaction::getCreatedAt), null)
                .create();
        final var otherOwnerGroceries = defaultTransaction()
                .set(field(Transaction::getOwnerId), SECOND_OWNER_ID)
                .set(field(Transaction::getCategory), Category.GROCERIES)
                .set(field(Transaction::getCreatedAt), null)
                .create();
        setupGivenTransactionsOfTwoOwnersWhenFindAllByOwnerIdAndCategoryIsCalledThenOnlyTheOwnerTransactionIsReturned(
                groceries, otherOwnerGroceries);

        final var result = repository.findAllByOwnerIdAndCategory(OWNER_ID, Category.GROCERIES);

        verifyGivenTransactionsOfTwoOwnersWhenFindAllByOwnerIdAndCategoryIsCalledThenOnlyTheOwnerTransactionIsReturned(
                result, groceries);
    }

    @Test
    @DisplayName("Given transactions of two owners in a period When findAllByOwnerIdAndCreatedAtBetween is called Then only the owner transaction is returned")
    void givenTransactionsOfTwoOwnersInPeriodWhenFindAllByOwnerIdAndCreatedAtBetweenIsCalledThenOnlyTheOwnerTransactionIsReturned() {
        final var ownerTransaction = defaultTransaction()
                .set(field(Transaction::getOwnerId), OWNER_ID)
                .set(field(Transaction::getCreatedAt), null)
                .create();
        final var otherOwnerTransaction = defaultTransaction()
                .set(field(Transaction::getOwnerId), SECOND_OWNER_ID)
                .set(field(Transaction::getCreatedAt), null)
                .create();
        final var from = Instant.parse("2026-10-01T00:00:00Z");
        final var to = Instant.parse("2026-10-31T23:59:59Z");
        setupGivenTransactionsOfTwoOwnersInPeriodWhenFindAllByOwnerIdAndCreatedAtBetweenIsCalledThenOnlyTheOwnerTransactionIsReturned(
                ownerTransaction, otherOwnerTransaction);

        final var result = repository.findAllByOwnerIdAndCreatedAtBetween(OWNER_ID, from, to);

        verifyGivenTransactionsOfTwoOwnersInPeriodWhenFindAllByOwnerIdAndCreatedAtBetweenIsCalledThenOnlyTheOwnerTransactionIsReturned(
                result, ownerTransaction);
    }

    private void setupGivenTransactionsOfTwoOwnersWhenFindAllByOwnerIdAndCategoryIsCalledThenOnlyTheOwnerTransactionIsReturned(
            final Transaction groceries, final Transaction pharma) {
        repository.save(groceries);
        repository.save(pharma);
    }

    private void verifyGivenTransactionsOfTwoOwnersWhenFindAllByOwnerIdAndCategoryIsCalledThenOnlyTheOwnerTransactionIsReturned(
            final List<Transaction> actual, final Transaction expected) {
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt")
                .containsExactly(expected);
        assertThat(actual.getFirst().getCreatedAt()).isNotNull();
    }

    private void setupGivenTransactionsOfTwoOwnersInPeriodWhenFindAllByOwnerIdAndCreatedAtBetweenIsCalledThenOnlyTheOwnerTransactionIsReturned(
            final Transaction inside, final Transaction outside) {
        repository.save(inside);
        repository.save(outside);
        jdbcTemplate.update(
                "UPDATE transaction_entity SET created_at = ? WHERE id = UUID_TO_BIN(?)",
                Instant.parse("2026-10-15T12:00:00Z"),
                inside.getId().uuid().toString());
        jdbcTemplate.update(
                "UPDATE transaction_entity SET created_at = ? WHERE id = UUID_TO_BIN(?)",
                Instant.parse("2026-10-16T12:00:00Z"),
                outside.getId().uuid().toString());
    }

    private void verifyGivenTransactionsOfTwoOwnersInPeriodWhenFindAllByOwnerIdAndCreatedAtBetweenIsCalledThenOnlyTheOwnerTransactionIsReturned(
            final List<Transaction> actual, final Transaction expected) {
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt")
                .containsExactly(expected);
        assertThat(actual.getFirst().getCreatedAt())
                .isBetween(Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"));
    }
}
