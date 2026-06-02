package pl.patryk.budget_api.summary;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.patryk.budget_api.common.BadRequestException;
import pl.patryk.budget_api.transaction.TransactionEntity;
import pl.patryk.budget_api.transaction.TransactionType;

@Service
public class SummaryService {

    private final EntityManager entityManager;

    public SummaryService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        BigDecimal totalIncome = sumAmountByType(TransactionType.INCOME, from, to);
        BigDecimal totalExpense = sumAmountByType(TransactionType.EXPENSE, from, to);
        List<CategoryExpenseResponse> expensesByCategory = sumExpensesGroupedByCategory(from, to);

        return new SummaryResponse(
                zeroIfNull(totalIncome),
                zeroIfNull(totalExpense),
                expensesByCategory
        );
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Parameter 'from' cannot be after parameter 'to'.");
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value;
    }

    private BigDecimal sumAmountByType(TransactionType type, LocalDate from, LocalDate to) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = criteriaBuilder.createQuery(BigDecimal.class);
        Root<TransactionEntity> transaction = query.from(TransactionEntity.class);
        query.select(criteriaBuilder.sum(transaction.get("amount")));
        query.where(summaryPredicates(criteriaBuilder, transaction, type, from, to).toArray(Predicate[]::new));
        return entityManager.createQuery(query).getSingleResult();
    }

    private List<CategoryExpenseResponse> sumExpensesGroupedByCategory(LocalDate from, LocalDate to) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<CategoryExpenseResponse> query = criteriaBuilder.createQuery(CategoryExpenseResponse.class);
        Root<TransactionEntity> transaction = query.from(TransactionEntity.class);

        query.select(criteriaBuilder.construct(
                CategoryExpenseResponse.class,
                transaction.get("category"),
                criteriaBuilder.sum(transaction.get("amount"))
        ));
        query.where(summaryPredicates(criteriaBuilder, transaction, TransactionType.EXPENSE, from, to)
                .toArray(Predicate[]::new));
        query.groupBy(transaction.get("category"));
        query.orderBy(criteriaBuilder.asc(transaction.get("category")));

        return entityManager.createQuery(query).getResultList();
    }

    private List<Predicate> summaryPredicates(
            CriteriaBuilder criteriaBuilder,
            Root<TransactionEntity> transaction,
            TransactionType type,
            LocalDate from,
            LocalDate to
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(transaction.get("type"), type));
        if (from != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(transaction.get("transactionDate"), from));
        }
        if (to != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(transaction.get("transactionDate"), to));
        }
        return predicates;
    }
}
