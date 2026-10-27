package dio.budgeting.support.request;

import dio.budgeting.infrastructure.ai.request.PersistTransactionToolRequest;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import static dio.budgeting.support.TestConstants.AMOUNT_IN_CENTS;
import static dio.budgeting.support.TestConstants.CATEGORY;
import static dio.budgeting.support.TestConstants.DESCRIPTION;
import static org.instancio.Select.field;

public final class PersistTransactionToolRequestSupport {

    private PersistTransactionToolRequestSupport() {
    }

    public static InstancioApi<PersistTransactionToolRequest> defaultPersistTransactionToolRequest() {
        return Instancio.of(PersistTransactionToolRequest.class)
                .set(field(PersistTransactionToolRequest::description), DESCRIPTION)
                .set(field(PersistTransactionToolRequest::amount), AMOUNT_IN_CENTS)
                .set(field(PersistTransactionToolRequest::category), CATEGORY);
    }
}
