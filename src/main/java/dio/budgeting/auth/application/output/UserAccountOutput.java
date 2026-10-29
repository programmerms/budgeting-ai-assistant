package dio.budgeting.auth.application.output;

import dio.budgeting.auth.domain.UserAccount;

public record UserAccountOutput(String id, String username, String status) {
    public static UserAccountOutput from(UserAccount userAccount) {
        return new UserAccountOutput(
                userAccount.getId().uuid().toString(),
                userAccount.getUsername(),
                userAccount.getStatus().name());
    }
}
