package dio.budgeting.auth.infrastructure.persistence.repository;

import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountId;
import dio.budgeting.auth.domain.UserAccountRepository;
import dio.budgeting.auth.domain.UserRole;
import dio.budgeting.auth.domain.UserStatus;
import dio.budgeting.auth.infrastructure.persistence.entity.UserAccountEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaUserAccountRepository implements UserAccountRepository {
    private final UserAccountEntityRepository userAccountEntityRepository;

    public JpaUserAccountRepository(UserAccountEntityRepository userAccountEntityRepository) {
        this.userAccountEntityRepository = userAccountEntityRepository;
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        var entity = UserAccountEntity.from(userAccount);
        return userAccountEntityRepository.save(entity).toDomain();
    }

    @Override
    public Optional<UserAccount> findById(UserAccountId id) {
        return userAccountEntityRepository.findById(id.uuid()).map(UserAccountEntity::toDomain);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return userAccountEntityRepository.findByUsername(username).map(UserAccountEntity::toDomain);
    }

    @Override
    public List<UserAccount> findAllByStatus(UserStatus status) {
        return userAccountEntityRepository.findAllByStatus(status)
                .stream()
                .map(UserAccountEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByUsername(String username) {
        return userAccountEntityRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByRole(UserRole role) {
        return userAccountEntityRepository.existsByRole(role);
    }
}
