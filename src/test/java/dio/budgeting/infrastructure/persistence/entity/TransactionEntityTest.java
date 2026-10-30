package dio.budgeting.infrastructure.persistence.entity;

import dio.budgeting.domain.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dio.budgeting.support.domain.TransactionSupport.defaultTransaction;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionEntityTest {

    @Test
    @DisplayName("Given a transaction When converted to entity and back to domain Then all fields are kept")
    void givenTransactionWhenConvertedToEntityAndBackToDomainThenAllFieldsAreKept() {
        final var transaction = defaultTransaction().create();

        final var actual = TransactionEntity.from(transaction).toDomain();

        verifyGivenTransactionWhenConvertedToEntityAndBackToDomainThenAllFieldsAreKept(actual, transaction);
    }

    private void verifyGivenTransactionWhenConvertedToEntityAndBackToDomainThenAllFieldsAreKept(
            final Transaction actual, final Transaction expected) {
        assertThat(TransactionEntity.from(expected).getOwnerId()).isEqualTo(expected.getOwnerId().uuid());
        assertThat(actual.getOwnerId()).isEqualTo(expected.getOwnerId());
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
