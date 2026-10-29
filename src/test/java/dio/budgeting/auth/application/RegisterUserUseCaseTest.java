package dio.budgeting.auth.application;

import dio.budgeting.auth.application.input.RegisterUserInput;
import dio.budgeting.auth.application.output.UserAccountOutput;
import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountRepository;
import dio.budgeting.auth.domain.UserRole;
import dio.budgeting.auth.domain.UserStatus;
import dio.budgeting.auth.domain.UsernameAlreadyTakenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static dio.budgeting.support.TestConstants.ENCODED_PASSWORD;
import static dio.budgeting.support.TestConstants.RAW_PASSWORD;
import static dio.budgeting.support.TestConstants.USERNAME;
import static dio.budgeting.support.domain.UserAccountSupport.defaultUserAccount;
import static dio.budgeting.support.input.RegisterUserInputSupport.defaultRegisterUserInput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoderGateway passwordEncoderGateway;

    @InjectMocks
    private RegisterUserUseCase useCase;

    @Test
    @DisplayName("Given a valid input When execute is called Then the account is saved as pending and returned without the password")
    void givenValidInputWhenExecuteIsCalledThenAccountIsSavedAsPendingAndReturnedWithoutPassword() {
        final var input = defaultRegisterUserInput().create();
        final var saved = defaultUserAccount().create();
        setupGivenValidInputWhenExecuteIsCalledThenAccountIsSavedAsPendingAndReturnedWithoutPassword(saved);

        final var result = useCase.execute(input);

        verifyGivenValidInputWhenExecuteIsCalledThenAccountIsSavedAsPendingAndReturnedWithoutPassword(result, saved);
    }

    @Test
    @DisplayName("Given a valid input When execute is called Then the raw password is encoded before being persisted")
    void givenValidInputWhenExecuteIsCalledThenRawPasswordIsEncodedBeforeBeingPersisted() {
        final var input = defaultRegisterUserInput().create();
        final var saved = defaultUserAccount().create();
        setupGivenValidInputWhenExecuteIsCalledThenRawPasswordIsEncodedBeforeBeingPersisted(saved);

        useCase.execute(input);

        verifyGivenValidInputWhenExecuteIsCalledThenRawPasswordIsEncodedBeforeBeingPersisted();
    }

    @Test
    @DisplayName("Given an input with uppercase username When execute is called Then the username is normalized to lowercase")
    void givenInputWithUppercaseUsernameWhenExecuteIsCalledThenUsernameIsNormalizedToLowercase() {
        final var input = defaultRegisterUserInput()
                .set(field(RegisterUserInput::username), "MARIA")
                .create();
        final var saved = defaultUserAccount().create();
        setupGivenInputWithUppercaseUsernameWhenExecuteIsCalledThenUsernameIsNormalizedToLowercase(saved);

        useCase.execute(input);

        verifyGivenInputWithUppercaseUsernameWhenExecuteIsCalledThenUsernameIsNormalizedToLowercase();
    }

    @Test
    @DisplayName("Given a taken username When execute is called Then it throws UsernameAlreadyTakenException and nothing is persisted")
    void givenTakenUsernameWhenExecuteIsCalledThenItThrowsUsernameAlreadyTakenExceptionAndNothingIsPersisted() {
        final var input = defaultRegisterUserInput().create();
        setupGivenTakenUsernameWhenExecuteIsCalledThenItThrowsUsernameAlreadyTakenExceptionAndNothingIsPersisted();

        final var thrown = catchThrowable(() -> useCase.execute(input));

        verifyGivenTakenUsernameWhenExecuteIsCalledThenItThrowsUsernameAlreadyTakenExceptionAndNothingIsPersisted(
                thrown);
    }

    @Test
    @DisplayName("Given a username taken only in a different case When execute is called Then it throws UsernameAlreadyTakenException")
    void givenUsernameTakenOnlyInDifferentCaseWhenExecuteIsCalledThenItThrowsUsernameAlreadyTakenException() {
        final var input = defaultRegisterUserInput()
                .set(field(RegisterUserInput::username), "MARIA")
                .create();
        setupGivenUsernameTakenOnlyInDifferentCaseWhenExecuteIsCalledThenItThrowsUsernameAlreadyTakenException();

        final var thrown = catchThrowable(() -> useCase.execute(input));

        verifyGivenUsernameTakenOnlyInDifferentCaseWhenExecuteIsCalledThenItThrowsUsernameAlreadyTakenException(thrown);
    }

    @Test
    @DisplayName("Given a blank username When execute is called Then it throws IllegalArgumentException and nothing is persisted")
    void givenBlankUsernameWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsPersisted() {
        final var input = defaultRegisterUserInput()
                .set(field(RegisterUserInput::username), "  ")
                .create();

        final var thrown = catchThrowable(() -> useCase.execute(input));

        verifyGivenBlankUsernameWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsPersisted(thrown);
    }

    @Test
    @DisplayName("Given a blank password When execute is called Then it throws IllegalArgumentException and nothing is persisted")
    void givenBlankPasswordWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsPersisted() {
        final var input = defaultRegisterUserInput()
                .set(field(RegisterUserInput::password), "  ")
                .create();

        final var thrown = catchThrowable(() -> useCase.execute(input));

        verifyGivenBlankPasswordWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsPersisted(thrown);
    }

    @Test
    @DisplayName("Given a null input When execute is called Then it throws IllegalArgumentException and nothing is persisted")
    void givenNullInputWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsPersisted() {
        final var thrown = catchThrowable(() -> useCase.execute(null));

        verifyGivenNullInputWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsPersisted(thrown);
    }

    private void setupGivenValidInputWhenExecuteIsCalledThenAccountIsSavedAsPendingAndReturnedWithoutPassword(
            final UserAccount saved) {
        when(userAccountRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(passwordEncoderGateway.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(saved);
    }

    private void verifyGivenValidInputWhenExecuteIsCalledThenAccountIsSavedAsPendingAndReturnedWithoutPassword(
            final UserAccountOutput actual, final UserAccount saved) {
        final var captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ROLE_USER);
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.PENDING);

        assertThat(actual).isEqualTo(new UserAccountOutput(
                saved.getId().uuid().toString(), saved.getUsername(), saved.getStatus().name()));
    }

    private void setupGivenValidInputWhenExecuteIsCalledThenRawPasswordIsEncodedBeforeBeingPersisted(
            final UserAccount saved) {
        when(userAccountRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(passwordEncoderGateway.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(saved);
    }

    private void verifyGivenValidInputWhenExecuteIsCalledThenRawPasswordIsEncodedBeforeBeingPersisted() {
        verify(passwordEncoderGateway, times(1)).encode(RAW_PASSWORD);

        final var captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEncodedPassword())
                .isEqualTo(ENCODED_PASSWORD)
                .isNotEqualTo(RAW_PASSWORD);
    }

    private void setupGivenInputWithUppercaseUsernameWhenExecuteIsCalledThenUsernameIsNormalizedToLowercase(
            final UserAccount saved) {
        when(userAccountRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(passwordEncoderGateway.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(saved);
    }

    private void verifyGivenInputWithUppercaseUsernameWhenExecuteIsCalledThenUsernameIsNormalizedToLowercase() {
        verify(userAccountRepository, times(1)).existsByUsername(USERNAME);

        final var captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo(USERNAME);
    }

    private void setupGivenTakenUsernameWhenExecuteIsCalledThenItThrowsUsernameAlreadyTakenExceptionAndNothingIsPersisted() {
        when(userAccountRepository.existsByUsername(USERNAME)).thenReturn(true);
    }

    private void verifyGivenTakenUsernameWhenExecuteIsCalledThenItThrowsUsernameAlreadyTakenExceptionAndNothingIsPersisted(
            final Throwable actual) {
        assertThat(actual).isInstanceOf(UsernameAlreadyTakenException.class);
        assertNothingPersisted();
    }

    private void setupGivenUsernameTakenOnlyInDifferentCaseWhenExecuteIsCalledThenItThrowsUsernameAlreadyTakenException() {
        when(userAccountRepository.existsByUsername(USERNAME)).thenReturn(true);
    }

    private void verifyGivenUsernameTakenOnlyInDifferentCaseWhenExecuteIsCalledThenItThrowsUsernameAlreadyTakenException(
            final Throwable actual) {
        assertThat(actual).isInstanceOf(UsernameAlreadyTakenException.class);
        verify(userAccountRepository, times(1)).existsByUsername(USERNAME);
        assertNothingPersisted();
    }

    private void verifyGivenBlankUsernameWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsPersisted(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username must not be blank");
        assertNothingPersisted();
    }

    private void verifyGivenBlankPasswordWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsPersisted(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must not be blank");
        assertNothingPersisted();
    }

    private void verifyGivenNullInputWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsPersisted(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Register user input must not be null");
        assertNothingPersisted();
    }

    private void assertNothingPersisted() {
        verify(userAccountRepository, never()).save(any(UserAccount.class));
        verifyNoInteractions(passwordEncoderGateway);
    }
}
