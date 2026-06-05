package pl.patryk.budget_api.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.patryk.budget_api.common.BadRequestException;
import pl.patryk.budget_api.common.ResourceNotFoundException;
import pl.patryk.budget_api.transaction.TransactionRepository;
import pl.patryk.budget_api.transaction.TransactionType;

@ExtendWith(MockitoExtension.class)
class CategoryBudgetServiceTest {

    @Mock
    private CategoryBudgetRepository categoryBudgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryBudgetService categoryBudgetService;

    @Test
    void checkMonthlyLimitReturnsWarningWhenExpenseExceedsCategoryBudget() {
        CategoryBudget budget = new CategoryBudget("Jedzenie", new BigDecimal("300.00"));
        LocalDate transactionDate = LocalDate.of(2026, 6, 15);

        when(categoryBudgetRepository.findByCategoryIgnoreCase("Jedzenie")).thenReturn(Optional.of(budget));
        when(transactionRepository.sumMonthlyExpenseByCategory(
                "Jedzenie",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        )).thenReturn(new BigDecimal("350.00"));

        BudgetWarningResponse warning = categoryBudgetService.checkMonthlyLimit(
                TransactionType.EXPENSE,
                "Jedzenie",
                transactionDate
        );

        assertEquals("Jedzenie", warning.category());
        assertEquals(0, new BigDecimal("300.00").compareTo(warning.monthlyLimit()));
        assertEquals(0, new BigDecimal("350.00").compareTo(warning.spentInMonth()));
        assertEquals(0, new BigDecimal("50.00").compareTo(warning.exceededBy()));
    }

    @Test
    void checkMonthlyLimitIgnoresIncomeTransactions() {
        BudgetWarningResponse warning = categoryBudgetService.checkMonthlyLimit(
                TransactionType.INCOME,
                "Wynagrodzenie",
                LocalDate.of(2026, 6, 1)
        );

        assertNull(warning);
        verifyNoInteractions(categoryBudgetRepository, transactionRepository);
    }

    @Test
    void checkMonthlyLimitReturnsNullWhenBudgetDoesNotExist() {
        when(categoryBudgetRepository.findByCategoryIgnoreCase("Rozrywka")).thenReturn(Optional.empty());

        BudgetWarningResponse warning = categoryBudgetService.checkMonthlyLimit(
                TransactionType.EXPENSE,
                "Rozrywka",
                LocalDate.of(2026, 6, 1)
        );

        assertNull(warning);
    }

    @Test
    void checkMonthlyLimitReturnsNullWhenExpenseDoesNotExceedBudget() {
        CategoryBudget budget = new CategoryBudget("Transport", new BigDecimal("250.00"));
        LocalDate transactionDate = LocalDate.of(2026, 6, 15);

        when(categoryBudgetRepository.findByCategoryIgnoreCase("Transport")).thenReturn(Optional.of(budget));
        when(transactionRepository.sumMonthlyExpenseByCategory(
                "Transport",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        )).thenReturn(new BigDecimal("250.00"));

        BudgetWarningResponse warning = categoryBudgetService.checkMonthlyLimit(
                TransactionType.EXPENSE,
                "Transport",
                transactionDate
        );

        assertNull(warning);
    }

    @Test
    void upsertBudgetCreatesNewBudgetWithTrimmedCategory() {
        UpsertCategoryBudgetRequest request = new UpsertCategoryBudgetRequest(
                "  Jedzenie  ",
                new BigDecimal("300.00")
        );

        when(categoryBudgetRepository.findByCategoryIgnoreCase("Jedzenie")).thenReturn(Optional.empty());
        when(categoryBudgetRepository.save(any(CategoryBudget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryBudgetResponse response = categoryBudgetService.upsertBudget(request);

        assertEquals("Jedzenie", response.category());
        assertEquals(0, new BigDecimal("300.00").compareTo(response.monthlyLimit()));
    }

    @Test
    void upsertBudgetUpdatesExistingBudget() {
        CategoryBudget existing = new CategoryBudget("Jedzenie", new BigDecimal("300.00"));
        UpsertCategoryBudgetRequest request = new UpsertCategoryBudgetRequest(
                "Jedzenie",
                new BigDecimal("450.00")
        );

        when(categoryBudgetRepository.findByCategoryIgnoreCase("Jedzenie")).thenReturn(Optional.of(existing));
        when(categoryBudgetRepository.save(existing)).thenReturn(existing);

        CategoryBudgetResponse response = categoryBudgetService.upsertBudget(request);

        assertEquals("Jedzenie", response.category());
        assertEquals(0, new BigDecimal("450.00").compareTo(response.monthlyLimit()));
    }

    @Test
    void upsertBudgetRejectsMoreThanTwoDecimalPlaces() {
        UpsertCategoryBudgetRequest request = new UpsertCategoryBudgetRequest(
                "Jedzenie",
                new BigDecimal("300.123")
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> categoryBudgetService.upsertBudget(request)
        );

        assertEquals("Monthly budget limit can have at most 2 decimal places.", exception.getMessage());
    }

    @Test
    void deleteBudgetThrowsNotFoundWhenBudgetDoesNotExist() {
        when(categoryBudgetRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> categoryBudgetService.deleteBudget(99L)
        );

        assertEquals("Category budget with id 99 was not found.", exception.getMessage());
    }

    @Test
    void deleteBudgetDeletesExistingBudget() {
        CategoryBudget budget = new CategoryBudget("Jedzenie", new BigDecimal("300.00"));
        when(categoryBudgetRepository.findById(1L)).thenReturn(Optional.of(budget));

        categoryBudgetService.deleteBudget(1L);

        verify(categoryBudgetRepository).delete(budget);
    }
}
