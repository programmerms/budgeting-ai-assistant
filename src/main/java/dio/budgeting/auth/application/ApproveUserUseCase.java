package dio.budgeting.auth.application;

import dio.budgeting.auth.application.output.UserAccountOutput;
import dio.budgeting.auth.domain.UserAccountId;
import dio.budgeting.auth.domain.UserAccountNotFoundException;
import dio.budgeting.auth.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class ApproveUserUseCase {
    private final UserAccountRepository userAccountRepository;

    public ApproveUserUseCase(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccountOutput execute(UserAccountId id) {
        Assert.notNull(id, "User account ID must not be null");

        var userAccount = userAccountRepository.findById(id)
                .orElseThrow(() -> new UserAccountNotFoundException(id));

        userAccount.approve();

        return UserAccountOutput.from(userAccountRepository.save(userAccount));
    }
}
