package dio.budgeting.support.request;

import dio.budgeting.infrastructure.http.request.TransactionRequest;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import static dio.budgeting.support.TestConstants.AMOUNT_IN_CENTS;
import static dio.budgeting.support.TestConstants.CATEGORY;
import static dio.budgeting.support.TestConstants.DESCRIPTION;
import static org.instancio.Select.field;

public final class TransactionRequestSupport {

    private TransactionRequestSupport() {
    }

    public static InstancioApi<TransactionRequest> defaultTransactionRequest() {
        return Instancio.of(TransactionRequest.class)
                .set(field(TransactionRequest::description), DESCRIPTION)
                .set(field(TransactionRequest::amount), AMOUNT_IN_CENTS)
                .set(field(TransactionRequest::category), CATEGORY);
    }
}
