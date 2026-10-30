package dio.budgeting.support;

import dio.budgeting.auth.domain.UserAccountId;
import dio.budgeting.auth.domain.UserRole;
import dio.budgeting.auth.domain.UserStatus;
import dio.budgeting.auth.infrastructure.security.AuthenticatedUser;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import static dio.budgeting.support.TestConstants.ENCODED_PASSWORD;
import static dio.budgeting.support.TestConstants.OWNER_UUID;
import static dio.budgeting.support.TestConstants.USERNAME;
import static org.instancio.Select.field;

public final class AuthenticatedUserSupport {

    private AuthenticatedUserSupport() {
    }

    public static InstancioApi<AuthenticatedUser> defaultAuthenticatedUser() {
        return Instancio.of(AuthenticatedUser.class)
                .set(field(AuthenticatedUser::id), new UserAccountId(OWNER_UUID))
                .set(field(AuthenticatedUser::username), USERNAME)
                .set(field(AuthenticatedUser::encodedPassword), ENCODED_PASSWORD)
                .set(field(AuthenticatedUser::role), UserRole.ROLE_USER)
                .set(field(AuthenticatedUser::status), UserStatus.ACTIVE);
    }
}
