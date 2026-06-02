package pl.patryk.budget_api.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.patryk.budget_api.account.Account;
import pl.patryk.budget_api.account.AccountRepository;
import pl.patryk.budget_api.budget.BudgetWarningResponse;
import pl.patryk.budget_api.budget.CategoryBudgetService;
import pl.patryk.budget_api.common.BadRequestException;
import pl.patryk.budget_api.common.ResourceNotFoundException;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryBudgetService categoryBudgetService;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            CategoryBudgetService categoryBudgetService
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryBudgetService = categoryBudgetService;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(LocalDate from, LocalDate to, String category) {
        validateDateRange(from, to);
        String normalizedCategory = normalizeCategoryFilter(category);
        Sort sort = Sort.by(Sort.Direction.DESC, "transactionDate")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return transactionRepository.findAll(TransactionSpecifications.withFilters(from, to, normalizedCategory), sort)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account with id " + request.accountId() + " was not found."
                ));

        BigDecimal amount = normalizeAmount(request.amount());
        String category = request.category().trim();
        TransactionEntity transaction = new TransactionEntity(
                amount,
                request.type(),
                category,
                normalizeDescription(request.description()),
                request.transactionDate(),
                account
        );

        applyTransaction(account, request.type(), amount);
        TransactionEntity savedTransaction = transactionRepository.save(transaction);
        BudgetWarningResponse warning = categoryBudgetService.checkMonthlyLimit(
                request.type(),
                category,
                request.transactionDate()
        );
        return TransactionResponse.from(savedTransaction, warning);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        TransactionEntity transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction with id " + id + " was not found."
                ));

        reverseTransaction(transaction.getAccount(), transaction.getType(), transaction.getAmount());
        transactionRepository.delete(transaction);
    }

    private void applyTransaction(Account account, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.INCOME) {
            account.addToBalance(amount);
            return;
        }
        account.subtractFromBalance(amount);
    }

    private void reverseTransaction(Account account, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.INCOME) {
            account.subtractFromBalance(amount);
            return;
        }
        account.addToBalance(amount);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BadRequestException("Transaction amount can have at most 2 decimal places.");
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Parameter 'from' cannot be after parameter 'to'.");
        }
    }

    private String normalizeCategoryFilter(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return category.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
