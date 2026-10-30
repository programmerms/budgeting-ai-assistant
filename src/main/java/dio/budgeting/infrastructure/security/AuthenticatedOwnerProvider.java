package dio.budgeting.infrastructure.security;

import dio.budgeting.auth.infrastructure.security.AuthenticatedUser;
import dio.budgeting.domain.OwnerId;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedOwnerProvider {

    public OwnerId currentOwnerId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("Authenticated owner is required");
        }

        return new OwnerId(user.id().uuid());
    }
}
