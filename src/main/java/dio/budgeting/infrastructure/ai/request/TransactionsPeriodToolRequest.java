package dio.budgeting.infrastructure.ai.request;

import dio.budgeting.application.input.TransactionsPeriodInput;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDate;

public record TransactionsPeriodToolRequest(
        @ToolParam(description = "Data inicial no formato YYYY-MM-DD") LocalDate from,
        @ToolParam(description = "Data final no formato YYYY-MM-DD") LocalDate to) {
    public TransactionsPeriodInput toInput() {
        return new TransactionsPeriodInput(from, to);
    }
}
