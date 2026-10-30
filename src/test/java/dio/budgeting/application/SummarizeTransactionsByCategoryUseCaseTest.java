package dio.budgeting.application;

import dio.budgeting.application.output.CategorySummaryOutput;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static dio.budgeting.support.TestConstants.CATEGORY;
import static dio.budgeting.support.TestConstants.OWNER_ID;
import static dio.budgeting.support.domain.TransactionSupport.defaultTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.instancio.Select.field;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummarizeTransactionsByCategoryUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private SummarizeTransactionsByCategoryUseCase useCase;

    @Test
    @DisplayName("Given stored transactions in a category When execute is called Then it returns the sum in reais and count")
    void givenStoredTransactionsInCategoryWhenExecuteIsCalledThenItReturnsSumInReaisAndCount() {
        final var first = defaultTransaction()
                .set(field(Transaction::getAmount), 8000L)
                .create();
        final var second = defaultTransaction()
                .set(field(Transaction::getAmount), 4000L)
                .create();
        setupGivenStoredTransactionsInCategoryWhenExecuteIsCalledThenItReturnsSumInReaisAndCount(first, second);

        final var result = useCase.execute(OWNER_ID, CATEGORY);

        verifyGivenStoredTransactionsInCategoryWhenExecuteIsCalledThenItReturnsSumInReaisAndCount(result);
    }

    @Test
    @DisplayName("Given no transactions in a category When execute is called Then it returns total 0.00 and count 0")
    void givenNoTransactionsInCategoryWhenExecuteIsCalledThenItReturnsTotalZeroAndCountZero() {
        setupGivenNoTransactionsInCategoryWhenExecuteIsCalledThenItReturnsTotalZeroAndCountZero();

        final var result = useCase.execute(OWNER_ID, CATEGORY);

        verifyGivenNoTransactionsInCategoryWhenExecuteIsCalledThenItReturnsTotalZeroAndCountZero(result);
    }

    @Test
    @DisplayName("Given a null category When execute is called Then it throws IllegalArgumentException")
    void givenNullCategoryWhenExecuteIsCalledThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(() -> useCase.execute(OWNER_ID, null));

        verifyGivenNullCategoryWhenExecuteIsCalledThenItThrowsIllegalArgumentException(thrown);
    }

    private void setupGivenStoredTransactionsInCategoryWhenExecuteIsCalledThenItReturnsSumInReaisAndCount(
            final Transaction first, final Transaction second) {
        when(transactionRepository.findAllByOwnerIdAndCategory(OWNER_ID, CATEGORY))
                .thenReturn(List.of(first, second));
    }

    private void verifyGivenStoredTransactionsInCategoryWhenExecuteIsCalledThenItReturnsSumInReaisAndCount(
            final CategorySummaryOutput actual) {
        verify(transactionRepository, times(1)).findAllByOwnerIdAndCategory(OWNER_ID, CATEGORY);
        assertThat(actual).isEqualTo(new CategorySummaryOutput(
                CATEGORY.name(),
                new BigDecimal("120.00"),
                2L));
    }

    private void setupGivenNoTransactionsInCategoryWhenExecuteIsCalledThenItReturnsTotalZeroAndCountZero() {
        when(transactionRepository.findAllByOwnerIdAndCategory(OWNER_ID, CATEGORY)).thenReturn(List.of());
    }

    private void verifyGivenNoTransactionsInCategoryWhenExecuteIsCalledThenItReturnsTotalZeroAndCountZero(
            final CategorySummaryOutput actual) {
        verify(transactionRepository, times(1)).findAllByOwnerIdAndCategory(OWNER_ID, CATEGORY);
        assertThat(actual).isEqualTo(new CategorySummaryOutput(
                CATEGORY.name(),
                new BigDecimal("0.00"),
                0L));
    }

    private void verifyGivenNullCategoryWhenExecuteIsCalledThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category must not be null");
        verifyNoInteractions(transactionRepository);
    }
}
