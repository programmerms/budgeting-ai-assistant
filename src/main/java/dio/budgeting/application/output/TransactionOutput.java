package dio.budgeting.application.output;

import dio.budgeting.domain.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionOutput(String id, String description, String category, BigDecimal value, Instant createdAt) {
    public static TransactionOutput from(Transaction transaction) {
        return new TransactionOutput(
                transaction.getId().uuid().toString(),
                transaction.getDescription(),
                transaction.getCategory().name(),
                BigDecimal.valueOf(transaction.getAmount(), 2),
                transaction.getCreatedAt());
    }
}
