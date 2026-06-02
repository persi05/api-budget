package pl.patryk.budget_api.account;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.patryk.budget_api.common.ConflictException;
import pl.patryk.budget_api.common.ResourceNotFoundException;
import pl.patryk.budget_api.transaction.TransactionEntity;
import pl.patryk.budget_api.transaction.TransactionRepository;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll()
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = new Account(request.name().trim());
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        return AccountResponse.from(findAccount(id));
    }

    @Transactional
    public void deleteAccount(Long id) {
        Account account = findAccount(id);
        if (transactionRepository.existsByAccountId(id)) {
            throw new ConflictException("Account cannot be deleted because it has transactions.");
        }
        accountRepository.delete(account);
    }

    @Transactional(readOnly = true)
    public byte[] exportTransactionsToCsv(Long id) {
        Account account = findAccount(id);
        List<TransactionEntity> transactions = transactionRepository.findByAccountIdOrderByTransactionDateDescIdDesc(id);
        StringBuilder csv = new StringBuilder();
        csv.append("id,amount,type,category,description,transactionDate,accountId,accountName\r\n");
        for (TransactionEntity transaction : transactions) {
            csv.append(transaction.getId()).append(',')
                    .append(transaction.getAmount()).append(',')
                    .append(transaction.getType()).append(',')
                    .append(escape(transaction.getCategory())).append(',')
                    .append(escape(transaction.getDescription())).append(',')
                    .append(transaction.getTransactionDate()).append(',')
                    .append(account.getId()).append(',')
                    .append(escape(account.getName()))
                    .append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Account findAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account with id " + id + " was not found."));
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
