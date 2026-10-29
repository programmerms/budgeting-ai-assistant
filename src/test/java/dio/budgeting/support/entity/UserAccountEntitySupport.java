package dio.budgeting.support.entity;

import dio.budgeting.auth.domain.UserRole;
import dio.budgeting.auth.domain.UserStatus;
import dio.budgeting.auth.infrastructure.persistence.entity.UserAccountEntity;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import static dio.budgeting.support.TestConstants.ENCODED_PASSWORD;
import static dio.budgeting.support.TestConstants.USERNAME;
import static org.instancio.Select.field;

public final class UserAccountEntitySupport {

    private UserAccountEntitySupport() {
    }

    public static InstancioApi<UserAccountEntity> defaultUserAccountEntity() {
        return Instancio.of(UserAccountEntity.class)
                .set(field(UserAccountEntity::getUsername), USERNAME)
                .set(field(UserAccountEntity::getPassword), ENCODED_PASSWORD)
                .set(field(UserAccountEntity::getRole), UserRole.ROLE_USER)
                .set(field(UserAccountEntity::getStatus), UserStatus.PENDING);
    }
}
