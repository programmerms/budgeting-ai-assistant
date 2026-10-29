package dio.budgeting.auth.infrastructure.http.request;

import dio.budgeting.auth.application.input.RegisterUserInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 8, max = 72) String password) {
    public RegisterUserInput toInput() {
        return new RegisterUserInput(username, password);
    }
}
