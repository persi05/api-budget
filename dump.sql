DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS category_budgets;
DROP TABLE IF EXISTS accounts;

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00
);

CREATE TABLE category_budgets (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(100) NOT NULL UNIQUE,
    monthly_limit NUMERIC(19, 2) NOT NULL CHECK (monthly_limit > 0)
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    type VARCHAR(20) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    category VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    transaction_date DATE NOT NULL,
    account_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT
);

CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_transaction_date ON transactions(transaction_date);
CREATE INDEX idx_transactions_category ON transactions(category);

INSERT INTO accounts (id, name, balance) VALUES
    (1, 'Konto glowne', 3650.00),
    (2, 'Oszczednosci', 1200.00);

INSERT INTO category_budgets (id, category, monthly_limit) VALUES
    (1, 'Jedzenie', 300.00),
    (2, 'Transport', 250.00),
    (3, 'Mieszkanie', 1200.00);

INSERT INTO transactions (id, amount, type, category, description, transaction_date, account_id) VALUES
    (1, 5000.00, 'INCOME', 'Wynagrodzenie', 'Wyplata za maj', '2026-05-31', 1),
    (2, 350.00, 'EXPENSE', 'Jedzenie', 'Zakupy spozywcze', '2026-06-01', 1),
    (3, 100.00, 'EXPENSE', 'Transport', 'Bilet miesieczny', '2026-06-01', 1),
    (4, 900.00, 'EXPENSE', 'Mieszkanie', 'Czynsz', '2026-06-02', 1),
    (5, 1200.00, 'INCOME', 'Oszczednosci', 'Przelew na konto oszczednosciowe', '2026-06-02', 2);

SELECT setval('accounts_id_seq', (SELECT MAX(id) FROM accounts));
SELECT setval('category_budgets_id_seq', (SELECT MAX(id) FROM category_budgets));
SELECT setval('transactions_id_seq', (SELECT MAX(id) FROM transactions));
