package dio.budgeting.auth.infrastructure.http;

import dio.budgeting.auth.application.RegisterUserUseCase;
import dio.budgeting.auth.infrastructure.http.request.RegisterUserRequest;
import dio.budgeting.auth.infrastructure.http.response.UserAccountResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final RegisterUserUseCase registerUserUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserAccountResponse registerUser(@RequestBody @Valid RegisterUserRequest request) {
        var output = registerUserUseCase.execute(request.toInput());
        return UserAccountResponse.from(output);
    }
}
