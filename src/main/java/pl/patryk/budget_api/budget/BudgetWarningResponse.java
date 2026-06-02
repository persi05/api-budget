package pl.patryk.budget_api.budget;

import java.math.BigDecimal;

public record BudgetWarningResponse(
        String category,
        BigDecimal monthlyLimit,
        BigDecimal spentInMonth,
        BigDecimal exceededBy,
        String message
) {
}
