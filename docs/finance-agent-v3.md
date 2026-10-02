# Finance Advisor V3 — Deterministic Java Math & Grounded Google Search (`finance.v3`)

> **Milestone 3 Objective**: Extend Finance Advisor V2 by combining **real-time Google Search grounding** with **deterministic mathematical tools** using Google ADK Java. Learn how to solve the Gemini API tool exclusivity constraint using the **Single Responsibility Agent Architecture** (`AgentTool`), offload financial calculations to deterministic Java code (`PortfolioMathTool`), and produce evidence-backed decision support backed by verifiable live web data and SQLite customer state.

---

## 1. High-Level Architecture & Multi-Agent Grounding

```
                                  +─────────────────────────────────────────+
                                  |           Finance Advisor V3            |
                                  |        (com.google.adk.LlmAgent)        |
                                  |        Model: gemini / gemma            |
                                  +────────────────────┬────────────────────+
                                                       │
                     ┌─────────────────────────────────┼─────────────────────────────────┐
                     ▼                                 ▼                                 ▼
      +─────────────────────────────+   +─────────────────────────────+   +─────────────────────────────+
      |  AgentTool(stockmarket_res) |   |      PortfolioMathTool      |   |  LoadCustomerPortfolioTool  |
      | Isolated Search Agent:      |   | Deterministic Java Math:    |   | Relational DB Ingestion:    |
      | - GoogleSearchTool.INSTANCE |   | - PnL & Cost Basis          |   | - SQLite customer & holding |
      | - Live company news         |   | - Allocation Weights (%)    |   | - Customer 1001 & 1002      |
      | - Quarterly earnings & EPS  |   | - Concentration Risk (>25%) |   | - State injection           |
      | - Analyst consensus/targets |   | - Technical Indicators (SMA)|   |                             |
      +─────────────────────────────+   +─────────────────────────────+   +─────────────────────────────+
```

---

## 2. Core Google ADK Concepts Demonstrated

### 2.1 The Gemini API Tool Exclusivity Constraint
The Gemini API enforces a strict operational rule: **Google Search (`GoogleSearchTool.INSTANCE`) and client function declarations (`FunctionDeclaration`) cannot be declared in the same model turn.**

Attempting to register both `GoogleSearchTool.INSTANCE` and `PortfolioMathTool` directly on the same `LlmAgent` causes runtime API rejection:
```
INVALID_ARGUMENT: Tool declarations cannot mix built-in search tools and function declarations.
```

### 2.2 The Solution: Decoupled AgentTool Wrapper
To solve this cleanly without compromising capabilities, V3 implements the **Single Responsibility Principle (SRP)**:
1. **Isolated Specialist Agent** (`MarketResearchAgentFactory`):
   - Named `stockmarket_researcher`.
   - Dedicated solely to web search grounding.
   - Equipped *only* with `GoogleSearchTool.INSTANCE`.
   - Requires Gemini GenAI model to leverage native server-side search grounding.
2. **Root Orchestrator** (`FinanceAgentV3Factory`):
   - Wraps the search specialist via `AgentTool.create(searchAgent)`.
   - From the root agent's perspective, `stockmarket_researcher` is simply another client function declaration!
   - Root agent now cleanly hosts three function declarations: `stockmarket_researcher`, `portfolio_math`, and `load_customer_portfolio`. Zero tool collision.

### 2.3 Deterministic Financial Arithmetic via `PortfolioMathTool`
Large Language Models frequently hallucinate arithmetic, round floating-point numbers inconsistently, or produce inaccurate percentage returns. In finance, arithmetic must be 100% deterministic.

`PortfolioMathTool` offloads three core financial calculations to Java:
1. `calculate_pnl`: Computes total cost basis, current valuation, total unrealized profit/loss, and percentage return:
   $$\text{PnL} = (\text{Current Price} - \text{Buy Price}) \times \text{Quantity}$$
   $$\text{Return } \% = \left(\frac{\text{Current Price} - \text{Buy Price}}{\text{Buy Price}}\right) \times 100$$
2. `calculate_allocation`: Computes percentage weights across holdings and automatically flags single-stock concentration risk if any asset exceeds **25%** of total portfolio value.
3. `calculate_technical_indicator`: Computes Simple Moving Averages ($N$-period SMA), price spread ranges, and percentage deviations from moving averages.

---

## 3. Capability Decision Matrix: Where Does Each Fact Come From?

| Analytical Requirement | Google Search (`stockmarket_researcher`) | Java Math (`PortfolioMathTool`) | Relational DB (`LoadCustomerPortfolioTool`) | LLM Synthesis |
|---|:---:|:---:|:---:|:---:|
| **Recent Company News** | ✅ **Primary** | ❌ | ❌ | Contextualization |
| **Quarterly Earnings & EPS** | ✅ **Primary** | ❌ | ❌ | Comparison vs. Guidance |
| **Analyst Consensus & Targets** | ✅ **Primary** | ❌ | ❌ | Sentiment synthesis |
| **Macro / Regulatory Events** | ✅ **Primary** | ❌ | ❌ | Impact assessment |
| **Profit & Loss (PnL) Math** | ❌ | ✅ **Primary** | ❌ | Output formatting |
| **Asset Allocation Weights** | ❌ | ✅ **Primary** | ❌ | Risk interpretation |
| **Concentration Alerts (>25%)** | ❌ | ✅ **Primary** | ❌ | Warning disclosure |
| **Technical Indicators (SMA)** | ❌ | ✅ **Primary** | ❌ | Trend analysis |
| **Customer Positions & Cost Basis** | ❌ | ❌ | ✅ **Primary** | Holding verification |
| **Investment Trade Execution** | ❌ **PROHIBITED** | ❌ **PROHIBITED** | ❌ **PROHIBITED** | ❌ **Human gate required** |

---

## 4. Key Files & Implementation Reference

| File | Package Path | Role & Purpose |
|---|---|---|
| `PortfolioMathTool.java` | `src/main/java/com/google/adk/finance/tools/` | Custom `BaseTool` executing exact arithmetic for PnL, asset allocation, concentration alerts, and SMAs. |
| `MarketResearchAgentFactory.java` | `src/main/java/com/google/adk/finance/v3/agents/` | Decoupled factory creating `stockmarket_researcher` with `GoogleSearchTool.INSTANCE`. |
| `FinanceAgentV3Factory.java` | `src/main/java/com/google/adk/finance/v3/agents/` | Root factory creating `finance_advisor_v3` coordinating `AgentTool`, math tool, and DB ingestion. |
| `FinanceConsoleV3.java` | `src/main/java/com/google/adk/finance/v3/` | Interactive CLI displaying live tool execution events, search grounding citations, and math results. |
| `PortfolioMathToolTest.java` | `src/test/java/com/google/adk/finance/v3/` | Comprehensive unit tests verifying calculation tolerances for PnL, weights, concentration flags, and SMAs. |
| `FinanceV3IntegrationTest.java` | `src/test/java/com/google/adk/finance/v3/` | Integration tests verifying decoupled agent wiring, tool registrations, and execution flow. |

---

## 5. Evolutionary Comparison: V2 vs. V3

| Capability | Finance Advisor V2 | Finance Advisor V3 |
|---|:---:|:---:|
| **Conversational Analyst Persona** | Yes | Yes |
| **SQLite Customer State Ingestion** | Yes | Yes |
| **Dynamic State Templating** | Yes | Yes |
| **Live Web News & Earnings Retrieval** | No | **Yes (Google Search via `stockmarket_researcher`)** |
| **Gemini Tool Exclusivity Resolution** | No | **Yes (Decoupled via `AgentTool`)** |
| **Deterministic Arithmetic** | No (LLM arithmetic) | **Yes (`PortfolioMathTool`: PnL, Weights, SMA)** |
| **Concentration Risk Alerting (>25%)** | No | **Yes (Automatic risk flags in math tool)** |
| **Domain Skills (`skills/finance/`)** | No | No (Introduced in V4) |
| **Curated Grounding Knowledge (`knowledge/`)** | No | No (Introduced in V4) |
| **Model Context Protocol (MCP)** | No | No (Introduced in V5) |

---

## 6. How to Run & Test Finance Advisor V3

### 6.1 Interactive CLI Console
Launch the interactive terminal:
```bash
# Windows
test-finance-v3.bat

# Linux / macOS
./test-finance-v3.sh
```

Sample interactive queries to test V3 capabilities:
1. **Google Search Grounding**: `"What are the latest developments and analyst price targets for Infosys?"`
2. **Portfolio Math Tool**: `"Calculate PnL for 10 shares bought at 1500 and now trading at 1820."`
3. **Integrated Customer Portfolio Analysis**: `"Load customer 1001, research recent news for their holdings, and calculate their current allocations."`

### 6.2 Executing Automated Tests
Run the complete V3 unit and integration test suite:
```bash
# Windows
gradlew.bat test --tests "com.google.adk.finance.v3.*"

# Linux / macOS
./gradlew test --tests "com.google.adk.finance.v3.*"
```
