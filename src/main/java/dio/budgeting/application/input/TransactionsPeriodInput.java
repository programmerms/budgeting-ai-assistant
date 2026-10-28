package dio.budgeting.application.input;

import java.time.LocalDate;

public record TransactionsPeriodInput(LocalDate from, LocalDate to) {
}
