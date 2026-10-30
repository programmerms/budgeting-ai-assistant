package dio.budgeting.infrastructure.security;

import dio.budgeting.auth.infrastructure.security.AuthenticatedUser;
import dio.budgeting.domain.OwnerId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static dio.budgeting.support.AuthenticatedUserSupport.defaultAuthenticatedUser;
import static dio.budgeting.support.TestConstants.OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class AuthenticatedOwnerProviderTest {

    private final AuthenticatedOwnerProvider provider = new AuthenticatedOwnerProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Given an authenticated user When currentOwnerId is called Then its ID is returned as OwnerId")
    void givenAuthenticatedUserWhenCurrentOwnerIdIsCalledThenItsIdIsReturnedAsOwnerId() {
        final var user = defaultAuthenticatedUser().create();
        setupGivenAuthenticatedUserWhenCurrentOwnerIdIsCalledThenItsIdIsReturnedAsOwnerId(user);

        final var actual = provider.currentOwnerId();

        verifyGivenAuthenticatedUserWhenCurrentOwnerIdIsCalledThenItsIdIsReturnedAsOwnerId(actual);
    }

    @Test
    @DisplayName("Given an empty security context When currentOwnerId is called Then it throws IllegalStateException")
    void givenEmptySecurityContextWhenCurrentOwnerIdIsCalledThenItThrowsIllegalStateException() {
        setupGivenEmptySecurityContextWhenCurrentOwnerIdIsCalledThenItThrowsIllegalStateException();

        final var thrown = catchThrowable(provider::currentOwnerId);

        verifyGivenEmptySecurityContextWhenCurrentOwnerIdIsCalledThenItThrowsIllegalStateException(thrown);
    }

    @Test
    @DisplayName("Given an unexpected principal When currentOwnerId is called Then it throws IllegalStateException")
    void givenUnexpectedPrincipalWhenCurrentOwnerIdIsCalledThenItThrowsIllegalStateException() {
        setupGivenUnexpectedPrincipalWhenCurrentOwnerIdIsCalledThenItThrowsIllegalStateException();

        final var thrown = catchThrowable(provider::currentOwnerId);

        verifyGivenUnexpectedPrincipalWhenCurrentOwnerIdIsCalledThenItThrowsIllegalStateException(thrown);
    }

    private void setupGivenAuthenticatedUserWhenCurrentOwnerIdIsCalledThenItsIdIsReturnedAsOwnerId(
            final AuthenticatedUser user) {
        final var authentication = UsernamePasswordAuthenticationToken.authenticated(
                user, user.getPassword(), user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void verifyGivenAuthenticatedUserWhenCurrentOwnerIdIsCalledThenItsIdIsReturnedAsOwnerId(
            final OwnerId actual) {
        assertThat(actual).isEqualTo(OWNER_ID);
    }

    private void setupGivenEmptySecurityContextWhenCurrentOwnerIdIsCalledThenItThrowsIllegalStateException() {
        SecurityContextHolder.clearContext();
    }

    private void verifyGivenEmptySecurityContextWhenCurrentOwnerIdIsCalledThenItThrowsIllegalStateException(
            final Throwable actual) {
        assertMissingOwner(actual);
    }

    private void setupGivenUnexpectedPrincipalWhenCurrentOwnerIdIsCalledThenItThrowsIllegalStateException() {
        final var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "unexpected-principal", "password", java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void verifyGivenUnexpectedPrincipalWhenCurrentOwnerIdIsCalledThenItThrowsIllegalStateException(
            final Throwable actual) {
        assertMissingOwner(actual);
    }

    private void assertMissingOwner(final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authenticated owner is required");
    }
}
