package pl.patryk.budget_api.budget;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/category-budgets")
public class CategoryBudgetController {

    private final CategoryBudgetService categoryBudgetService;

    public CategoryBudgetController(CategoryBudgetService categoryBudgetService) {
        this.categoryBudgetService = categoryBudgetService;
    }

    @GetMapping
    public List<CategoryBudgetResponse> getBudgets() {
        return categoryBudgetService.getBudgets();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryBudgetResponse upsertBudget(@Valid @RequestBody UpsertCategoryBudgetRequest request) {
        return categoryBudgetService.upsertBudget(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBudget(@PathVariable Long id) {
        categoryBudgetService.deleteBudget(id);
    }
}
