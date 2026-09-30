# Finance Advisor V5 — Yahoo Finance MCP Integration & Architecture Reference

> **Milestone 5 Objective**: Extend Finance Advisor V4 by integrating the **Model Context Protocol (MCP)**, connecting the Google ADK Java agent to a standalone Java-based **Yahoo Finance MCP Server** over STDIO transport, dynamically discovering structured financial market tools, and orchestrating multi-source intelligence across Yahoo Finance MCP, live Google Search, curated project grounding knowledge, domain skills, and relational customer portfolio state.

---

## 1. V5 Objective & Evolutionary Leap

In **Finance Advisor V4**, the agent combined three distinct intelligence layers:
- **Project Domain Skills** (`skills/finance/`): Local Markdown procedures loaded on demand via `SkillToolset`.
- **Project Grounding Knowledge** (`knowledge/`): Curated, authoritative definitions and valuation frameworks accessed via `read_project_knowledge`.
- **Live Google Search** (`market_researcher`): Real-time web retrieval via `GoogleSearchTool.INSTANCE` for company news and macro developments.
- **Custom Java Tools**: Deterministic arithmetic (`PortfolioMathTool`) and SQLite state management (`LoadCustomerPortfolioTool`).

### The Shift in V5: Consuming MCP Tools
**Finance Advisor V5** introduces the **Model Context Protocol (MCP)**. Instead of requiring developers to write custom Java or ADK function tools for market data (e.g. `getStockPrice()`, `getHistoricalPrices()`, `getFinancialStatements()`), the agent consumes tools exposed by an independent, standards-compliant MCP server.

```
V4:  Domain Skills  +  Project Grounding Knowledge  +  Google Search  +  Custom Java Math & DB
                                     ▼
V5:  Domain Skills  +  Project Grounding Knowledge  +  Google Search  +  Custom Java Math & DB
                     +  YAHOO FINANCE MCP SERVER (Structured Market Data)
```

---

## 2. High-Level System Architecture

```
                                 +---------------------------------------------+
                                 |             User / CLI Console              |
                                 |    (FinanceConsoleV5 / Interactive Shell)   |
                                 +----------------------+----------------------+
                                                        |
                                                        v
                                 +---------------------------------------------+
                                 |           Finance Advisor Agent V5          |
                                 |           (com.google.adk.LlmAgent)         |
                                 |           Model: gemini / gemma             |
                                 +----------------------+----------------------+
                                                        |
        +───────────────────────┬───────────────────────┼───────────────────────┬───────────────────────+
        |                       |                       |                       |                       |
        v                       v                       v                       v                       v
+---------------+       +---------------+       +---------------+       +---------------+       +---------------+
|  SkillToolset |       |    Project    |       |     Agent     |       |   Portfolio   |       |   ADK Java    |
| (Local Skills)|       |   Knowledge   |       |  MarketSearch |       |  Math & DB    |       |  McpToolset   |
| skills/       |       |  knowledge/   |       | (GoogleSearch)|       | (Deterministic|       |  (MCP Client) |
| finance/      |       |  glossary.md  |       | Live News &   |       |  Math & SQLite|       +-------+-------+
+---------------+       +---------------+       +---------------+       +---------------+               |
                                                                                                        | JSON-RPC 2.0
                                                                                                        | over STDIO
                                                                                                        v
                                                                                        +---------------+---------------+
                                                                                        |    Yahoo Finance MCP Server   |
                                                                                        |    (Standalone Java Process)  |
                                                                                        |    mcp/yahoo-finance-mcp.jar  |
                                                                                        +---------------+---------------+
                                                                                                        |
                                                                                                        | REST HTTP
                                                                                                        | (crumb + cookie)
                                                                                                        v
                                                                                        +-------------------------------+
                                                                                        |    Yahoo Finance APIs         |
                                                                                        | query2.finance.yahoo.com      |
                                                                                        +-------------------------------+
```

---

## 3. MCP Protocol & Architectural Mechanics

The **Model Context Protocol (MCP)** is an open standard created to decouple tools, context, and prompts from LLM runtime engines.

### Key MCP Architectural Roles
1. **MCP Server**: A standalone server process exposing a catalog of tools, resources, or prompts via standardized JSON-RPC 2.0 messages. In V5, this is `com.google.adk.mcp.yahoofinance.YahooFinanceMcpServer`.
2. **MCP Client**: A client component embedded within the agent runtime that initiates connections, performs capability negotiation, discovers tools, and handles invocation requests. In V5, this is Google ADK's `com.google.adk.tools.mcp.McpToolset`.
3. **Transport Mechanism**: The communication channel. V5 uses **STDIO (Standard Input/Output)**:
   - The host application spawns the server as a child process: `java -jar mcp/yahoo-finance-mcp.jar`.
   - Communication occurs via line-delimited JSON-RPC messages on `stdin` and `stdout`.
   - **Crucial Rule**: Standard server logs must strictly route to `System.err`. Any arbitrary logging on `System.out` will corrupt the JSON-RPC framing.
4. **Dynamic Tool Discovery**: Upon startup, the client sends a `tools/list` request. The server replies with a list of tools including names, descriptions, and JSON Schema definitions for parameters.
5. **Tool Invocation**: When the LLM decides to call an MCP tool, ADK constructs a `tools/call` JSON-RPC message, sends it over `stdin` to the server, and awaits the `CallToolResult` response containing structured text or JSON.

---

## 4. Java Implementation Reference

The V5 implementation is modular, robust, and cleanly isolated in `com.google.adk.finance.v5` and `com.google.adk.mcp.yahoofinance`.

### 1. `YahooFinanceMcpServer.java` (`com.google.adk.mcp.yahoofinance`)
- **Role**: Standalone MCP Server entry point.
- **Why it exists**: Implements the official `io.modelcontextprotocol.sdk:mcp:1.1.2` server over `StdioServerTransportProvider`.
- **How it connects**: Listens on `System.in`, replies on `System.out`, and redirects Logback and SLF4J logs to `System.err` via `configureLoggingToStderr()`.
- **Tool Registrations**: Registers the 4 tools using `McpServerFeatures.SyncToolSpecification` with JSON schema validation. Supports both `ticker` and `symbol` aliases.

### 2. `YahooFinanceService.java` (`com.google.adk.mcp.yahoofinance`)
- **Role**: Domain service layer executing market queries.
- **Why it exists**: Parses raw Yahoo Finance quote summaries, financial statements, actions, and recommendations into clean, structured Markdown and JSON payloads.

### 3. `YahooFinanceApiClient.java` (`com.google.adk.mcp.yahoofinance`)
- **Role**: HTTP client handling Yahoo Finance session authentication.
- **Why it exists**: Handles automatic cookie retrieval from `fc.yahoo.com` and crumb generation from `query2.finance.yahoo.com/v1/test/getcrumb`, ensuring seamless API access without manual API keys.

### 4. `YahooFinanceMcpClientManager.java` (`com.google.adk.finance.v5`)
- **Role**: Client connection & lifecycle manager.
- **Why it exists**: Encapsulates subprocess launching, readiness verification, tool discovery, and graceful shutdown.
- **How it connects to ADK**:
  - Builds `ServerParameters` configured with the resolved Java runtime and `-jar mcp/yahoo-finance-mcp.jar`.
  - Instantiates `com.google.adk.tools.mcp.McpToolset`.
  - Calls `mcpToolset.getTools(null)` with a 15-second timeout to verify readiness.
  - Implements `AutoCloseable#close()` to terminate the child process when the agent shuts down.

### 5. `FinanceAdvisorAgentV5Factory.java` (`com.google.adk.finance.v5`)
- **Role**: Root agent factory creating `finance_advisor_v5`.
- **Why it exists**: Wires all tools into `com.google.adk.agents.LlmAgent`.
- **Wired Toolsets**:
  - `SkillToolset`: Domain skills from `skills/finance/`.
  - `ProjectKnowledgeTool`: Grounding knowledge from `knowledge/`.
  - `AgentTool(market_researcher)`: Google Search grounding for live company news.
  - `PortfolioMathTool`: Deterministic PnL, allocation weights, and technical indicators.
  - `LoadCustomerPortfolioTool`: Customer portfolio state ingestion from SQLite.
  - `McpToolset`: Yahoo Finance MCP tools (`get_stock_info`, `get_stock_actions`, `get_financial_statement`, `get_recommendations`).

### 6. `FinanceConsoleV5.java` (`com.google.adk.finance.v5`)
- **Role**: Dedicated interactive terminal console.
- **Why it exists**: Provides interactive exploration, one-shot argument queries, command shortcuts (`mcp`, `tools`, `skills`, `knowledge`, `state`), real-time tool execution tracing, and JVM shutdown hook registration.

---

## 5. MCP Server Lifecycle & Process Management

```
   Application Start (FinanceConsoleV5)
                 │
                 ▼
 1. Check MCP Jar (`mcp/yahoo-finance-mcp.jar`)
                 │
                 ▼
 2. Spawn Subprocess (`java -jar ...`) via ServerParameters
                 │
                 ▼
 3. MCP Handshake:
    Client -> `initialize` (Protocol: 2024-11-05)
    Server -> `initialize` result (ServerCapabilities)
    Client -> `notifications/initialized`
                 │
                 ▼
 4. Readiness & Tool Discovery:
    Client -> `tools/list`
    Server -> returns 4 tools (`get_stock_info`, `get_stock_actions`, etc.)
    Manager verifies all 4 tools discovered (Timeout: 15s)
                 │
                 ▼
 5. Create Finance Advisor V5 Agent
                 │
                 ▼
 6. Multi-Turn Session / Queries (MCP tool calls executed on demand)
                 │
                 ▼
 7. Shutdown (Exit command or JVM termination)
    Runtime shutdown hook calls `mcpClientManager.close()`
    `mcpToolset.close()` terminates stdio transport and child process
                 │
                 ▼
       Clean Application Exit (No Orphaned Processes)
```

---

## 6. Discovered Yahoo Finance MCP Tools

| Tool Name | Purpose | Required Inputs | Optional Inputs | Sample Output |
|---|---|---|---|---|
| `get_stock_info` | Comprehensive stock quote, company profile, valuation multiples, margins, and 52-week ranges | `ticker` (or `symbol`) (String, e.g. `'INFY'`, `'AAPL'`) | None | JSON object with `regularMarketPrice`, `trailingPE`, `forwardPE`, `marketCap`, `beta`, `longBusinessSummary` |
| `get_stock_actions` | Historical dividend payouts and stock splits | `ticker` (or `symbol`) (String, e.g. `'AAPL'`) | None | JSON array of `{Date, Dividends, Stock Splits}` |
| `get_financial_statement` | Balance sheets, income statements, and cash flow statements | `financial_type` (Enum: `income_stmt`, `quarterly_income_stmt`, `balance_sheet`, `quarterly_balance_sheet`, `cashflow`, `quarterly_cashflow`) | `ticker` (or `symbol`) | Formatted statement items with dates and numerical figures |
| `get_recommendations` | Wall Street analyst consensus ratings, price targets, and upgrades/downgrades | `recommendation_type` (Enum: `recommendations`, `upgrades_downgrades`) | `ticker` (or `symbol`), `months_back` (int, default 12) | Consensus breakdown (`strongBuy`, `buy`, `hold`, `sell`) or upgrade/downgrade history |

---

## 7. Google Search vs. Yahoo Finance MCP Decision Matrix

| Analytical Requirement | Google Search (`market_researcher`) | Yahoo Finance MCP | Rationale |
|---|:---:|:---:|---|
| **Current Stock Price** | Possible (unstructured) | **Preferred** (`get_stock_info`) | MCP returns exact, structured numerical quotes without web scraping artifacts. |
| **P/E, Beta, Market Cap** | Possible (inconsistent) | **Preferred** (`get_stock_info`) | Deterministic valuation metrics directly from primary market feeds. |
| **Dividend History & Splits** | Not ideal | **Preferred** (`get_stock_actions`) | Structured historical corporate action tables. |
| **Balance Sheet / Income Stmt** | Search snippets only | **Preferred** (`get_financial_statement`) | Complete financial statement line items. |
| **Wall Street Ratings / Targets** | News summaries | **Preferred** (`get_recommendations`) | Precise consensus breakdown and price target distributions. |
| **Breaking News & Catalysts** | **Preferred** (`market_researcher`) | No | Real-time news retrieval, press releases, leadership changes. |
| **Earnings Call Reactions** | **Preferred** (`market_researcher`) | No | Contextual qualitative analyst commentary and transcript sentiment. |
| **Macro / Regulatory Events** | **Preferred** (`market_researcher`) | No | Central bank interest rate decisions, SEC regulatory changes, geopolitical events. |
| **Portfolio Math (PnL, SMA)** | No | No (Custom Java `PortfolioMathTool`) | Arithmetic must never be delegated to search or LLM approximation. |
| **Customer Holdings Data** | No | No (Custom Java `LoadCustomerPortfolioTool`)| Relational database queries for private user account holdings. |

---

## 8. Evolutionary Comparison: V4 vs. V5

| Capability | Finance Advisor V4 | Finance Advisor V5 |
|---|:---:|:---:|
| **Domain Skills (`skills/finance/`)** | Yes | Yes (Preserved read-only) |
| **Project Grounding Knowledge (`knowledge/`)** | Yes | Yes (Preserved read-only) |
| **Google Search Grounding (`market_researcher`)** | Yes | Yes (Preserved) |
| **Relational Customer State (`LoadCustomerPortfolioTool`)** | Yes | Yes (Preserved) |
| **Deterministic Math (`PortfolioMathTool`)** | Yes | Yes (Preserved) |
| **Model Context Protocol (MCP) Integration** | No | **Yes (ADK `McpToolset` + STDIO)** |
| **Yahoo Finance MCP Server** | No | **Yes (Standalone Java Process)** |
| **Dynamic Tool Discovery** | No (Static Java Tools) | **Yes (`get_stock_info`, `get_stock_actions`, etc.)** |
| **Structured Market Data Retrieval** | Web Search snippets | **Direct Institutional MCP Data** |
| **Lifecycle & Process Management** | In-process JVM only | **Automatic Subprocess Startup & Graceful Shutdown** |

---

## 9. What V5 Deliberately Does NOT Contain

To maintain clean architectural boundaries and isolate the MCP learning milestone, V5 deliberately does not implement:
1. **Multi-Agent Specialist Hierarchies**: Division of labor between Researcher, Scenario Analyst, and Report Writer sub-agents (introduced in V6).
2. **Sequential & Parallel Pipelines**: RxJava fan-out / fan-in pipelines (introduced in V7).
3. **Execution Interceptors & Guardrails**: PII scrubbing, disclaimer injection, and hallucination filters via callbacks (introduced in V8).
4. **Automated LLM-as-a-Judge Evaluation**: Benchmark test suites scoring groundedness and fidelity (introduced in V9).
5. **OpenTelemetry & Observability Tracing**: Distributed tracing and token accounting (introduced in V10).
6. **Trade Execution & Brokerage**: Automated trade submission (requires human-in-the-loop approval gates).

---

## 10. What I Learned from V5 (Key Takeaways)

1. **What MCP Is**: An open standard enabling AI agents to connect to external tool providers uniformly, regardless of language or hosting environment.
2. **MCP Server vs. Client**: The server hosts tools and logic; the client (ADK) discovers tool schemas and manages invocations.
3. **Transport Cleanliness on STDIO**: All stdout output from an MCP server must be strictly valid JSON-RPC frames. All logging must be redirected to `stderr`.
4. **The MCP Handshake**: `initialize` -> `ServerCapabilities` -> `notifications/initialized` -> `tools/list` -> `tools/call`.
5. **Dynamic Tool Discovery**: ADK queries the MCP server at runtime, transforming MCP tool schemas into agent-callable function declarations without compile-time coupling.
6. **MCP vs. Custom ADK Tools**: MCP enables tool reuse across different AI frameworks (Claude Desktop, Google ADK, Cursor, Open WebUI) without rewriting tool code.
7. **MCP vs. Google Search Grounding**: MCP provides structured, deterministic factual metrics (quotes, ratios, balance sheets); Google Search provides real-time narrative events and qualitative commentary.
8. **Multi-Source Intelligence Triangulation**: Grounded decisions require combining structured data (MCP), current events (Search), domain expertise (Skills), and institutional principles (Knowledge).
9. **Process Lifecycle Management**: Automated subprocess startup must always be paired with readiness verification and JVM shutdown hooks to guarantee that child processes are never orphaned.
10. **Graceful Degradation**: If an external data source or MCP server is unreachable, the agent must fail with clear, actionable diagnostics rather than silent errors.
