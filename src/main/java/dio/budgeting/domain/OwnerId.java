package dio.budgeting.domain;

import org.springframework.util.Assert;

import java.util.UUID;

public record OwnerId(UUID uuid) {
    public OwnerId {
        Assert.notNull(uuid, "Owner ID must not be null");
    }

    public OwnerId() {
        this(UUID.randomUUID());
    }
}
