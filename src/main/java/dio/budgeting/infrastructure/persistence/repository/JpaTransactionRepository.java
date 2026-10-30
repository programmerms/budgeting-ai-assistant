package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.OwnerId;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class JpaTransactionRepository implements TransactionRepository {
    private final TransactionEntityRepository transactionEntityRepository;

    public JpaTransactionRepository(TransactionEntityRepository transactionEntityRepository) {
        this.transactionEntityRepository = transactionEntityRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        var entity = TransactionEntity.from(transaction);
        return transactionEntityRepository.save(entity).toDomain();
    }

    @Override
    public List<Transaction> findAllByOwnerIdAndCategory(OwnerId ownerId, Category category) {
        return transactionEntityRepository.findAllByOwnerIdAndCategory(ownerId.uuid(), category)
                .stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }

    @Override
    public List<Transaction> findAllByOwnerIdAndCreatedAtBetween(OwnerId ownerId, Instant from, Instant to) {
        return transactionEntityRepository.findAllByOwnerIdAndCreatedAtBetween(ownerId.uuid(), from, to)
                .stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }
}
