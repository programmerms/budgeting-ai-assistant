package dio.budgeting.infrastructure.http.response;

import dio.budgeting.application.output.CategorySummaryOutput;

import java.math.BigDecimal;

public record CategorySummaryResponse(String category, BigDecimal total, long count) {
    public static CategorySummaryResponse from(CategorySummaryOutput output) {
        return new CategorySummaryResponse(output.category(), output.total(), output.count());
    }
}
