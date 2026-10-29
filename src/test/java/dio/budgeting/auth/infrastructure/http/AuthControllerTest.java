package dio.budgeting.auth.infrastructure.http;

import dio.budgeting.auth.application.RegisterUserUseCase;
import dio.budgeting.auth.application.input.RegisterUserInput;
import dio.budgeting.auth.application.output.UserAccountOutput;
import dio.budgeting.auth.domain.UserStatus;
import dio.budgeting.auth.infrastructure.http.request.RegisterUserRequest;
import dio.budgeting.auth.infrastructure.http.response.UserAccountResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static dio.budgeting.support.TestConstants.RAW_PASSWORD;
import static dio.budgeting.support.TestConstants.USERNAME;
import static dio.budgeting.support.request.RegisterUserRequestSupport.defaultRegisterUserRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @InjectMocks
    private AuthController controller;

    @Test
    @DisplayName("Given a register request When registerUser is called Then it delegates to RegisterUserUseCase and returns the response")
    void givenRegisterRequestWhenRegisterUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse() {
        final var request = defaultRegisterUserRequest().create();
        final var output = pendingOutput();
        setupGivenRegisterRequestWhenRegisterUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse(output);

        final var result = controller.registerUser(request);

        verifyGivenRegisterRequestWhenRegisterUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse(result, output);
    }

    @Test
    @DisplayName("Given a register request When registerUser is called Then the use case receives the raw request values")
    void givenRegisterRequestWhenRegisterUserIsCalledThenTheUseCaseReceivesTheRawRequestValues() {
        final var request = defaultRegisterUserRequest().create();
        setupGivenRegisterRequestWhenRegisterUserIsCalledThenTheUseCaseReceivesTheRawRequestValues();

        controller.registerUser(request);

        verifyGivenRegisterRequestWhenRegisterUserIsCalledThenTheUseCaseReceivesTheRawRequestValues();
    }

    private UserAccountOutput pendingOutput() {
        return new UserAccountOutput(UUID.randomUUID().toString(), USERNAME, UserStatus.PENDING.name());
    }

    private void setupGivenRegisterRequestWhenRegisterUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse(
            final UserAccountOutput output) {
        when(registerUserUseCase.execute(new RegisterUserInput(USERNAME, RAW_PASSWORD))).thenReturn(output);
    }

    private void verifyGivenRegisterRequestWhenRegisterUserIsCalledThenItDelegatesToUseCaseAndReturnsResponse(
            final UserAccountResponse actual, final UserAccountOutput expected) {
        verify(registerUserUseCase, times(1)).execute(new RegisterUserInput(USERNAME, RAW_PASSWORD));
        assertThat(actual).isEqualTo(
                new UserAccountResponse(expected.id(), expected.username(), expected.status()));
    }

    private void setupGivenRegisterRequestWhenRegisterUserIsCalledThenTheUseCaseReceivesTheRawRequestValues() {
        when(registerUserUseCase.execute(new RegisterUserInput(USERNAME, RAW_PASSWORD))).thenReturn(pendingOutput());
    }

    private void verifyGivenRegisterRequestWhenRegisterUserIsCalledThenTheUseCaseReceivesTheRawRequestValues() {
        verify(registerUserUseCase, times(1)).execute(new RegisterUserInput(USERNAME, RAW_PASSWORD));
    }

    @Test
    @DisplayName("Given a register request When converted to input Then the request maps username and password")
    void givenRegisterRequestWhenConvertedToInputThenTheRequestMapsUsernameAndPassword() {
        final var request = new RegisterUserRequest(USERNAME, RAW_PASSWORD);

        final var actual = request.toInput();

        verifyGivenRegisterRequestWhenConvertedToInputThenTheRequestMapsUsernameAndPassword(actual);
    }

    private void verifyGivenRegisterRequestWhenConvertedToInputThenTheRequestMapsUsernameAndPassword(
            final RegisterUserInput actual) {
        assertThat(actual).isEqualTo(new RegisterUserInput(USERNAME, RAW_PASSWORD));
    }
}
