package dio.budgeting.application;

import dio.budgeting.application.output.CategorySummaryOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;

@Service
public class SummarizeTransactionsByCategoryUseCase {
    private final TransactionRepository transactionRepository;

    public SummarizeTransactionsByCategoryUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public CategorySummaryOutput execute(Category category) {
        Assert.notNull(category, "Category must not be null");

        var transactions = transactionRepository.findAllByCategory(category);
        long totalInCents = transactions.stream().mapToLong(Transaction::getAmount).sum();

        return new CategorySummaryOutput(
                category.name(),
                BigDecimal.valueOf(totalInCents, 2),
                transactions.size());
    }
}
