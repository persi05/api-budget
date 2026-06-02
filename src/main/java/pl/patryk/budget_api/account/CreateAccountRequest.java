package pl.patryk.budget_api.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank(message = "Account name is required.")
        @Size(max = 100, message = "Account name cannot be longer than 100 characters.")
        String name
) {
}
