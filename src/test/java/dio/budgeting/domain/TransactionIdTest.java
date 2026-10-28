package dio.budgeting.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TransactionIdTest {

    @Test
    @DisplayName("Given a non-null UUID When creating a TransactionId Then it stores the UUID")
    void givenNonNullUuidWhenCreatingTransactionIdThenItStoresUuid() {
        final var uuid = UUID.randomUUID();

        final var actual = new TransactionId(uuid);

        verifyGivenNonNullUuidWhenCreatingTransactionIdThenItStoresUuid(actual, uuid);
    }

    @Test
    @DisplayName("Given the default constructor When creating a TransactionId Then it generates a non-null UUID")
    void givenDefaultConstructorWhenCreatingTransactionIdThenItGeneratesNonNullUuid() {
        final var actual = new TransactionId();

        verifyGivenDefaultConstructorWhenCreatingTransactionIdThenItGeneratesNonNullUuid(actual);
    }

    @Test
    @DisplayName("Given a null UUID When creating a TransactionId Then it throws IllegalArgumentException")
    void givenNullUuidWhenCreatingTransactionIdThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(() -> new TransactionId(null));

        verifyGivenNullUuidWhenCreatingTransactionIdThenItThrowsIllegalArgumentException(thrown);
    }

    private void verifyGivenNonNullUuidWhenCreatingTransactionIdThenItStoresUuid(
            final TransactionId actual, final UUID expected) {
        assertThat(actual.uuid()).isEqualTo(expected);
    }

    private void verifyGivenDefaultConstructorWhenCreatingTransactionIdThenItGeneratesNonNullUuid(
            final TransactionId actual) {
        assertThat(actual.uuid()).isNotNull();
    }

    private void verifyGivenNullUuidWhenCreatingTransactionIdThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction ID must not be null");
    }
}
