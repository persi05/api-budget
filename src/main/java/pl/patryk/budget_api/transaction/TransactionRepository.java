package pl.patryk.budget_api.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long>, JpaSpecificationExecutor<TransactionEntity> {

    boolean existsByAccountId(Long accountId);

    List<TransactionEntity> findByAccountIdOrderByTransactionDateDescIdDesc(Long accountId);

    @Query("""
            select sum(t.amount)
            from TransactionEntity t
            where t.type = pl.patryk.budget_api.transaction.TransactionType.EXPENSE
              and lower(t.category) = lower(:category)
              and t.transactionDate >= :from
              and t.transactionDate <= :to
            """)
    BigDecimal sumMonthlyExpenseByCategory(
            @Param("category") String category,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
