package dio.budgeting.auth.domain;

public class UserAlreadyDecidedException extends RuntimeException {
    public UserAlreadyDecidedException(UserAccountId id, UserStatus status) {
        super("User account with id " + id.uuid() + " was already decided and is " + status);
    }
}
