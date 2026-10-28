package dio.budgeting.support.input;

import dio.budgeting.application.input.TransactionsPeriodInput;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import static dio.budgeting.support.TestConstants.PERIOD_FROM;
import static dio.budgeting.support.TestConstants.PERIOD_TO;
import static org.instancio.Select.field;

public final class TransactionsPeriodInputSupport {

    private TransactionsPeriodInputSupport() {
    }

    public static InstancioApi<TransactionsPeriodInput> defaultTransactionsPeriodInput() {
        return Instancio.of(TransactionsPeriodInput.class)
                .set(field(TransactionsPeriodInput::from), PERIOD_FROM)
                .set(field(TransactionsPeriodInput::to), PERIOD_TO);
    }
}
