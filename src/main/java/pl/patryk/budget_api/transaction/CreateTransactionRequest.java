package pl.patryk.budget_api.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
        @NotNull(message = "Transaction amount is required.")
        @DecimalMin(value = "0.01", message = "Transaction amount must be greater than 0.")
        @DecimalMax(value = "9999999.99", message = "Transaction amount is too high. Maximum allowed amount is 9999999.99.")
        @Digits(integer = 7, fraction = 2, message = "Transaction amount can have up to 7 integer digits and 2 decimal places.")
        BigDecimal amount,

        @NotNull(message = "Transaction type is required.")
        TransactionType type,

        @NotBlank(message = "Transaction category is required.")
        @Size(max = 100, message = "Transaction category cannot be longer than 100 characters.")
        String category,

        @Size(max = 500, message = "Transaction description cannot be longer than 500 characters.")
        String description,

        @NotNull(message = "Transaction date is required.")
        LocalDate transactionDate,

        @NotNull(message = "Account id is required.")
        Long accountId
) {
}
