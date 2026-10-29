package dio.budgeting.auth.infrastructure.persistence.repository;

import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountRepository;
import dio.budgeting.auth.domain.UserRole;
import dio.budgeting.auth.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.List;
import java.util.Optional;

import static dio.budgeting.support.TestConstants.ENCODED_PASSWORD;
import static dio.budgeting.support.TestConstants.USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-key",
        "app.admin.username=test-admin",
        "app.admin.password=test-admin-password"})
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class JpaUserAccountRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:9.6")
            .withStartupTimeoutSeconds(300);

    private final UserAccountRepository repository;
    private final UserAccountEntityRepository entityRepository;

    JpaUserAccountRepositoryIntegrationTest(final UserAccountRepository repository,
                                            final UserAccountEntityRepository entityRepository) {
        this.repository = repository;
        this.entityRepository = entityRepository;
    }

    @BeforeEach
    void cleanDatabase() {
        entityRepository.deleteAll();
    }

    @Test
    @DisplayName("Given a registered user account When saved and read back Then all fields survive the round trip")
    void givenRegisteredUserAccountWhenSavedAndReadBackThenAllFieldsSurviveTheRoundTrip() {
        final var userAccount = UserAccount.register(USERNAME, ENCODED_PASSWORD);
        setupGivenRegisteredUserAccountWhenSavedAndReadBackThenAllFieldsSurviveTheRoundTrip(userAccount);

        final var actual = repository.findById(userAccount.getId());

        verifyGivenRegisteredUserAccountWhenSavedAndReadBackThenAllFieldsSurviveTheRoundTrip(actual, userAccount);
    }

    @Test
    @DisplayName("Given accounts in different statuses When findAllByStatus is called Then only the accounts with that status are returned")
    void givenAccountsInDifferentStatusesWhenFindAllByStatusIsCalledThenOnlyTheAccountsWithThatStatusAreReturned() {
        final var pending = UserAccount.register("pending-user", ENCODED_PASSWORD);
        final var active = UserAccount.register("active-user", ENCODED_PASSWORD);
        setupGivenAccountsInDifferentStatusesWhenFindAllByStatusIsCalledThenOnlyTheAccountsWithThatStatusAreReturned(
                pending, active);

        final var actual = repository.findAllByStatus(UserStatus.PENDING);

        verifyGivenAccountsInDifferentStatusesWhenFindAllByStatusIsCalledThenOnlyTheAccountsWithThatStatusAreReturned(
                actual, pending);
    }

    @Test
    @DisplayName("Given a stored account When findByUsername is called Then the account is returned")
    void givenStoredAccountWhenFindByUsernameIsCalledThenTheAccountIsReturned() {
        final var userAccount = UserAccount.register(USERNAME, ENCODED_PASSWORD);
        setupGivenStoredAccountWhenFindByUsernameIsCalledThenTheAccountIsReturned(userAccount);

        final var actual = repository.findByUsername(USERNAME);

        verifyGivenStoredAccountWhenFindByUsernameIsCalledThenTheAccountIsReturned(actual, userAccount);
    }

    @Test
    @DisplayName("Given a stored account When existsByUsername is called with the same name Then it returns true")
    void givenStoredAccountWhenExistsByUsernameIsCalledWithTheSameNameThenItReturnsTrue() {
        setupGivenStoredAccountWhenExistsByUsernameIsCalledWithTheSameNameThenItReturnsTrue();

        final var actual = repository.existsByUsername(USERNAME);

        verifyGivenStoredAccountWhenExistsByUsernameIsCalledWithTheSameNameThenItReturnsTrue(actual);
    }

    @Test
    @DisplayName("Given only common users When existsByRole is called for the administrator role Then it returns false")
    void givenOnlyCommonUsersWhenExistsByRoleIsCalledForTheAdministratorRoleThenItReturnsFalse() {
        setupGivenOnlyCommonUsersWhenExistsByRoleIsCalledForTheAdministratorRoleThenItReturnsFalse();

        final var actual = repository.existsByRole(UserRole.ROLE_ADMIN);

        verifyGivenOnlyCommonUsersWhenExistsByRoleIsCalledForTheAdministratorRoleThenItReturnsFalse(actual);
    }

    @Test
    @DisplayName("Given a stored administrator When existsByRole is called for the administrator role Then it returns true")
    void givenStoredAdministratorWhenExistsByRoleIsCalledForTheAdministratorRoleThenItReturnsTrue() {
        setupGivenStoredAdministratorWhenExistsByRoleIsCalledForTheAdministratorRoleThenItReturnsTrue();

        final var actual = repository.existsByRole(UserRole.ROLE_ADMIN);

        verifyGivenStoredAdministratorWhenExistsByRoleIsCalledForTheAdministratorRoleThenItReturnsTrue(actual);
    }

    @Test
    @DisplayName("Given a stored username When another account with the same name is saved Then the database rejects it")
    void givenStoredUsernameWhenAnotherAccountWithTheSameNameIsSavedThenTheDatabaseRejectsIt() {
        setupGivenStoredUsernameWhenAnotherAccountWithTheSameNameIsSavedThenTheDatabaseRejectsIt();

        final var thrown = catchThrowable(
                () -> repository.save(UserAccount.register(USERNAME, ENCODED_PASSWORD)));

        verifyGivenStoredUsernameWhenAnotherAccountWithTheSameNameIsSavedThenTheDatabaseRejectsIt(thrown);
    }

    private void setupGivenRegisteredUserAccountWhenSavedAndReadBackThenAllFieldsSurviveTheRoundTrip(
            final UserAccount userAccount) {
        repository.save(userAccount);
    }

    private void verifyGivenRegisteredUserAccountWhenSavedAndReadBackThenAllFieldsSurviveTheRoundTrip(
            final Optional<UserAccount> actual, final UserAccount expected) {
        assertThat(actual).get().usingRecursiveComparison().isEqualTo(expected);
    }

    private void setupGivenAccountsInDifferentStatusesWhenFindAllByStatusIsCalledThenOnlyTheAccountsWithThatStatusAreReturned(
            final UserAccount pending, final UserAccount active) {
        active.approve();
        repository.save(pending);
        repository.save(active);
    }

    private void verifyGivenAccountsInDifferentStatusesWhenFindAllByStatusIsCalledThenOnlyTheAccountsWithThatStatusAreReturned(
            final List<UserAccount> actual, final UserAccount expected) {
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expected);
    }

    private void setupGivenStoredAccountWhenFindByUsernameIsCalledThenTheAccountIsReturned(
            final UserAccount userAccount) {
        repository.save(userAccount);
    }

    private void verifyGivenStoredAccountWhenFindByUsernameIsCalledThenTheAccountIsReturned(
            final Optional<UserAccount> actual, final UserAccount expected) {
        assertThat(actual).get().usingRecursiveComparison().isEqualTo(expected);
    }

    private void setupGivenStoredAccountWhenExistsByUsernameIsCalledWithTheSameNameThenItReturnsTrue() {
        repository.save(UserAccount.register(USERNAME, ENCODED_PASSWORD));
    }

    private void verifyGivenStoredAccountWhenExistsByUsernameIsCalledWithTheSameNameThenItReturnsTrue(
            final boolean actual) {
        assertThat(actual).isTrue();
    }

    private void setupGivenOnlyCommonUsersWhenExistsByRoleIsCalledForTheAdministratorRoleThenItReturnsFalse() {
        repository.save(UserAccount.register(USERNAME, ENCODED_PASSWORD));
    }

    private void verifyGivenOnlyCommonUsersWhenExistsByRoleIsCalledForTheAdministratorRoleThenItReturnsFalse(
            final boolean actual) {
        assertThat(actual).isFalse();
    }

    private void setupGivenStoredAdministratorWhenExistsByRoleIsCalledForTheAdministratorRoleThenItReturnsTrue() {
        repository.save(UserAccount.administrator(USERNAME, ENCODED_PASSWORD));
    }

    private void verifyGivenStoredAdministratorWhenExistsByRoleIsCalledForTheAdministratorRoleThenItReturnsTrue(
            final boolean actual) {
        assertThat(actual).isTrue();
    }

    private void setupGivenStoredUsernameWhenAnotherAccountWithTheSameNameIsSavedThenTheDatabaseRejectsIt() {
        repository.save(UserAccount.register(USERNAME, ENCODED_PASSWORD));
    }

    private void verifyGivenStoredUsernameWhenAnotherAccountWithTheSameNameIsSavedThenTheDatabaseRejectsIt(
            final Throwable actual) {
        assertThat(actual).isNotNull();
        assertThat(entityRepository.count()).isEqualTo(1);
    }
}
