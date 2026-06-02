package pl.patryk.budget_api.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import pl.patryk.budget_api.budget.BudgetWarningResponse;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        TransactionType type,
        String category,
        String description,
        LocalDate transactionDate,
        Long accountId,
        String accountName,
        BudgetWarningResponse budgetWarning
) {
    static TransactionResponse from(TransactionEntity transaction) {
        return from(transaction, null);
    }

    public static TransactionResponse from(TransactionEntity transaction, BudgetWarningResponse budgetWarning) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getCategory(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getAccount().getId(),
                transaction.getAccount().getName(),
                budgetWarning
        );
    }
}
