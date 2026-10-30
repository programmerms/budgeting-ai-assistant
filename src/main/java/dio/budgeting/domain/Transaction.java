package dio.budgeting.domain;

import lombok.Getter;
import org.springframework.util.Assert;

import java.time.Instant;

@Getter
public class Transaction {
    private OwnerId ownerId;
    private TransactionId id;
    private String description;
    private long amount;
    private Category category;
    private Instant createdAt;

    public Transaction(OwnerId ownerId, TransactionId id, String description, long amount,
                       Category category, Instant createdAt) {
        Assert.notNull(ownerId, "Owner ID must not be null");
        Assert.notNull(id, "Transaction ID must not be null");
        Assert.hasText(description, "Description must not be blank");
        Assert.isTrue(amount > 0, "Amount must be greater than zero");
        Assert.notNull(category, "Category must not be null");

        this.ownerId = ownerId;
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.createdAt = createdAt;
    }

    public Transaction(OwnerId ownerId, TransactionId id, String description, long amount, Category category) {
        this(ownerId, id, description, amount, category, null);
    }

    public Transaction(OwnerId ownerId, String description, long amount, Category category) {
        this(ownerId, new TransactionId(), description, amount, category, null);
    }
}
