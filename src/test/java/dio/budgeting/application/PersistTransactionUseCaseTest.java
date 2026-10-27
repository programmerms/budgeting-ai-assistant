package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static dio.budgeting.support.TestConstants.AMOUNT_IN_REAIS;
import static dio.budgeting.support.domain.TransactionSupport.defaultTransaction;
import static dio.budgeting.support.input.PersistTransactionInputSupport.defaultPersistTransactionInput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistTransactionUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PersistTransactionUseCase useCase;

    @Test
    @DisplayName("Given a valid input When execute is called Then the transaction is saved and returned in reais")
    void givenValidInputWhenExecuteIsCalledThenTransactionIsSavedAndReturnedInReais() {
        final var input = defaultPersistTransactionInput().create();
        final var saved = defaultTransaction().create();
        setupGivenValidInputWhenExecuteIsCalledThenTransactionIsSavedAndReturnedInReais(saved);

        final var result = useCase.execute(input);

        verifyGivenValidInputWhenExecuteIsCalledThenTransactionIsSavedAndReturnedInReais(result, input, saved);
    }

    private void setupGivenValidInputWhenExecuteIsCalledThenTransactionIsSavedAndReturnedInReais(
            final Transaction saved) {
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);
    }

    private void verifyGivenValidInputWhenExecuteIsCalledThenTransactionIsSavedAndReturnedInReais(
            final TransactionOutput actual, final PersistTransactionInput input, final Transaction expected) {
        final var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue())
                .extracting(Transaction::getDescription, Transaction::getAmount, Transaction::getCategory)
                .containsExactly(input.description(), input.amount(), input.category());

        assertThat(actual).isEqualTo(new TransactionOutput(
                expected.getId().uuid().toString(),
                expected.getDescription(),
                expected.getCategory().name(),
                AMOUNT_IN_REAIS));
    }
}
