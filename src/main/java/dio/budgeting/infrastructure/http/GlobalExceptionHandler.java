package dio.budgeting.infrastructure.http;

import dio.budgeting.auth.domain.UserAccountNotFoundException;
import dio.budgeting.auth.domain.UserAlreadyDecidedException;
import dio.budgeting.auth.domain.UsernameAlreadyTakenException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return errors;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(IllegalArgumentException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleUsernameAlreadyTakenException(UsernameAlreadyTakenException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(UserAlreadyDecidedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleUserAlreadyDecidedException(UserAlreadyDecidedException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(UserAccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleUserAccountNotFoundException(UserAccountNotFoundException ex) {
        return ex.getMessage();
    }
}
