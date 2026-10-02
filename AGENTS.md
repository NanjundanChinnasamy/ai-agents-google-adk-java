# Multi-Agent Architecture & Engineering Reference (Google ADK Java)

This document provides a comprehensive architectural blueprint, code-level implementation reference, and progressive evolutionary roadmap for the multi-agent backend built in **Java 21** using the official **[Google Agent Development Kit (ADK) for Java](https://adk.dev/)** (`com.google.adk:google-adk:1.4.0`, `com.google.adk:google-adk-a2a:1.4.0`, and `com.google.adk:google-adk-dev:1.4.0`).

> [!IMPORTANT]
> **Documentation Synchronization Invariant (Mandatory)**:
> Whenever a new feature, agent milestone (e.g. `v1`, `v2`, `v3`, etc.), tool, schema, or command is implemented or updated in this repository:
> 1. **Synchronize `README.md`**: Update [`README.md`](README.md) to reflect the new agent milestones, architectural topology, directory trees, launcher scripts, interactive CLI commands, and test verification procedures.
> 2. **Synchronize `AGENTS.md`**: Update this reference with the granular code-level specifications, ADK concepts demonstrated, state schemas, and sequence flows.
> 3. **Relative Paths Only**: All file, class, and folder links across documentation files must strictly use **relative paths** (e.g., `src/main/java/...`). Absolute paths (`file:///...`) are strictly prohibited.

---

# PART I: DUAL-DOMAIN SYSTEM TOPOLOGY

The application accommodates two complementary autonomous multi-agent systems sharing a common core foundation of hybrid model execution, dynamic skills, Model Context Protocol (MCP) toolsets, embedded SQLite persistence, and reactive Server-Sent Events (SSE) streaming via the **AG-UI Protocol**:

```
                                      +---------------------------------------------+
                                      |            Frontend Interfaces              |
                                      | - Next.js CopilotKit UI (Port 3000)        |
                                      | - Google ADK Official Dev UI (Port 8080)   |
                                      | - Direct CLI Console (Interactive Terminal)|
                                      +----------------------+----------------------+
                                                             |
                                            AG-UI Protocol / SSE / REST
                                                             |
                                      +----------------------v----------------------+
                                      |       Javalin 6 Web Server (Port 8000)      |
                                      |     com.google.adk.socialspark.Application  |
                                      +----------------------+----------------------+
                                                             |
                     +---------------------------------------+---------------------------------------+
                     |                                                                               |
                     v                                                                               v
+------------------------------------------+                   +-------------------------------------------------------------+
|        DOMAIN 1: Social Spark            |                   |             DOMAIN 2: Finance Portfolio Agent               |
| - Root Orchestrator (social_poster)      |                   | "Research my portfolio, explain what changed, identify      |
| - Research Specialist (research_agent)   |                   |  evidence, compare scenarios, produce decision report"     |
| - Drafting Specialist (draft_agent)      |                   |                                                             |
| - Memory Specialist (Remote A2A)         |                   |  Evolutionary Progression:                                   |
| - MCP Publishing (LinkedIn & Buffer)     |                   |  v1 -> v2 -> v3 -> v4 -> v5 -> v6 -> v7 -> v8 -> v9 -> v10 -> Prod |
+------------------------------------------+                   +-------------------------------------------------------------+
                     |                                                                               |
                     +---------------------------------------+---------------------------------------+
                                                             |
                                      +----------------------v----------------------+
                                      |         Shared Core Foundation              |
                                      | - Hybrid LLMs (Gemini GenAI & Ollama/OpenAI)|
                                      | - MCP Toolsets (FastMCP stdio & HTTP)       |
                                      | - Dynamic Markdown Skills (LocalSkillSource)|
                                      | - Embedded SQLite WAL Persistence           |
                                      | - AG-UI Reactive SSE Translation Engine     |
                                      +---------------------------------------------+
```

---

# PART II: FINANCE PORTFOLIO DECISION-SUPPORT AGENT — 6-WEEK ROADMAP

## 1. Domain Objective & Strategic Intent

> **Core Objective:**
> *"Research my portfolio, explain what changed, identify evidence, compare scenarios, and produce a decision-support report."*

Rather than introducing ad-hoc features, the Finance Portfolio Agent follows a strict **versioned educational progression (v1 through v10 and Production)**. Each version isolates specific Google ADK concepts and domain capabilities, allowing developers and agents to inspect, run, test, and compare the system at each evolutionary milestone.

### The Five Analytical Pillars
1. **Portfolio Ingestion & State Tracking**: Ingest and structure current asset holdings, asset classes, target weightings, purchase prices, and current valuations.
2. **Variance & Attribution Analysis**: Explain *what changed* (PnL, asset allocation drift, performance vs. benchmark, volatility spikes).
3. **Evidence Identification & Grounding**: Retrieve verifiable real-world evidence (earnings reports, macroeconomic indicators, interest rates, SEC filings, sector news) using Google Search and financial data MCP tools.
4. **Scenario & Stress-Testing**: Compare forward-looking scenarios (e.g. *Rate Hike Shock*, *Tech Sector Correction*, *Soft Landing*, *Stagflation*) to evaluate portfolio resilience and risk-reward outcomes.
5. **Decision-Support Synthesis**: Generate an institutional-grade, actionable decision-support report featuring executive thesis, evidence matrices, scenario comparative tables, risk flags, and rebalancing trade-offs.

---

## 2. Progressive Learning Curriculum (v1 to Production)

| Week | Milestone Version | Core Google ADK Concepts | Domain Capability Added | Package Path |
|---|---|---|---|---|
| **1** | **Finance Advisor v1** | `LlmAgent`, `AppConfig.createModel()`, Instruction Engineering, `InMemoryRunner`, `SessionService` | Conversational portfolio analyst with basic Q&A | `com.google.adk.finance.v1` |
| **1** | **Finance Advisor v2** | `Context`, State variables, `{placeholder?}` prompt templating, `stateDelta` inspection | Persistent portfolio state memory & multi-turn drift awareness | `com.google.adk.finance.v2` |
| **2** | **Finance Advisor v3** | Custom `BaseTool`, `GoogleSearchTool.INSTANCE`, Tool schema validation (`FunctionDeclaration`) | Deterministic financial math calculations + live market news retrieval | `com.google.adk.finance.v3` |
| **2** | **Finance Advisor v4** | `LocalSkillSource`, `SkillToolset`, Markdown domain guidelines | Audit rules, scenario modeling templates & report formatting skills | `com.google.adk.finance.v4` |
| **3** | **Finance Advisor v5** | `McpToolset`, `ServerParameters`, FastMCP stdio/HTTP | Real-time market quotes, SEC fundamentals & macro indicators via MCP | `com.google.adk.finance.v5` |
| **3** | **Finance Agent v6** | Multi-Agent hierarchy, `AgentTool.create(...)`, Specialist division of labor | Specialist sub-agents: Researcher, Scenario Analyst, Report Writer | `com.google.adk.finance.v6` |
| **4** | **Finance Agent v7** | Sequential pipelines, Parallel fan-out/fan-in (`RxJava`/`Reactor`), Critic loops | End-to-end multi-step orchestration with parallel stock research | `com.google.adk.finance.v7` |
| **4** | **Finance Agent v8** | Callbacks (`BeforeAgent`, `AfterAgent`, `BeforeTool`, `AfterTool`, `AfterModel`), Guardrails | Compliance disclaimer injection, PII scrubbing, hallucination filters | `com.google.adk.finance.v8` |
| **5** | **Finance Agent v9** | Automated evaluation, LLM-as-a-judge, Groundedness scoring, Red-teaming | Benchmark tests verifying report fidelity, math accuracy & failure recovery | `src/test/java/com/google/adk/finance/eval/` |
| **5** | **Finance Agent v10** | OpenTelemetry/Micrometer tracing, SQLite post-run persistence, Audit logging | Full run traceability, token accounting & historical report archives | `com.google.adk.finance.v10` |
| **6** | **Production Release** | Containerization, Cloud Run / Agent Engine, GCP IAM Workload Identity, CI/CD | Secure, scalable enterprise deployment with automated quality gates | `com.google.adk.finance.prod` |

---

## 3. Deep Dive: Architectural Specifications per Version

### Week 1 — Version 1: Foundational Agent & Session (`finance.v1`)
- **Package**: `com.google.adk.finance.v1`
- **Core Files**:
  - `FinanceAgentV1Factory.java`: Factory creating baseline `LlmAgent`.
  - `FinanceConsoleV1.java`: Dedicated CLI runner demonstrating session creation.
- **ADK Classes Learned**:
  - `com.google.adk.agents.LlmAgent`
  - `com.google.adk.runner.InMemoryRunner`
  - `com.google.adk.sessions.SessionService`
  - `com.google.adk.agents.RunConfig`
- **Architecture**:
  ```
  User Prompt ("What does my portfolio look like?")
         │
         ▼
  +─────────────────────────────────────────────────────────+
  | FinanceAgentV1 (LlmAgent)                               |
  | Model: gemini-2.5-flash / gemma4:31b                    |
  | Instruction: Financial advisor assistant persona        |
  | Session: sessionService.createSession("finance-user-1") |
  +─────────────────────────────────────────────────────────+
  ```
- **Takeaway**: Learn agent instantiation, instruction engineering for financial contexts, and managing multi-turn conversation sessions.

---

### Week 1 — Version 2: Context & Holdings State Management (`finance.v2`)
- **Package**: `com.google.adk.finance.v2`
- **Core Files**:
  - [`PortfolioModels.java`](src/main/java/com/google/adk/finance/v2/PortfolioModels.java): Domain records for `CustomerRecord`, `PortfolioHoldingRecord`, and `CustomerPortfolio`.
  - [`CustomerPortfolioRepository.java`](src/main/java/com/google/adk/finance/v2/CustomerPortfolioRepository.java): SQLite DAO with WAL mode managing `customer` and `portfolio_holding` tables with auto-seeding.
  - [`LoadCustomerPortfolioTool.java`](src/main/java/com/google/adk/finance/tools/LoadCustomerPortfolioTool.java) (with alias in `v2`): Custom `BaseTool` querying SQLite by customer ID and injecting holdings into session state via `toolContext.state()`.
  - [`FinanceAgentV2Factory.java`](src/main/java/com/google/adk/finance/v2/FinanceAgentV2Factory.java): Instantiates `finance_agent_v2` with dynamic prompt placeholders `{customer_id?}`, `{portfolio_id?}`, and `{portfolio_holdings?}`.
  - [`FinanceConsoleV2.java`](src/main/java/com/google/adk/finance/v2/FinanceConsoleV2.java): Interactive multi-turn CLI console displaying live session state transitions and answering questions without redundant queries.
- **SQLite Database Schema**:
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
- **Seed Data**:
  - Customer `1001` -> Portfolio `100001`:
    - Reliance (`RELIANCE`), Qty: 2, Buy: 1000.0 INR, Bought: `01-01-2025`
    - TCS (`TCS`), Qty: 2, Buy: 1000.0 INR, Bought: `01-01-2025`
  - Customer `1002` -> Portfolio `100002`:
    - Infosys (`INFY`), Qty: 10, Buy: 1500.0 INR, Bought: `10-08-2026`
- **ADK Classes & Concepts Learned**:
  - `BaseTool` database access & `ToolContext.state()`: Populating persistent session variables dynamically inside tool calls.
  - Dynamic prompt templating: `{customer_id?}`, `{portfolio_id?}`, `{portfolio_holdings?}` injected into agent instructions.
  - Multi-turn state preservation: Agent asks for Customer ID on Turn 1, executes `load_customer_portfolio` to seed state, and subsequently answers all inquiries (holding names, quantities, buy prices, purchase dates, total invested capital) directly from state without re-asking or re-querying the database.
- **Architecture**:
  ```
  Turn 1: "What does my portfolio look like?"
       │ (State empty -> Agent requests Customer ID)
       ▼
  Turn 2: "My customer ID is 1001"
       │
       ▼
  [LoadCustomerPortfolioTool.runAsync(customer_id="1001")]
       │ ──> Queries SQLite customer & portfolio_holding
       │ ──> toolContext.state().put("customer_id", "1001")
       │ ──> toolContext.state().put("portfolio_holdings", formattedSummary)
       ▼
  Turn 3: "What is my total invested capital?"
       │ (Answered directly from session state context: 4000.0 INR)
       ▼
  [FinanceAgentV2 Response with Regulatory Disclaimer]
  ```

---

### Week 2 — Version 3: Java Tools & Grounded Google Search (`finance.v3`)
- **Package**: `com.google.adk.finance.v3`
- **Core Files**:
  - [`PortfolioMathTool.java`](src/main/java/com/google/adk/finance/tools/PortfolioMathTool.java) (with alias in `v3`): Custom `BaseTool` for deterministic PnL, allocation weights, concentration risk, and technical indicators.
  - [`FinanceAgentV3Factory.java`](src/main/java/com/google/adk/finance/v3/agents/FinanceAgentV3Factory.java): Root agent factory in `v3.agents` coordinating market research, deterministic math, and portfolio ingestion.
  - [`MarketResearchAgentFactory.java`](src/main/java/com/google/adk/finance/v3/agents/MarketResearchAgentFactory.java): Decoupled atomic agent factory in `v3.agents` adhering to SRP, isolating `GoogleSearchTool.INSTANCE` within `stockmarket_researcher`.
  - [`FinanceConsoleV3.java`](src/main/java/com/google/adk/finance/v3/FinanceConsoleV3.java): Interactive terminal console showcasing real-time tool inspection and search grounding.
  - [`PortfolioMathToolTest.java`](src/test/java/com/google/adk/finance/v3/PortfolioMathToolTest.java): Unit tests verifying math calculation accuracy.
  - [`FinanceV3IntegrationTest.java`](src/test/java/com/google/adk/finance/v3/FinanceV3IntegrationTest.java): Integration tests verifying atomic agent bindings, tool registrations, and agent execution.
- **ADK Classes & Concepts Learned**:
  - `com.google.adk.tools.GoogleSearchTool`: Grounding responses with live web search results (news, earnings, analyst commentary, filings).
  - `com.google.adk.tools.AgentTool`: Wrapping an isolated search agent (`stockmarket_researcher`) to satisfy Gemini's API constraint (which prohibits mixing native search tools and client function declarations in the same generation step).
  - Custom `com.google.adk.tools.BaseTool`: Offloading arithmetic and technical indicators to deterministic Java code to avoid LLM hallucinations.
  - Multi-tool binding: Combining web search grounding, deterministic math, and relational database loading in a single agent.
- **Capability Matrix: Google Search Grounding vs. Java Tools**:

| Analytical Capability | Google Search? | Tool / Method | Rationale |
|---|:---:|---|---|
| **Recent Company News** | ✅ Excellent | `stockmarket_researcher` | Real-time news retrieval, press releases, operational changes |
| **Latest Results & Earnings** | ✅ Excellent | `stockmarket_researcher` | Quarterly SEC filings, revenue/EPS metrics, earnings calls |
| **Recent Analyst Commentary** | ✅ Yes | `stockmarket_researcher` | Consensus ratings, price targets, upgrades/downgrades |
| **Official Company Announcements** | ✅ Excellent | `stockmarket_researcher` | Corporate governance, regulatory disclosures, buybacks |
| **Market & Macro Events** | ✅ Excellent | `stockmarket_researcher` | Interest rate hikes, inflation reports, geopolitical events, M&A |
| **Historical Price & Return Calculation** | ⚠️ Not Search | `portfolio_math` (`calculate_pnl`) | Deterministic arithmetic: Cost Basis, Current Valuation, PnL, Return % |
| **Portfolio Allocation & Concentration** | ❌ Not Search | `portfolio_math` (`calculate_allocation`) | Asset weights (%) and single-stock concentration risk flags (>25%) |
| **Technical Indicators** | ❌ Not Search | `portfolio_math` (`calculate_technical_indicator`) | Simple Moving Average (SMA), price range spread, percentage change |
| **Synthesized Recommendations** | ❌ Not Search | Agent Synthesis | Multi-source grounded reasoning (bull/bear trade-offs, catalysts) |
| **Execute a Trade** | ❌ Separate | Human Approval Gate | Strictly prohibited without controlled trading tools and user consent |

- **Architecture**:
  ```
                                +─────────────────────────────────────────+
                                |             FinanceAgentV3              |
                                |       Model: gemini-2.5-flash           |
                                +────────────────────┬────────────────────+
                                                     │
                   ┌─────────────────────────────────┼─────────────────────────────────┐
                   ▼                                 ▼                                 ▼
    +─────────────────────────────+   +─────────────────────────────+   +─────────────────────────────+
    | AgentTool(stockmarket_res.) |   |      PortfolioMathTool      |   |  LoadCustomerPortfolioTool  |
    | Isolated Search Agent:      |   | Deterministic Math:         |   | Relational DB Ingestion:    |
    | - GoogleSearchTool.INSTANCE |   | - PnL & Cost Basis          |   | - Customer 1001 (RELIANCE)  |
    | - Live news & Q3 earnings   |   | - Allocation Weights (%)    |   | - Customer 1002 (INFY)      |
    | - Analyst ratings & targets |   | - Concentration Risk (>25%) |   | - Persistent Session State  |
    | - Macro & market events     |   | - Technical Indicators (SMA)|   | Output: Holding records     |
    +─────────────────────────────+   +─────────────────────────────+   +─────────────────────────────+
  ```
- **Rule**: Never permit the LLM to perform arithmetic on financial figures. Offload all calculations to `PortfolioMathTool`. Always cite search sources and conclude with the mandatory regulatory disclaimer.

---

### Week 2 — Version 4: Skills + Grounding Knowledge (`finance.v4`)
- **Package**: `com.google.adk.finance.v4`
- **Core Files**:
  - [`FinanceAdvisorAgentV4Factory.java`](src/main/java/com/google/adk/finance/v4/FinanceAdvisorAgentV4Factory.java): Builds `finance_advisor_v4` combining `SkillToolset`, `ProjectKnowledgeTool`, `AgentTool(stockmarket_researcher)`, `PortfolioMathTool`, and `LoadCustomerPortfolioTool`.
  - [`ProjectKnowledgeTool.java`](src/main/java/com/google/adk/finance/tools/ProjectKnowledgeTool.java) (with alias in `v4`): Custom `BaseTool` executing `read_project_knowledge` against curated Markdown documents in `knowledge/`.
  - [`FinanceConsoleV4.java`](src/main/java/com/google/adk/finance/v4/FinanceConsoleV4.java): Dedicated interactive CLI runner supporting skill listing, knowledge reading, state inspection, and real-time tool tracking.
  - [`FinanceV4IntegrationTest.java`](src/test/java/com/google/adk/finance/v4/FinanceV4IntegrationTest.java): Comprehensive test suite covering the 5 canonical learning tests, portfolio ingestion, and tool validations.
- **The Three Knowledge Layers**:
  1. **Model Knowledge**: General world knowledge already internal to the LLM (e.g. *"What is a stock?"*).
  2. **Project Grounding Knowledge**: Curated, authoritative frameworks maintained in `knowledge/` rather than in massive static system prompts:
     - `knowledge/glossary.md`: Essential terminology (shares, market cap, revenue, EPS, dividends, cash flow, CAGR).
     - `knowledge/valuation-principles.md`: Multiples framework (P/E, forward P/E, EV/EBITDA, DCF concept, relative valuation).
     - `knowledge/fundamental-analysis.md`: Operating metrics, margins, ROE, ROCE, cash conversion, and balance sheet quality.
     - `knowledge/risk-framework.md`: Taxonomy of risks (systematic vs unsystematic, single-stock/sector concentration, liquidity, volatility, drawdown).
     - `knowledge/portfolio-principles.md`: Asset allocation, correlation ($\rho$), diversification benefits, and investment horizons.
     - `knowledge/market-research-framework.md`: Structured 8-stage methodology for corporate filings, earnings surprise, and guidance revisions.
  3. **Google Search Grounding**: Real-time external web information retrieved on-demand via `stockmarket_researcher` when recent news, earnings results, or market developments are required.
- **The 6 Domain Skills in `skills/finance/`**:
  - `skills/finance/finance-fundamentals/SKILL.md`: Core corporate finance concepts and ratio definitions.
  - `skills/finance/fundamental-analysis/SKILL.md`: Holistic multi-pillar analysis evaluating revenue, margins, capital return, and moat.
  - `skills/finance/valuation/SKILL.md`: Contextual interpretation of multiples (enforcing that high P/E does not automatically mean overvalued).
  - `skills/finance/risk-management/SKILL.md`: Framework for classifying and mitigating investment risks.
  - `skills/finance/portfolio-analysis/SKILL.md`: Conceptual guidelines for analyzing asset allocation and correlation without ad-hoc math.
  - `skills/finance/market-research/SKILL.md`: 8-stage investigation process synthesizing corporate announcements, earnings, regulatory news, and analyst commentary.
- **Conceptual Flow**:
  ```
                           USER
                             │
                             ▼
                    Finance Advisor V4
                             │
                 ┌───────────┴───────────┐
                 │                       │
                 ▼                       ▼
          Project Skills           Current Information?
                 │                       │
                 │                  YES  │
                 │                       ▼
                 │                 Google Search
                 │            (stockmarket_researcher)
                 │                       │
                 └───────────┬───────────┘
                             ▼
                           Gemini / Ollama
                             │
                             ▼
                      Grounded Response
  ```
- **Grounding Transparency Format**: Responses systematically distinguish:
  - **Executive Summary**: Direct high-level answer.
  - **Project Knowledge**: Principles grounded in curated project knowledge files.
  - **Current Information**: Verifiable empirical facts from Google Search (cited with dates and sources).
  - **Analysis & Interpretation**: Objective reasoning without asserting false causation.
  - **Regulatory Disclaimer**: Mandatory non-advice compliance notice.

---

### Week 3 — Version 5: Market Context Protocol (MCP) Integration (`finance.v5` & `mcp.yahoofinance`)
- **Package**: `com.google.adk.finance.v5` and `com.google.adk.mcp.yahoofinance`
- **Documentation**: See comprehensive reference in [`docs/finance-agent-v5.md`](docs/finance-agent-v5.md).
- **MCP Server**: Standalone executable Java MCP server located in `mcp/` (`mcp/yahoo-finance-mcp.jar`, launched via `mcp/yahoo_finance_server.bat` or `mcp/yahoo_finance_server.sh`).
- **Core Files**:
  - [`YahooFinanceMcpServer.java`](src/main/java/com/google/adk/mcp/yahoofinance/YahooFinanceMcpServer.java): Main entrypoint and `McpSyncServer` over stdio transport registering the 4 core financial tools with `ticker` and `symbol` alias support.
  - [`YahooFinanceService.java`](src/main/java/com/google/adk/mcp/yahoofinance/YahooFinanceService.java): Business logic implementing data extraction, normalization, and JSON formatting for stock information, corporate actions, financial statements, and recommendations.
  - [`YahooFinanceApiClient.java`](src/main/java/com/google/adk/mcp/yahoofinance/YahooFinanceApiClient.java): Low-level HTTP client managing cookie sessions, crumb authentication tokens, and US class-share ticker normalization (e.g. `BRK.B` -> `BRK-B`).
  - [`YahooFinanceMcpClientManager.java`](src/main/java/com/google/adk/finance/v5/YahooFinanceMcpClientManager.java): Client lifecycle manager handling subprocess execution, 15-second readiness timeout, tool verification, and clean process termination.
  - [`FinanceAdvisorAgentV5Factory.java`](src/main/java/com/google/adk/finance/v5/FinanceAdvisorAgentV5Factory.java): Root agent factory wiring `McpToolset`, `SkillToolset`, `ProjectKnowledgeTool`, `AgentTool(stockmarket_researcher)`, `PortfolioMathTool`, and `LoadCustomerPortfolioTool`.
  - [`FinanceConsoleV5.java`](src/main/java/com/google/adk/finance/v5/FinanceConsoleV5.java): Dedicated interactive CLI console supporting real-time MCP tool tracking, multi-turn state, and automated JVM shutdown hook.
  - [`FinanceV5IntegrationTest.java`](src/test/java/com/google/adk/finance/v5/FinanceV5IntegrationTest.java): Comprehensive integration test suite verifying startup, tool discovery, quote retrieval, actions, search wiring, math, and shutdown.
- **The 4 Discovered MCP Tools**:
  1. `get_stock_info`: Comprehensive stock price & trading metrics, valuation (P/E, forward P/E, PEG, P/B), operating margins, return ratios, balance sheet highlights, and company profile.
  2. `get_stock_actions`: Historical dividend payments and stock splits with execution dates over 5 years.
  3. `get_financial_statement`: Annual or quarterly income statements, balance sheets, and cash flow statements (`income_stmt`, `quarterly_income_stmt`, `balance_sheet`, `quarterly_balance_sheet`, `cashflow`, `quarterly_cashflow`).
  4. `get_recommendations`: Consensus analyst recommendation trends or firm upgrade/downgrade history within a configurable lookback window.
- **ADK Classes & Concepts Learned**:
  - `com.google.adk.tools.mcp.McpToolset`: Seamlessly consuming MCP servers as first-class ADK toolsets.
  - `io.modelcontextprotocol.client.transport.ServerParameters`: Configuring subprocess execution for stdio transport.
  - Dynamic Tool Discovery: Transforming runtime MCP schemas into client function declarations without compile-time coupling.
  - Multi-Source Intelligence Triangulation: Orchestrating structured market data (Yahoo Finance MCP), current web news (`stockmarket_researcher` via Google Search), domain skills, project knowledge, and relational customer state.
- **Architecture**:
  ```
                        Finance Advisor V5 (LlmAgent)
                                     │
         ┌───────────────────────────┼───────────────────────────┐
         ▼                           ▼                           ▼
    Domain Skills &             Google Search             Yahoo Finance MCP
    Grounding Knowledge    (stockmarket_researcher)        (McpToolset / stdio)
   (skills/ & knowledge/)     (Live news & events)               │
         │                           │                           ▼
         │                           │              mcp/yahoo-finance-mcp.jar
         │                           │                           │
         │                           │                           ▼
         │                           │                  Yahoo Finance APIs
         └───────────────────────────┼───────────────────────────┘
                                     ▼
                               Gemini / Gemma
                                     │
                                     ▼
                       Institutional Decision-Support
  ```
- **Takeaway**: Provide institutional-grade financial data tools to agents without rewriting custom API integrations by consuming standards-compliant MCP servers via Google ADK Java.

---

### Week 3 — Version 6: Specialist Multi-Agent System (`finance.v6`)
- **Package**: `com.google.adk.finance.v6` and `com.google.adk.finance.v6.subagents`
- **Documentation**: See comprehensive guides in [`docs/finance-agent-v6.md`](docs/finance-agent-v6.md), [`v6/README.md`](v6/README.md), and [`v6/sub-agents/README.md`](v6/sub-agents/README.md).
- **Core Files**:
  - [`FinanceAdvisorAgentV6.java`](src/main/java/com/google/adk/finance/v6/FinanceAdvisorAgentV6.java): Root orchestrator (`portfolio_director` / `finance_advisor_v6`) delegating inquiries to specialists via `AgentTool`.
  - [`FinanceConsoleV6.java`](src/main/java/com/google/adk/finance/v6/FinanceConsoleV6.java): Interactive terminal CLI runner with real-time sub-agent delegation tracking and lifecycle callbacks.
  - [`MarketResearchAgentV6.java`](src/main/java/com/google/adk/finance/v6/subagents/MarketResearchAgentV6.java): Specialist sub-agent (`stockmarket_researcher`) for live public-web developments and price change attribution via `GoogleSearchTool.INSTANCE`.
  - [`ScenarioAnalystAgentV6.java`](src/main/java/com/google/adk/finance/v6/subagents/ScenarioAnalystAgentV6.java): Specialist sub-agent (`scenario_analyst`) modeling Baseline, Bull, and Bear macro scenarios and portfolio stress-testing.
  - [`ReportWriterAgentV6.java`](src/main/java/com/google/adk/finance/v6/subagents/ReportWriterAgentV6.java): Specialist sub-agent (`report_writer`) synthesizing multi-agent research notes into an institutional decision-support report.
  - [`FundamentalAnalysisAgentV6.java`](src/main/java/com/google/adk/finance/v6/subagents/FundamentalAnalysisAgentV6.java): Specialist sub-agent (`fundamental_analysis_agent`) for company fundamentals, valuation multiples, and financial statements via Yahoo Finance MCP.
  - [`PortfolioRiskAgentV6.java`](src/main/java/com/google/adk/finance/v6/subagents/PortfolioRiskAgentV6.java): Specialist sub-agent (`portfolio_risk_agent`) for market sensitivity (beta), volatility, capital structure leverage, and concentration alerts (>25%).
  - [`FinanceV6IntegrationTest.java`](src/test/java/com/google/adk/finance/v6/FinanceV6IntegrationTest.java): Comprehensive integration test suite validating sub-agent configurations, parent delegation, multi-specialist synthesis, and failure isolation.
- **Root Orchestrator & Specialist Roles**:
  1. `portfolio_director` (Root Orchestrator): Interprets user intent, manages session state, routes tasks to specialists via `AgentTool`, and conducts user alignment and multi-specialist synthesis.
  2. `stockmarket_researcher`: Queries Google Search and collaborates with market data tools to identify why asset prices changed, investigate real-time corporate announcements, earnings release reactions, C-suite changes, and macro catalysts using `GoogleSearchTool.INSTANCE` and the 8-stage market research framework.
  3. `scenario_analyst`: Models forward-looking macro scenarios (Baseline, Bull, Bear) and stress-tests portfolio asset allocations against interest rate shocks, tech corrections, and multiple compression using Yahoo Finance MCP, deterministic math (`PortfolioMathTool`), and curated risk frameworks.
  4. `report_writer`: Synthesizes disparate specialist research notes, quantitative fundamentals, and scenario models into an executive decision-support report featuring thesis, evidence matrix, scenario table, and risk flags.
  5. `fundamental_analysis_agent`: Analyzes quantitative company metrics, financial statements, and valuation ratios using Yahoo Finance MCP tools (`get_stock_info`, `get_financial_statement`, `get_stock_actions`, `get_recommendations`) and curated valuation frameworks.
  6. `portfolio_risk_agent`: Evaluates structural risk, beta sensitivity, volatility, and portfolio-level concentration hazards (>25%) using Yahoo Finance MCP, deterministic math (`PortfolioMathTool`), and curated risk frameworks.
- **ADK Classes & Concepts Learned**:
  - `com.google.adk.tools.AgentTool`: Wrapping independent sub-agents as callable tools for the parent orchestrator.
  - **Multi-Agent State Isolation & Delegation Hierarchies**: Keeping sub-agents independent with isolated execution contexts and toolsets.
  - **Gemini Search Grounding Exclusivity Resolution**: Separating `GoogleSearchTool.INSTANCE` into its own sub-agent turn (`stockmarket_researcher`) so client MCP function tools can be used by other specialists without API conflicts.
  - **Standardized Sub-Agent Output Contract**: Requiring each sub-agent to produce structured reports with explicit identification, task, findings, sources, and limitations.
  - **Multi-Specialist Synthesis**: Invoking multiple sub-agents in a single turn for multi-faceted questions and synthesizing a cohesive institutional decision-support response.
  - **Failure Isolation & Graceful Degradation**: Isolating specialist unavailability so that failure of one sub-agent does not crash the overall advisory pipeline.
- **Sub-Agent Delegation Architecture**:
  ```
                                      User Inquiry
                                           │
                                           ▼
                            +──────────────────────────────+
                            |      portfolio_director      |
                            |    (FinanceAdvisorAgentV6)   |
                            |   Workflow Orchestration &   |
                            |   Multi-Specialist Synthesis |
                            +──────────────┬───────────────+
                                           │
       ┌────────────────────┬──────────────┼──────────────┬────────────────────┐
       │                    │              │              │                    │
 [Delegation 1]       [Delegation 2] [Delegation 3] [Delegation 4]        [Delegation 5]
       │                    │              │              │                    │
       ▼                    ▼              ▼              ▼                    ▼
+───────────────────+ +─────────────────+ +────────────────+ +──────────────────+ +──────────────────+
|stockmarket_res.   | |fundamental_agent| |portfolio_risk  | |scenario_analyst  | |report_writer     |
|"Why did asset     | |"Quantitative    | |"Identify beta, | |"Model Bull/Bear  | |"Synthesize       |
| prices change?"   | | fundamentals &  | | volatility &   | | scenarios &      | | executive        |
|                   | | multiples"      | | concentration" | | stress-test"     | | decision report" |
+─────────┬─────────+ +────────┬────────+ +────────┬───────+ +────────┬─────────+ +────────┬─────────+
       │                    │                   │                  │                    │
       ▼                    ▼                   ▼                  ▼                    ▼
+───────────────────+ +─────────────────+ +────────────────+ +──────────────────+ +──────────────────+
| GoogleSearchTool  | |Yahoo Finance MCP| |Yahoo Finance   | |Yahoo Finance MCP | |Project Knowledge |
| (Live Web News &  | |(Stock info,     | |(Beta, 52-wk)   | |(Beta & Multiples)| |(Curated Briefing |
|  attributions)    | | statements,     | |+ PortfolioMath | |+ PortfolioMath   | | Frameworks)      |
| + 8-Stage Research| | recommendations)| |+ Risk Framework| |+ Risk Framework  | |+ Domain Skills   |
+─────────┬─────────+ +────────┬────────+ +────────┬───────+ +────────┬─────────+ +────────┬─────────+
       │                    │                   │                  │                    │
       └────────────────────┴──────────────┬────┴──────────────────┴────────────────────┘
                                           │ Standardized Sub-Agent Reports
                                           ▼
                            +──────────────────────────────+
                            |  portfolio_director Synthesis|
                            | - Executive Strategic Thesis |
                            | - Multi-Specialist Evidence  |
                            | - Macro Scenario Matrix      |
                            | - Risk Flags & Disclaimers   |
                            +──────────────┬───────────────+
                                           │
                                           ▼
                                      Final Answer

  ```
- **What V6 Deliberately Does NOT Introduce**: Sequential workflows, parallel fan-out/fan-in, loop/critic agents, guardrails/PII filtering, or autonomous trading.

---

### Week 4 — Version 7: Advanced Orchestration Workflows (`finance.v7`)
- **Package**: `com.google.adk.finance.v7` and sub-packages (`tools`, `workflows.sequential`, `workflows.parallel`, `workflows.loop`, `subagents`) alongside shared tools in `com.google.adk.finance.tools`
- **Documentation**: See comprehensive guides in [`docs/finance-agent-v7.md`](docs/finance-agent-v7.md), [`v7/README.md`](v7/README.md), [`v7/workflows/sequential/README.md`](v7/workflows/sequential/README.md), [`v7/workflows/parallel/README.md`](v7/workflows/parallel/README.md), [`v7/workflows/loop/README.md`](v7/workflows/loop/README.md), [`v7/sub-agents/README.md`](v7/sub-agents/README.md), and [`v7/agents/README.md`](v7/agents/README.md).
- **Core Files**:
  - [`FinanceAdvisorAgentV7.java`](src/main/java/com/google/adk/finance/v7/FinanceAdvisorAgentV7.java): Root orchestrator (`workflow_director` / `finance_advisor_v7`) exposing 3 workflow invocation tools (`run_sequential_research_workflow`, `run_parallel_portfolio_research_workflow`, `run_critic_loop_research_workflow`), SQLite portfolio loader, and math calculation tools.
  - [`FinanceConsoleV7.java`](src/main/java/com/google/adk/finance/v7/FinanceConsoleV7.java): Dedicated interactive CLI terminal runner supporting workflow shortcut commands (`sequential <ticker>`, `parallel <tickers>`, `parallel-fail <tickers>`, `loop <ticker>`), session state inspection, and graceful JVM shutdown hooks.
  - **Dedicated V7 Workflow Tools** ([`src/main/java/com/google/adk/finance/v7/tools/`](src/main/java/com/google/adk/finance/v7/tools/)):
    - [`RunSequentialWorkflowTool.java`](src/main/java/com/google/adk/finance/v7/tools/RunSequentialWorkflowTool.java): Invocation tool executing 5-stage sequential research pipeline.
    - [`RunParallelWorkflowTool.java`](src/main/java/com/google/adk/finance/v7/tools/RunParallelWorkflowTool.java): Invocation tool executing concurrent fan-out/fan-in research.
    - [`RunCriticLoopWorkflowTool.java`](src/main/java/com/google/adk/finance/v7/tools/RunCriticLoopWorkflowTool.java): Invocation tool executing iterative authoring and compliance critic loop.
  - **Shared Finance Tools** ([`src/main/java/com/google/adk/finance/tools/`](src/main/java/com/google/adk/finance/tools/)):
    - [`LoadCustomerPortfolioTool.java`](src/main/java/com/google/adk/finance/tools/LoadCustomerPortfolioTool.java): Loads customer portfolio from SQLite and populates session state.
    - [`PortfolioMathTool.java`](src/main/java/com/google/adk/finance/tools/PortfolioMathTool.java): Pure Java BaseTool for deterministic arithmetic, PnL, allocation weights, and concentration flags.
    - [`ProjectKnowledgeTool.java`](src/main/java/com/google/adk/finance/tools/ProjectKnowledgeTool.java): Dynamic access to curated project grounding principles in `knowledge/`.
  - [`InvestmentResearchSequentialWorkflowV7.java`](src/main/java/com/google/adk/finance/v7/workflows/sequential/InvestmentResearchSequentialWorkflowV7.java): Deterministic 5-stage pipeline executing research, fundamentals, risk, valuation, and synthesis in strict chronological order with explicit `outputKey` chaining.
  - [`PortfolioParallelResearchWorkflowV7.java`](src/main/java/com/google/adk/finance/v7/workflows/parallel/PortfolioParallelResearchWorkflowV7.java): Concurrent fan-out/fan-in engine executing multi-ticker company research in parallel using `CompletableFuture`, logging real asynchronous events, tolerating partial failures, and synthesizing comparative insights without data fabrication.
  - [`ResearchCriticLoopWorkflowV7.java`](src/main/java/com/google/adk/finance/v7/workflows/loop/ResearchCriticLoopWorkflowV7.java): Iterative author-critic feedback loop combining `ReportDraftingAgentV7` and `ComplianceEvidenceCriticAgentV7` inside a native ADK `LoopAgent` bounded by `maxIterations(3)` and controlled via `ExitLoopTool.INSTANCE`.
  - **The 10 Specialized Sub-Agents** ([`src/main/java/com/google/adk/finance/v7/subagents/`](src/main/java/com/google/adk/finance/v7/subagents/)):
    - [`CompanyResearchAgentV7.java`](src/main/java/com/google/adk/finance/v7/subagents/CompanyResearchAgentV7.java): Sequential Stage 1 specialist (`company_research_agent_v7`) executing live web search via `GoogleSearchTool.INSTANCE`.
    - [`FundamentalAnalysisAgentV7.java`](src/main/java/com/google/adk/finance/v7/subagents/FundamentalAnalysisAgentV7.java): Sequential Stage 2 specialist (`fundamental_analysis_agent_v7`) querying Yahoo Finance MCP for financial statements, revenues, and operating margins.
    - [`RiskAnalysisAgentV7.java`](src/main/java/com/google/adk/finance/v7/subagents/RiskAnalysisAgentV7.java): Sequential Stage 3 specialist (`risk_analysis_agent_v7`) assessing systematic beta, volatility, capital structure, and operational risks.
    - [`ValuationAnalysisAgentV7.java`](src/main/java/com/google/adk/finance/v7/subagents/ValuationAnalysisAgentV7.java): Sequential Stage 4 specialist (`valuation_analysis_agent_v7`) evaluating valuation multiples (P/E, forward P/E, PEG, EV/EBITDA) with strict anti-hallucination constraints.
    - [`SequentialReportSynthesisAgentV7.java`](src/main/java/com/google/adk/finance/v7/subagents/SequentialReportSynthesisAgentV7.java): Sequential Stage 5 synthesis specialist (`sequential_synthesis_agent_v7`) assembling all upstream stages into an institutional 7-section report.
    - [`CompanyParallelResearchWorkerV7.java`](src/main/java/com/google/adk/finance/v7/subagents/CompanyParallelResearchWorkerV7.java): Autonomous concurrent worker (`company_parallel_worker_v7`) combining live news, Yahoo Finance fundamentals, and risk analysis for a single ticker.
    - [`ParallelPortfolioComparisonAgentV7.java`](src/main/java/com/google/adk/finance/v7/subagents/ParallelPortfolioComparisonAgentV7.java): Fan-in synthesis specialist (`parallel_comparison_agent_v7`) creating structured comparative matrices, highlighting failed/missing tickers, and noting data limitations.
    - [`ReportDraftingAgentV7.java`](src/main/java/com/google/adk/finance/v7/subagents/ReportDraftingAgentV7.java): Iterative author specialist (`report_drafting_agent_v7`) drafting initial reports and revising drafts based on critic feedback.
    - [`ComplianceEvidenceCriticAgentV7.java`](src/main/java/com/google/adk/finance/v7/subagents/ComplianceEvidenceCriticAgentV7.java): Iterative compliance specialist (`compliance_evidence_critic_agent_v7`) auditing empirical claims, requiring citations, providing revision instructions, and terminating the loop via `ExitLoopTool.INSTANCE`.
    - [`FinalReportPresenterAgentV7.java`](src/main/java/com/google/adk/finance/v7/subagents/FinalReportPresenterAgentV7.java): Post-loop presentation specialist (`final_report_presenter_agent_v7`) appending the compliance audit seal and disclaimers.
  - [`FinanceV7IntegrationTest.java`](src/test/java/com/google/adk/finance/v7/FinanceV7IntegrationTest.java): Comprehensive integration test suite validating sequential chaining, parallel concurrency, partial failure resilience, critic loop termination, and root agent delegation.
- **Core Paradigm Shift: Dynamic Delegation (V6) vs. Deterministic Orchestration (V7)**:

| Dimension | Version 6: Dynamic Sub-Agent Delegation | Version 7: Deterministic Workflow Orchestration |
|---|---|---|
| **Control Flow Authority** | The root LLM dynamically decides at runtime whether, when, and which sub-agent to invoke. | Hardened Java workflows explicitly govern execution sequences, concurrency, and termination conditions. |
| **Execution Order** | Non-deterministic; the parent LLM might skip specialists or call them in varying order. | Deterministic; guaranteed execution sequence (e.g. Stage 1 -> Stage 2 -> Stage 3 -> Stage 4 -> Stage 5). |
| **Concurrency** | Sequential LLM function-calling turns; cannot guarantee concurrent execution of independent tasks. | Native parallel fan-out via `CompletableFuture` / `ParallelAgent`; simultaneous execution with bounded latency. |
| **Quality Control & Feedback** | Single-pass generation; no formal automated audit gate before returning results to user. | Iterative loop via `LoopAgent` + `ComplianceEvidenceCriticAgentV7` with `ExitLoopTool` approval gating. |
| **Failure Handling** | Ad-hoc LLM error recovery if a sub-agent tool call fails. | Structured partial failure resilience; fan-in synthesis receives audit logs and reports limitations transparently. |

- **ADK Classes & Concepts Learned**:
  - `com.google.adk.agents.SequentialAgent`: Constructing multi-step pipelines where each step's output is recorded into session state under a dedicated `outputKey` (`stage1_company_research`, `stage2_fundamental_analysis`, etc.) and automatically injected into subsequent steps' prompt templates (`{stage1_company_research?}`).
  - `com.google.adk.agents.ParallelAgent`: Executing independent specialist agents across parallel branches and merging branch outputs.
  - `com.google.adk.agents.LoopAgent`: Constructing cyclic agent workflows with configurable iteration caps (`maxIterations(3)`) to prevent runaway infinite loops.
  - `com.google.adk.tools.ExitLoopTool`: Official ADK control tool (`ExitLoopTool.INSTANCE` / `exit_loop`) allowing a designated evaluator agent to break out of a `LoopAgent` early upon meeting criteria.
  - **Hybrid Composition (Workflows as Tools)**: Encapsulating end-to-end workflow execution logic into deterministic custom `BaseTool` instances (`run_sequential_research_workflow`, `run_parallel_portfolio_research_workflow`, `run_critic_loop_research_workflow`) exposed to the root `FinanceAdvisorAgentV7`.
  - **Asynchronous Fan-Out / Fan-In with Java Concurrency**: Coordinating parallel agent runners across virtual or platform worker threads with real timestamped logging and resilient error boundaries.

---

#### Workflow 1: Sequential Investment Research Pipeline (`InvestmentResearchSequentialWorkflowV7`)
```
User Request: "Prepare a structured investment research report on Infosys."
                              │
                              ▼
  +─────────────────────────────────────────────────────────────+
  | Stage 1: Company Research (CompanyResearchAgentV7)           |
  | - Grounded Google Search (GoogleSearchTool.INSTANCE)        |
  | - Output Key: stage1_company_research                       |
  +─────────────────────────────┬───────────────────────────────+
                                │
                                ▼
  +─────────────────────────────────────────────────────────────+
  | Stage 2: Fundamental Analysis (FundamentalAnalysisAgentV7)   |
  | - Yahoo Finance MCP (Revenue, Margins, Financial Statements)|
  | - Input: {stage1_company_research?}                         |
  | - Output Key: stage2_fundamental_analysis                   |
  +─────────────────────────────┬───────────────────────────────+
                                │
                                ▼
  +─────────────────────────────────────────────────────────────+
  | Stage 3: Risk Analysis (RiskAnalysisAgentV7)                |
  | - Market Beta, 52-Week Range, Leverage & Operational Risks  |
  | - Input: {stage1_company_research?}, {stage2_fundamental...}|
  | - Output Key: stage3_risk_analysis                          |
  +─────────────────────────────┬───────────────────────────────+
                                │
                                ▼
  +─────────────────────────────────────────────────────────────+
  | Stage 4: Valuation Analysis (ValuationAnalysisAgentV7)       |
  | - Multiples (P/E, Forward P/E, PEG, EV/EBITDA); No Guesses  |
  | - Input: Upstream findings from Stages 1-3                  |
  | - Output Key: stage4_valuation_analysis                     |
  +─────────────────────────────┬───────────────────────────────+
                                │
                                ▼
  +─────────────────────────────────────────────────────────────+
  | Stage 5: Synthesis (SequentialReportSynthesisAgentV7)        |
  | - Synthesizes 7-Section Institutional Research Report        |
  | - Output Key: stage5_synthesis_report                       |
  +─────────────────────────────────────────────────────────────+
```

---

#### Workflow 2: Parallel Portfolio Research & Comparison (`PortfolioParallelResearchWorkflowV7`)
```
User Request: "Analyse Infosys, HDFC Bank and Reliance and give me a comparable research summary."
                                    │
                                    ▼
                         [Fan-Out Dispatcher]
                                    │
          ┌─────────────────────────┼─────────────────────────┐
          │ (Thread 1)              │ (Thread 2)              │ (Thread 3)
          ▼                         ▼                         ▼
  +───────────────────────+ +───────────────────────+ +───────────────────────+
  | Worker: INFY.NS       | | Worker: HDB           | | Worker: RELIANCE.NS   |
  | - Google Search       | | - Google Search       | | - Google Search       |
  | - Yahoo Finance MCP   | | - Yahoo Finance MCP   | | - Yahoo Finance MCP   |
  | - Risk Assessment     | | - Risk Assessment     | | - Risk Assessment     |
  | Status: SUCCESS       | | Status: FAILURE/ERROR | | Status: SUCCESS       |
  +───────────┬───────────+ +───────────┬───────────+ +───────────┬───────────+
              │                         │                         │
              └─────────────────────────┼─────────────────────────┘
                                        │
                                        ▼
                                [Fan-In Aggregator]
                                        │
                                        ▼
  +───────────────────────────────────────────────────────────────────────────+
  | ParallelPortfolioComparisonAgentV7                                        |
  | - Cross-sectional comparative matrix (Valuation, Growth, Risk Profile)    |
  | - Explicit Failure Section: Documents HDB error & missing metrics         |
  | - Strict Anti-Fabrication: Does not invent missing data                   |
  | - Relative strengths, key catalysts, and mandatory disclaimer            |
  +───────────────────────────────────────────────────────────────────────────+
```

---

#### Workflow 3: Iterative Author-Critic Loop (`ResearchCriticLoopWorkflowV7`)
```
User Request: "Create an investment research report on Infosys and ensure important factual claims are supported by evidence."
                              │
                              ▼
  +─────────────────────────────────────────────────────────────+
  | Step 1: Initial Draft or Revision (ReportDraftingAgentV7)   |
  | - Drafts or revises 6-section evidence-backed research report|
  | - State Variable: draft_report                              |
  +─────────────────────────────┬───────────────────────────────+
                                │
                                ▼
  +─────────────────────────────────────────────────────────────+
  | Step 2: Evidence & Compliance Audit                         |
  |         (ComplianceEvidenceCriticAgentV7)                   |
  | - Audits factual claims, citations, numbers, and dates      |
  | - State Variable: critic_feedback                           |
  +─────────────────────────────┬───────────────────────────────+
                                │
               ┌────────────────┴────────────────┐
               │ Decision: Acceptable Evidence?  │
               ▼                                 ▼
      [YES: Approved]                   [NO: Unsubstantiated Claims]
               │                                 │
               ▼                                 ▼
    Calls ExitLoopTool.INSTANCE          Provides Specific Revision Tasks
    (Terminates LoopAgent)               (Loops back to Step 1, max 3)
               │                                 │
               ▼                                 └───────┐
  +───────────────────────────────────────────+          │
  | Step 3: Final Presentation                |          │
  |         (FinalReportPresenterAgentV7)     |<─────────┘ (After 3 iterations)
  | - Institutional formatting                |
  | - Compliance audit seal & verification log|
  | - Non-advice regulatory disclaimer        |
  +───────────────────────────────────────────+
```

---

### Week 4 — Version 8: Guardrails, Safety & Lifecycle Callbacks (`finance.v8`)
- **Package**: `com.google.adk.finance.v8`
- **Core Files**:
  - [`FinanceAdvisorAgentV8.java`](src/main/java/com/google/adk/finance/v8/FinanceAdvisorAgentV8.java): Root orchestrator wiring the 5-stage callback perimeter, research sub-agent, mock trading harness, and CLI runner.
  - [`FinanceConsoleV8.java`](src/main/java/com/google/adk/finance/v8/FinanceConsoleV8.java): Dedicated interactive CLI console supporting real-time guardrail introspection, evidence inspection, and automated test triggers.
  - **Guardrail Layer (`com.google.adk.finance.v8.guardrails`)**:
    - [`GuardrailResult.java`](src/main/java/com/google/adk/finance/v8/guardrails/GuardrailResult.java): Standardized immutable result contract (`ALLOW`, `BLOCK`, `SANITIZE`, `WARN`) with name, reason, sanitized output, and contextual metadata.
    - [`PiiDetector.java`](src/main/java/com/google/adk/finance/v8/guardrails/input/PiiDetector.java): High-precision pattern detector for sensitive financial identifiers (credit cards, bank accounts, emails, phone numbers, PAN, SSN, Aadhaar, customer IDs).
    - [`PiiSanitizer.java`](src/main/java/com/google/adk/finance/v8/guardrails/input/PiiSanitizer.java): Deterministic redaction engine substituting sensitive values with privacy tokens (`[REDACTED_...]`).
    - [`PromptInjectionDetector.java`](src/main/java/com/google/adk/finance/v8/guardrails/input/PromptInjectionDetector.java): Defends against system prompt overrides, secret/prompt extraction (`reveal system prompt`), tool manipulation ("call every tool"), and jailbreaking.
    - [`TickerValidator.java`](src/main/java/com/google/adk/finance/v8/guardrails/tool/TickerValidator.java): Validates ticker symbol conventions (NSE `.NS`, BSE `.BO`, US tickers), enforces length boundaries (<= 15 chars), and categorically rejects SQL/shell injection vectors.
    - [`ToolOperationGuard.java`](src/main/java/com/google/adk/finance/v8/guardrails/tool/ToolOperationGuard.java): Authorizes analytical read-only tools and categorically blocks transactional operations (`execute_trade`, `place_order`, `transfer_funds`).
    - [`OffensiveLanguageDetector.java`](src/main/java/com/google/adk/finance/v8/guardrails/output/OffensiveLanguageDetector.java): Scans model outputs for toxic or abusive language, substituting deterministic safe responses.
    - [`ComplianceDisclaimerGuard.java`](src/main/java/com/google/adk/finance/v8/guardrails/output/ComplianceDisclaimerGuard.java): Inspects and appends mandatory institutional non-advice regulatory disclaimers.
    - [`HallucinationDetector.java`](src/main/java/com/google/adk/finance/v8/guardrails/output/HallucinationDetector.java): Audits model numerical assertions (prices, metrics) against empirical observations stored in `EvidenceStore` (1% tolerance).
  - **Callback Interception Layer (`com.google.adk.finance.v8.callbacks`)**:
    - [`BeforeAgentGuardrail.java`](src/main/java/com/google/adk/finance/v8/callbacks/BeforeAgentGuardrail.java): Implements `BeforeAgentCallbackSync`. Evaluates user input before execution; halts prompt injections early via `invocationContext.setEndInvocation(true)`, and stores sanitized input into session state.
    - [`BeforeModelGuardrail.java`](src/main/java/com/google/adk/finance/v8/callbacks/BeforeModelGuardrail.java): Implements `BeforeModelCallbackSync`. Wire-level defense scrubbing raw PII from `LlmRequest` contents before outbound dispatch to the model.
    - [`BeforeToolGuardrail.java`](src/main/java/com/google/adk/finance/v8/callbacks/BeforeToolGuardrail.java): Implements `BeforeToolCallbackSync`. Authorizes tool invocation and validates ticker arguments before external execution, returning an override map to prevent unauthorized runs.
    - [`AfterToolEvidenceCapture.java`](src/main/java/com/google/adk/finance/v8/callbacks/AfterToolEvidenceCapture.java): Implements `AfterToolCallbackSync`. Automatically extracts structured empirical observations from tool outputs into `EvidenceStore`.
    - [`AfterModelGuardrail.java`](src/main/java/com/google/adk/finance/v8/callbacks/AfterModelGuardrail.java): Implements `AfterModelCallbackSync`. Coordinates toxic language replacement, numerical fact auditing against `EvidenceStore`, and mandatory disclaimer injection.
  - **Evidence Repository (`com.google.adk.finance.v8.evidence`)**:
    - [`EvidenceStore.java`](src/main/java/com/google/adk/finance/v8/evidence/EvidenceStore.java): Thread-safe in-memory fact store capturing quotes, prices, and metrics from tool outputs for hallucination detection.
  - **Sub-Agents (`com.google.adk.finance.v8.subagents`)**:
    - [`GuardedMarketResearchAgentV8.java`](src/main/java/com/google/adk/finance/v8/subagents/GuardedMarketResearchAgentV8.java): Isolated Google Search sub-agent wrapped with V8 guardrail callbacks.
    - [`MockTradingAgentV8.java`](src/main/java/com/google/adk/finance/v8/subagents/MockTradingAgentV8.java): Prohibited transactional tool harness verifying that `execute_trade` is strictly blocked by `BeforeToolGuardrail`.
- **ADK Classes & Concepts Learned**:
  - `com.google.adk.agents.callbacks.BeforeAgentCallbackSync`: Intercepting initial user prompt, mutating state, or aborting execution via `InvocationContext.setEndInvocation(true)`.
  - `com.google.adk.agents.callbacks.BeforeModelCallbackSync`: Intercepting `LlmRequest` before wire transmission to sanitize prompt contents.
  - `com.google.adk.agents.callbacks.BeforeToolCallbackSync`: Intercepting tool name and args, returning `Optional<Map<String, Object>>` override to short-circuit execution.
  - `com.google.adk.agents.callbacks.AfterToolCallbackSync`: Capturing empirical tool execution outputs without altering tool response.
  - `com.google.adk.agents.callbacks.AfterModelCallbackSync`: Intercepting `LlmResponse` to inspect, sanitize, fact-audit, or substitute final generated text.
- **5-Stage Defensive Interception Topology**:
  ```
                     USER INPUT
                         │
                         ▼
             [Stage 1: BeforeAgentGuardrail]
             ├── PiiDetector (Detect sensitive tokens)
             ├── PiiSanitizer (Generate redacted prompt)
             ├── PromptInjectionDetector (Check override / exfiltration)
             │   └── If Injection: setEndInvocation(true) -> Return User-Safe Rejection
             └── Else: Store sanitized_user_input in session state
                         │
                         ▼
             [Stage 2: BeforeModelGuardrail]
             └── Scrub raw PII from LlmRequest contents before wire transmission
                         │
                         ▼
                   LLM AGENT / MODEL
                         │
                 (Tool Decision Made)
                         │
                         ▼
             [Stage 3: BeforeToolGuardrail]
             ├── ToolOperationGuard (Verify read-only vs transactional)
             │   └── If Transactional (e.g. execute_trade): Return Tool Override (BLOCKED)
             ├── TickerValidator (Validate format & check SQL/shell injection)
             │   └── If Invalid / Injected: Return Tool Override (BLOCKED)
             └── Else: Allow external tool execution
                         │
                         ▼
               TOOL EXECUTION (MCP / Search)
                         │
                         ▼
             [Stage 4: AfterToolEvidenceCapture]
             └── Ingest output facts (Ticker, Price, Metrics) into EvidenceStore
                         │
                         ▼
                   LLM AGENT / MODEL
                 (Synthesizes Response)
                         │
                         ▼
             [Stage 5: AfterModelGuardrail]
             ├── OffensiveLanguageDetector (Check toxicity -> Replace with safe fallback)
             ├── HallucinationDetector (Audit claimed figures vs EvidenceStore facts)
             │   └── If Mismatch > 1%: Append factual discrepancy warning
             └── ComplianceDisclaimerGuard (Verify & append mandatory non-advice disclaimer)
                         │
                         ▼
                   FINAL RESPONSE
  ```

---

### Week 5 — Version 9: Evaluation + Failure Testing (`finance.v9`)
- **Package**: `com.google.adk.finance.v9` (mirrored in `v9/`)
- **Core Files**:
  - [`FinanceAdvisorAgentV9.java`](src/main/java/com/google/adk/finance/v9/FinanceAdvisorAgentV9.java): Root agent factory integrating Google Search, Yahoo Finance MCP, `PortfolioMathTool`, `LoadCustomerPortfolioTool`, and V8 safety guardrails.
  - [`FinanceConsoleV9.java`](src/main/java/com/google/adk/finance/v9/FinanceConsoleV9.java): Interactive terminal console supporting slash commands (`/cases`, `/eval`, `/fail`, `/evidence`).
  - [`EvidenceRecord.java`](src/main/java/com/google/adk/finance/v9/evaluation/EvidenceRecord.java): Immutable model capturing tool observations (ticker, field, value, numericValue, sourceReference, raw payload).
  - [`EvidenceStoreV9.java`](src/main/java/com/google/adk/finance/v9/evaluation/EvidenceStoreV9.java): Thread-safe session evidence repository.
  - [`FaithfulnessEvaluator.java`](src/main/java/com/google/adk/finance/v9/evaluation/FaithfulnessEvaluator.java): Deterministic claim-to-evidence consistency auditor ($\le 2\%$ tolerance).
  - [`CalculationFidelityEvaluator.java`](src/main/java/com/google/adk/finance/v9/evaluation/CalculationFidelityEvaluator.java): Deterministic arithmetic verifier validating PnL and allocation weights against `PortfolioMathTool`.
  - [`ScenarioCompletenessEvaluator.java`](src/main/java/com/google/adk/finance/v9/evaluation/ScenarioCompletenessEvaluator.java): Deterministic 3-tier scenario structure verifier (Baseline, Upside, Stress).
  - [`LlmJudgeEvaluator.java`](src/main/java/com/google/adk/finance/v9/evaluation/LlmJudgeEvaluator.java): Qualitative judge assessing evidence explanation, uncertainty calibration, and reasoning coherence without overriding arithmetic.
  - [`EvaluationRunner.java`](src/main/java/com/google/adk/finance/v9/evaluation/EvaluationRunner.java): Central orchestrator producing structured ASCII `EvaluationReport`.
  - [`GetCapturedEvidenceTool.java`](src/main/java/com/google/adk/finance/v9/tools/GetCapturedEvidenceTool.java): Diagnostic tool enabling real-time inspection of captured empirical evidence directly from the ADK Web Dev UI.
  - [`FailureScenarioRunner.java`](src/main/java/com/google/adk/finance/v9/testing/FailureScenarioRunner.java): Adversarial failure harness testing PII injection, prompt overrides, invalid tickers, unauthorized operations, and tool timeouts.
- **Datasets**:
  - [`finance-evaluation-cases.json`](src/main/resources/finance/v9/datasets/finance-evaluation-cases.json): Golden evaluation benchmark cases.
  - [`failure-test-cases.json`](src/main/resources/finance/v9/datasets/failure-test-cases.json): Adversarial failure fixtures.
- **JUnit 5 & AssertJ Test Suite**:
  - [`FaithfulnessEvaluationTest.java`](src/test/java/com/google/adk/finance/v9/FaithfulnessEvaluationTest.java)
  - [`CalculationFidelityTest.java`](src/test/java/com/google/adk/finance/v9/CalculationFidelityTest.java)
  - [`ScenarioCompletenessTest.java`](src/test/java/com/google/adk/finance/v9/ScenarioCompletenessTest.java)
  - [`LlmJudgeEvaluationTest.java`](src/test/java/com/google/adk/finance/v9/LlmJudgeEvaluationTest.java)
  - [`GuardrailRegressionTest.java`](src/test/java/com/google/adk/finance/v9/GuardrailRegressionTest.java)
  - [`FailureTestingTest.java`](src/test/java/com/google/adk/finance/v9/FailureTestingTest.java)
  - [`FinanceAdvisorV9EvaluationTest.java`](src/test/java/com/google/adk/finance/v9/FinanceAdvisorV9EvaluationTest.java)
- **ADK Classes & Concepts Learned**:
  - `AfterToolCallbackSync` evidence ingestion via `AfterToolEvidenceCaptureV9`.
  - Systematic offline benchmark evaluation of multi-turn sessions.
  - Clean separation of deterministic truth (`PortfolioMathTool`, `EvidenceRecord`) from qualitative LLM judging.
  - Adversarial red-teaming and failure injection without data fabrication.
- **Evaluation Architecture**:
  ```
  User Request / Golden Benchmark Case
             │
             ▼
  FinanceAdvisorAgentV9
             │
             ├── Google Search Tool (stockmarket_researcher)
             ├── Yahoo Finance MCP Toolset
             ├── PortfolioMathTool (Deterministic Math)
             └── LoadCustomerPortfolioTool (SQLite State)
             │
             ▼ (Tool Call Interception via AfterToolEvidenceCaptureV9)
       EvidenceStoreV9 ◄─────────────────────────┐
             │                                   │
             ├─► get_captured_evidence Tool      │ (Live Web Dev UI /
             │   (Returns verified fact table) ──┘  CLI Interactive Query)
             │
             ▼
       Generated Report
             │
     ┌───────┴────────────────────────┐
     ▼                                ▼
  Deterministic Evaluators       LLM-as-a-Judge
  ├── Faithfulness               ├── Evidence Usage
  ├── Calculation Fidelity       ├── Reasoning Consistency
  └── Scenario Completeness      ├── Uncertainty Calibration
     │                           └── Query Responsiveness
     └───────┬────────────────────────┘
             ▼
       EvaluationReport (ASCII & Structured Status)
  ```
- **Documentation**: See [`v9/README.md`](v9/README.md) and [`v9/docs/finance-agent-v9-evaluation.md`](v9/docs/finance-agent-v9-evaluation.md).

---

### Week 5 — Version 10: Observability & Enterprise Persistence (`finance.v10`)
- **Package**: `com.google.adk.finance.v10`
- **Components**:
  - **Telemetry**: OpenTelemetry / Micrometer metrics tracking tool call counts, latency per sub-agent, and token expenditure.
  - **Durable Persistence**: SQLite/PostgreSQL tables storing portfolio snapshots (`portfolio_snapshots`), run steps (`agent_execution_logs`), and completed decision reports (`decision_reports`).

---

### Week 6 — Production Release: Cloud Native Architecture (`finance.prod`)
- **Package**: `com.google.adk.finance.prod`
- **Deployment Artifacts**:
  - Multi-stage `Dockerfile` producing an optimized Java 21 JRE runtime image.
  - Google Cloud Run service configuration with Workload Identity Federation.
  - Google Vertex AI Agent Engine deployment configuration.
  - Automated CI/CD pipeline via GitHub Actions.

---

# PART III: SOCIAL SPARK AGENT ARCHITECTURE (DOMAIN 1)

Social Spark is the production-grade multi-agent social media post generator, researcher, image creator, human approval gate, and publisher built with Google ADK.

## 1. Specialist Agent Structure

### Root Orchestrator: `social_poster`
- **Source File**: [`SocialPosterAgentFactory.java`](src/main/java/com/google/adk/socialspark/agents/SocialPosterAgentFactory.java)
- **Role**: Coordinates the entire lifecycle. It does not draft or search on its own; it decomposes user requests, delegates to sub-agents, prompts the user for approvals, and dispatches to publishing MCP tools.
- **Model**: Configured via `AppConfig.ORCHESTRATOR_MODEL` (supports `gemma4:31b` via Ollama or `gemini-2.5-flash`).
- **Tools**:
  - `AgentTool.create(researchAgent)`
  - `AgentTool.create(draftAgent)`
  - `AgentTool.create(memoryAgent)` (conditional on `MEMORY_AGENT_CARD_URL`)
  - Posting toolsets from `PostingToolsetsFactory` (LinkedIn & Buffer MCPs)
- **Lifecycle Callbacks**:
  - `initStageCallback` (`beforeAgentCallbackSync`): Initializes `context.state().put("pipeline_stage", "idea")` if unset.
  - `delayBufferPostCallback` (`beforeToolCallbackSync`): Intercepts `create_post` calls destined for Buffer (`channelId` present) and calculates a delayed execution timestamp (`dueAt = now + BUFFER_REVIEW_DELAY_MINUTES`) in `customScheduled` mode.
  - `trackStageAndSavePostCallback` (`afterToolCallbackSync`): Updates pipeline stages and commits published post metadata directly to SQLite `PostRepository`.
  - `stageAfterAgentCallback` (`afterAgentCallbackSync`): Transitions stage to `"awaiting_approval"` when drafting/researching finishes.

### Research Specialist: `research_agent`
- **Source File**: [`ResearchAgentFactory.java`](src/main/java/com/google/adk/socialspark/agents/ResearchAgentFactory.java)
- **Role**: Performs real-time web search and factual retrieval without drafting post copy.
- **Model**: Gemini 2.x/3.x (`gemini-2.5-flash`) required by `GoogleSearchTool`.
- **Tools**: `GoogleSearchTool.INSTANCE`.
- **Output Key**: `research_notes`.

### Drafting Specialist: `draft_agent`
- **Source File**: [`DraftAgentFactory.java`](src/main/java/com/google/adk/socialspark/agents/DraftAgentFactory.java)
- **Role**: Crafts and refines post copy adhering to platform constraints, brand tone, and format guidelines.
- **Model**: Configured via `AppConfig.DRAFT_MODEL`.
- **Tools**:
  - `SkillToolset`: Dynamically reads markdown instructions and reference files from `skills/` using `LocalSkillSource`.
  - `CheckTextLengthTool`: Measures exact character counts and overages against platform constraints.
- **State Injection**: Injects `{research_notes?}` and `{memory_notes?}` dynamically.
- **Output Key**: `current_draft`.

### Memory Specialist: `memory_agent` (A2A)
- **Source File**: [`MemoryAgentFactory.java`](src/main/java/com/google/adk/socialspark/agents/MemoryAgentFactory.java)
- **Role**: Provides long-term memory about user post history, preferred topics, and stylistic quirks.
- **Protocol**: Implemented via `RemoteA2AAgent` connecting to `MEMORY_AGENT_CARD_URL`.

---

# PART IV: COMMON PLATFORM SUBSYSTEMS

## 1. Hybrid LLM Architecture & Ollama Adapter

### Factory Method (`AppConfig.createModel`)
Located in [`AppConfig.java`](src/main/java/com/google/adk/socialspark/config/AppConfig.java):
1. **Gemini Routing**: If the model starts with `gemini-` or `google/`, or if `GOOGLE_GENAI_USE_VERTEXAI=TRUE`, it instantiates `com.google.adk.models.Gemini` using either direct API key (`GEMINI_API_KEY`) or Google Cloud Vertex AI project/location configuration.
2. **Ollama / OpenAI Routing**: If `OLLAMA_API_KEY` is present or if the model name is non-Gemini (e.g. `gemma4:31b`), it instantiates `OllamaOpenAiLlm`.

### Ollama BaseLlm Adapter (`OllamaOpenAiLlm`)
Located in [`OllamaOpenAiLlm.java`](src/main/java/com/google/adk/socialspark/models/OllamaOpenAiLlm.java):
- Extends `com.google.adk.models.BaseLlm`.
- Bridges ADK's native `LlmRequest` / `LlmResponse` pipeline to the standard `/v1/chat/completions` endpoint using Google ADK's built-in `ChatCompletionsHttpClient`.
- Automatically normalizes endpoints (e.g. `https://ollama.com` or `http://localhost:11434` -> `.../v1`).
- Delivers reactive streaming via RxJava `Flowable<LlmResponse>`.

---

## 2. Model Context Protocol (MCP) Subsystem

Publishing and market data tools are decoupled through the **Model Context Protocol (MCP)** using `io.modelcontextprotocol.sdk:mcp:1.1.2` and ADK's `com.google.adk.tools.mcp.McpToolset`.

- **LinkedIn MCP**: Subprocess execution via `uv run --with fastmcp --with httpx python mcp/linkedin_server.py`.
- **Buffer MCP**: Remote Streamable HTTP MCP connection to `https://mcp.buffer.com/mcp` with automated review delay scheduling.
- **Finance Market MCP**: Subprocess execution via `uv run --with fastmcp --with httpx python mcp/finance_market_server.py`.

---

## 3. Embedded Persistence Subsystem

- **Implementation**: [`PostRepository.java`](src/main/java/com/google/adk/socialspark/db/PostRepository.java) & [`PostRecord.java`](src/main/java/com/google/adk/socialspark/db/PostRecord.java).
- **Database Engine**: Embedded SQLite JDBC (`org.xerial:sqlite-jdbc:3.47.2.0`) writing to `social_spark.db`.
- **Concurrency & Performance**: Configured with `PRAGMA journal_mode=WAL;` and `PRAGMA busy_timeout = 30000;`.
- **Table Schema**:
  ```sql
  CREATE TABLE IF NOT EXISTS posts (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      platform TEXT NOT NULL,
      text TEXT NOT NULL,
      image_path TEXT,
      image_url TEXT,
      post_url TEXT,
      created_at TEXT NOT NULL
  );
  ```

---

## 4. AG-UI Protocol Engine & Web Server

Built on **Javalin 6.3.0** to interface with `@ag-ui/client` and CopilotKit Next.js frontends.

### REST & Streaming Endpoints
Defined in [`Application.java`](src/main/java/com/google/adk/socialspark/Application.java):
| Endpoint | Method | Description |
|---|---|---|
| `/healthz` | `GET` | Health probe returning `{"status":"ok","service":"social-spark-adk-java"}`. |
| `/api/posts` | `GET` | Returns list of recently published posts (`PostRecord`) from SQLite. |
| `/agents/state` | `POST` | Rehydrates agent state and message history for a given `threadId`. |
| `/api/adk` | `POST` | High-performance Server-Sent Events (SSE) stream executing the Social Spark turn. |
| `/outputs/{filename}` | `GET` | Serves local static generated images stored in `gallery/`. |

### Reactive Event Translation
Defined in [`AgUiEventTranslator.java`](src/main/java/com/google/adk/socialspark/agui/AgUiEventTranslator.java):
- Translates Google ADK's RxJava `Flowable<Event>` stream into AG-UI protocol events:
  - `RUN_STARTED`, `STATE_SNAPSHOT`, `TEXT_MESSAGE_START`, `TEXT_MESSAGE_CONTENT`, `TEXT_MESSAGE_END`, `TOOL_CALL_START`, `TOOL_CALL_ARGS`, `TOOL_CALL_END`, `TOOL_CALL_RESULT`, `RUN_FINISHED`, `RUN_ERROR`.

---

# PART V: COMPLETE FOLDER & FILE DIRECTORY TREE

```
ai-agents-google-adk-java/
├── AGENTS.md                                  # [THIS FILE] Canonical architecture & roadmap reference
├── README.md                                  # User-facing overview, quickstart & setup instructions
├── build.gradle.kts                           # Primary Gradle build (Java 21 toolchain, ADK 1.4.0)
├── pom.xml                                    # Alternative Maven POM build
├── settings.gradle.kts                        # Gradle root project settings
├── gradlew.bat / gradlew                      # Gradle wrapper scripts
├── run-backend.bat / .sh                      # Launch script for Javalin AG-UI server (Port 8000)
├── run-adk-web.bat / .sh                      # Launch script for Google ADK Official Dev UI (Port 8080)
├── test-agent.bat / .sh                       # Launch script for Direct CLI Agent Console
├── .env                                       # Local environment variables (API keys, ports, models)
├── .env.example                               # Environment template with documented parameters
├── social_spark.db                            # SQLite database (WAL mode) storing published post history
│
├── gallery/                                   # Local image staging directory served at /outputs/{file}
│
├── knowledge/                                 # Curated finance domain grounding knowledge
│   ├── glossary.md                            # Financial terms, valuation, profitability & market cap
│   ├── valuation-principles.md                # Multiples framework, forward P/E, relative valuation
│   ├── fundamental-analysis.md                # Operating metrics, margins, ROE/ROCE, capital return
│   ├── risk-framework.md                      # Systematic vs unsystematic, concentration, liquidity
│   ├── portfolio-principles.md                # Allocation, correlation, diversification, horizon
│   └── market-research-framework.md           # 8-step corporate news & earnings investigation
│
├── skills/                                    # Dynamic Agent Skills loaded via LocalSkillSource
│   ├── brand-voice/                           # Social Spark: Brand tone & British English rules
│   │   └── SKILL.md
│   ├── platform-style/                        # Social Spark: LinkedIn vs X platform rules
│   │   ├── SKILL.md
│   │   └── references/
│   │       ├── linkedin.md
│   │       └── x.md
│   ├── post-formatter/                        # Social Spark: Post structural templates
│   │   └── SKILL.md
│   ├── poster-style/                          # Social Spark: Aesthetic rules
│   │   └── SKILL.md
│   └── finance/                               # Finance Agent Skills (Week 2 — Version 4)
│       ├── finance-fundamentals/
│       │   └── SKILL.md                       # Core financial concepts, ratios, and terms
│       ├── fundamental-analysis/
│       │   └── SKILL.md                       # Multi-factor business quality, moat, and margins
│       ├── valuation/
│       │   └── SKILL.md                       # Multiple contextualization (high P/E != overvalued)
│       ├── risk-management/
│       │   └── SKILL.md                       # Risk taxonomy and mitigation strategies
│       ├── portfolio-analysis/
│       │   └── SKILL.md                       # Asset allocation and correlation principles
│       └── market-research/
│           └── SKILL.md                       # 8-step investigation methodology with Google Search
│
├── mcp/                                       # Model Context Protocol server definitions
│   ├── README.md                              # MCP server setup guide
│   ├── linkedin_server.py                     # Social Spark: LinkedIn FastMCP server
│   └── finance_market_server.py               # [NEW] Finance Agent: Market data FastMCP server
│
└── src/
    ├── main/
    │   ├── java/com/google/adk/
    │   │   │
    │   │   ├── socialspark/                       # DOMAIN 1: Social Spark Multi-Agent System
    │   │   │   ├── Application.java               # Main Javalin 6 server (Port 8000)
    │   │   │   ├── AdkDevUiApplication.java       # Dev UI launcher (Port 8080)
    │   │   │   ├── AgentConsoleRunner.java        # Interactive REPL & one-shot CLI runner
    │   │   │   │
    │   │   │   ├── config/
    │   │   │   │   └── AppConfig.java             # Shared environment, model routing & factory
    │   │   │   │
    │   │   │   ├── models/
    │   │   │   │   └── OllamaOpenAiLlm.java       # Custom BaseLlm adapter bridging ADK to Ollama/OpenAI
    │   │   │   │
    │   │   │   ├── agents/                        # Social Spark Specialist Agents
    │   │   │   │   ├── SocialPosterAgentFactory.java  # Root orchestrator & callbacks
    │   │   │   │   ├── ResearchAgentFactory.java      # Isolated GoogleSearchTool agent
    │   │   │   │   ├── DraftAgentFactory.java         # Skill-driven drafting specialist
    │   │   │   │   ├── MemoryAgentFactory.java        # Remote A2A client
    │   │   │   │   └── PostingToolsetsFactory.java    # LinkedIn & Buffer MCP toolsets
    │   │   │   │
    │   │   │   ├── tools/                         # Social Spark Tools
    │   │   │   │   ├── CheckTextLengthTool.java
    │   │   │   │   ├── GenerateImageTool.java
    │   │   │   │   ├── UploadImageTool.java
    │   │   │   │   └── UseProvidedImageUrlTool.java
    │   │   │   │
    │   │   │   ├── agui/                          # AG-UI Reactive Protocol Engine
    │   │   │   │   ├── AgUiModels.java
    │   │   │   │   ├── SessionStore.java
    │   │   │   │   └── AgUiEventTranslator.java
    │   │   │   │
    │   │   │   └── db/                            # Social Spark Persistence
    │   │   │       ├── PostRecord.java
    │   │   │       └── PostRepository.java
    │   │   │
    │   │   └── finance/                               # DOMAIN 2: Finance Portfolio Decision-Support
    │   │       ├── tools/                             # Shared Finance Tools (Cross-Version)
    │   │       │   ├── LoadCustomerPortfolioTool.java # BaseTool injecting state via ToolContext
    │   │       │   ├── PortfolioMathTool.java         # BaseTool for PnL, allocation & SMA math
    │   │       │   └── ProjectKnowledgeTool.java      # BaseTool reading curated knowledge markdown
    │   │       │
    │   │       ├── v1/                                # Week 1: Foundational Agent & Session
    │   │       │   ├── FinanceAgentV1Factory.java     # LlmAgent factory & instruction engineering
    │   │       │   └── FinanceConsoleV1.java          # Dedicated interactive CLI console
    │   │       │
    │   │       ├── v2/                                # Week 1: Context & Holdings State Management
    │   │       │   ├── PortfolioModels.java           # Customer & holding domain records
    │   │       │   ├── CustomerPortfolioRepository.java # SQLite DAO with WAL mode & auto-seed
    │   │       │   ├── LoadCustomerPortfolioTool.java # Backward-compat alias -> finance.tools
    │   │       │   ├── FinanceAgentV2Factory.java     # Agent factory with dynamic prompt templates
    │   │       │   └── FinanceConsoleV2.java          # Multi-turn stateful CLI console
    │   │       │
    │   │       ├── v3/                                # Week 2: Java Tools & Grounded Google Search
    │   │       │   ├── agents/
    │   │       │   │   ├── FinanceAgentV3Factory.java         # Root decision-support agent
    │   │       │   │   └── MarketResearchAgentFactory.java    # Isolated GoogleSearchTool agent
    │   │       │   ├── PortfolioMathTool.java                 # Backward-compat alias -> finance.tools
    │   │       │   └── FinanceConsoleV3.java                  # Interactive search & math CLI console
    │   │       │
    │   │       ├── v4/                                # Week 2: Skills + Grounding Knowledge
    │   │       │   ├── ProjectKnowledgeTool.java              # Backward-compat alias -> finance.tools
    │   │       │   ├── FinanceAdvisorAgentV4Factory.java      # Root advisor factory wiring skills, knowledge & search
    │   │       │   └── FinanceConsoleV4.java                  # Interactive skills & knowledge CLI console
    │   │       │
    │   │       ├── v5/                                # Week 3: Yahoo Finance MCP Integration
    │   │       │   ├── YahooFinanceMcpClientManager.java      # Client lifecycle, subprocess launch & readiness
    │   │       │   ├── FinanceAdvisorAgentV5Factory.java      # Root advisor factory wiring MCP toolset & search
    │   │       │   └── FinanceConsoleV5.java                  # Interactive MCP console & tool execution tracing
    │   │       │
    │   │       ├── v6/                                # Week 3: Specialist Multi-Agent System
    │   │       │   ├── FinanceAdvisorAgentV6.java             # Root orchestrator (portfolio_director)
    │   │       │   ├── FinanceConsoleV6.java                  # Interactive CLI runner with delegation observability
    │   │       │   └── subagents/                             # Specialized Sub-Agents (Narrow responsibilities)
    │   │       │       ├── MarketResearchAgentV6.java         # stockmarket_researcher: Google Search & news attribution
    │   │       │       ├── ScenarioAnalystAgentV6.java        # scenario_analyst: Bull/Bear macro stress-testing
    │   │       │       ├── ReportWriterAgentV6.java           # report_writer: Synthesizes institutional decision report
    │   │       │       ├── FundamentalAnalysisAgentV6.java    # fundamental_analysis_agent: Yahoo Finance MCP fundamentals
    │   │       │       └── PortfolioRiskAgentV6.java          # portfolio_risk_agent: Volatility, beta, concentration & risk
    │   │       │
    │   │       └── v7/                                # Week 4: Workflow Orchestration
    │   │           ├── FinanceAdvisorAgentV7.java             # Root orchestrator (workflow_director)
    │   │           ├── FinanceConsoleV7.java                  # Interactive CLI runner for workflows
    │   │           ├── tools/                                 # V7 Workflow Invocation Tools
    │   │           │   ├── RunSequentialWorkflowTool.java     # BaseTool executing sequential workflow
    │   │           │   ├── RunParallelWorkflowTool.java       # BaseTool executing parallel fan-out/in
    │   │           │   └── RunCriticLoopWorkflowTool.java     # BaseTool executing iterative critic loop
    │   │           ├── workflows/                             # 3 Deterministic Workflow Engines
    │   │           │   ├── sequential/
    │   │           │   │   └── InvestmentResearchSequentialWorkflowV7.java # 5-Stage Sequential Pipeline (SequentialAgent)
    │   │           │   ├── parallel/
    │   │           │   │   ├── PortfolioParallelResearchWorkflowV7.java    # Concurrent Fan-Out/In with failure isolation
    │   │           │   │   └── ThreadSafeMcpToolset.java                   # Synchronized toolset wrapper for parallel safety
    │   │           │   └── loop/
    │   │           │       └── ResearchCriticLoopWorkflowV7.java           # Iterative Critic Loop (LoopAgent + ExitLoopTool)
    │   │           └── subagents/                             # 10 Specialized V7 Sub-Agents
    │   │               ├── CompanyResearchAgentV7.java        # Stage 1: Google Search grounding
    │   │               ├── FundamentalAnalysisAgentV7.java    # Stage 2: Yahoo Finance MCP fundamentals
    │   │               ├── RiskAnalysisAgentV7.java           # Stage 3: Volatility, Beta & Leverage
    │   │               ├── ValuationAnalysisAgentV7.java      # Stage 4: Grounded valuation multiples
    │   │               ├── SequentialReportSynthesisAgentV7.java # Stage 5: Institutional 7-section report synthesis
    │   │               ├── CompanyParallelResearchWorkerV7.java # Parallel worker for concurrent research
    │   │               ├── ParallelPortfolioComparisonAgentV7.java # Fan-in comparison aggregator
    │   │               ├── ReportDraftingAgentV7.java         # Author/reviser in iterative critic loop
    │   │               ├── ComplianceEvidenceCriticAgentV7.java # Quality critic with ExitLoopTool
    │   │               └── FinalReportPresenterAgentV7.java   # Final presenter of audited report
    │   │
    │   │       └── v8/                                # Week 4: Guardrails, Safety & Callbacks
    │   │           ├── FinanceAdvisorAgentV8.java             # Root orchestrator with full callback perimeter
    │   │           ├── FinanceConsoleV8.java                  # Interactive CLI runner for guardrails
    │   │           ├── guardrails/                            # Deterministic Guardrails
    │   │           │   ├── GuardrailResult.java               # Standardized result contract (ALLOW, BLOCK, SANITIZE, WARN)
    │   │           │   ├── input/
    │   │           │   │   ├── PiiDetector.java               # Regex detector for account/card/phone/email/PAN/SSN
    │   │           │   │   ├── PiiSanitizer.java              # Masks PII with privacy tokens ([REDACTED_...])
    │   │           │   │   └── PromptInjectionDetector.java   # Detects overrides, secrets, and jailbreaks
    │   │           │   ├── tool/
    │   │           │   │   ├── TickerValidator.java           # Validates symbols (.NS, .BO, US) and blocks injection
    │   │           │   │   └── ToolOperationGuard.java        # Authorizes analytics & blocks trade execution
    │   │           │   └── output/
    │   │           │       ├── OffensiveLanguageDetector.java # Screens toxic/abusive terms with safe fallback
    │   │           │       ├── ComplianceDisclaimerGuard.java # Injects mandatory institutional non-advice disclaimers
    │   │           │       └── HallucinationDetector.java     # Audits model claims against empirical EvidenceStore
    │   │           ├── callbacks/                             # Google ADK Lifecycle Interceptions
    │   │           │   ├── BeforeAgentGuardrail.java          # BeforeAgentCallbackSync (PII check & injection halt)
    │   │           │   ├── BeforeModelGuardrail.java          # BeforeModelCallbackSync (Wire-level PII scrubber)
    │   │           │   ├── BeforeToolGuardrail.java           # BeforeToolCallbackSync (Tool auth & ticker validation)
    │   │           │   ├── AfterToolEvidenceCapture.java      # AfterToolCallbackSync (Empirical fact capture)
    │   │           │   └── AfterModelGuardrail.java           # AfterModelCallbackSync (Toxicity, facts & disclaimer)
    │   │           ├── evidence/
    │   │           │   └── EvidenceStore.java                 # Thread-safe empirical fact repository
    │   │           └── subagents/
    │   │               ├── GuardedMarketResearchAgentV8.java  # Isolated search sub-agent with V8 callbacks
    │   │               └── MockTradingAgentV8.java            # Prohibited trading harness for tool-block testing
    │   │
    │   └── resources/
    │       └── logback.xml                    # SLF4J / Logback console logging configuration
    │
    └── test/
        └── java/com/google/adk/
            │
            ├── socialspark/                   # Social Spark Test Suite
            │   ├── ApplicationTest.java
            │   ├── db/PostRepositoryTest.java
            │   ├── models/OllamaOpenAiLlmTest.java
            │   ├── tools/CheckTextLengthToolTest.java
            │   └── tools/UseProvidedImageUrlToolTest.java
            │
            └── finance/                       # Finance Agent Test Suite
                ├── v1/
                │   └── FinanceV1IntegrationTest.java
                ├── v2/
                │   ├── CustomerPortfolioRepositoryTest.java
                │   ├── LoadCustomerPortfolioToolTest.java
                │   └── FinanceV2IntegrationTest.java
                ├── v3/
                │   ├── PortfolioMathToolTest.java
                │   └── FinanceV3IntegrationTest.java
                ├── v4/
                │   └── FinanceV4IntegrationTest.java
                ├── v5/
                │   └── FinanceV5IntegrationTest.java
                ├── v6/
                │   └── FinanceV6IntegrationTest.java
                ├── v7/
                │   └── FinanceV7IntegrationTest.java
                ├── v8/
                │   ├── GuardrailResultTest.java
                │   ├── PiiGuardrailTest.java
                │   ├── PromptInjectionGuardrailTest.java
                │   ├── ToolGuardrailsTest.java
                │   ├── OutputGuardrailsTest.java
                │   ├── EvidenceStoreTest.java
                │   ├── LifecycleCallbacksTest.java
                │   └── FinanceAdvisorAgentV8Test.java
                ├── v9/
                │   ├── FaithfulnessEvaluatorTest.java
                │   ├── CalculationFidelityEvaluatorTest.java
                │   ├── ScenarioCompletenessEvaluatorTest.java
                │   ├── LlmJudgeEvaluatorTest.java
                │   ├── FailureScenarioRunnerTest.java
                │   └── GuardrailRegressionTest.java
                └── v10/
                    ├── ObservabilityTest.java
                    ├── TraceCompletenessTest.java
                    ├── PersistenceTest.java
                    ├── PiiSafetyTest.java
                    ├── FailurePersistenceTest.java
                    ├── MetricsTest.java
                    ├── GoldenTraceTest.java
                    └── ObservabilityFailureTest.java
```

---

# PART VI: RUNNER MODES & COMMANDS

The application provides three complementary ways to run and test both Social Spark and Finance Agent versions:

### Mode 1: Official Google ADK Developer Web UI
- **Class**: [`AdkDevUiApplication.java`](src/main/java/com/google/adk/socialspark/AdkDevUiApplication.java)
- **Port**: 8080 (`http://localhost:8080/dev-ui`)
- **Launcher**:
  ```powershell
  .\run-adk-web.bat
  ```
- **Agent Switcher**: Dropdown in top navigation allows switching between:
  - `social_poster`: Root orchestrator for social media research, drafting, and publishing.
  - `draft_agent`: Isolated skill-based post drafting specialist.
  - `research_agent`: Isolated Google Search research specialist.
  - `finance_agent_v1`: Baseline portfolio analyst for conversational inquiries.
  - `finance_agent_v2`: State-managed portfolio analyst with SQLite holdings persistence.
  - `finance_agent_v3`: Grounded research and valuation analyst with Google Search and math tools.
  - `finance_advisor_v4`: Structured advisor with 6 domain skills, curated project knowledge, and Google Search.

### Mode 2: Interactive Terminal Console
- **Class**: [`AgentConsoleRunner.java`](src/main/java/com/google/adk/socialspark/AgentConsoleRunner.java)
- **Launcher**:
  ```powershell
  .\test-agent.bat
  ```
- **Commands**:
  - Interactive chat: Type any topic (defaults to `social_poster`).
  - Isolated drafting test: `draft_only Write a tip on record patterns`.
  - Finance Agent v1 test: `finance_v1 Explain growth vs value investing`.
  - Finance Agent v2 test: `finance_v2 Can you review my portfolio?`.
  - Finance Agent v3 test: `finance_v3 Search recent earnings results for TCS`.
  - Finance Advisor v4 test: `finance_v4 What is P/E and is Infosys currently considered high?`.
  - Inspect state variables: `state`.
  - One-shot execution: `.\test-agent.bat "finance_v4 What is concentration risk?"`.

### Mode 3: Javalin AG-UI Web Server
- **Class**: [`Application.java`](src/main/java/com/google/adk/socialspark/Application.java)
- **Port**: 8000
- **Launcher**:
  ```powershell
  .\run-backend.bat
  ```
- **Use Case**: Connects to the Next.js CopilotKit frontend at `http://localhost:3000`.

### Mode 4: Dedicated Finance Milestone Consoles
- **Finance Advisor v1 Console**:
  - Class: [`FinanceConsoleV1.java`](src/main/java/com/google/adk/finance/v1/FinanceConsoleV1.java)
  - Launcher: `.\test-finance-v1.bat` (or Gradle: `.\gradlew.bat runFinanceV1 --console=plain -q`)
- **Finance Advisor v2 Console (State & Holdings)**:
  - Class: [`FinanceConsoleV2.java`](src/main/java/com/google/adk/finance/v2/FinanceConsoleV2.java)
  - Launcher: `.\test-finance-v2.bat` (or Gradle: `.\gradlew.bat runFinanceV2 --console=plain -q`)
  - Features: Multi-turn session state inspection (`state`), customer identification (`1001` or `1002`), SQLite queries, and contextual answers.
- **Finance Advisor v3 Console (Grounded Search & Math Tools)**:
  - Class: [`FinanceConsoleV3.java`](src/main/java/com/google/adk/finance/v3/FinanceConsoleV3.java)
  - Launcher: `.\test-finance-v3.bat` (or Gradle: `.\gradlew.bat runFinanceV3 --console=plain -q`)
  - Features: Real-time Google Search grounding for earnings and analyst ratings via `stockmarket_researcher`, deterministic arithmetic via `PortfolioMathTool`, and portfolio database integration.
- **Finance Advisor v4 Console (Skills + Grounding Knowledge)**:
  - Class: [`FinanceConsoleV4.java`](src/main/java/com/google/adk/finance/v4/FinanceConsoleV4.java)
  - Launcher: `.\test-finance-v4.bat` (or Gradle: `.\gradlew.bat runFinanceV4 --console=plain -q`)
  - Features: 6 domain skills in `skills/finance/`, 6 curated grounding documents in `knowledge/`, Google Search grounding via `stockmarket_researcher`, and explicit source transparency.
- **Finance Advisor v5 Console (Yahoo Finance MCP Integration)**:
  - Class: [`FinanceConsoleV5.java`](src/main/java/com/google/adk/finance/v5/FinanceConsoleV5.java)
  - Launcher: `.\test-finance-v5.bat` (or Gradle: `.\gradlew.bat runFinanceV5 --console=plain -q`)
  - Features: Model Context Protocol (MCP) toolset consumption over stdio, automated startup and shutdown of `mcp/yahoo-finance-mcp.jar`, 4 structured tools (`get_stock_info`, `get_stock_actions`, `get_financial_statement`, `get_recommendations`), Google Search (`stockmarket_researcher`), domain skills, grounding knowledge, SQLite portfolio context, and deterministic math.
- **Finance Advisor v6 Console (Specialist Sub-Agents)**:
  - Class: [`FinanceConsoleV6.java`](src/main/java/com/google/adk/finance/v6/FinanceConsoleV6.java)
  - Launcher: `.\test-finance-v6.bat` (or Gradle: `.\gradlew.bat runFinanceV6 --console=plain -q`)
  - Features: Dynamic multi-agent delegation across 5 specialists (`stockmarket_researcher`, `scenario_analyst`, `report_writer`, `fundamental_analysis_agent`, `portfolio_risk_agent`) via `AgentTool`, formatted sub-agent execution logs, and synthesis reporting.
- **Finance Advisor v7 Console (Workflow Orchestration)**:
  - Class: [`FinanceConsoleV7.java`](src/main/java/com/google/adk/finance/v7/FinanceConsoleV7.java)
  - Launcher: `.\test-finance-v7.bat` (or Gradle: `.\gradlew.bat runFinanceV7 --console=plain -q`)
  - Features: 3 deterministic workflow engines: Sequential 5-Stage Pipeline (`run_sequential_research_workflow`), Parallel Fan-Out/In with failure tolerance (`run_parallel_portfolio_research_workflow`), and Iterative Research-Critic Loop with `ExitLoopTool` dynamic stopping (`run_critic_loop_research_workflow`).
- **Finance Advisor v8 Console (Guardrails & Lifecycle Callbacks)**:
  - Class: [`FinanceConsoleV8.java`](src/main/java/com/google/adk/finance/v8/FinanceConsoleV8.java)
  - Launcher: `.\test-finance-v8.bat` (or Gradle: `.\gradlew.bat runFinanceV8 --console=plain -q`)
  - Features: 5-stage lifecycle interception (`BeforeAgent`, `BeforeModel`, `BeforeTool`, `AfterTool`, `AfterModel`), PII detection & masking (`test-pii`), prompt injection halting (`test-injection`), unauthorized trade execution blocking (`test-trade`), empirical fact tracking in `EvidenceStore` (`facts`), numerical hallucination detection (`test-hallucination`), and regulatory disclaimer enforcement.
- **Finance Advisor v9 Console (Evaluation & Failure Testing)**:
  - Class: [`FinanceConsoleV9.java`](src/main/java/com/google/adk/finance/v9/FinanceConsoleV9.java)
  - Launcher: `.\test-finance-v9.bat` (or Gradle: `.\gradlew.bat runFinanceV9 --console=plain -q`)
  - Features: Golden benchmark evaluation (`/cases`, `/eval <caseId>`), live failure simulations (`/fail <category>`), empirical fact tracking in `EvidenceStoreV9` (`/evidence`), deterministic evaluators (Faithfulness, Calculation Fidelity, Scenario Completeness), and qualitative LLM judge.
- **Finance Advisor v10 Console (Observability & Persistence)**:
  - Class: [`FinanceConsoleV10.java`](src/main/java/com/google/adk/finance/v10/FinanceConsoleV10.java)
  - Launcher: `.\test-finance-v10.bat` (or Gradle: `.\gradlew.bat runFinanceV10 --console=plain -q`)
  - Features: Correlation spine (`exec-YYYYMMDD-<uuid8>`), structured event streams (`AGENT_STARTED`, `MODEL_CALL_STARTED`, `TOOL_CALL_STARTED`, `DISCLAIMER_ENFORCED`, `AGENT_COMPLETED`), PII-safe telemetry redaction, human-readable ASCII timeline traces (`trace`), persisted execution archives in JSON and SQLite (`history`, `inspect <id>`), diagnostic queries (`failed`, `guardrails`, `eval-failures`), and golden case evaluation persistence (`eval <caseId>`, `eval-all`).

---

# PART VII: DEVELOPER GUIDELINES & INVARIANTS

### 1. Invariant Rules for Financial Agents
- **Deterministic Math Rule**: Never ask the LLM to calculate portfolio returns, cost basis, yields, or sector allocation weights in text. Always bind and invoke `PortfolioMathTool`.
- **Evidence Citation Rule**: Any assertion explaining an asset price change or market movement must cite either a Google Search snippet URL or an MCP data source.
- **Regulatory Disclaimers**: All generated reports must terminate with a standard decision-support disclaimer clarifying that the output does not constitute certified investment or legal advice.

### 2. State Key Conventions
- **Social Spark**:
  - `pipeline_stage`: `idea` -> `researching` -> `drafting` -> `awaiting_approval` -> `posted`
  - `research_notes`: Output string generated by `research_agent`
  - `current_draft`: Output draft string generated by `draft_agent`
- **Finance Agent**:
  - `customer_id`: Active customer ID loaded into session state (e.g., `1001`)
  - `portfolio_id`: Active portfolio ID linked to customer (e.g., `100001`)
  - `portfolio_holdings`: Formatted markdown summary of holdings for prompt templating
  - `portfolio_holdings_list`: List of individual holding maps for programmatic tools
  - `total_invested`: Aggregated capital invested across all holdings
  - `portfolio_loaded`: Boolean flag indicating state readiness

### 3. Documentation Synchronization Invariant (Mandatory)
Whenever a new agent milestone, tool, database schema, or CLI command is implemented or modified:
1. **Update `AGENTS.md`**: Detail the milestone architecture, package path, core classes, SQLite schemas, seed data, and interaction sequence.
2. **Update `README.md`**: Ensure the repository README reflects the new domain agents, file tree, setup steps, CLI launcher scripts, and test commands.
3. **Relative Path Enforcement**: All markdown documentation, links, and code references MUST strictly use relative file paths (e.g., `src/main/java/com/google/adk/finance/v2/...`). Never use local absolute paths (`file:///...`).

