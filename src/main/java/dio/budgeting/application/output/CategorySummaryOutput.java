package dio.budgeting.application.output;

import java.math.BigDecimal;

public record CategorySummaryOutput(String category, BigDecimal total, long count) {
}
