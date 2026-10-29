package dio.budgeting.auth.application;

import dio.budgeting.auth.application.output.UserAccountOutput;
import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountRepository;
import dio.budgeting.auth.domain.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static dio.budgeting.support.domain.UserAccountSupport.defaultUserAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUsersByStatusUseCaseTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private ListUsersByStatusUseCase useCase;

    @Test
    @DisplayName("Given accounts in the status When execute is called Then they are returned without the password")
    void givenAccountsInTheStatusWhenExecuteIsCalledThenTheyAreReturnedWithoutThePassword() {
        final var account = defaultUserAccount().create();
        setupGivenAccountsInTheStatusWhenExecuteIsCalledThenTheyAreReturnedWithoutThePassword(account);

        final var result = useCase.execute(UserStatus.PENDING);

        verifyGivenAccountsInTheStatusWhenExecuteIsCalledThenTheyAreReturnedWithoutThePassword(result, account);
    }

    @Test
    @DisplayName("Given no account in the status When execute is called Then an empty list is returned")
    void givenNoAccountInTheStatusWhenExecuteIsCalledThenAnEmptyListIsReturned() {
        setupGivenNoAccountInTheStatusWhenExecuteIsCalledThenAnEmptyListIsReturned();

        final var result = useCase.execute(UserStatus.REJECTED);

        verifyGivenNoAccountInTheStatusWhenExecuteIsCalledThenAnEmptyListIsReturned(result);
    }

    @Test
    @DisplayName("Given a null status When execute is called Then it throws IllegalArgumentException and the repository is not touched")
    void givenNullStatusWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndRepositoryIsNotTouched() {
        final var thrown = catchThrowable(() -> useCase.execute(null));

        verifyGivenNullStatusWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndRepositoryIsNotTouched(thrown);
    }

    private void setupGivenAccountsInTheStatusWhenExecuteIsCalledThenTheyAreReturnedWithoutThePassword(
            final UserAccount account) {
        when(userAccountRepository.findAllByStatus(UserStatus.PENDING)).thenReturn(List.of(account));
    }

    private void verifyGivenAccountsInTheStatusWhenExecuteIsCalledThenTheyAreReturnedWithoutThePassword(
            final List<UserAccountOutput> actual, final UserAccount account) {
        verify(userAccountRepository, times(1)).findAllByStatus(UserStatus.PENDING);
        assertThat(actual).containsExactly(new UserAccountOutput(
                account.getId().uuid().toString(), account.getUsername(), account.getStatus().name()));
    }

    private void setupGivenNoAccountInTheStatusWhenExecuteIsCalledThenAnEmptyListIsReturned() {
        when(userAccountRepository.findAllByStatus(UserStatus.REJECTED)).thenReturn(List.of());
    }

    private void verifyGivenNoAccountInTheStatusWhenExecuteIsCalledThenAnEmptyListIsReturned(
            final List<UserAccountOutput> actual) {
        assertThat(actual).isEmpty();
    }

    private void verifyGivenNullStatusWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndRepositoryIsNotTouched(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Status must not be null");
        verifyNoInteractions(userAccountRepository);
    }
}
