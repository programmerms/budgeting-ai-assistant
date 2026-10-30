package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.OwnerId;
import dio.budgeting.domain.TransactionId;
import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static dio.budgeting.support.TestConstants.CATEGORY;
import static dio.budgeting.support.TestConstants.CREATED_AT;
import static dio.budgeting.support.TestConstants.OWNER_ID;
import static dio.budgeting.support.TestConstants.OWNER_UUID;
import static dio.budgeting.support.domain.TransactionSupport.defaultTransaction;
import static dio.budgeting.support.entity.TransactionEntitySupport.defaultTransactionEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaTransactionRepositoryTest {

    @Mock
    private TransactionEntityRepository transactionEntityRepository;

    @InjectMocks
    private JpaTransactionRepository repository;

    @Test
    @DisplayName("Given a transaction When save is called Then it is saved as entity and the saved entity is returned as domain")
    void givenTransactionWhenSaveIsCalledThenItIsSavedAsEntityAndTheSavedEntityIsReturnedAsDomain() {
        final var transaction = defaultTransaction().create();
        final var savedEntity = defaultTransactionEntity().create();
        setupGivenTransactionWhenSaveIsCalledThenItIsSavedAsEntityAndTheSavedEntityIsReturnedAsDomain(savedEntity);

        final var result = repository.save(transaction);

        verifyGivenTransactionWhenSaveIsCalledThenItIsSavedAsEntityAndTheSavedEntityIsReturnedAsDomain(
                result, transaction, savedEntity);
    }

    @Test
    @DisplayName("Given stored entities When findAllByCategory is called Then the entities of the category are returned as domain")
    void givenStoredEntitiesWhenFindAllByCategoryIsCalledThenTheEntitiesOfTheCategoryAreReturnedAsDomain() {
        final var entity = defaultTransactionEntity().create();
        setupGivenStoredEntitiesWhenFindAllByCategoryIsCalledThenTheEntitiesOfTheCategoryAreReturnedAsDomain(entity);

        final var result = repository.findAllByOwnerIdAndCategory(OWNER_ID, CATEGORY);

        verifyGivenStoredEntitiesWhenFindAllByCategoryIsCalledThenTheEntitiesOfTheCategoryAreReturnedAsDomain(
                result, entity);
    }

    @Test
    @DisplayName("Given stored entities When findAllByCreatedAtBetween is called Then the entities in the period are returned as domain")
    void givenStoredEntitiesWhenFindAllByCreatedAtBetweenIsCalledThenTheEntitiesInPeriodAreReturnedAsDomain() {
        final var from = CREATED_AT.minusSeconds(3600);
        final var to = CREATED_AT.plusSeconds(3600);
        final var entity = defaultTransactionEntity().create();
        setupGivenStoredEntitiesWhenFindAllByCreatedAtBetweenIsCalledThenTheEntitiesInPeriodAreReturnedAsDomain(
                from, to, entity);

        final var result = repository.findAllByOwnerIdAndCreatedAtBetween(OWNER_ID, from, to);

        verifyGivenStoredEntitiesWhenFindAllByCreatedAtBetweenIsCalledThenTheEntitiesInPeriodAreReturnedAsDomain(
                result, from, to, entity);
    }

    private void setupGivenTransactionWhenSaveIsCalledThenItIsSavedAsEntityAndTheSavedEntityIsReturnedAsDomain(
            final TransactionEntity savedEntity) {
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenReturn(savedEntity);
    }

    private void verifyGivenTransactionWhenSaveIsCalledThenItIsSavedAsEntityAndTheSavedEntityIsReturnedAsDomain(
            final Transaction actual, final Transaction transaction, final TransactionEntity savedEntity) {
        final var captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionEntityRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new TransactionEntity(
                transaction.getId().uuid(),
                transaction.getOwnerId().uuid(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getCategory(),
                transaction.getCreatedAt()));

        assertThat(actual).usingRecursiveComparison().isEqualTo(toExpectedDomain(savedEntity));
    }

    private void setupGivenStoredEntitiesWhenFindAllByCategoryIsCalledThenTheEntitiesOfTheCategoryAreReturnedAsDomain(
            final TransactionEntity entity) {
        when(transactionEntityRepository.findAllByOwnerIdAndCategory(OWNER_UUID, CATEGORY))
                .thenReturn(List.of(entity));
    }

    private void verifyGivenStoredEntitiesWhenFindAllByCategoryIsCalledThenTheEntitiesOfTheCategoryAreReturnedAsDomain(
            final List<Transaction> actual, final TransactionEntity entity) {
        verify(transactionEntityRepository, times(1)).findAllByOwnerIdAndCategory(OWNER_UUID, CATEGORY);
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(toExpectedDomain(entity));
    }

    private void setupGivenStoredEntitiesWhenFindAllByCreatedAtBetweenIsCalledThenTheEntitiesInPeriodAreReturnedAsDomain(
            final Instant from, final Instant to, final TransactionEntity entity) {
        when(transactionEntityRepository.findAllByOwnerIdAndCreatedAtBetween(OWNER_UUID, from, to))
                .thenReturn(List.of(entity));
    }

    private void verifyGivenStoredEntitiesWhenFindAllByCreatedAtBetweenIsCalledThenTheEntitiesInPeriodAreReturnedAsDomain(
            final List<Transaction> actual, final Instant from, final Instant to, final TransactionEntity entity) {
        verify(transactionEntityRepository, times(1))
                .findAllByOwnerIdAndCreatedAtBetween(OWNER_UUID, from, to);
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(toExpectedDomain(entity));
    }

    private Transaction toExpectedDomain(final TransactionEntity entity) {
        return new Transaction(
                new OwnerId(entity.getOwnerId()),
                new TransactionId(entity.getId()),
                entity.getDescription(),
                entity.getAmount(),
                entity.getCategory(),
                entity.getCreatedAt());
    }
}
