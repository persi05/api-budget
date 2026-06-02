package pl.patryk.budget_api.summary;

import java.math.BigDecimal;

public record CategoryExpenseResponse(
        String category,
        BigDecimal total
) {
}
