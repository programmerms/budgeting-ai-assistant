package dio.budgeting.auth.domain;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository {
    UserAccount save(UserAccount userAccount);

    Optional<UserAccount> findById(UserAccountId id);

    Optional<UserAccount> findByUsername(String username);

    List<UserAccount> findAllByStatus(UserStatus status);

    boolean existsByUsername(String username);

    boolean existsByRole(UserRole role);
}
