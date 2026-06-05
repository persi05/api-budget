package pl.patryk.budget_api.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.patryk.budget_api.budget.BudgetWarningResponse;
import pl.patryk.budget_api.account.Account;
import pl.patryk.budget_api.account.AccountRepository;
import pl.patryk.budget_api.budget.CategoryBudgetService;
import pl.patryk.budget_api.common.BadRequestException;
import pl.patryk.budget_api.common.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryBudgetService categoryBudgetService;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void createExpenseSubtractsAmountFromAccountBalance() {
        Account account = new Account("Konto glowne");
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("125.50"),
                TransactionType.EXPENSE,
                "Jedzenie",
                "Zakupy",
                LocalDate.of(2026, 6, 2),
                1L
        );

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryBudgetService.checkMonthlyLimit(eq(TransactionType.EXPENSE), eq("Jedzenie"), eq(request.transactionDate())))
                .thenReturn(null);

        TransactionResponse response = transactionService.createTransaction(request);

        assertEquals(0, new BigDecimal("-125.50").compareTo(account.getBalance()));
        assertEquals(TransactionType.EXPENSE, response.type());
        assertEquals("Jedzenie", response.category());
        assertNull(response.budgetWarning());
    }

    @Test
    void deleteExpenseAddsAmountBackToAccountBalance() {
        Account account = new Account("Konto glowne");
        TransactionEntity transaction = new TransactionEntity(
                new BigDecimal("50.00"),
                TransactionType.EXPENSE,
                "Transport",
                null,
                LocalDate.of(2026, 6, 2),
                account
        );

        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(10L);

        assertEquals(0, new BigDecimal("50.00").compareTo(account.getBalance()));
        verify(transactionRepository).delete(transaction);
    }

    @Test
    void createIncomeAddsAmountToAccountBalance() {
        Account account = new Account("Konto glowne");
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("5000.00"),
                TransactionType.INCOME,
                "Wynagrodzenie",
                null,
                LocalDate.of(2026, 6, 1),
                1L
        );

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.createTransaction(request);

        assertEquals(0, new BigDecimal("5000.00").compareTo(account.getBalance()));
    }

    @Test
    void createTransactionTrimsCategoryAndDescription() {
        Account account = new Account("Konto glowne");
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("20.00"),
                TransactionType.EXPENSE,
                "  Jedzenie  ",
                "  Obiad  ",
                LocalDate.of(2026, 6, 3),
                1L
        );

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.createTransaction(request);

        assertEquals("Jedzenie", response.category());
        assertEquals("Obiad", response.description());
    }

    @Test
    void createTransactionRejectsAmountWithMoreThanTwoDecimalPlaces() {
        Account account = new Account("Konto glowne");
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("10.123"),
                TransactionType.EXPENSE,
                "Jedzenie",
                null,
                LocalDate.of(2026, 6, 3),
                1L
        );

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> transactionService.createTransaction(request)
        );

        assertEquals("Transaction amount can have at most 2 decimal places.", exception.getMessage());
    }

    @Test
    void createTransactionThrowsNotFoundWhenAccountDoesNotExist() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("10.00"),
                TransactionType.EXPENSE,
                "Jedzenie",
                null,
                LocalDate.of(2026, 6, 3),
                99L
        );

        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.createTransaction(request)
        );

        assertEquals("Account with id 99 was not found.", exception.getMessage());
        verifyNoInteractions(categoryBudgetService);
    }

    @Test
    void deleteIncomeSubtractsAmountFromAccountBalance() {
        Account account = new Account("Konto glowne");
        account.addToBalance(new BigDecimal("500.00"));
        TransactionEntity transaction = new TransactionEntity(
                new BigDecimal("150.00"),
                TransactionType.INCOME,
                "Zwrot",
                null,
                LocalDate.of(2026, 6, 2),
                account
        );

        when(transactionRepository.findById(11L)).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(11L);

        assertEquals(0, new BigDecimal("350.00").compareTo(account.getBalance()));
        verify(transactionRepository).delete(transaction);
    }

    @Test
    void deleteTransactionThrowsNotFoundWhenTransactionDoesNotExist() {
        when(transactionRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.deleteTransaction(404L)
        );

        assertEquals("Transaction with id 404 was not found.", exception.getMessage());
    }

    @Test
    void getTransactionsRejectsInvalidDateRange() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> transactionService.getTransactions(
                        LocalDate.of(2026, 6, 10),
                        LocalDate.of(2026, 6, 1),
                        null
                )
        );

        assertEquals("Parameter 'from' cannot be after parameter 'to'.", exception.getMessage());
    }

    @Test
    void getTransactionsUsesFiltersAndDescendingSort() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);

        when(transactionRepository.findAll(anySpecification(), any(Sort.class))).thenReturn(List.of());

        transactionService.getTransactions(from, to, " Jedzenie ");

        verify(transactionRepository).findAll(anySpecification(), eq(
                Sort.by(Sort.Direction.DESC, "transactionDate")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        ));
    }

    private Specification<TransactionEntity> anySpecification() {
        return any();
    }
}
