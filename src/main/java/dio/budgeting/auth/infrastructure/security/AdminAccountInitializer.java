package dio.budgeting.auth.infrastructure.security;

import dio.budgeting.auth.application.PasswordEncoderGateway;
import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountRepository;
import dio.budgeting.auth.domain.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoderGateway passwordEncoderGateway;
    private final String adminUsername;
    private final String adminPassword;

    public AdminAccountInitializer(UserAccountRepository userAccountRepository,
                                   PasswordEncoderGateway passwordEncoderGateway,
                                   @Value("${app.admin.username}") String adminUsername,
                                   @Value("${app.admin.password}") String adminPassword) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoderGateway = passwordEncoderGateway;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userAccountRepository.existsByRole(UserRole.ROLE_ADMIN)) {
            return;
        }

        var administrator = UserAccount.administrator(adminUsername, passwordEncoderGateway.encode(adminPassword));
        userAccountRepository.save(administrator);

        LOGGER.info("Initial administrator account created for username {}", administrator.getUsername());
    }
}
