package dio.budgeting.application;

import dio.budgeting.application.input.TransactionsPeriodInput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.domain.OwnerId;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ListTransactionsByPeriodUseCase {
    private final TransactionRepository transactionRepository;

    public ListTransactionsByPeriodUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<TransactionOutput> execute(OwnerId ownerId, TransactionsPeriodInput input) {
        Assert.notNull(ownerId, "Owner ID must not be null");
        Assert.notNull(input, "Period input must not be null");
        Assert.notNull(input.from(), "Start date must not be null");
        Assert.notNull(input.to(), "End date must not be null");
        Assert.isTrue(!input.from().isAfter(input.to()), "Start date must not be after end date");

        var fromInstant = input.from().atStartOfDay(ZoneOffset.UTC).toInstant();
        var toInstant = input.to().atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();

        return transactionRepository.findAllByOwnerIdAndCreatedAtBetween(ownerId, fromInstant, toInstant)
                .stream()
                .map(TransactionOutput::from)
                .toList();
    }
}
