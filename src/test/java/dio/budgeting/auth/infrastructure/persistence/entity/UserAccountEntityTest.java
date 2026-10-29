package dio.budgeting.auth.infrastructure.persistence.entity;

import dio.budgeting.auth.domain.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dio.budgeting.support.domain.UserAccountSupport.defaultUserAccount;
import static org.assertj.core.api.Assertions.assertThat;

class UserAccountEntityTest {

    @Test
    @DisplayName("Given a user account When converted to entity and back to domain Then all fields are kept")
    void givenUserAccountWhenConvertedToEntityAndBackToDomainThenAllFieldsAreKept() {
        final var userAccount = defaultUserAccount().create();

        final var actual = UserAccountEntity.from(userAccount).toDomain();

        verifyGivenUserAccountWhenConvertedToEntityAndBackToDomainThenAllFieldsAreKept(actual, userAccount);
    }

    @Test
    @DisplayName("Given a user account When converted to entity Then the encoded password is mapped to the password column")
    void givenUserAccountWhenConvertedToEntityThenEncodedPasswordIsMappedToPasswordColumn() {
        final var userAccount = defaultUserAccount().create();

        final var actual = UserAccountEntity.from(userAccount);

        verifyGivenUserAccountWhenConvertedToEntityThenEncodedPasswordIsMappedToPasswordColumn(actual, userAccount);
    }

    private void verifyGivenUserAccountWhenConvertedToEntityAndBackToDomainThenAllFieldsAreKept(
            final UserAccount actual, final UserAccount expected) {
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    private void verifyGivenUserAccountWhenConvertedToEntityThenEncodedPasswordIsMappedToPasswordColumn(
            final UserAccountEntity actual, final UserAccount expected) {
        assertThat(actual.getPassword()).isEqualTo(expected.getEncodedPassword());
    }
}
