package pl.patryk.budget_api.summary;

import java.math.BigDecimal;
import java.util.List;

public record SummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        List<CategoryExpenseResponse> expensesByCategory
) {
}
