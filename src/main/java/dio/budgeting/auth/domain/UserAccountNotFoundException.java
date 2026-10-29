package dio.budgeting.auth.domain;

public class UserAccountNotFoundException extends RuntimeException {
    public UserAccountNotFoundException(UserAccountId id) {
        super("User account with id " + id.uuid() + " not found");
    }
}
