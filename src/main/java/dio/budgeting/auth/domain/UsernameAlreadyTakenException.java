package dio.budgeting.auth.domain;

public class UsernameAlreadyTakenException extends RuntimeException {
    public UsernameAlreadyTakenException(String username) {
        super("Username " + username + " is already taken");
    }
}
