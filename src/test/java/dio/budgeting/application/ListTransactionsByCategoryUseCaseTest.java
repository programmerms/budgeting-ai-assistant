package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static dio.budgeting.support.TestConstants.AMOUNT_IN_REAIS;
import static dio.budgeting.support.TestConstants.CATEGORY;
import static dio.budgeting.support.domain.TransactionSupport.defaultTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTransactionsByCategoryUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ListTransactionsByCategoryUseCase useCase;

    @Test
    @DisplayName("Given stored transactions When execute is called with their category Then they are returned in reais")
    void givenStoredTransactionsWhenExecuteIsCalledWithTheirCategoryThenTheyAreReturnedInReais() {
        final var stored = defaultTransaction().create();
        setupGivenStoredTransactionsWhenExecuteIsCalledWithTheirCategoryThenTheyAreReturnedInReais(stored);

        final var result = useCase.execute(CATEGORY);

        verifyGivenStoredTransactionsWhenExecuteIsCalledWithTheirCategoryThenTheyAreReturnedInReais(result, stored);
    }

    private void setupGivenStoredTransactionsWhenExecuteIsCalledWithTheirCategoryThenTheyAreReturnedInReais(
            final Transaction stored) {
        when(transactionRepository.findAllByCategory(CATEGORY)).thenReturn(List.of(stored));
    }

    private void verifyGivenStoredTransactionsWhenExecuteIsCalledWithTheirCategoryThenTheyAreReturnedInReais(
            final List<TransactionOutput> actual, final Transaction expected) {
        verify(transactionRepository, times(1)).findAllByCategory(CATEGORY);
        assertThat(actual).containsExactly(new TransactionOutput(
                expected.getId().uuid().toString(),
                expected.getDescription(),
                expected.getCategory().name(),
                AMOUNT_IN_REAIS,
                expected.getCreatedAt()));
    }
}
