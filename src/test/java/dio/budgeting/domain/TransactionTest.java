package dio.budgeting.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static dio.budgeting.support.TestConstants.AMOUNT_IN_CENTS;
import static dio.budgeting.support.TestConstants.CATEGORY;
import static dio.budgeting.support.TestConstants.DESCRIPTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TransactionTest {

    @Test
    @DisplayName("Given valid fields When creating a Transaction Then it generates an ID and stores the fields")
    void givenValidFieldsWhenCreatingTransactionThenItGeneratesIdAndStoresFields() {
        final var actual = new Transaction(DESCRIPTION, AMOUNT_IN_CENTS, CATEGORY);

        verifyGivenValidFieldsWhenCreatingTransactionThenItGeneratesIdAndStoresFields(actual);
    }

    @Test
    @DisplayName("Given a null TransactionId When creating a Transaction Then it throws IllegalArgumentException")
    void givenNullTransactionIdWhenCreatingTransactionThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(
                () -> new Transaction(null, DESCRIPTION, AMOUNT_IN_CENTS, CATEGORY));

        verifyGivenNullTransactionIdWhenCreatingTransactionThenItThrowsIllegalArgumentException(thrown);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    @DisplayName("Given a blank description When creating a Transaction Then it throws IllegalArgumentException")
    void givenBlankDescriptionWhenCreatingTransactionThenItThrowsIllegalArgumentException(
            final String invalidDescription) {
        final var thrown = catchThrowable(
                () -> new Transaction(invalidDescription, AMOUNT_IN_CENTS, CATEGORY));

        verifyGivenBlankDescriptionWhenCreatingTransactionThenItThrowsIllegalArgumentException(thrown);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -8000L})
    @DisplayName("Given a non-positive amount When creating a Transaction Then it throws IllegalArgumentException")
    void givenNonPositiveAmountWhenCreatingTransactionThenItThrowsIllegalArgumentException(
            final long invalidAmount) {
        final var thrown = catchThrowable(
                () -> new Transaction(DESCRIPTION, invalidAmount, CATEGORY));

        verifyGivenNonPositiveAmountWhenCreatingTransactionThenItThrowsIllegalArgumentException(thrown);
    }

    @Test
    @DisplayName("Given a null category When creating a Transaction Then it throws IllegalArgumentException")
    void givenNullCategoryWhenCreatingTransactionThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(
                () -> new Transaction(DESCRIPTION, AMOUNT_IN_CENTS, null));

        verifyGivenNullCategoryWhenCreatingTransactionThenItThrowsIllegalArgumentException(thrown);
    }

    private void verifyGivenValidFieldsWhenCreatingTransactionThenItGeneratesIdAndStoresFields(
            final Transaction actual) {
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getId().uuid()).isNotNull();
        assertThat(actual.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(actual.getAmount()).isEqualTo(AMOUNT_IN_CENTS);
        assertThat(actual.getCategory()).isEqualTo(CATEGORY);
    }

    private void verifyGivenNullTransactionIdWhenCreatingTransactionThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction ID must not be null");
    }

    private void verifyGivenBlankDescriptionWhenCreatingTransactionThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description must not be blank");
    }

    private void verifyGivenNonPositiveAmountWhenCreatingTransactionThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than zero");
    }

    private void verifyGivenNullCategoryWhenCreatingTransactionThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category must not be null");
    }
}
