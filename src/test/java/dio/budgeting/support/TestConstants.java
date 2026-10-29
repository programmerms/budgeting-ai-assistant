package dio.budgeting.support;

import dio.budgeting.domain.Category;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class TestConstants {

    public static final String DESCRIPTION = "Compras no mercado";
    public static final long AMOUNT_IN_CENTS = 8000L;
    public static final BigDecimal AMOUNT_IN_REAIS = new BigDecimal("80.00");
    public static final Category CATEGORY = Category.GROCERIES;
    public static final Instant CREATED_AT = Instant.parse("2026-10-01T12:00:00Z");
    public static final LocalDate PERIOD_FROM = LocalDate.of(2026, 10, 1);
    public static final LocalDate PERIOD_TO = LocalDate.of(2026, 10, 31);
    public static final String USER_MESSAGE = "Gastei 80 reais no mercado";
    public static final String ASSISTANT_REPLY = "Transação de R$ 80,00 registrada em GROCERIES.";
    public static final byte[] AUDIO_BYTES = new byte[]{1, 2, 3, 4};
    public static final String USERNAME = "maria";
    public static final String RAW_PASSWORD = "senha-forte";
    public static final String ENCODED_PASSWORD = "$2a$10$encodedPasswordPlaceholderValue........................";
    public static final String ADMIN_USERNAME = "test-admin";
    public static final String ADMIN_PASSWORD = "test-admin-password";

    private TestConstants() {
    }
}
