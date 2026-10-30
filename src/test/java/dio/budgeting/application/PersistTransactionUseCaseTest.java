package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static dio.budgeting.support.TestConstants.AMOUNT_IN_REAIS;
import static dio.budgeting.support.TestConstants.OWNER_ID;
import static dio.budgeting.support.domain.TransactionSupport.defaultTransaction;
import static dio.budgeting.support.input.PersistTransactionInputSupport.defaultPersistTransactionInput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

        final var result = useCase.execute(OWNER_ID, input);

        verifyGivenValidInputWhenExecuteIsCalledThenTransactionIsSavedAndReturnedInReais(result, input, saved);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("Given an input with a blank description When execute is called Then it throws IllegalArgumentException and nothing is saved")
    void givenInputWithBlankDescriptionWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved(
            final String invalidDescription) {
        final var input = defaultPersistTransactionInput()
                .set(field(PersistTransactionInput::description), invalidDescription)
                .create();

        final var thrown = catchThrowable(() -> useCase.execute(OWNER_ID, input));

        verifyGivenInputWithBlankDescriptionWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved(thrown);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -500L})
    @DisplayName("Given an input with a non-positive amount When execute is called Then it throws IllegalArgumentException and nothing is saved")
    void givenInputWithNonPositiveAmountWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved(
            final long invalidAmount) {
        final var input = defaultPersistTransactionInput()
                .set(field(PersistTransactionInput::amount), invalidAmount)
                .create();

        final var thrown = catchThrowable(() -> useCase.execute(OWNER_ID, input));

        verifyGivenInputWithNonPositiveAmountWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved(thrown);
    }

    @Test
    @DisplayName("Given an input with a null category When execute is called Then it throws IllegalArgumentException and nothing is saved")
    void givenInputWithNullCategoryWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved() {
        final var input = defaultPersistTransactionInput()
                .set(field(PersistTransactionInput::category), null)
                .create();

        final var thrown = catchThrowable(() -> useCase.execute(OWNER_ID, input));

        verifyGivenInputWithNullCategoryWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved(thrown);
    }

    @Test
    @DisplayName("Given a null owner When execute is called Then it throws IllegalArgumentException and nothing is saved")
    void givenNullOwnerWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved() {
        final var input = defaultPersistTransactionInput().create();

        final var thrown = catchThrowable(() -> useCase.execute(null, input));

        verifyGivenNullOwnerWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved(thrown);
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
                .extracting(Transaction::getOwnerId, Transaction::getDescription,
                        Transaction::getAmount, Transaction::getCategory)
                .containsExactly(OWNER_ID, input.description(), input.amount(), input.category());

        assertThat(actual).isEqualTo(new TransactionOutput(
                expected.getId().uuid().toString(),
                expected.getDescription(),
                expected.getCategory().name(),
                AMOUNT_IN_REAIS,
                expected.getCreatedAt()));
    }

    private void verifyGivenNullOwnerWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Owner ID must not be null");
        assertNothingPersisted();
    }

    private void verifyGivenInputWithBlankDescriptionWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description must not be blank");
        assertNothingPersisted();
    }

    private void verifyGivenInputWithNonPositiveAmountWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than zero");
        assertNothingPersisted();
    }

    private void verifyGivenInputWithNullCategoryWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category must not be null");
        assertNothingPersisted();
    }

    private void assertNothingPersisted() {
        verifyNoInteractions(transactionRepository);
    }
}
