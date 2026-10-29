package dio.budgeting.infrastructure.http;

import dio.budgeting.auth.domain.UserAccountId;
import dio.budgeting.auth.domain.UserAccountNotFoundException;
import dio.budgeting.auth.domain.UserAlreadyDecidedException;
import dio.budgeting.auth.domain.UserStatus;
import dio.budgeting.auth.domain.UsernameAlreadyTakenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static dio.budgeting.support.TestConstants.USERNAME;
import static dio.budgeting.support.request.TransactionRequestSupport.defaultTransactionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Given a MethodArgumentNotValidException with field errors When handleValidationExceptions is called Then it returns a map of field names to messages")
    void givenMethodArgumentNotValidExceptionWithFieldErrorsWhenHandleValidationExceptionsIsCalledThenItReturnsMapOfFieldNamesToMessages() {
        final var exception = mock(MethodArgumentNotValidException.class);
        final var expected = Map.of(
                "description", "must not be blank",
                "amount", "must be greater than 0");
        setupGivenMethodArgumentNotValidExceptionWithFieldErrorsWhenHandleValidationExceptionsIsCalledThenItReturnsMapOfFieldNamesToMessages(
                exception);

        final var actual = handler.handleValidationExceptions(exception);

        verifyGivenMethodArgumentNotValidExceptionWithFieldErrorsWhenHandleValidationExceptionsIsCalledThenItReturnsMapOfFieldNamesToMessages(
                actual, expected);
    }

    @Test
    @DisplayName("Given an IllegalArgumentException When handleIllegalArgumentException is called Then it returns the exception message")
    void givenIllegalArgumentExceptionWhenHandleIllegalArgumentExceptionIsCalledThenItReturnsExceptionMessage() {
        final var expectedMessage = "Audio file must not be empty";
        final var exception = new IllegalArgumentException(expectedMessage);

        final var actual = handler.handleIllegalArgumentException(exception);

        verifyGivenIllegalArgumentExceptionWhenHandleIllegalArgumentExceptionIsCalledThenItReturnsExceptionMessage(
                actual, expectedMessage);
    }

    @Test
    @DisplayName("Given a UsernameAlreadyTakenException When handleUsernameAlreadyTakenException is called Then it returns the exception message")
    void givenUsernameAlreadyTakenExceptionWhenHandleUsernameAlreadyTakenExceptionIsCalledThenItReturnsExceptionMessage() {
        final var exception = new UsernameAlreadyTakenException(USERNAME);

        final var actual = handler.handleUsernameAlreadyTakenException(exception);

        verifyExceptionMessageIsReturned(actual, exception.getMessage());
    }

    @Test
    @DisplayName("Given a UserAlreadyDecidedException When handleUserAlreadyDecidedException is called Then it returns the exception message")
    void givenUserAlreadyDecidedExceptionWhenHandleUserAlreadyDecidedExceptionIsCalledThenItReturnsExceptionMessage() {
        final var exception = new UserAlreadyDecidedException(new UserAccountId(), UserStatus.ACTIVE);

        final var actual = handler.handleUserAlreadyDecidedException(exception);

        verifyExceptionMessageIsReturned(actual, exception.getMessage());
    }

    @Test
    @DisplayName("Given a UserAccountNotFoundException When handleUserAccountNotFoundException is called Then it returns the exception message")
    void givenUserAccountNotFoundExceptionWhenHandleUserAccountNotFoundExceptionIsCalledThenItReturnsExceptionMessage() {
        final var exception = new UserAccountNotFoundException(new UserAccountId());

        final var actual = handler.handleUserAccountNotFoundException(exception);

        verifyExceptionMessageIsReturned(actual, exception.getMessage());
    }

    private void verifyExceptionMessageIsReturned(final String actual, final String expected) {
        assertThat(actual).isEqualTo(expected);
    }

    private void setupGivenMethodArgumentNotValidExceptionWithFieldErrorsWhenHandleValidationExceptionsIsCalledThenItReturnsMapOfFieldNamesToMessages(
            final MethodArgumentNotValidException exception) {
        final var target = defaultTransactionRequest().create();
        final var bindingResult = new BeanPropertyBindingResult(target, "transactionRequest");
        bindingResult.addError(new FieldError("transactionRequest", "description", "must not be blank"));
        bindingResult.addError(new FieldError("transactionRequest", "amount", "must be greater than 0"));
        when(exception.getBindingResult()).thenReturn(bindingResult);
    }

    private void verifyGivenMethodArgumentNotValidExceptionWithFieldErrorsWhenHandleValidationExceptionsIsCalledThenItReturnsMapOfFieldNamesToMessages(
            final Map<String, String> actual, final Map<String, String> expected) {
        assertThat(actual).isEqualTo(expected);
    }

    private void verifyGivenIllegalArgumentExceptionWhenHandleIllegalArgumentExceptionIsCalledThenItReturnsExceptionMessage(
            final String actual, final String expected) {
        assertThat(actual).isEqualTo(expected);
    }
}
