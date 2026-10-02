# Finance Advisor V2 — Context & Holdings State Management (`finance.v2`)

> **Milestone 2 Objective**: Extend Finance Advisor V1 by introducing **relational database persistence** and **dynamic session state management** using Google ADK Java. Learn how to implement custom ADK tools (`BaseTool`), query an embedded SQLite database in WAL mode, inject structured portfolio holdings into session state via `ToolContext#state()`, bind dynamic prompt templates (`{placeholder?}`), and maintain conversational context across multi-turn interactions without redundant database queries.

---

## 1. High-Level Architecture & Multi-Turn State Flow

```
   Turn 1: "What does my portfolio look like?"
       │ (State is empty: customer_id is null)
       ▼
   [Finance Advisor V2 asks: "Please provide your Customer ID (e.g. 1001 or 1002)"]
       │
   Turn 2: "My Customer ID is 1001"
       │
       ▼
   +─────────────────────────────────────────────────────────────+
   |                  LoadCustomerPortfolioTool                  |
   | 1. Receives customer_id="1001"                              |
   | 2. Queries SQLite tables: customer & portfolio_holding      |
   | 3. Injects into session state:                              |
   |    toolContext.state().put("customer_id", "1001")           |
   |    toolContext.state().put("portfolio_id", "100001")        |
   |    toolContext.state().put("portfolio_holdings", summary)   |
   |    toolContext.state().put("portfolio_loaded", true)        |
   +──────────────────────────────┬──────────────────────────────+
                                  │
                                  ▼
   [Finance Advisor V2 renders formatted Markdown table of holdings]
       │
   Turn 3: "What is my total invested capital in Reliance?"
       │ (State is populated: {portfolio_holdings} active in context)
       ▼
   [Finance Advisor V2 answers directly from session state: 2000.0 INR]
   (Zero database queries executed; zero customer ID re-prompts)
```

---

## 2. Core Google ADK Concepts Demonstrated

### 2.1 Dynamic Prompt Templating with State Placeholders
ADK supports prompt interpolation using `{placeholder?}` syntax. If a key exists in session state, ADK automatically injects its value into the instruction before calling the model:
```
Current Session State:
- Customer ID: {customer_id?}
- Portfolio ID: {portfolio_id?}
- Active Portfolio Holdings:
{portfolio_holdings?}
```
The question mark `?` indicates that the placeholder is optional; if absent from state, it resolves to an empty string rather than throwing an error.

### 2.2 Custom ADK Tools via `BaseTool`
Custom tools extend `com.google.adk.tools.BaseTool` and override two methods:
1. `declaration()`: Returns an `Optional<FunctionDeclaration>` defining the JSON schema for tool arguments:
   ```java
   Map<String, Schema> properties = new HashMap<>();
   properties.put("customer_id", Schema.builder()
           .type(Type.Known.STRING)
           .description("The customer ID whose portfolio holdings to retrieve.")
           .build());
   ```
2. `runAsync(Map<String, Object> args, ToolContext toolContext)`: Executes business logic and returns a reactive `Single<Map<String, Object>>`.

### 2.3 Dynamic State Injection via `ToolContext#state()`
Tools can inspect and mutate the ongoing conversation's session state. When `LoadCustomerPortfolioTool` executes, it registers the loaded records directly into `toolContext.state()`:
```java
if (toolContext != null && toolContext.state() != null) {
    toolContext.state().put("customer_id", portfolio.customerId());
    toolContext.state().put("portfolio_id", portfolio.portfolioId());
    toolContext.state().put("portfolio_holdings", portfolio.toFormattedSummary());
    toolContext.state().put("portfolio_holdings_list", portfolio.holdingsAsMaps());
    toolContext.state().put("total_invested", portfolio.totalInvested());
    toolContext.state().put("portfolio_loaded", true);
}
```
This enables subsequent turns to access the portfolio details immediately without re-running the database query.

---

## 3. Relational Database Schema & Domain Model

SQLite persistence is managed by `CustomerPortfolioRepository` using WAL (Write-Ahead Logging) mode and connection pooling. The database is initialized and seeded automatically:

```sql
-- Table 1: customer (Composite Primary Key)
CREATE TABLE IF NOT EXISTS customer (
    customer_id TEXT NOT NULL,
    portfolio_id TEXT NOT NULL,
    PRIMARY KEY (customer_id, portfolio_id)
);

-- Table 2: portfolio_holding (Holdings per portfolio)
CREATE TABLE IF NOT EXISTS portfolio_holding (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    portfolio_id TEXT NOT NULL,
    symbol TEXT NOT NULL,
    name TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    buy_price REAL NOT NULL,
    currency TEXT NOT NULL DEFAULT 'INR',
    bought_date TEXT NOT NULL
);
```

### Seed Test Data
- **Customer 1001** (Portfolio `100001`):
  - `RELIANCE` (Reliance Industries): Qty: 2, Buy: ₹1000.00, Bought: 2025-01-01
  - `TCS` (Tata Consultancy Services): Qty: 2, Buy: ₹1000.00, Bought: 2025-01-01
  - *Total Invested: ₹4,000.00*
- **Customer 1002** (Portfolio `100002`):
  - `INFY` (Infosys Ltd): Qty: 10, Buy: ₹1500.00, Bought: 2026-08-10
  - *Total Invested: ₹15,000.00*

---

## 4. Key Files & Implementation Reference

| File | Package Path | Role & Purpose |
|---|---|---|
| `PortfolioModels.java` | `src/main/java/com/google/adk/finance/v2/` | Strongly typed Java records (`CustomerRecord`, `PortfolioHoldingRecord`, `CustomerPortfolio`). |
| `CustomerPortfolioRepository.java` | `src/main/java/com/google/adk/finance/v2/` | SQLite DAO managing schema creation, auto-seeding, and holding lookups in WAL mode. |
| `LoadCustomerPortfolioTool.java` | `src/main/java/com/google/adk/finance/tools/` | Custom `BaseTool` querying SQLite and injecting records into `toolContext.state()`. |
| `FinanceAgentV2Factory.java` | `src/main/java/com/google/adk/finance/v2/` | Root factory creating `finance_advisor_v2` with state placeholders and tool binding. |
| `FinanceConsoleV2.java` | `src/main/java/com/google/adk/finance/v2/` | Interactive CLI displaying live session state transitions and answering questions across multi-turn sessions. |
| `CustomerPortfolioRepositoryTest.java` | `src/test/java/com/google/adk/finance/v2/` | Unit test verifying SQLite schema creation, query correctness, and test data seeding. |
| `LoadCustomerPortfolioToolTest.java` | `src/test/java/com/google/adk/finance/v2/` | Unit test verifying schema generation, tool execution, and state injection. |
| `FinanceV2IntegrationTest.java` | `src/test/java/com/google/adk/finance/v2/` | Integration tests verifying multi-turn state preservation and agent execution. |

---

## 5. Evolutionary Comparison: V1 vs. V2

| Capability | Finance Advisor V1 | Finance Advisor V2 |
|---|:---:|:---:|
| **Conversational Analyst Persona** | Yes | Yes |
| **Model Abstraction (Gemini / Ollama)** | Yes | Yes |
| **Session Lifecycle (`InMemoryRunner`)** | Yes | Yes |
| **Relational Database Ingestion** | No | **Yes (SQLite WAL mode)** |
| **Custom ADK Tools (`BaseTool`)** | No | **Yes (`load_customer_portfolio`)** |
| **Dynamic State Injection (`ToolContext#state()`)** | No | **Yes** |
| **Instruction State Templating (`{placeholder?}`)** | No | **Yes** |
| **Multi-Turn Memory without Re-Querying** | No | **Yes** |
| **Deterministic Financial Math** | No (LLM arithmetic) | No (Introduced in V3) |
| **Live Web Search Grounding** | No | No (Introduced in V3) |

---

## 6. How to Run & Test Finance Advisor V2

### 6.1 Interactive CLI Console
Launch the interactive terminal:
```bash
# Windows
test-finance-v2.bat

# Linux / macOS
./test-finance-v2.sh
```

Within the console:
- Type `state` to inspect the live session state map.
- Type `help` to display sample customer queries.
- Type `exit` to terminate.

### 6.2 Executing Automated Tests
Run the complete V2 unit and integration test suite:
```bash
# Windows
gradlew.bat test --tests "com.google.adk.finance.v2.*"

# Linux / macOS
./gradlew test --tests "com.google.adk.finance.v2.*"
```
