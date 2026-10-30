package dio.budgeting.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static dio.budgeting.support.TestConstants.AMOUNT_IN_CENTS;
import static dio.budgeting.support.TestConstants.CATEGORY;
import static dio.budgeting.support.TestConstants.CREATED_AT;
import static dio.budgeting.support.TestConstants.DESCRIPTION;
import static dio.budgeting.support.TestConstants.OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TransactionTest {

    @Test
    @DisplayName("Given valid fields When creating a Transaction Then it generates an ID, stores the fields and leaves createdAt null")
    void givenValidFieldsWhenCreatingTransactionThenItGeneratesIdStoresFieldsAndLeavesCreatedAtNull() {
        final var actual = new Transaction(OWNER_ID, DESCRIPTION, AMOUNT_IN_CENTS, CATEGORY);

        verifyGivenValidFieldsWhenCreatingTransactionThenItGeneratesIdStoresFieldsAndLeavesCreatedAtNull(actual);
    }

    @Test
    @DisplayName("Given valid fields including createdAt When reconstructing a Transaction Then all fields are stored")
    void givenValidFieldsIncludingCreatedAtWhenReconstructingTransactionThenAllFieldsAreStored() {
        final var id = new TransactionId();

        final var actual = new Transaction(OWNER_ID, id, DESCRIPTION, AMOUNT_IN_CENTS, CATEGORY, CREATED_AT);

        verifyGivenValidFieldsIncludingCreatedAtWhenReconstructingTransactionThenAllFieldsAreStored(actual, id);
    }

    @Test
    @DisplayName("Given an owner When creating a Transaction Then it stores the owner")
    void givenOwnerWhenCreatingTransactionThenItStoresTheOwner() {
        final var actual = new Transaction(OWNER_ID, DESCRIPTION, AMOUNT_IN_CENTS, CATEGORY);

        verifyGivenOwnerWhenCreatingTransactionThenItStoresTheOwner(actual, OWNER_ID);
    }

    @Test
    @DisplayName("Given a null owner When creating a Transaction Then it throws IllegalArgumentException")
    void givenNullOwnerWhenCreatingTransactionThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(
                () -> new Transaction(null, DESCRIPTION, AMOUNT_IN_CENTS, CATEGORY));

        verifyGivenNullOwnerWhenCreatingTransactionThenItThrowsIllegalArgumentException(thrown);
    }

    @Test
    @DisplayName("Given a null TransactionId When creating a Transaction Then it throws IllegalArgumentException")
    void givenNullTransactionIdWhenCreatingTransactionThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(
                () -> new Transaction(OWNER_ID, null, DESCRIPTION, AMOUNT_IN_CENTS, CATEGORY));

        verifyGivenNullTransactionIdWhenCreatingTransactionThenItThrowsIllegalArgumentException(thrown);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    @DisplayName("Given a blank description When creating a Transaction Then it throws IllegalArgumentException")
    void givenBlankDescriptionWhenCreatingTransactionThenItThrowsIllegalArgumentException(
            final String invalidDescription) {
        final var thrown = catchThrowable(
                () -> new Transaction(OWNER_ID, invalidDescription, AMOUNT_IN_CENTS, CATEGORY));

        verifyGivenBlankDescriptionWhenCreatingTransactionThenItThrowsIllegalArgumentException(thrown);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -8000L})
    @DisplayName("Given a non-positive amount When creating a Transaction Then it throws IllegalArgumentException")
    void givenNonPositiveAmountWhenCreatingTransactionThenItThrowsIllegalArgumentException(
            final long invalidAmount) {
        final var thrown = catchThrowable(
                () -> new Transaction(OWNER_ID, DESCRIPTION, invalidAmount, CATEGORY));

        verifyGivenNonPositiveAmountWhenCreatingTransactionThenItThrowsIllegalArgumentException(thrown);
    }

    @Test
    @DisplayName("Given a null category When creating a Transaction Then it throws IllegalArgumentException")
    void givenNullCategoryWhenCreatingTransactionThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(
                () -> new Transaction(OWNER_ID, DESCRIPTION, AMOUNT_IN_CENTS, null));

        verifyGivenNullCategoryWhenCreatingTransactionThenItThrowsIllegalArgumentException(thrown);
    }

    private void verifyGivenValidFieldsWhenCreatingTransactionThenItGeneratesIdStoresFieldsAndLeavesCreatedAtNull(
            final Transaction actual) {
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(actual.getId().uuid()).isNotNull();
        assertThat(actual.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(actual.getAmount()).isEqualTo(AMOUNT_IN_CENTS);
        assertThat(actual.getCategory()).isEqualTo(CATEGORY);
        assertThat(actual.getCreatedAt()).isNull();
    }

    private void verifyGivenValidFieldsIncludingCreatedAtWhenReconstructingTransactionThenAllFieldsAreStored(
            final Transaction actual, final TransactionId expectedId) {
        assertThat(actual.getId()).isEqualTo(expectedId);
        assertThat(actual.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(actual.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(actual.getAmount()).isEqualTo(AMOUNT_IN_CENTS);
        assertThat(actual.getCategory()).isEqualTo(CATEGORY);
        assertThat(actual.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    private void verifyGivenOwnerWhenCreatingTransactionThenItStoresTheOwner(
            final Transaction actual, final OwnerId expected) {
        assertThat(actual.getOwnerId()).isEqualTo(expected);
    }

    private void verifyGivenNullOwnerWhenCreatingTransactionThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Owner ID must not be null");
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
