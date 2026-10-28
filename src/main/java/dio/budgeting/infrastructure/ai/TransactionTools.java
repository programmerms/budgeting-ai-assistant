package dio.budgeting.infrastructure.ai;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.ListTransactionsByPeriodUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeTransactionsByCategoryUseCase;
import dio.budgeting.application.output.CategorySummaryOutput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.infrastructure.ai.request.PersistTransactionToolRequest;
import dio.budgeting.infrastructure.ai.request.TransactionsPeriodToolRequest;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionTools {
    private final PersistTransactionUseCase persistTransactionUseCase;
    private final ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;
    private final SummarizeTransactionsByCategoryUseCase summarizeTransactionsByCategoryUseCase;
    private final ListTransactionsByPeriodUseCase listTransactionsByPeriodUseCase;

    public TransactionTools(PersistTransactionUseCase persistTransactionUseCase,
                            ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                            SummarizeTransactionsByCategoryUseCase summarizeTransactionsByCategoryUseCase,
                            ListTransactionsByPeriodUseCase listTransactionsByPeriodUseCase) {
        this.persistTransactionUseCase = persistTransactionUseCase;
        this.listTransactionsByCategoryUseCase = listTransactionsByCategoryUseCase;
        this.summarizeTransactionsByCategoryUseCase = summarizeTransactionsByCategoryUseCase;
        this.listTransactionsByPeriodUseCase = listTransactionsByPeriodUseCase;
    }

    @Tool(name = "persist-transaction", description = "Persiste uma nova transação financeira")
    public TransactionOutput persistTransaction(PersistTransactionToolRequest input) {
        return persistTransactionUseCase.execute(input.toInput());
    }

    @Tool(name = "list-transactions-by-category", description = "Lista transações financeiras por categoria")
    public List<TransactionOutput> listTransactionsByCategory(
            @ToolParam(description = "Categoria de uma transação") Category category) {
        return listTransactionsByCategoryUseCase.execute(category);
    }

    @Tool(name = "summarize-transactions-by-category", description = "Resume o total gasto e a quantidade de transações de uma categoria")
    public CategorySummaryOutput summarizeTransactionsByCategory(
            @ToolParam(description = "Categoria de uma transação") Category category) {
        return summarizeTransactionsByCategoryUseCase.execute(category);
    }

    @Tool(name = "list-transactions-by-period", description = "Lista transações financeiras realizadas em um período de datas")
    public List<TransactionOutput> listTransactionsByPeriod(TransactionsPeriodToolRequest input) {
        return listTransactionsByPeriodUseCase.execute(input.toInput());
    }
}
