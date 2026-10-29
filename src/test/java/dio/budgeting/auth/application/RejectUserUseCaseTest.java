package dio.budgeting.auth.application;

import dio.budgeting.auth.application.output.UserAccountOutput;
import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountId;
import dio.budgeting.auth.domain.UserAccountNotFoundException;
import dio.budgeting.auth.domain.UserAccountRepository;
import dio.budgeting.auth.domain.UserAlreadyDecidedException;
import dio.budgeting.auth.domain.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static dio.budgeting.support.domain.UserAccountSupport.defaultUserAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RejectUserUseCaseTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private RejectUserUseCase useCase;

    @Test
    @DisplayName("Given a pending account When execute is called Then the account becomes rejected and is saved")
    void givenPendingAccountWhenExecuteIsCalledThenAccountBecomesRejectedAndIsSaved() {
        final var account = defaultUserAccount().create();
        setupGivenPendingAccountWhenExecuteIsCalledThenAccountBecomesRejectedAndIsSaved(account);

        final var result = useCase.execute(account.getId());

        verifyGivenPendingAccountWhenExecuteIsCalledThenAccountBecomesRejectedAndIsSaved(result, account);
    }

    @Test
    @DisplayName("Given an unknown identifier When execute is called Then it throws UserAccountNotFoundException and nothing is saved")
    void givenUnknownIdentifierWhenExecuteIsCalledThenItThrowsUserAccountNotFoundExceptionAndNothingIsSaved() {
        final var id = new UserAccountId();
        setupGivenUnknownIdentifierWhenExecuteIsCalledThenItThrowsUserAccountNotFoundExceptionAndNothingIsSaved(id);

        final var thrown = catchThrowable(() -> useCase.execute(id));

        verifyGivenUnknownIdentifierWhenExecuteIsCalledThenItThrowsUserAccountNotFoundExceptionAndNothingIsSaved(
                thrown);
    }

    @Test
    @DisplayName("Given an already rejected account When execute is called Then it throws UserAlreadyDecidedException and nothing is saved")
    void givenAlreadyRejectedAccountWhenExecuteIsCalledThenItThrowsUserAlreadyDecidedExceptionAndNothingIsSaved() {
        final var account = defaultUserAccount()
                .set(field(UserAccount::getStatus), UserStatus.REJECTED)
                .create();
        setupGivenAlreadyRejectedAccountWhenExecuteIsCalledThenItThrowsUserAlreadyDecidedExceptionAndNothingIsSaved(
                account);

        final var thrown = catchThrowable(() -> useCase.execute(account.getId()));

        verifyGivenAlreadyRejectedAccountWhenExecuteIsCalledThenItThrowsUserAlreadyDecidedExceptionAndNothingIsSaved(
                thrown, account);
    }

    @Test
    @DisplayName("Given a null identifier When execute is called Then it throws IllegalArgumentException and nothing is saved")
    void givenNullIdentifierWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved() {
        final var thrown = catchThrowable(() -> useCase.execute(null));

        verifyGivenNullIdentifierWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved(thrown);
    }

    private void setupGivenPendingAccountWhenExecuteIsCalledThenAccountBecomesRejectedAndIsSaved(
            final UserAccount account) {
        when(userAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(call -> call.getArgument(0));
    }

    private void verifyGivenPendingAccountWhenExecuteIsCalledThenAccountBecomesRejectedAndIsSaved(
            final UserAccountOutput actual, final UserAccount account) {
        final var captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.REJECTED);

        assertThat(actual).isEqualTo(new UserAccountOutput(
                account.getId().uuid().toString(), account.getUsername(), UserStatus.REJECTED.name()));
    }

    private void setupGivenUnknownIdentifierWhenExecuteIsCalledThenItThrowsUserAccountNotFoundExceptionAndNothingIsSaved(
            final UserAccountId id) {
        when(userAccountRepository.findById(id)).thenReturn(Optional.empty());
    }

    private void verifyGivenUnknownIdentifierWhenExecuteIsCalledThenItThrowsUserAccountNotFoundExceptionAndNothingIsSaved(
            final Throwable actual) {
        assertThat(actual).isInstanceOf(UserAccountNotFoundException.class);
        assertNothingSaved();
    }

    private void setupGivenAlreadyRejectedAccountWhenExecuteIsCalledThenItThrowsUserAlreadyDecidedExceptionAndNothingIsSaved(
            final UserAccount account) {
        when(userAccountRepository.findById(account.getId())).thenReturn(Optional.of(account));
    }

    private void verifyGivenAlreadyRejectedAccountWhenExecuteIsCalledThenItThrowsUserAlreadyDecidedExceptionAndNothingIsSaved(
            final Throwable actual, final UserAccount account) {
        assertThat(actual).isInstanceOf(UserAlreadyDecidedException.class);
        assertThat(account.getStatus()).isEqualTo(UserStatus.REJECTED);
        assertNothingSaved();
    }

    private void verifyGivenNullIdentifierWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndNothingIsSaved(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User account ID must not be null");
        assertNothingSaved();
    }

    private void assertNothingSaved() {
        verify(userAccountRepository, never()).save(any(UserAccount.class));
    }
}
