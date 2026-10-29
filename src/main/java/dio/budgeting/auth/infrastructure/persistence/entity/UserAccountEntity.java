package dio.budgeting.auth.infrastructure.persistence.entity;

import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountId;
import dio.budgeting.auth.domain.UserRole;
import dio.budgeting.auth.domain.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAccountEntity {
    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    public static UserAccountEntity from(UserAccount userAccount) {
        return new UserAccountEntity(
                userAccount.getId().uuid(),
                userAccount.getUsername(),
                userAccount.getEncodedPassword(),
                userAccount.getRole(),
                userAccount.getStatus());
    }

    public UserAccount toDomain() {
        return new UserAccount(
                new UserAccountId(this.id),
                this.username,
                this.password,
                this.role,
                this.status);
    }
}
