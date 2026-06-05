package pl.patryk.budget_api.transaction;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import jakarta.validation.ConstraintViolation;

class CreateTransactionRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsAmountAboveDomainLimitWithReadableMessage() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("10000000000000"),
                TransactionType.EXPENSE,
                "Jedzenie",
                null,
                LocalDate.of(2026, 6, 2),
                1L
        );

        boolean hasReadableAmountMessage = validator.validate(request)
                .stream()
                .anyMatch(violation -> violation.getMessage()
                        .equals("Transaction amount is too high. Maximum allowed amount is 9999999.99."));

        assertTrue(hasReadableAmountMessage);
    }

    @Test
    void rejectsBlankCategoryAndMissingAccountId() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("10.00"),
                TransactionType.EXPENSE,
                " ",
                null,
                LocalDate.of(2026, 6, 2),
                null
        );

        Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(violation -> violation.getMessage()
                .equals("Transaction category is required.")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getMessage()
                .equals("Account id is required.")));
    }
}
