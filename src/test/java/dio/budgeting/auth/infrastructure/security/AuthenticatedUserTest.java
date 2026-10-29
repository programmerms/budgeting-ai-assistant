package dio.budgeting.auth.infrastructure.security;

import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserRole;
import dio.budgeting.auth.domain.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static dio.budgeting.support.domain.UserAccountSupport.defaultUserAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

class AuthenticatedUserTest {

    @Test
    @DisplayName("Given a user account When converted to an authenticated user Then username and encoded password are exposed to Spring Security")
    void givenUserAccountWhenConvertedToAuthenticatedUserThenUsernameAndEncodedPasswordAreExposedToSpringSecurity() {
        final var account = defaultUserAccount().create();

        final var actual = AuthenticatedUser.from(account);

        verifyGivenUserAccountWhenConvertedToAuthenticatedUserThenUsernameAndEncodedPasswordAreExposedToSpringSecurity(
                actual, account);
    }

    @Test
    @DisplayName("Given a common user account When converted to an authenticated user Then the authority is ROLE_USER")
    void givenCommonUserAccountWhenConvertedToAuthenticatedUserThenAuthorityIsRoleUser() {
        final var account = defaultUserAccount().create();

        final var actual = AuthenticatedUser.from(account);

        verifyGivenCommonUserAccountWhenConvertedToAuthenticatedUserThenAuthorityIsRoleUser(actual);
    }

    @Test
    @DisplayName("Given an administrator account When converted to an authenticated user Then the authority is ROLE_ADMIN")
    void givenAdministratorAccountWhenConvertedToAuthenticatedUserThenAuthorityIsRoleAdmin() {
        final var account = defaultUserAccount()
                .set(field(UserAccount::getRole), UserRole.ROLE_ADMIN)
                .create();

        final var actual = AuthenticatedUser.from(account);

        verifyGivenAdministratorAccountWhenConvertedToAuthenticatedUserThenAuthorityIsRoleAdmin(actual);
    }

    @Test
    @DisplayName("Given an active account When converted to an authenticated user Then it is enabled")
    void givenActiveAccountWhenConvertedToAuthenticatedUserThenItIsEnabled() {
        final var account = defaultUserAccount()
                .set(field(UserAccount::getStatus), UserStatus.ACTIVE)
                .create();

        final var actual = AuthenticatedUser.from(account);

        verifyGivenActiveAccountWhenConvertedToAuthenticatedUserThenItIsEnabled(actual);
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"PENDING", "REJECTED"})
    @DisplayName("Given an account that is not active When converted to an authenticated user Then it is disabled")
    void givenAccountThatIsNotActiveWhenConvertedToAuthenticatedUserThenItIsDisabled(final UserStatus status) {
        final var account = defaultUserAccount()
                .set(field(UserAccount::getStatus), status)
                .create();

        final var actual = AuthenticatedUser.from(account);

        verifyGivenAccountThatIsNotActiveWhenConvertedToAuthenticatedUserThenItIsDisabled(actual);
    }

    private void verifyGivenUserAccountWhenConvertedToAuthenticatedUserThenUsernameAndEncodedPasswordAreExposedToSpringSecurity(
            final AuthenticatedUser actual, final UserAccount expected) {
        assertThat(actual.id()).isEqualTo(expected.getId());
        assertThat(actual.getUsername()).isEqualTo(expected.getUsername());
        assertThat(actual.getPassword()).isEqualTo(expected.getEncodedPassword());
    }

    private void verifyGivenCommonUserAccountWhenConvertedToAuthenticatedUserThenAuthorityIsRoleUser(
            final AuthenticatedUser actual) {
        assertThat(actual.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    private void verifyGivenAdministratorAccountWhenConvertedToAuthenticatedUserThenAuthorityIsRoleAdmin(
            final AuthenticatedUser actual) {
        assertThat(actual.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    private void verifyGivenActiveAccountWhenConvertedToAuthenticatedUserThenItIsEnabled(
            final AuthenticatedUser actual) {
        assertThat(actual.isEnabled()).isTrue();
    }

    private void verifyGivenAccountThatIsNotActiveWhenConvertedToAuthenticatedUserThenItIsDisabled(
            final AuthenticatedUser actual) {
        assertThat(actual.isEnabled()).isFalse();
    }
}
