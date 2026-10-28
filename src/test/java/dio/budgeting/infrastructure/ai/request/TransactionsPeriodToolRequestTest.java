package dio.budgeting.infrastructure.ai.request;

import dio.budgeting.application.input.TransactionsPeriodInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dio.budgeting.support.request.TransactionsPeriodToolRequestSupport.defaultTransactionsPeriodToolRequest;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionsPeriodToolRequestTest {

    @Test
    @DisplayName("Given a period tool request When converted to input Then from and to dates are kept")
    void givenPeriodToolRequestWhenConvertedToInputThenFromAndToDatesAreKept() {
        final var request = defaultTransactionsPeriodToolRequest().create();

        final var actual = request.toInput();

        verifyGivenPeriodToolRequestWhenConvertedToInputThenFromAndToDatesAreKept(actual, request);
    }

    private void verifyGivenPeriodToolRequestWhenConvertedToInputThenFromAndToDatesAreKept(
            final TransactionsPeriodInput actual, final TransactionsPeriodToolRequest expected) {
        assertThat(actual).isEqualTo(new TransactionsPeriodInput(expected.from(), expected.to()));
    }
}
