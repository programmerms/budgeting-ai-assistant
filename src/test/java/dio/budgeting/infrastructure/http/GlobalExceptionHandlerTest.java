package dio.budgeting.infrastructure.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

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
