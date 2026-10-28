package dio.budgeting.domain;

import lombok.Getter;
import org.springframework.util.Assert;

import java.time.Instant;

@Getter
public class Transaction {
    private TransactionId id;
    private String description;
    private long amount;
    private Category category;
    private Instant createdAt;

    public Transaction(TransactionId id, String description, long amount, Category category, Instant createdAt) {
        Assert.notNull(id, "Transaction ID must not be null");
        Assert.hasText(description, "Description must not be blank");
        Assert.isTrue(amount > 0, "Amount must be greater than zero");
        Assert.notNull(category, "Category must not be null");

        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.createdAt = createdAt;
    }

    public Transaction(TransactionId id, String description, long amount, Category category) {
        this(id, description, amount, category, null);
    }

    public Transaction(String description, long amount, Category category) {
        this(new TransactionId(), description, amount, category, null);
    }
}
