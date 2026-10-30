package dio.budgeting.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class OwnerIdTest {

    @Test
    @DisplayName("Given a non-null UUID When creating an OwnerId Then it stores the UUID")
    void givenNonNullUuidWhenCreatingOwnerIdThenItStoresTheUuid() {
        final var uuid = UUID.randomUUID();

        final var actual = new OwnerId(uuid);

        verifyGivenNonNullUuidWhenCreatingOwnerIdThenItStoresTheUuid(actual, uuid);
    }

    @Test
    @DisplayName("Given a null UUID When creating an OwnerId Then it throws IllegalArgumentException")
    void givenNullUuidWhenCreatingOwnerIdThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(() -> new OwnerId(null));

        verifyGivenNullUuidWhenCreatingOwnerIdThenItThrowsIllegalArgumentException(thrown);
    }

    @Test
    @DisplayName("Given two default constructors When creating OwnerIds Then they are unique")
    void givenTwoDefaultConstructorsWhenCreatingOwnerIdsThenTheyAreUnique() {
        final var first = new OwnerId();
        final var second = new OwnerId();

        verifyGivenTwoDefaultConstructorsWhenCreatingOwnerIdsThenTheyAreUnique(first, second);
    }

    private void verifyGivenNonNullUuidWhenCreatingOwnerIdThenItStoresTheUuid(
            final OwnerId actual, final UUID expected) {
        assertThat(actual.uuid()).isEqualTo(expected);
    }

    private void verifyGivenNullUuidWhenCreatingOwnerIdThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Owner ID must not be null");
    }

    private void verifyGivenTwoDefaultConstructorsWhenCreatingOwnerIdsThenTheyAreUnique(
            final OwnerId first, final OwnerId second) {
        assertThat(first.uuid()).isNotNull();
        assertThat(second.uuid()).isNotNull();
        assertThat(first).isNotEqualTo(second);
    }
}
