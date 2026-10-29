package dio.budgeting.auth.infrastructure.http;

import dio.budgeting.auth.application.ApproveUserUseCase;
import dio.budgeting.auth.application.ListUsersByStatusUseCase;
import dio.budgeting.auth.application.RejectUserUseCase;
import dio.budgeting.auth.application.output.UserAccountOutput;
import dio.budgeting.auth.domain.UserAccountId;
import dio.budgeting.auth.domain.UserStatus;
import dio.budgeting.auth.infrastructure.http.response.UserAccountResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static dio.budgeting.support.TestConstants.USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ListUsersByStatusUseCase listUsersByStatusUseCase;

    @Mock
    private ApproveUserUseCase approveUserUseCase;

    @Mock
    private RejectUserUseCase rejectUserUseCase;

    @InjectMocks
    private AdminUserController controller;

    @Test
    @DisplayName("Given a status When readUsersByStatus is called Then it delegates to ListUsersByStatusUseCase and returns the responses")
    void givenStatusWhenReadUsersByStatusIsCalledThenItDelegatesToUseCaseAndReturnsResponses() {
        final var output = outputWith(UserStatus.PENDING);
        setupGivenStatusWhenReadUsersByStatusIsCalledThenItDelegatesToUseCaseAndReturnsResponses(output);

        final var result = controller.readUsersByStatus(UserStatus.PENDING);

        verifyGivenStatusWhenReadUsersByStatusIsCalledThenItDelegatesToUseCaseAndReturnsResponses(result, output);
    }

    @Test
    @DisplayName("Given no account in the requested status When readUsersByStatus is called Then it returns an empty list")
    void givenNoAccountInTheRequestedStatusWhenReadUsersByStatusIsCalledThenItReturnsAnEmptyList() {
        setupGivenNoAccountInTheRequestedStatusWhenReadUsersByStatusIsCalledThenItReturnsAnEmptyList();

        final var result = controller.readUsersByStatus(UserStatus.REJECTED);

        verifyGivenNoAccountInTheRequestedStatusWhenReadUsersByStatusIsCalledThenItReturnsAnEmptyList(result);
    }

    @Test
    @DisplayName("Given an account identifier When approveUser is called Then it delegates to ApproveUserUseCase and returns the response")
    void givenAccountIdentifierWhenApproveUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse() {
        final var output = outputWith(UserStatus.ACTIVE);
        setupGivenAccountIdentifierWhenApproveUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse(output);

        final var result = controller.approveUser(USER_ID);

        verifyGivenAccountIdentifierWhenApproveUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse(result, output);
    }

    @Test
    @DisplayName("Given an account identifier When rejectUser is called Then it delegates to RejectUserUseCase and returns the response")
    void givenAccountIdentifierWhenRejectUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse() {
        final var output = outputWith(UserStatus.REJECTED);
        setupGivenAccountIdentifierWhenRejectUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse(output);

        final var result = controller.rejectUser(USER_ID);

        verifyGivenAccountIdentifierWhenRejectUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse(result, output);
    }

    private UserAccountOutput outputWith(final UserStatus status) {
        return new UserAccountOutput(USER_ID.toString(), USERNAME, status.name());
    }

    private void setupGivenStatusWhenReadUsersByStatusIsCalledThenItDelegatesToUseCaseAndReturnsResponses(
            final UserAccountOutput output) {
        when(listUsersByStatusUseCase.execute(UserStatus.PENDING)).thenReturn(List.of(output));
    }

    private void verifyGivenStatusWhenReadUsersByStatusIsCalledThenItDelegatesToUseCaseAndReturnsResponses(
            final List<UserAccountResponse> actual, final UserAccountOutput expected) {
        verify(listUsersByStatusUseCase, times(1)).execute(UserStatus.PENDING);
        assertThat(actual).containsExactly(
                new UserAccountResponse(expected.id(), expected.username(), expected.status()));
    }

    private void setupGivenNoAccountInTheRequestedStatusWhenReadUsersByStatusIsCalledThenItReturnsAnEmptyList() {
        when(listUsersByStatusUseCase.execute(UserStatus.REJECTED)).thenReturn(List.of());
    }

    private void verifyGivenNoAccountInTheRequestedStatusWhenReadUsersByStatusIsCalledThenItReturnsAnEmptyList(
            final List<UserAccountResponse> actual) {
        verify(listUsersByStatusUseCase, times(1)).execute(UserStatus.REJECTED);
        assertThat(actual).isEmpty();
    }

    private void setupGivenAccountIdentifierWhenApproveUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse(
            final UserAccountOutput output) {
        when(approveUserUseCase.execute(new UserAccountId(USER_ID))).thenReturn(output);
    }

    private void verifyGivenAccountIdentifierWhenApproveUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse(
            final UserAccountResponse actual, final UserAccountOutput expected) {
        verify(approveUserUseCase, times(1)).execute(new UserAccountId(USER_ID));
        assertThat(actual).isEqualTo(
                new UserAccountResponse(expected.id(), expected.username(), expected.status()));
    }

    private void setupGivenAccountIdentifierWhenRejectUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse(
            final UserAccountOutput output) {
        when(rejectUserUseCase.execute(new UserAccountId(USER_ID))).thenReturn(output);
    }

    private void verifyGivenAccountIdentifierWhenRejectUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse(
            final UserAccountResponse actual, final UserAccountOutput expected) {
        verify(rejectUserUseCase, times(1)).execute(new UserAccountId(USER_ID));
        assertThat(actual).isEqualTo(
                new UserAccountResponse(expected.id(), expected.username(), expected.status()));
    }
}
