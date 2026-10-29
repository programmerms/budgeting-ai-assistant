package dio.budgeting.auth.infrastructure.security;

import dio.budgeting.auth.application.PasswordEncoderGateway;
import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountRepository;
import dio.budgeting.auth.domain.UserRole;
import dio.budgeting.auth.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static dio.budgeting.support.TestConstants.ADMIN_PASSWORD;
import static dio.budgeting.support.TestConstants.ADMIN_USERNAME;
import static dio.budgeting.support.TestConstants.ENCODED_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoderGateway passwordEncoderGateway;

    private AdminAccountInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new AdminAccountInitializer(
                userAccountRepository, passwordEncoderGateway, ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test
    @DisplayName("Given no administrator account When the application starts Then an active administrator is created with an encoded password")
    void givenNoAdministratorAccountWhenTheApplicationStartsThenAnActiveAdministratorIsCreatedWithAnEncodedPassword() {
        setupGivenNoAdministratorAccountWhenTheApplicationStartsThenAnActiveAdministratorIsCreatedWithAnEncodedPassword();

        initializer.run();

        verifyGivenNoAdministratorAccountWhenTheApplicationStartsThenAnActiveAdministratorIsCreatedWithAnEncodedPassword();
    }

    @Test
    @DisplayName("Given an existing administrator account When the application starts Then no account is created")
    void givenExistingAdministratorAccountWhenTheApplicationStartsThenNoAccountIsCreated() {
        setupGivenExistingAdministratorAccountWhenTheApplicationStartsThenNoAccountIsCreated();

        initializer.run();

        verifyGivenExistingAdministratorAccountWhenTheApplicationStartsThenNoAccountIsCreated();
    }

    @Test
    @DisplayName("Given an administrator username in uppercase When the application starts Then the stored username is normalized to lowercase")
    void givenAdministratorUsernameInUppercaseWhenTheApplicationStartsThenTheStoredUsernameIsNormalizedToLowercase() {
        setupGivenAdministratorUsernameInUppercaseWhenTheApplicationStartsThenTheStoredUsernameIsNormalizedToLowercase();

        new AdminAccountInitializer(userAccountRepository, passwordEncoderGateway, "ADMIN", ADMIN_PASSWORD).run();

        verifyGivenAdministratorUsernameInUppercaseWhenTheApplicationStartsThenTheStoredUsernameIsNormalizedToLowercase();
    }

    private void setupGivenNoAdministratorAccountWhenTheApplicationStartsThenAnActiveAdministratorIsCreatedWithAnEncodedPassword() {
        when(userAccountRepository.existsByRole(UserRole.ROLE_ADMIN)).thenReturn(false);
        when(passwordEncoderGateway.encode(ADMIN_PASSWORD)).thenReturn(ENCODED_PASSWORD);
    }

    private void verifyGivenNoAdministratorAccountWhenTheApplicationStartsThenAnActiveAdministratorIsCreatedWithAnEncodedPassword() {
        final var captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository, times(1)).save(captor.capture());

        assertThat(captor.getValue().getUsername()).isEqualTo(ADMIN_USERNAME);
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ROLE_ADMIN);
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(captor.getValue().getEncodedPassword())
                .isEqualTo(ENCODED_PASSWORD)
                .isNotEqualTo(ADMIN_PASSWORD);
    }

    private void setupGivenExistingAdministratorAccountWhenTheApplicationStartsThenNoAccountIsCreated() {
        when(userAccountRepository.existsByRole(UserRole.ROLE_ADMIN)).thenReturn(true);
    }

    private void verifyGivenExistingAdministratorAccountWhenTheApplicationStartsThenNoAccountIsCreated() {
        verify(userAccountRepository, never()).save(any(UserAccount.class));
        verifyNoInteractions(passwordEncoderGateway);
    }

    private void setupGivenAdministratorUsernameInUppercaseWhenTheApplicationStartsThenTheStoredUsernameIsNormalizedToLowercase() {
        when(userAccountRepository.existsByRole(UserRole.ROLE_ADMIN)).thenReturn(false);
        when(passwordEncoderGateway.encode(ADMIN_PASSWORD)).thenReturn(ENCODED_PASSWORD);
    }

    private void verifyGivenAdministratorUsernameInUppercaseWhenTheApplicationStartsThenTheStoredUsernameIsNormalizedToLowercase() {
        final var captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
    }
}
