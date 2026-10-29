package dio.budgeting;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-key",
        "app.admin.username=test-admin",
        "app.admin.password=test-admin-password"})
@Testcontainers
class BudgetingApplicationTests {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:9.6")
            .withStartupTimeoutSeconds(300);

    @Test
    void contextLoads() {
    }

}
