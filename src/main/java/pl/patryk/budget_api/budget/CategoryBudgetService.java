package pl.patryk.budget_api.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.patryk.budget_api.common.BadRequestException;
import pl.patryk.budget_api.common.ResourceNotFoundException;
import pl.patryk.budget_api.transaction.TransactionRepository;
import pl.patryk.budget_api.transaction.TransactionType;

@Service
public class CategoryBudgetService {

    private final CategoryBudgetRepository categoryBudgetRepository;
    private final TransactionRepository transactionRepository;

    public CategoryBudgetService(
            CategoryBudgetRepository categoryBudgetRepository,
            TransactionRepository transactionRepository
    ) {
        this.categoryBudgetRepository = categoryBudgetRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryBudgetResponse> getBudgets() {
        return categoryBudgetRepository.findAll()
                .stream()
                .map(CategoryBudgetResponse::from)
                .toList();
    }

    @Transactional
    public CategoryBudgetResponse upsertBudget(UpsertCategoryBudgetRequest request) {
        String category = request.category().trim();
        BigDecimal monthlyLimit = normalizeAmount(request.monthlyLimit());
        CategoryBudget budget = categoryBudgetRepository.findByCategoryIgnoreCase(category)
                .map(existing -> {
                    existing.update(category, monthlyLimit);
                    return existing;
                })
                .orElseGet(() -> new CategoryBudget(category, monthlyLimit));

        return CategoryBudgetResponse.from(categoryBudgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(Long id) {
        CategoryBudget budget = categoryBudgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category budget with id " + id + " was not found."
                ));
        categoryBudgetRepository.delete(budget);
    }

    @Transactional(readOnly = true)
    public BudgetWarningResponse checkMonthlyLimit(TransactionType type, String category, LocalDate transactionDate) {
        if (type != TransactionType.EXPENSE) {
            return null;
        }

        return categoryBudgetRepository.findByCategoryIgnoreCase(category)
                .map(budget -> buildWarningIfExceeded(budget, transactionDate))
                .orElse(null);
    }

    private BudgetWarningResponse buildWarningIfExceeded(CategoryBudget budget, LocalDate transactionDate) {
        YearMonth month = YearMonth.from(transactionDate);
        BigDecimal spent = transactionRepository.sumMonthlyExpenseByCategory(
                budget.getCategory(),
                month.atDay(1),
                month.atEndOfMonth()
        );
        if (spent == null) {
            spent = BigDecimal.ZERO.setScale(2);
        }
        if (spent.compareTo(budget.getMonthlyLimit()) <= 0) {
            return null;
        }

        BigDecimal exceededBy = spent.subtract(budget.getMonthlyLimit());
        return new BudgetWarningResponse(
                budget.getCategory(),
                budget.getMonthlyLimit(),
                spent,
                exceededBy,
                "Monthly budget limit for category '" + budget.getCategory() + "' has been exceeded by " + exceededBy + "."
        );
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BadRequestException("Monthly budget limit can have at most 2 decimal places.");
        }
    }
}
