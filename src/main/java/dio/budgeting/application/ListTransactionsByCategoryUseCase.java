package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.OwnerId;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Service
public class ListTransactionsByCategoryUseCase {
    private final TransactionRepository transactionRepository;

    public ListTransactionsByCategoryUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<TransactionOutput> execute(OwnerId ownerId, Category category) {
        Assert.notNull(ownerId, "Owner ID must not be null");
        return transactionRepository.findAllByOwnerIdAndCategory(ownerId, category)
                .stream()
                .map(TransactionOutput::from)
                .toList();
    }
}
