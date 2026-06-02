package pl.patryk.budget_api.account;

import java.math.BigDecimal;

public record AccountResponse(Long id, String name, BigDecimal balance) {
    static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getName(), account.getBalance());
    }
}
