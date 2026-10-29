package dio.budgeting.auth.application;

import dio.budgeting.auth.application.output.UserAccountOutput;
import dio.budgeting.auth.domain.UserAccountRepository;
import dio.budgeting.auth.domain.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Service
public class ListUsersByStatusUseCase {
    private final UserAccountRepository userAccountRepository;

    public ListUsersByStatusUseCase(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public List<UserAccountOutput> execute(UserStatus status) {
        Assert.notNull(status, "Status must not be null");

        return userAccountRepository.findAllByStatus(status)
                .stream()
                .map(UserAccountOutput::from)
                .toList();
    }
}
