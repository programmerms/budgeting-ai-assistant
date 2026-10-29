package dio.budgeting.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class UserAccountIdTest {

    @Test
    @DisplayName("Given a non-null UUID When creating a UserAccountId Then it stores the UUID")
    void givenNonNullUuidWhenCreatingUserAccountIdThenItStoresUuid() {
        final var uuid = UUID.randomUUID();

        final var actual = new UserAccountId(uuid);

        verifyGivenNonNullUuidWhenCreatingUserAccountIdThenItStoresUuid(actual, uuid);
    }

    @Test
    @DisplayName("Given the default constructor When creating a UserAccountId Then it generates a non-null UUID")
    void givenDefaultConstructorWhenCreatingUserAccountIdThenItGeneratesNonNullUuid() {
        final var actual = new UserAccountId();

        verifyGivenDefaultConstructorWhenCreatingUserAccountIdThenItGeneratesNonNullUuid(actual);
    }

    @Test
    @DisplayName("Given a null UUID When creating a UserAccountId Then it throws IllegalArgumentException")
    void givenNullUuidWhenCreatingUserAccountIdThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(() -> new UserAccountId(null));

        verifyGivenNullUuidWhenCreatingUserAccountIdThenItThrowsIllegalArgumentException(thrown);
    }

    private void verifyGivenNonNullUuidWhenCreatingUserAccountIdThenItStoresUuid(
            final UserAccountId actual, final UUID expected) {
        assertThat(actual.uuid()).isEqualTo(expected);
    }

    private void verifyGivenDefaultConstructorWhenCreatingUserAccountIdThenItGeneratesNonNullUuid(
            final UserAccountId actual) {
        assertThat(actual.uuid()).isNotNull();
    }

    private void verifyGivenNullUuidWhenCreatingUserAccountIdThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User account ID must not be null");
    }
}
