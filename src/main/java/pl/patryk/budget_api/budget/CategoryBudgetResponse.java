package pl.patryk.budget_api.budget;

import java.math.BigDecimal;

public record CategoryBudgetResponse(
        Long id,
        String category,
        BigDecimal monthlyLimit
) {
    static CategoryBudgetResponse from(CategoryBudget budget) {
        return new CategoryBudgetResponse(budget.getId(), budget.getCategory(), budget.getMonthlyLimit());
    }
}
