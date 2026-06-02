package pl.patryk.budget_api.budget;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryBudgetRepository extends JpaRepository<CategoryBudget, Long> {

    Optional<CategoryBudget> findByCategoryIgnoreCase(String category);
}
