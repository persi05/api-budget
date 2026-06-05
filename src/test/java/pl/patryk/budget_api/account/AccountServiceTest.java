package pl.patryk.budget_api.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.patryk.budget_api.common.ConflictException;
import pl.patryk.budget_api.common.ResourceNotFoundException;
import pl.patryk.budget_api.transaction.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountTrimsNameAndStartsWithZeroBalance() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.createAccount(new CreateAccountRequest("  Konto glowne  "));

        assertEquals("Konto glowne", response.name());
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(response.balance()));
    }

    @Test
    void createAccountRejectsDuplicateNameIgnoringCaseAndSpaces() {
        when(accountRepository.existsByNameIgnoreCase("Konto glowne")).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> accountService.createAccount(new CreateAccountRequest("  Konto glowne  "))
        );

        assertEquals("Account with name 'Konto glowne' already exists.", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getAccountThrowsNotFoundWhenAccountDoesNotExist() {
        when(accountRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.getAccount(404L)
        );

        assertEquals("Account with id 404 was not found.", exception.getMessage());
    }

    @Test
    void deleteAccountRejectsAccountWithTransactions() {
        Account account = new Account("Konto glowne");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.existsByAccountId(1L)).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> accountService.deleteAccount(1L)
        );

        assertEquals("Account cannot be deleted because it has transactions.", exception.getMessage());
        verify(accountRepository, never()).delete(account);
    }

    @Test
    void deleteAccountDeletesAccountWithoutTransactions() {
        Account account = new Account("Konto glowne");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.existsByAccountId(1L)).thenReturn(false);

        accountService.deleteAccount(1L);

        verify(accountRepository).delete(account);
    }
}
