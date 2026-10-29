package dio.budgeting.auth.infrastructure.persistence.repository;

import dio.budgeting.auth.domain.UserRole;
import dio.budgeting.auth.domain.UserStatus;
import dio.budgeting.auth.infrastructure.persistence.entity.UserAccountEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountEntityRepository extends CrudRepository<UserAccountEntity, UUID> {
    Optional<UserAccountEntity> findByUsername(String username);

    List<UserAccountEntity> findAllByStatus(UserStatus status);

    boolean existsByUsername(String username);

    boolean existsByRole(UserRole role);
}
