package dio.budgeting.auth.infrastructure.persistence.repository;

import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountId;
import dio.budgeting.auth.domain.UserRole;
import dio.budgeting.auth.domain.UserStatus;
import dio.budgeting.auth.infrastructure.persistence.entity.UserAccountEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static dio.budgeting.support.TestConstants.USERNAME;
import static dio.budgeting.support.domain.UserAccountSupport.defaultUserAccount;
import static dio.budgeting.support.entity.UserAccountEntitySupport.defaultUserAccountEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserAccountRepositoryTest {

    @Mock
    private UserAccountEntityRepository userAccountEntityRepository;

    @InjectMocks
    private JpaUserAccountRepository repository;

    @Test
    @DisplayName("Given a user account When save is called Then it is saved as entity and the saved entity is returned as domain")
    void givenUserAccountWhenSaveIsCalledThenItIsSavedAsEntityAndTheSavedEntityIsReturnedAsDomain() {
        final var userAccount = defaultUserAccount().create();
        final var savedEntity = defaultUserAccountEntity().create();
        setupGivenUserAccountWhenSaveIsCalledThenItIsSavedAsEntityAndTheSavedEntityIsReturnedAsDomain(savedEntity);

        final var result = repository.save(userAccount);

        verifyGivenUserAccountWhenSaveIsCalledThenItIsSavedAsEntityAndTheSavedEntityIsReturnedAsDomain(
                result, userAccount, savedEntity);
    }

    @Test
    @DisplayName("Given a stored entity When findById is called Then the entity is returned as domain")
    void givenStoredEntityWhenFindByIdIsCalledThenTheEntityIsReturnedAsDomain() {
        final var entity = defaultUserAccountEntity().create();
        final var id = new UserAccountId(entity.getId());
        setupGivenStoredEntityWhenFindByIdIsCalledThenTheEntityIsReturnedAsDomain(id, entity);

        final var result = repository.findById(id);

        verifyGivenStoredEntityWhenFindByIdIsCalledThenTheEntityIsReturnedAsDomain(result, id, entity);
    }

    @Test
    @DisplayName("Given no stored entity When findById is called Then an empty optional is returned")
    void givenNoStoredEntityWhenFindByIdIsCalledThenAnEmptyOptionalIsReturned() {
        final var id = new UserAccountId();
        setupGivenNoStoredEntityWhenFindByIdIsCalledThenAnEmptyOptionalIsReturned(id);

        final var result = repository.findById(id);

        verifyGivenNoStoredEntityWhenFindByIdIsCalledThenAnEmptyOptionalIsReturned(result, id);
    }

    @Test
    @DisplayName("Given a stored entity When findByUsername is called Then the entity is returned as domain")
    void givenStoredEntityWhenFindByUsernameIsCalledThenTheEntityIsReturnedAsDomain() {
        final var entity = defaultUserAccountEntity().create();
        setupGivenStoredEntityWhenFindByUsernameIsCalledThenTheEntityIsReturnedAsDomain(entity);

        final var result = repository.findByUsername(USERNAME);

        verifyGivenStoredEntityWhenFindByUsernameIsCalledThenTheEntityIsReturnedAsDomain(result, entity);
    }

    @Test
    @DisplayName("Given stored entities When findAllByStatus is called Then the entities with the status are returned as domain")
    void givenStoredEntitiesWhenFindAllByStatusIsCalledThenTheEntitiesWithTheStatusAreReturnedAsDomain() {
        final var entity = defaultUserAccountEntity().create();
        setupGivenStoredEntitiesWhenFindAllByStatusIsCalledThenTheEntitiesWithTheStatusAreReturnedAsDomain(entity);

        final var result = repository.findAllByStatus(UserStatus.PENDING);

        verifyGivenStoredEntitiesWhenFindAllByStatusIsCalledThenTheEntitiesWithTheStatusAreReturnedAsDomain(
                result, entity);
    }

    @Test
    @DisplayName("Given an existing username When existsByUsername is called Then it returns true")
    void givenExistingUsernameWhenExistsByUsernameIsCalledThenItReturnsTrue() {
        setupGivenExistingUsernameWhenExistsByUsernameIsCalledThenItReturnsTrue();

        final var result = repository.existsByUsername(USERNAME);

        verifyGivenExistingUsernameWhenExistsByUsernameIsCalledThenItReturnsTrue(result);
    }

    @Test
    @DisplayName("Given an existing administrator When existsByRole is called Then it returns true")
    void givenExistingAdministratorWhenExistsByRoleIsCalledThenItReturnsTrue() {
        setupGivenExistingAdministratorWhenExistsByRoleIsCalledThenItReturnsTrue();

        final var result = repository.existsByRole(UserRole.ROLE_ADMIN);

        verifyGivenExistingAdministratorWhenExistsByRoleIsCalledThenItReturnsTrue(result);
    }

    private void setupGivenUserAccountWhenSaveIsCalledThenItIsSavedAsEntityAndTheSavedEntityIsReturnedAsDomain(
            final UserAccountEntity savedEntity) {
        when(userAccountEntityRepository.save(any(UserAccountEntity.class))).thenReturn(savedEntity);
    }

    private void verifyGivenUserAccountWhenSaveIsCalledThenItIsSavedAsEntityAndTheSavedEntityIsReturnedAsDomain(
            final UserAccount actual, final UserAccount userAccount, final UserAccountEntity savedEntity) {
        final var captor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userAccountEntityRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new UserAccountEntity(
                userAccount.getId().uuid(),
                userAccount.getUsername(),
                userAccount.getEncodedPassword(),
                userAccount.getRole(),
                userAccount.getStatus()));

        assertThat(actual).usingRecursiveComparison().isEqualTo(savedEntity.toDomain());
    }

    private void setupGivenStoredEntityWhenFindByIdIsCalledThenTheEntityIsReturnedAsDomain(
            final UserAccountId id, final UserAccountEntity entity) {
        when(userAccountEntityRepository.findById(id.uuid())).thenReturn(Optional.of(entity));
    }

    private void verifyGivenStoredEntityWhenFindByIdIsCalledThenTheEntityIsReturnedAsDomain(
            final Optional<UserAccount> actual, final UserAccountId id, final UserAccountEntity entity) {
        verify(userAccountEntityRepository, times(1)).findById(id.uuid());
        assertThat(actual).get().usingRecursiveComparison().isEqualTo(entity.toDomain());
    }

    private void setupGivenNoStoredEntityWhenFindByIdIsCalledThenAnEmptyOptionalIsReturned(final UserAccountId id) {
        when(userAccountEntityRepository.findById(id.uuid())).thenReturn(Optional.empty());
    }

    private void verifyGivenNoStoredEntityWhenFindByIdIsCalledThenAnEmptyOptionalIsReturned(
            final Optional<UserAccount> actual, final UserAccountId id) {
        verify(userAccountEntityRepository, times(1)).findById(id.uuid());
        assertThat(actual).isEmpty();
    }

    private void setupGivenStoredEntityWhenFindByUsernameIsCalledThenTheEntityIsReturnedAsDomain(
            final UserAccountEntity entity) {
        when(userAccountEntityRepository.findByUsername(USERNAME)).thenReturn(Optional.of(entity));
    }

    private void verifyGivenStoredEntityWhenFindByUsernameIsCalledThenTheEntityIsReturnedAsDomain(
            final Optional<UserAccount> actual, final UserAccountEntity entity) {
        verify(userAccountEntityRepository, times(1)).findByUsername(USERNAME);
        assertThat(actual).get().usingRecursiveComparison().isEqualTo(entity.toDomain());
    }

    private void setupGivenStoredEntitiesWhenFindAllByStatusIsCalledThenTheEntitiesWithTheStatusAreReturnedAsDomain(
            final UserAccountEntity entity) {
        when(userAccountEntityRepository.findAllByStatus(UserStatus.PENDING)).thenReturn(List.of(entity));
    }

    private void verifyGivenStoredEntitiesWhenFindAllByStatusIsCalledThenTheEntitiesWithTheStatusAreReturnedAsDomain(
            final List<UserAccount> actual, final UserAccountEntity entity) {
        verify(userAccountEntityRepository, times(1)).findAllByStatus(UserStatus.PENDING);
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(entity.toDomain());
    }

    private void setupGivenExistingUsernameWhenExistsByUsernameIsCalledThenItReturnsTrue() {
        when(userAccountEntityRepository.existsByUsername(USERNAME)).thenReturn(true);
    }

    private void verifyGivenExistingUsernameWhenExistsByUsernameIsCalledThenItReturnsTrue(final boolean actual) {
        verify(userAccountEntityRepository, times(1)).existsByUsername(USERNAME);
        assertThat(actual).isTrue();
    }

    private void setupGivenExistingAdministratorWhenExistsByRoleIsCalledThenItReturnsTrue() {
        when(userAccountEntityRepository.existsByRole(UserRole.ROLE_ADMIN)).thenReturn(true);
    }

    private void verifyGivenExistingAdministratorWhenExistsByRoleIsCalledThenItReturnsTrue(final boolean actual) {
        verify(userAccountEntityRepository, times(1)).existsByRole(UserRole.ROLE_ADMIN);
        assertThat(actual).isTrue();
    }
}
