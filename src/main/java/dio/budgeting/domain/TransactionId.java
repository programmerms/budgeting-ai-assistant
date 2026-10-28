package dio.budgeting.domain;

import org.springframework.util.Assert;

import java.util.UUID;

public record TransactionId(UUID uuid) {
    public TransactionId {
        Assert.notNull(uuid, "Transaction ID must not be null");
    }

    public TransactionId() {
        this(UUID.randomUUID());
    }
}
