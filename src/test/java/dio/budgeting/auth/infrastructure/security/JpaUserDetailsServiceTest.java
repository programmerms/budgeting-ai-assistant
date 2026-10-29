package dio.budgeting.auth.infrastructure.security;

import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static dio.budgeting.support.TestConstants.USERNAME;
import static dio.budgeting.support.domain.UserAccountSupport.defaultUserAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private JpaUserDetailsService service;

    @Test
    @DisplayName("Given a stored account When loadUserByUsername is called Then the account is returned as an authenticated user")
    void givenStoredAccountWhenLoadUserByUsernameIsCalledThenTheAccountIsReturnedAsAnAuthenticatedUser() {
        final var account = defaultUserAccount().create();
        setupGivenStoredAccountWhenLoadUserByUsernameIsCalledThenTheAccountIsReturnedAsAnAuthenticatedUser(account);

        final var result = service.loadUserByUsername(USERNAME);

        verifyGivenStoredAccountWhenLoadUserByUsernameIsCalledThenTheAccountIsReturnedAsAnAuthenticatedUser(
                result, account);
    }

    @Test
    @DisplayName("Given a username in uppercase When loadUserByUsername is called Then the lookup uses the lowercase name")
    void givenUsernameInUppercaseWhenLoadUserByUsernameIsCalledThenTheLookupUsesTheLowercaseName() {
        final var account = defaultUserAccount().create();
        setupGivenUsernameInUppercaseWhenLoadUserByUsernameIsCalledThenTheLookupUsesTheLowercaseName(account);

        service.loadUserByUsername("MARIA");

        verifyGivenUsernameInUppercaseWhenLoadUserByUsernameIsCalledThenTheLookupUsesTheLowercaseName();
    }

    @Test
    @DisplayName("Given an unknown username When loadUserByUsername is called Then it throws UsernameNotFoundException")
    void givenUnknownUsernameWhenLoadUserByUsernameIsCalledThenItThrowsUsernameNotFoundException() {
        setupGivenUnknownUsernameWhenLoadUserByUsernameIsCalledThenItThrowsUsernameNotFoundException();

        final var thrown = catchThrowable(() -> service.loadUserByUsername(USERNAME));

        verifyGivenUnknownUsernameWhenLoadUserByUsernameIsCalledThenItThrowsUsernameNotFoundException(thrown);
    }

    @Test
    @DisplayName("Given a blank username When loadUserByUsername is called Then it throws UsernameNotFoundException without touching the repository")
    void givenBlankUsernameWhenLoadUserByUsernameIsCalledThenItThrowsUsernameNotFoundExceptionWithoutTouchingTheRepository() {
        final var thrown = catchThrowable(() -> service.loadUserByUsername("  "));

        verifyGivenBlankUsernameWhenLoadUserByUsernameIsCalledThenItThrowsUsernameNotFoundExceptionWithoutTouchingTheRepository(
                thrown);
    }

    private void setupGivenStoredAccountWhenLoadUserByUsernameIsCalledThenTheAccountIsReturnedAsAnAuthenticatedUser(
            final UserAccount account) {
        when(userAccountRepository.findByUsername(USERNAME)).thenReturn(Optional.of(account));
    }

    private void verifyGivenStoredAccountWhenLoadUserByUsernameIsCalledThenTheAccountIsReturnedAsAnAuthenticatedUser(
            final UserDetails actual, final UserAccount expected) {
        assertThat(actual).isEqualTo(AuthenticatedUser.from(expected));
    }

    private void setupGivenUsernameInUppercaseWhenLoadUserByUsernameIsCalledThenTheLookupUsesTheLowercaseName(
            final UserAccount account) {
        when(userAccountRepository.findByUsername(USERNAME)).thenReturn(Optional.of(account));
    }

    private void verifyGivenUsernameInUppercaseWhenLoadUserByUsernameIsCalledThenTheLookupUsesTheLowercaseName() {
        verify(userAccountRepository, times(1)).findByUsername(USERNAME);
    }

    private void setupGivenUnknownUsernameWhenLoadUserByUsernameIsCalledThenItThrowsUsernameNotFoundException() {
        when(userAccountRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
    }

    private void verifyGivenUnknownUsernameWhenLoadUserByUsernameIsCalledThenItThrowsUsernameNotFoundException(
            final Throwable actual) {
        assertThat(actual).isInstanceOf(UsernameNotFoundException.class);
    }

    private void verifyGivenBlankUsernameWhenLoadUserByUsernameIsCalledThenItThrowsUsernameNotFoundExceptionWithoutTouchingTheRepository(
            final Throwable actual) {
        assertThat(actual).isInstanceOf(UsernameNotFoundException.class);
        verifyNoInteractions(userAccountRepository);
    }
}
