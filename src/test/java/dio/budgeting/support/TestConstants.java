package dio.budgeting.support;

import dio.budgeting.domain.Category;

import java.math.BigDecimal;

public final class TestConstants {

    public static final String DESCRIPTION = "Compras no mercado";
    public static final long AMOUNT_IN_CENTS = 8000L;
    public static final BigDecimal AMOUNT_IN_REAIS = new BigDecimal("80.00");
    public static final Category CATEGORY = Category.GROCERIES;

    private TestConstants() {
    }
}
