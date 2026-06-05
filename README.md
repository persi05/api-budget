# Budget API

REST API do zarządzania budżetem osobistym. Aplikacja pozwala tworzyć konta, dodawać przychody i wydatki, automatycznie aktualizuje saldo konta, liczy podsumowanie wydatków oraz udostępnia minimalne GUI.

## Technologie

- Java 17
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Maven
- Docker Compose

## Uruchomienie przez Docker Compose

Wymagania:

- Docker Desktop lub Docker Engine

Start:

```powershell
docker compose up --build -d
```

Aplikacja:

```text
http://localhost:8080
```

PostgreSQL:

```text
localhost:5432
database: wartość DB_NAME z .env
user: wartość DB_USER z .env
password: wartość DB_PASSWORD z .env
```

Zatrzymanie:

```powershell
docker compose down
```

Usuniecie wolumenu bazy:

```powershell
docker compose down -v
```

## Uruchomienie lokalne bez Dockera

Wymagania:

- Java 17+
- PostgreSQL
- Maven Wrapper z repozytorium

Utworz baze:

```sql
CREATE DATABASE budget_api;
```

Opcjonalnie wczytaj dane startowe:

```powershell
psql -U postgres -d budget_api -f dump.sql
```

Uruchom aplikacje:

```powershell
cmd /c mvnw.cmd spring-boot:run
```

## GUI

GUI jest serwowany przez Springa:

```text
http://localhost:8080
```

W panelu mozna:

- dodawać i usuwać konta
- dodawać i usuwać transakcje
- filtrować transakcje
- ustawiać limity miesięczne dla kategorii
- sprawdzać podsumowanie
- eksportować transakcje konta do CSV

## Endpointy

### Konta

```http
GET /accounts
POST /accounts
GET /accounts/{id}
DELETE /accounts/{id}
GET /accounts/{id}/transactions/export
```

Przyklad utworzenia konta:

```json
{
  "name": "Konto glowne"
}
```

Konta mozna usunac tylko wtedy, gdy nie maja transakcji. W przeciwnym razie API zwroci `409 Conflict`.

### Transakcje

```http
GET /transactions
GET /transactions?from=2026-06-01&to=2026-06-30&category=Jedzenie
POST /transactions
DELETE /transactions/{id}
```

Przyklad dodania transakcji:

```json
{
  "amount": 125.50,
  "type": "EXPENSE",
  "category": "Jedzenie",
  "description": "Zakupy",
  "transactionDate": "2026-06-02",
  "accountId": 1
}
```

Kwota musi byc z zakresu `0.01` - `9999999.99` i moze miec maksymalnie 2 miejsca po przecinku. Dla zbyt duzej kwoty API zwraca czytelny komunikat walidacyjny, np. `Transaction amount is too high. Maximum allowed amount is 9999999.99.`

### Limity kategorii

```http
GET /category-budgets
POST /category-budgets
DELETE /category-budgets/{id}
```

Przyklad ustawienia limitu:

```json
{
  "category": "Jedzenie",
  "monthlyLimit": 300.00
}
```

Limit jest miesieczny i przypisany do kategorii. Po dodaniu wydatku API sumuje wydatki z tej kategorii w miesiacu daty transakcji. Jesli limit zostanie przekroczony, odpowiedz `POST /transactions` zawiera pole `budgetWarning`.

### Podsumowanie

```http
GET /summary
GET /summary?from=2026-06-01&to=2026-06-30
```

Zwraca:

- laczne przychody
- laczne wydatki
- wydatki pogrupowane po kategorii

## Testy

Uruchomienie testow:

```powershell
cmd /c mvnw.cmd test
```
