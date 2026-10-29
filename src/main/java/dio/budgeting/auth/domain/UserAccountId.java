package dio.budgeting.auth.domain;

import org.springframework.util.Assert;

import java.util.UUID;

public record UserAccountId(UUID uuid) {
    public UserAccountId {
        Assert.notNull(uuid, "User account ID must not be null");
    }

    public UserAccountId() {
        this(UUID.randomUUID());
    }
}
