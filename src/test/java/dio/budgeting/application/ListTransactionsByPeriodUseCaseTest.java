package dio.budgeting.application;

import dio.budgeting.application.input.TransactionsPeriodInput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static dio.budgeting.support.TestConstants.AMOUNT_IN_REAIS;
import static dio.budgeting.support.domain.TransactionSupport.defaultTransaction;
import static dio.budgeting.support.input.TransactionsPeriodInputSupport.defaultTransactionsPeriodInput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.instancio.Select.field;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTransactionsByPeriodUseCaseTest {

    private static final Instant EXPECTED_FROM_INSTANT = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant EXPECTED_TO_INSTANT = Instant.parse("2026-10-31T23:59:59.999999999Z");

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ListTransactionsByPeriodUseCase useCase;

    @Test
    @DisplayName("Given a valid period When execute is called Then it queries the full UTC day range and returns transactions in reais")
    void givenValidPeriodWhenExecuteIsCalledThenItQueriesFullUtcDayRangeAndReturnsTransactionsInReais() {
        final var input = defaultTransactionsPeriodInput().create();
        final var stored = defaultTransaction().create();
        setupGivenValidPeriodWhenExecuteIsCalledThenItQueriesFullUtcDayRangeAndReturnsTransactionsInReais(stored);

        final var result = useCase.execute(input);

        verifyGivenValidPeriodWhenExecuteIsCalledThenItQueriesFullUtcDayRangeAndReturnsTransactionsInReais(result, stored);
    }

    @Test
    @DisplayName("Given a null input When execute is called Then it throws IllegalArgumentException")
    void givenNullInputWhenExecuteIsCalledThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(() -> useCase.execute(null));

        verifyGivenNullInputWhenExecuteIsCalledThenItThrowsIllegalArgumentException(thrown);
    }

    @Test
    @DisplayName("Given a null start date When execute is called Then it throws IllegalArgumentException")
    void givenNullStartDateWhenExecuteIsCalledThenItThrowsIllegalArgumentException() {
        final var input = defaultTransactionsPeriodInput()
                .set(field(TransactionsPeriodInput::from), null)
                .create();

        final var thrown = catchThrowable(() -> useCase.execute(input));

        verifyGivenNullStartDateWhenExecuteIsCalledThenItThrowsIllegalArgumentException(thrown);
    }

    @Test
    @DisplayName("Given a null end date When execute is called Then it throws IllegalArgumentException")
    void givenNullEndDateWhenExecuteIsCalledThenItThrowsIllegalArgumentException() {
        final var input = defaultTransactionsPeriodInput()
                .set(field(TransactionsPeriodInput::to), null)
                .create();

        final var thrown = catchThrowable(() -> useCase.execute(input));

        verifyGivenNullEndDateWhenExecuteIsCalledThenItThrowsIllegalArgumentException(thrown);
    }

    @Test
    @DisplayName("Given a start date after the end date When execute is called Then it throws IllegalArgumentException")
    void givenStartDateAfterEndDateWhenExecuteIsCalledThenItThrowsIllegalArgumentException() {
        final var input = new TransactionsPeriodInput(
                LocalDate.of(2026, 10, 31),
                LocalDate.of(2026, 10, 1));

        final var thrown = catchThrowable(() -> useCase.execute(input));

        verifyGivenStartDateAfterEndDateWhenExecuteIsCalledThenItThrowsIllegalArgumentException(thrown);
    }

    private void setupGivenValidPeriodWhenExecuteIsCalledThenItQueriesFullUtcDayRangeAndReturnsTransactionsInReais(
            final Transaction stored) {
        when(transactionRepository.findAllByCreatedAtBetween(EXPECTED_FROM_INSTANT, EXPECTED_TO_INSTANT))
                .thenReturn(List.of(stored));
    }

    private void verifyGivenValidPeriodWhenExecuteIsCalledThenItQueriesFullUtcDayRangeAndReturnsTransactionsInReais(
            final List<TransactionOutput> actual, final Transaction expected) {
        verify(transactionRepository, times(1))
                .findAllByCreatedAtBetween(EXPECTED_FROM_INSTANT, EXPECTED_TO_INSTANT);
        assertThat(actual).containsExactly(new TransactionOutput(
                expected.getId().uuid().toString(),
                expected.getDescription(),
                expected.getCategory().name(),
                AMOUNT_IN_REAIS,
                expected.getCreatedAt()));
    }

    private void verifyGivenNullInputWhenExecuteIsCalledThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Period input must not be null");
        verifyNoInteractions(transactionRepository);
    }

    private void verifyGivenNullStartDateWhenExecuteIsCalledThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start date must not be null");
        verifyNoInteractions(transactionRepository);
    }

    private void verifyGivenNullEndDateWhenExecuteIsCalledThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date must not be null");
        verifyNoInteractions(transactionRepository);
    }

    private void verifyGivenStartDateAfterEndDateWhenExecuteIsCalledThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start date must not be after end date");
        verifyNoInteractions(transactionRepository);
    }
}
