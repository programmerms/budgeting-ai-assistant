package dio.budgeting.auth.infrastructure.http;

import dio.budgeting.auth.application.ApproveUserUseCase;
import dio.budgeting.auth.application.ListUsersByStatusUseCase;
import dio.budgeting.auth.application.RejectUserUseCase;
import dio.budgeting.auth.domain.UserAccountId;
import dio.budgeting.auth.domain.UserStatus;
import dio.budgeting.auth.infrastructure.http.response.UserAccountResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final ListUsersByStatusUseCase listUsersByStatusUseCase;
    private final ApproveUserUseCase approveUserUseCase;
    private final RejectUserUseCase rejectUserUseCase;

    public AdminUserController(ListUsersByStatusUseCase listUsersByStatusUseCase,
                               ApproveUserUseCase approveUserUseCase,
                               RejectUserUseCase rejectUserUseCase) {
        this.listUsersByStatusUseCase = listUsersByStatusUseCase;
        this.approveUserUseCase = approveUserUseCase;
        this.rejectUserUseCase = rejectUserUseCase;
    }

    @GetMapping
    public List<UserAccountResponse> readUsersByStatus(@RequestParam UserStatus status) {
        return listUsersByStatusUseCase.execute(status).stream().map(UserAccountResponse::from).toList();
    }

    @PostMapping("/{id}/approve")
    public UserAccountResponse approveUser(@PathVariable UUID id) {
        var output = approveUserUseCase.execute(new UserAccountId(id));
        return UserAccountResponse.from(output);
    }

    @PostMapping("/{id}/reject")
    public UserAccountResponse rejectUser(@PathVariable UUID id) {
        var output = rejectUserUseCase.execute(new UserAccountId(id));
        return UserAccountResponse.from(output);
    }
}
