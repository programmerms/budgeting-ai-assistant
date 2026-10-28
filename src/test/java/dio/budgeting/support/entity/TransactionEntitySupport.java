package dio.budgeting.support.entity;

import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import static dio.budgeting.support.TestConstants.AMOUNT_IN_CENTS;
import static dio.budgeting.support.TestConstants.CATEGORY;
import static dio.budgeting.support.TestConstants.CREATED_AT;
import static dio.budgeting.support.TestConstants.DESCRIPTION;
import static org.instancio.Select.field;

public final class TransactionEntitySupport {

    private TransactionEntitySupport() {
    }

    public static InstancioApi<TransactionEntity> defaultTransactionEntity() {
        return Instancio.of(TransactionEntity.class)
                .set(field(TransactionEntity::getDescription), DESCRIPTION)
                .set(field(TransactionEntity::getAmount), AMOUNT_IN_CENTS)
                .set(field(TransactionEntity::getCategory), CATEGORY)
                .set(field(TransactionEntity::getCreatedAt), CREATED_AT);
    }
}
