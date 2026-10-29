package dio.budgeting.auth.infrastructure.security;

import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountId;
import dio.budgeting.auth.domain.UserRole;
import dio.budgeting.auth.domain.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record AuthenticatedUser(UserAccountId id,
                                String username,
                                String encodedPassword,
                                UserRole role,
                                UserStatus status) implements UserDetails {

    public static AuthenticatedUser from(UserAccount userAccount) {
        return new AuthenticatedUser(
                userAccount.getId(),
                userAccount.getUsername(),
                userAccount.getEncodedPassword(),
                userAccount.getRole(),
                userAccount.getStatus());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return encodedPassword;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
