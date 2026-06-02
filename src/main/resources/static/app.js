const state = {
    accounts: [],
    transactions: [],
    budgets: [],
    summary: null
};

const money = new Intl.NumberFormat("pl-PL", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
});

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("transactionDate").valueAsDate = new Date();
    bindForms();
    runSafely(loadAll);
});

function bindForms() {
    document.getElementById("refreshButton").addEventListener("click", () => runSafely(loadAll));
    document.getElementById("accountForm").addEventListener("submit", event => runSafely(() => createAccount(event)));
    document.getElementById("budgetForm").addEventListener("submit", event => runSafely(() => saveBudget(event)));
    document.getElementById("transactionForm").addEventListener("submit", event => runSafely(() => createTransaction(event)));
    document.getElementById("filterForm").addEventListener("submit", event => {
        event.preventDefault();
        runSafely(async () => {
            await Promise.all([loadTransactions(), loadSummary()]);
        });
    });
    document.getElementById("clearFiltersButton").addEventListener("click", () => {
        document.getElementById("filterFrom").value = "";
        document.getElementById("filterTo").value = "";
        document.getElementById("filterCategory").value = "";
        runSafely(async () => {
            await Promise.all([loadTransactions(), loadSummary()]);
        });
    });
}

async function loadAll() {
    hideWarning();
    await Promise.all([loadAccounts(), loadBudgets(), loadTransactions(), loadSummary()]);
}

async function loadAccounts() {
    state.accounts = await api("/accounts");
    renderAccounts();
    renderAccountOptions();
}

async function loadBudgets() {
    state.budgets = await api("/category-budgets");
    renderBudgets();
}

async function loadTransactions() {
    state.transactions = await api(`/transactions${filterQuery()}`);
    renderTransactions();
}

async function loadSummary() {
    state.summary = await api(`/summary${summaryQuery()}`);
    renderSummary();
}

async function createAccount(event) {
    event.preventDefault();
    const input = document.getElementById("accountName");
    await api("/accounts", {
        method: "POST",
        body: JSON.stringify({ name: input.value.trim() })
    });
    input.value = "";
    showToast("Konto dodane.");
    await loadAccounts();
}

async function saveBudget(event) {
    event.preventDefault();
    const category = document.getElementById("budgetCategory");
    const monthlyLimit = document.getElementById("budgetLimit");
    await api("/category-budgets", {
        method: "POST",
        body: JSON.stringify({
            category: category.value.trim(),
            monthlyLimit: monthlyLimit.value
        })
    });
    category.value = "";
    monthlyLimit.value = "";
    showToast("Limit zapisany.");
    await loadBudgets();
}

async function createTransaction(event) {
    event.preventDefault();
    const payload = {
        accountId: Number(document.getElementById("transactionAccount").value),
        type: document.getElementById("transactionType").value,
        amount: document.getElementById("transactionAmount").value,
        category: document.getElementById("transactionCategory").value.trim(),
        transactionDate: document.getElementById("transactionDate").value,
        description: optionalValue("transactionDescription")
    };
    const response = await api("/transactions", {
        method: "POST",
        body: JSON.stringify(payload)
    });
    document.getElementById("transactionAmount").value = "";
    document.getElementById("transactionDescription").value = "";
    renderWarning(response.budgetWarning);
    showToast("Transakcja dodana.");
    await Promise.all([loadAccounts(), loadTransactions(), loadSummary()]);
}

async function deleteAccount(id) {
    await api(`/accounts/${id}`, { method: "DELETE" });
    showToast("Konto usuniete.");
    await loadAccounts();
}

async function deleteBudget(id) {
    await api(`/category-budgets/${id}`, { method: "DELETE" });
    showToast("Limit usuniety.");
    await loadBudgets();
}

async function deleteTransaction(id) {
    await api(`/transactions/${id}`, { method: "DELETE" });
    showToast("Transakcja usunieta.");
    await Promise.all([loadAccounts(), loadTransactions(), loadSummary()]);
}

function renderAccounts() {
    document.getElementById("accountCount").textContent = state.accounts.length;
    const container = document.getElementById("accountsList");
    container.innerHTML = "";
    if (state.accounts.length === 0) {
        container.append(emptyItem("Brak kont."));
        return;
    }
    for (const account of state.accounts) {
        const item = document.createElement("div");
        item.className = "item";
        item.innerHTML = `
            <div>
                <strong>${escapeHtml(account.name)}</strong>
                <span>Saldo: ${money.format(Number(account.balance))}</span>
            </div>
            <div class="item-actions">
                <a class="button-link" href="/accounts/${account.id}/transactions/export">CSV</a>
                <button class="danger" type="button" data-account-delete="${account.id}">Usun</button>
            </div>
        `;
        container.append(item);
    }
    container.querySelectorAll("[data-account-delete]").forEach(button => {
        button.addEventListener("click", () => deleteAccount(button.dataset.accountDelete));
    });
}

function renderAccountOptions() {
    const select = document.getElementById("transactionAccount");
    const selected = select.value;
    select.innerHTML = "";
    for (const account of state.accounts) {
        const option = document.createElement("option");
        option.value = account.id;
        option.textContent = `${account.name} (${money.format(Number(account.balance))})`;
        select.append(option);
    }
    if (selected) {
        select.value = selected;
    }
}

function renderBudgets() {
    const container = document.getElementById("budgetsList");
    container.innerHTML = "";
    if (state.budgets.length === 0) {
        container.append(emptyItem("Brak limitow."));
        return;
    }
    for (const budget of state.budgets) {
        const item = document.createElement("div");
        item.className = "item";
        item.innerHTML = `
            <div>
                <strong>${escapeHtml(budget.category)}</strong>
                <span>Limit miesieczny: ${money.format(Number(budget.monthlyLimit))}</span>
            </div>
            <button class="danger" type="button" data-budget-delete="${budget.id}">Usun</button>
        `;
        container.append(item);
    }
    container.querySelectorAll("[data-budget-delete]").forEach(button => {
        button.addEventListener("click", () => deleteBudget(button.dataset.budgetDelete));
    });
}

function renderTransactions() {
    const table = document.getElementById("transactionsTable");
    table.innerHTML = "";
    if (state.transactions.length === 0) {
        const row = document.createElement("tr");
        row.innerHTML = `<td colspan="6" class="muted">Brak transakcji.</td>`;
        table.append(row);
        return;
    }
    for (const transaction of state.transactions) {
        const amountClass = transaction.type === "INCOME" ? "amount-income" : "amount-expense";
        const sign = transaction.type === "INCOME" ? "+" : "-";
        const row = document.createElement("tr");
        row.innerHTML = `
            <td>${transaction.transactionDate}</td>
            <td>${escapeHtml(transaction.accountName)}</td>
            <td>${transaction.type}</td>
            <td>${escapeHtml(transaction.category)}</td>
            <td class="${amountClass}">${sign}${money.format(Number(transaction.amount))}</td>
            <td><button class="danger" type="button" data-transaction-delete="${transaction.id}">Usun</button></td>
        `;
        table.append(row);
    }
    table.querySelectorAll("[data-transaction-delete]").forEach(button => {
        button.addEventListener("click", () => deleteTransaction(button.dataset.transactionDelete));
    });
}

function renderSummary() {
    if (!state.summary) {
        return;
    }
    document.getElementById("totalIncome").textContent = money.format(Number(state.summary.totalIncome));
    document.getElementById("totalExpense").textContent = money.format(Number(state.summary.totalExpense));
    const container = document.getElementById("categorySummary");
    container.innerHTML = "";
    const rows = state.summary.expensesByCategory || [];
    if (rows.length === 0) {
        container.append(emptyItem("Brak wydatkow w podanym zakresie."));
        return;
    }
    const max = Math.max(...rows.map(row => Number(row.total)), 1);
    for (const row of rows) {
        const percent = Math.max(4, Math.round((Number(row.total) / max) * 100));
        const item = document.createElement("div");
        item.className = "bar-row";
        item.innerHTML = `
            <div class="bar-head">
                <strong>${escapeHtml(row.category)}</strong>
                <span>${money.format(Number(row.total))}</span>
            </div>
            <div class="bar-track"><div class="bar-fill" style="width: ${percent}%"></div></div>
        `;
        container.append(item);
    }
}

function renderWarning(warning) {
    if (!warning) {
        hideWarning();
        return;
    }
    const box = document.getElementById("warningBox");
    box.textContent = warning.message;
    box.classList.remove("hidden");
}

function hideWarning() {
    const box = document.getElementById("warningBox");
    box.textContent = "";
    box.classList.add("hidden");
}

function filterQuery() {
    const params = new URLSearchParams();
    addParam(params, "from", document.getElementById("filterFrom").value);
    addParam(params, "to", document.getElementById("filterTo").value);
    addParam(params, "category", document.getElementById("filterCategory").value.trim());
    const query = params.toString();
    return query ? `?${query}` : "";
}

function summaryQuery() {
    const params = new URLSearchParams();
    addParam(params, "from", document.getElementById("filterFrom").value);
    addParam(params, "to", document.getElementById("filterTo").value);
    const query = params.toString();
    return query ? `?${query}` : "";
}

function addParam(params, key, value) {
    if (value) {
        params.set(key, value);
    }
}

function optionalValue(id) {
    const value = document.getElementById(id).value.trim();
    return value ? value : null;
}

async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: { "Content-Type": "application/json" },
        ...options
    });
    if (response.status === 204) {
        return null;
    }
    const contentType = response.headers.get("content-type") || "";
    const body = contentType.includes("application/json") ? await response.json() : await response.text();
    if (!response.ok) {
        const details = body.details ? Object.values(body.details).filter(Boolean) : [];
        const message = details[0] || body.message || `Blad wywolania API: ${path}`;
        showToast(message);
        throw new Error(message);
    }
    return body;
}

async function runSafely(action) {
    try {
        await action();
    } catch (error) {
        showToast(error.message || "Nie udalo sie wykonac operacji.");
        console.error(error);
    }
}

function emptyItem(text) {
    const item = document.createElement("div");
    item.className = "item muted";
    item.textContent = text;
    return item;
}

function showToast(message) {
    const toast = document.getElementById("toast");
    toast.textContent = message;
    toast.classList.remove("hidden");
    window.clearTimeout(showToast.timeoutId);
    showToast.timeoutId = window.setTimeout(() => toast.classList.add("hidden"), 3200);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
