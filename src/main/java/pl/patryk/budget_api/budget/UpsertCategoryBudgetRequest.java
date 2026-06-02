package pl.patryk.budget_api.budget;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpsertCategoryBudgetRequest(
        @NotBlank(message = "Budget category is required.")
        @Size(max = 100, message = "Budget category cannot be longer than 100 characters.")
        String category,

        @NotNull(message = "Monthly budget limit is required.")
        @DecimalMin(value = "0.01", message = "Monthly budget limit must be greater than 0.")
        @DecimalMax(value = "9999999.99", message = "Monthly budget limit is too high. Maximum allowed limit is 9999999.99.")
        @Digits(integer = 7, fraction = 2, message = "Monthly budget limit can have up to 7 integer digits and 2 decimal places.")
        BigDecimal monthlyLimit
) {
}
