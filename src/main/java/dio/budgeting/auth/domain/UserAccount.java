package dio.budgeting.auth.domain;

import lombok.Getter;
import org.springframework.util.Assert;

import java.util.Locale;

@Getter
public class UserAccount {
    private UserAccountId id;
    private String username;
    private String encodedPassword;
    private UserRole role;
    private UserStatus status;

    public UserAccount(UserAccountId id, String username, String encodedPassword, UserRole role, UserStatus status) {
        Assert.notNull(id, "User account ID must not be null");
        Assert.hasText(encodedPassword, "Encoded password must not be blank");
        Assert.notNull(role, "Role must not be null");
        Assert.notNull(status, "Status must not be null");

        this.id = id;
        this.username = normalizeUsername(username);
        this.encodedPassword = encodedPassword;
        this.role = role;
        this.status = status;
    }

    public static UserAccount register(String username, String encodedPassword) {
        return new UserAccount(new UserAccountId(), username, encodedPassword, UserRole.ROLE_USER, UserStatus.PENDING);
    }

    public static UserAccount administrator(String username, String encodedPassword) {
        return new UserAccount(new UserAccountId(), username, encodedPassword, UserRole.ROLE_ADMIN, UserStatus.ACTIVE);
    }

    public static String normalizeUsername(String username) {
        Assert.hasText(username, "Username must not be blank");
        return username.trim().toLowerCase(Locale.ROOT);
    }

    public void approve() {
        ensurePending();
        this.status = UserStatus.ACTIVE;
    }

    public void reject() {
        ensurePending();
        this.status = UserStatus.REJECTED;
    }

    private void ensurePending() {
        if (this.status != UserStatus.PENDING) {
            throw new UserAlreadyDecidedException(this.id, this.status);
        }
    }
}
