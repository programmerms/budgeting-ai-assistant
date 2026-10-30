package dio.budgeting.domain;

import java.time.Instant;
import java.util.List;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    List<Transaction> findAllByOwnerIdAndCategory(OwnerId ownerId, Category category);

    List<Transaction> findAllByOwnerIdAndCreatedAtBetween(OwnerId ownerId, Instant from, Instant to);
}
