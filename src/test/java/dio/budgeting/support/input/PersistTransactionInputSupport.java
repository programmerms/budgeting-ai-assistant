package dio.budgeting.support.input;

import dio.budgeting.application.input.PersistTransactionInput;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import static dio.budgeting.support.TestConstants.AMOUNT_IN_CENTS;
import static dio.budgeting.support.TestConstants.CATEGORY;
import static dio.budgeting.support.TestConstants.DESCRIPTION;
import static org.instancio.Select.field;

public final class PersistTransactionInputSupport {

    private PersistTransactionInputSupport() {
    }

    public static InstancioApi<PersistTransactionInput> defaultPersistTransactionInput() {
        return Instancio.of(PersistTransactionInput.class)
                .set(field(PersistTransactionInput::description), DESCRIPTION)
                .set(field(PersistTransactionInput::amount), AMOUNT_IN_CENTS)
                .set(field(PersistTransactionInput::category), CATEGORY);
    }
}
