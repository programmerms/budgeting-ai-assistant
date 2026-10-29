package dio.budgeting.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static dio.budgeting.support.TestConstants.ENCODED_PASSWORD;
import static dio.budgeting.support.TestConstants.USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class UserAccountTest {

    @Test
    @DisplayName("Given a username and an encoded password When registering Then the account is created as a pending common user")
    void givenUsernameAndEncodedPasswordWhenRegisteringThenAccountIsCreatedAsPendingCommonUser() {
        final var actual = UserAccount.register(USERNAME, ENCODED_PASSWORD);

        verifyGivenUsernameAndEncodedPasswordWhenRegisteringThenAccountIsCreatedAsPendingCommonUser(actual);
    }

    @Test
    @DisplayName("Given a username and an encoded password When creating an administrator Then the account is created as an active admin")
    void givenUsernameAndEncodedPasswordWhenCreatingAdministratorThenAccountIsCreatedAsActiveAdmin() {
        final var actual = UserAccount.administrator(USERNAME, ENCODED_PASSWORD);

        verifyGivenUsernameAndEncodedPasswordWhenCreatingAdministratorThenAccountIsCreatedAsActiveAdmin(actual);
    }

    @Test
    @DisplayName("Given a username with uppercase letters and spaces When creating an account Then the username is normalized to lowercase")
    void givenUsernameWithUppercaseAndSpacesWhenCreatingAccountThenUsernameIsNormalizedToLowercase() {
        final var actual = UserAccount.register("  MaRiA  ", ENCODED_PASSWORD);

        verifyGivenUsernameWithUppercaseAndSpacesWhenCreatingAccountThenUsernameIsNormalizedToLowercase(actual);
    }

    @Test
    @DisplayName("Given a blank username When creating an account Then it throws IllegalArgumentException")
    void givenBlankUsernameWhenCreatingAccountThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(() -> UserAccount.register("  ", ENCODED_PASSWORD));

        verifyGivenBlankUsernameWhenCreatingAccountThenItThrowsIllegalArgumentException(thrown);
    }

    @Test
    @DisplayName("Given a blank encoded password When creating an account Then it throws IllegalArgumentException")
    void givenBlankEncodedPasswordWhenCreatingAccountThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(() -> UserAccount.register(USERNAME, "  "));

        verifyGivenBlankEncodedPasswordWhenCreatingAccountThenItThrowsIllegalArgumentException(thrown);
    }

    @Test
    @DisplayName("Given a null identifier When creating an account Then it throws IllegalArgumentException")
    void givenNullIdentifierWhenCreatingAccountThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(
                () -> new UserAccount(null, USERNAME, ENCODED_PASSWORD, UserRole.ROLE_USER, UserStatus.PENDING));

        verifyGivenNullIdentifierWhenCreatingAccountThenItThrowsIllegalArgumentException(thrown);
    }

    @Test
    @DisplayName("Given a null role When creating an account Then it throws IllegalArgumentException")
    void givenNullRoleWhenCreatingAccountThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(
                () -> new UserAccount(new UserAccountId(), USERNAME, ENCODED_PASSWORD, null, UserStatus.PENDING));

        verifyGivenNullRoleWhenCreatingAccountThenItThrowsIllegalArgumentException(thrown);
    }

    @Test
    @DisplayName("Given a null status When creating an account Then it throws IllegalArgumentException")
    void givenNullStatusWhenCreatingAccountThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(
                () -> new UserAccount(new UserAccountId(), USERNAME, ENCODED_PASSWORD, UserRole.ROLE_USER, null));

        verifyGivenNullStatusWhenCreatingAccountThenItThrowsIllegalArgumentException(thrown);
    }

    @Test
    @DisplayName("Given a pending account When approve is called Then the status becomes active")
    void givenPendingAccountWhenApproveIsCalledThenStatusBecomesActive() {
        final var account = UserAccount.register(USERNAME, ENCODED_PASSWORD);

        account.approve();

        verifyGivenPendingAccountWhenApproveIsCalledThenStatusBecomesActive(account);
    }

    @Test
    @DisplayName("Given a pending account When reject is called Then the status becomes rejected")
    void givenPendingAccountWhenRejectIsCalledThenStatusBecomesRejected() {
        final var account = UserAccount.register(USERNAME, ENCODED_PASSWORD);

        account.reject();

        verifyGivenPendingAccountWhenRejectIsCalledThenStatusBecomesRejected(account);
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "REJECTED"})
    @DisplayName("Given an already decided account When approve is called Then it throws UserAlreadyDecidedException and keeps the status")
    void givenAlreadyDecidedAccountWhenApproveIsCalledThenItThrowsUserAlreadyDecidedExceptionAndKeepsStatus(
            final UserStatus status) {
        final var account = accountWith(status);

        final var thrown = catchThrowable(account::approve);

        verifyGivenAlreadyDecidedAccountWhenApproveIsCalledThenItThrowsUserAlreadyDecidedExceptionAndKeepsStatus(
                thrown, account, status);
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "REJECTED"})
    @DisplayName("Given an already decided account When reject is called Then it throws UserAlreadyDecidedException and keeps the status")
    void givenAlreadyDecidedAccountWhenRejectIsCalledThenItThrowsUserAlreadyDecidedExceptionAndKeepsStatus(
            final UserStatus status) {
        final var account = accountWith(status);

        final var thrown = catchThrowable(account::reject);

        verifyGivenAlreadyDecidedAccountWhenRejectIsCalledThenItThrowsUserAlreadyDecidedExceptionAndKeepsStatus(
                thrown, account, status);
    }

    private UserAccount accountWith(final UserStatus status) {
        return new UserAccount(new UserAccountId(), USERNAME, ENCODED_PASSWORD, UserRole.ROLE_USER, status);
    }

    private void verifyGivenUsernameAndEncodedPasswordWhenRegisteringThenAccountIsCreatedAsPendingCommonUser(
            final UserAccount actual) {
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getUsername()).isEqualTo(USERNAME);
        assertThat(actual.getEncodedPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(actual.getRole()).isEqualTo(UserRole.ROLE_USER);
        assertThat(actual.getStatus()).isEqualTo(UserStatus.PENDING);
    }

    private void verifyGivenUsernameAndEncodedPasswordWhenCreatingAdministratorThenAccountIsCreatedAsActiveAdmin(
            final UserAccount actual) {
        assertThat(actual.getRole()).isEqualTo(UserRole.ROLE_ADMIN);
        assertThat(actual.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    private void verifyGivenUsernameWithUppercaseAndSpacesWhenCreatingAccountThenUsernameIsNormalizedToLowercase(
            final UserAccount actual) {
        assertThat(actual.getUsername()).isEqualTo("maria");
    }

    private void verifyGivenBlankUsernameWhenCreatingAccountThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username must not be blank");
    }

    private void verifyGivenBlankEncodedPasswordWhenCreatingAccountThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Encoded password must not be blank");
    }

    private void verifyGivenNullIdentifierWhenCreatingAccountThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User account ID must not be null");
    }

    private void verifyGivenNullRoleWhenCreatingAccountThenItThrowsIllegalArgumentException(final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Role must not be null");
    }

    private void verifyGivenNullStatusWhenCreatingAccountThenItThrowsIllegalArgumentException(final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Status must not be null");
    }

    private void verifyGivenPendingAccountWhenApproveIsCalledThenStatusBecomesActive(final UserAccount actual) {
        assertThat(actual.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    private void verifyGivenPendingAccountWhenRejectIsCalledThenStatusBecomesRejected(final UserAccount actual) {
        assertThat(actual.getStatus()).isEqualTo(UserStatus.REJECTED);
    }

    private void verifyGivenAlreadyDecidedAccountWhenApproveIsCalledThenItThrowsUserAlreadyDecidedExceptionAndKeepsStatus(
            final Throwable actual, final UserAccount account, final UserStatus expected) {
        assertThat(actual).isInstanceOf(UserAlreadyDecidedException.class);
        assertThat(account.getStatus()).isEqualTo(expected);
    }

    private void verifyGivenAlreadyDecidedAccountWhenRejectIsCalledThenItThrowsUserAlreadyDecidedExceptionAndKeepsStatus(
            final Throwable actual, final UserAccount account, final UserStatus expected) {
        assertThat(actual).isInstanceOf(UserAlreadyDecidedException.class);
        assertThat(account.getStatus()).isEqualTo(expected);
    }
}
