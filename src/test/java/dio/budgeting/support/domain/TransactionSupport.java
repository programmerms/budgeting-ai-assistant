package dio.budgeting.support.domain;

import dio.budgeting.domain.Transaction;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import static dio.budgeting.support.TestConstants.AMOUNT_IN_CENTS;
import static dio.budgeting.support.TestConstants.CATEGORY;
import static dio.budgeting.support.TestConstants.DESCRIPTION;
import static org.instancio.Select.field;

public final class TransactionSupport {

    private TransactionSupport() {
    }

    public static InstancioApi<Transaction> defaultTransaction() {
        return Instancio.of(Transaction.class)
                .set(field(Transaction::getDescription), DESCRIPTION)
                .set(field(Transaction::getAmount), AMOUNT_IN_CENTS)
                .set(field(Transaction::getCategory), CATEGORY);
    }
}
