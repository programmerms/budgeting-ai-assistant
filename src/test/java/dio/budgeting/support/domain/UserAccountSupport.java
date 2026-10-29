package dio.budgeting.support.domain;

import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserRole;
import dio.budgeting.auth.domain.UserStatus;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import static dio.budgeting.support.TestConstants.ENCODED_PASSWORD;
import static dio.budgeting.support.TestConstants.USERNAME;
import static org.instancio.Select.field;

public final class UserAccountSupport {

    private UserAccountSupport() {
    }

    public static InstancioApi<UserAccount> defaultUserAccount() {
        return Instancio.of(UserAccount.class)
                .set(field(UserAccount::getUsername), USERNAME)
                .set(field(UserAccount::getEncodedPassword), ENCODED_PASSWORD)
                .set(field(UserAccount::getRole), UserRole.ROLE_USER)
                .set(field(UserAccount::getStatus), UserStatus.PENDING);
    }
}
