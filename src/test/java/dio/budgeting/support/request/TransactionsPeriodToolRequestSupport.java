package dio.budgeting.support.request;

import dio.budgeting.infrastructure.ai.request.TransactionsPeriodToolRequest;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import static dio.budgeting.support.TestConstants.PERIOD_FROM;
import static dio.budgeting.support.TestConstants.PERIOD_TO;
import static org.instancio.Select.field;

public final class TransactionsPeriodToolRequestSupport {

    private TransactionsPeriodToolRequestSupport() {
    }

    public static InstancioApi<TransactionsPeriodToolRequest> defaultTransactionsPeriodToolRequest() {
        return Instancio.of(TransactionsPeriodToolRequest.class)
                .set(field(TransactionsPeriodToolRequest::from), PERIOD_FROM)
                .set(field(TransactionsPeriodToolRequest::to), PERIOD_TO);
    }
}
