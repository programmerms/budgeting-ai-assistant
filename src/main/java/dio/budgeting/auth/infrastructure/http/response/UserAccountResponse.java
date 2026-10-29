package dio.budgeting.auth.infrastructure.http.response;

import dio.budgeting.auth.application.output.UserAccountOutput;

public record UserAccountResponse(String id, String username, String status) {
    public static UserAccountResponse from(UserAccountOutput output) {
        return new UserAccountResponse(output.id(), output.username(), output.status());
    }
}
