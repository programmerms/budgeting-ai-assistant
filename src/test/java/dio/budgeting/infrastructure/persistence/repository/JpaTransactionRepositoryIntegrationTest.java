package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.List;

import static dio.budgeting.support.domain.TransactionSupport.defaultTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class JpaTransactionRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:9.6");

    private final TransactionRepository repository;

    JpaTransactionRepositoryIntegrationTest(final TransactionRepository repository) {
        this.repository = repository;
    }

    @Test
    @DisplayName("Given transactions of two categories When findAllByCategory is called Then only the transactions of that category are returned intact")
    void givenTransactionsOfTwoCategoriesWhenFindAllByCategoryIsCalledThenOnlyTheTransactionsOfThatCategoryAreReturnedIntact() {
        final var groceries = defaultTransaction()
                .set(field(Transaction::getCategory), Category.GROCERIES)
                .create();
        final var pharma = defaultTransaction()
                .set(field(Transaction::getCategory), Category.PHARMA)
                .create();
        setupGivenTransactionsOfTwoCategoriesWhenFindAllByCategoryIsCalledThenOnlyTheTransactionsOfThatCategoryAreReturnedIntact(
                groceries, pharma);

        final var result = repository.findAllByCategory(Category.GROCERIES);

        verifyGivenTransactionsOfTwoCategoriesWhenFindAllByCategoryIsCalledThenOnlyTheTransactionsOfThatCategoryAreReturnedIntact(
                result, groceries);
    }

    private void setupGivenTransactionsOfTwoCategoriesWhenFindAllByCategoryIsCalledThenOnlyTheTransactionsOfThatCategoryAreReturnedIntact(
            final Transaction groceries, final Transaction pharma) {
        repository.save(groceries);
        repository.save(pharma);
    }

    private void verifyGivenTransactionsOfTwoCategoriesWhenFindAllByCategoryIsCalledThenOnlyTheTransactionsOfThatCategoryAreReturnedIntact(
            final List<Transaction> actual, final Transaction expected) {
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expected);
    }
}
