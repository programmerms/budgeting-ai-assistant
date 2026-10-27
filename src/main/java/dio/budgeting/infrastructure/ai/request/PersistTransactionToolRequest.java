package dio.budgeting.infrastructure.ai.request;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import org.springframework.ai.tool.annotation.ToolParam;

public record PersistTransactionToolRequest(@ToolParam(description = "Descrição do gasto") String description,
                                            @ToolParam(description = "Valor do gasto (em centavos)") long amount,
                                            @ToolParam(description = "Categoria de uma transação") Category category) {
    public PersistTransactionInput toInput() {
        return new PersistTransactionInput(description, amount, category);
    }
}
