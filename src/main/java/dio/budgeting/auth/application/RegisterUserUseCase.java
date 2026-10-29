package dio.budgeting.auth.application;

import dio.budgeting.auth.application.input.RegisterUserInput;
import dio.budgeting.auth.application.output.UserAccountOutput;
import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountRepository;
import dio.budgeting.auth.domain.UsernameAlreadyTakenException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class RegisterUserUseCase {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoderGateway passwordEncoderGateway;

    public RegisterUserUseCase(UserAccountRepository userAccountRepository,
                               PasswordEncoderGateway passwordEncoderGateway) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoderGateway = passwordEncoderGateway;
    }

    public UserAccountOutput execute(RegisterUserInput input) {
        Assert.notNull(input, "Register user input must not be null");
        Assert.hasText(input.password(), "Password must not be blank");

        var username = UserAccount.normalizeUsername(input.username());

        if (userAccountRepository.existsByUsername(username)) {
            throw new UsernameAlreadyTakenException(username);
        }

        var encodedPassword = passwordEncoderGateway.encode(input.password());
        var saved = userAccountRepository.save(UserAccount.register(username, encodedPassword));

        return UserAccountOutput.from(saved);
    }
}
