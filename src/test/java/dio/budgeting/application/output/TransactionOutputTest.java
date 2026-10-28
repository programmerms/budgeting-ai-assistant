package dio.budgeting.application.output;

import dio.budgeting.domain.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static dio.budgeting.support.TestConstants.CREATED_AT;
import static dio.budgeting.support.domain.TransactionSupport.defaultTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

class TransactionOutputTest {

    @ParameterizedTest
    @CsvSource({"8000, 80.00", "8050, 80.50", "1, 0.01"})
    @DisplayName("Given an amount in cents When converting to output Then the value is in reais with two decimal places and createdAt is mapped")
    void givenAmountInCentsWhenConvertingToOutputThenValueIsInReaisWithTwoDecimalPlacesAndCreatedAtIsMapped(
            final long amountInCents,
            final BigDecimal expected) {
        final var transaction = defaultTransaction()
                .set(field(Transaction::getAmount), amountInCents)
                .create();

        final var actual = TransactionOutput.from(transaction);

        verifyGivenAmountInCentsWhenConvertingToOutputThenValueIsInReaisWithTwoDecimalPlacesAndCreatedAtIsMapped(
                actual, expected);
    }

    private void verifyGivenAmountInCentsWhenConvertingToOutputThenValueIsInReaisWithTwoDecimalPlacesAndCreatedAtIsMapped(
            final TransactionOutput actual, final BigDecimal expected) {
        assertThat(actual.value()).isEqualTo(expected);
        assertThat(actual.createdAt()).isEqualTo(CREATED_AT);
    }
}
