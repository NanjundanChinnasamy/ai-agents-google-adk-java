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
| **1** | **Finance Agent v1** | `LlmAgent`, `AppConfig.createModel()`, Instruction Engineering, `InMemoryRunner`, `SessionService` | Conversational portfolio analyst with basic Q&A | `com.google.adk.finance.v1` |
| **1** | **Finance Agent v2** | `Context`, State variables, `{placeholder?}` prompt templating, `stateDelta` inspection | Persistent portfolio state memory & multi-turn drift awareness | `com.google.adk.finance.v2` |
| **2** | **Finance Agent v3** | Custom `BaseTool`, `GoogleSearchTool.INSTANCE`, Tool schema validation (`FunctionDeclaration`) | Deterministic financial math calculations + live market news retrieval | `com.google.adk.finance.v3` |
| **2** | **Finance Agent v4** | `LocalSkillSource`, `SkillToolset`, Markdown domain guidelines | Audit rules, scenario modeling templates & report formatting skills | `com.google.adk.finance.v4` |
| **3** | **Finance Agent v5** | `McpToolset`, `ServerParameters`, FastMCP stdio/HTTP | Real-time market quotes, SEC fundamentals & macro indicators via MCP | `com.google.adk.finance.v5` |
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
  - [`LoadCustomerPortfolioTool.java`](src/main/java/com/google/adk/finance/v2/LoadCustomerPortfolioTool.java): Custom `BaseTool` querying SQLite by customer ID and injecting holdings into session state via `toolContext.state()`.
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
  - `PortfolioMathTool.java`: Extends `com.google.adk.tools.BaseTool` for deterministic arithmetic.
  - `FinanceAgentV3Factory.java`: Combines math tools and `GoogleSearchTool.INSTANCE`.
- **ADK Classes Learned**:
  - `com.google.adk.tools.BaseTool`
  - `com.google.genai.types.FunctionDeclaration`
  - `com.google.genai.types.Schema`
  - `com.google.adk.tools.GoogleSearchTool`
- **Architecture**:
  ```
                        +───────────────────────────+
                        |      FinanceAgentV3       |
                        +─────────────┬─────────────+
                                      │
                 ┌────────────────────┴────────────────────┐
                 ▼                                         ▼
  +─────────────────────────────+           +─────────────────────────────+
  |      PortfolioMathTool      |           |    GoogleSearchTool.INSTANCE|
  | Deterministic Calculations: |           | Real-time Web Grounding:    |
  | - Weightings & Sector Drift |           | - "NVDA Q3 earnings beat"   |
  | - Cost basis & Unrealized PnL|          | - "10Y Treasury yield shift"|
  | - Concentration risk ratio  |           | - Output: Search snippets   |
  +─────────────────────────────+           +─────────────────────────────+
  ```
- **Rule**: Never permit the LLM to perform arithmetic on financial figures. Offload all calculations to `PortfolioMathTool`.

---

### Week 2 — Version 4: Dynamic Skills & Structured Domain Knowledge (`finance.v4`)
- **Package**: `com.google.adk.finance.v4`
- **New Skill Directories**:
  - `skills/finance/portfolio-audit/SKILL.md`: Guidelines for assessing portfolio concentration and diversification limits.
  - `skills/finance/scenario-stress/SKILL.md`: Methodologies for modeling rate shocks and recessionary sell-offs.
  - `skills/finance/report-standards/SKILL.md`: Standard structural format for executive decision-support documents.
- **ADK Classes Learned**:
  - `com.google.adk.skills.LocalSkillSource`
  - `com.google.adk.tools.skills.SkillToolset`
- **Takeaway**: Externalize financial domain knowledge into markdown skills loaded on demand at runtime.

---

### Week 3 — Version 5: Market Context Protocol (MCP) Integration (`finance.v5`)
- **Package**: `com.google.adk.finance.v5`
- **MCP Server**: `mcp/finance_market_server.py` (FastMCP stdio server exposing `get_quote`, `get_fundamentals`, `get_macro_rates`).
- **Core Files**:
  - `MarketMcpToolsetFactory.java`: Builds `McpToolset` configured for FastMCP execution.
  - `FinanceAgentV5Factory.java`: Integrates live market data tools into the agent.
- **ADK Classes Learned**:
  - `com.google.adk.tools.mcp.McpToolset`
  - `io.modelcontextprotocol.client.transport.ServerParameters`
- **Takeaway**: Connect the agent system to standard financial data protocols over subprocess stdio or HTTP SSE.

---

### Week 3 — Version 6: Specialist Multi-Agent System (`finance.v6`)
- **Package**: `com.google.adk.finance.v6`
- **Specialist Roles**:
  1. `market_researcher`: Queries Google Search and Market MCP tools to identify why asset prices changed.
  2. `scenario_analyst`: Models bull, bear, and baseline macro scenarios based on portfolio weights.
  3. `report_writer`: Synthesizes research and scenario findings into an executive decision-support report.
  4. `portfolio_director` (Root): Orchestrates the workflow, delegates to specialists, and conducts user alignment.
- **ADK Classes Learned**:
  - `com.google.adk.tools.AgentTool`
  - Multi-agent state isolation and delegation hierarchies.

---

### Week 4 — Version 7: Advanced Orchestration Workflows (`finance.v7`)
- **Package**: `com.google.adk.finance.v7`
- **Workflow Patterns**:
  - **Sequential Pipeline**: Ingest Portfolio -> Research Attribution -> Simulate Scenarios -> Generate Report.
  - **Parallel Fan-Out/Fan-In**: Concurrent research across multiple portfolio holdings using RxJava `Flowable.merge` or Reactor `Flux`.
  - **Iterative Feedback Loop**: A `compliance_critic` agent reviews the initial draft and requests revisions until all assertions are backed by cited evidence.
- **Architecture**:
  ```
  [User Request] ──> [Portfolio Director]
                             │
            ┌────────────────┴────────────────┐
            ▼ (Parallel Fan-Out)              ▼
    [Research Asset A]                 [Research Asset B]
            │                                 │
            └────────────────┬────────────────┘
                             ▼ (Fan-In)
                    [Scenario Analyst]
                             │
                             ▼
                     [Report Writer]
                             │
                             ▼
                    [Compliance Critic] ──(Pass)──> [Final Report Output]
                             │
                       (Needs Cites)
                             │
                             └───> [Revise Draft Loop]
  ```

---

### Week 4 — Version 8: Guardrails, Safety & Callbacks (`finance.v8`)
- **Package**: `com.google.adk.finance.v8`
- **Guards & Callbacks**:
  - `BeforeAgentCallback`: PII sanitization (masks account numbers, user names).
  - `BeforeToolCallback`: Validates ticker symbols and execution limits.
  - `AfterModelCallback`: Enforces mandatory financial disclaimer ("Not financial or investment advice").
  - `HallucinationDetector`: Cross-checks cited tickers in the final report against the original portfolio state.

---

### Week 5 — Version 9: Automated Evaluation & Red-Teaming (`finance.v9`)
- **Package**: `src/test/java/com/google/adk/finance/eval/`
- **Evaluation Criteria**:
  - **Faithfulness / Groundedness**: Every statement must map to Google Search or MCP evidence.
  - **Calculation Fidelity**: PnL and weightings in report must match `PortfolioMathTool` output.
  - **Scenario Completeness**: Report must include baseline, upside, and stress-test projections.
- **Testing Tools**: JUnit 5, AssertJ, and an LLM-as-a-Judge evaluation harness.

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
│   └── finance/                               # [NEW] Finance Agent Skills
│       ├── portfolio-audit/
│       │   └── SKILL.md                       # Diversification, Sharpe, and concentration thresholds
│       ├── scenario-stress/
│       │   └── SKILL.md                       # Macroeconomic scenario rules (rate shocks, recessions)
│       └── report-standards/
│           └── SKILL.md                       # Decision-support report formatting guidelines
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
    │   │   └── finance/                           # DOMAIN 2: Finance Portfolio Decision Agent
    │   │       │                                  # Clean package root: com.google.adk.finance.*
    │   │       ├── common/                        # Shared finance domain models & math tools
    │   │       │   ├── PortfolioModels.java       # Records: Holding, Allocation, Variance, RiskMetrics
    │   │       │   └── PortfolioMathTool.java     # Pure Java BaseTool for exact financial calculations
    │   │       │
    │   │       ├── v1/                            # Week 1: Foundational Agent & Session
    │   │       │   ├── FinanceAgentV1Factory.java
    │   │       │   └── FinanceConsoleV1.java
    │   │       │
    │   │       ├── v2/                            # Week 1: Context & Holdings State Management
    │   │       │   └── FinanceAgentV2Factory.java
    │   │       │
    │   │       ├── v3/                            # Week 2: Java Tools + Grounded Google Search
    │   │       │   └── FinanceAgentV3Factory.java
    │   │       │
    │   │       ├── v4/                            # Week 2: Dynamic Skills & Domain Playbooks
    │   │       │   └── FinanceAgentV4Factory.java
    │   │       │
    │   │       ├── v5/                            # Week 3: Market Context Protocol (MCP) Integration
    │   │       │   ├── MarketMcpToolsetFactory.java
    │   │       │   └── FinanceAgentV5Factory.java
    │   │       │
    │   │       ├── v6/                            # Week 3: Specialist Sub-Agents (Research, Scenarios, Writer)
    │   │       │   └── FinanceDirectorV6Factory.java
    │   │       │
    │   │       ├── v7/                            # Week 4: Advanced Workflows (Fan-Out/Fan-In, Critic Loop)
    │   │       │   └── FinanceWorkflowV7.java
    │   │       │
    │   │       ├── v8/                            # Week 4: Guardrails, Safety & Callbacks
    │   │       │   └── FinanceGuardrailsV8.java
    │   │       │
    │   │       ├── v10/                           # Week 5: Observability & Enterprise Persistence
    │   │       │   ├── FinanceTelemetryV10.java
    │   │       │   └── db/FinanceReportRepository.java
    │   │       │
    │   │       └── prod/                          # Week 6: Production Release
    │   │           └── FinanceProdServer.java
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
            └── finance/                       # [NEW] Finance Agent Test Suite
                ├── PortfolioMathToolTest.java # Deterministic calculation validation
                ├── v1/FinanceV1IntegrationTest.java
                ├── v3/FinanceV3ToolTest.java
                └── eval/FinanceReportEvalTest.java # LLM-as-a-judge fidelity scoring
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
  - Inspect state variables: `state`.
  - One-shot execution: `.\test-agent.bat "finance_v2 Review my portfolio"`.

### Mode 3: Javalin AG-UI Web Server
- **Class**: [`Application.java`](src/main/java/com/google/adk/socialspark/Application.java)
- **Port**: 8000
- **Launcher**:
  ```powershell
  .\run-backend.bat
  ```
- **Use Case**: Connects to the Next.js CopilotKit frontend at `http://localhost:3000`.

### Mode 4: Dedicated Finance Milestone Consoles
- **Finance Agent v1 Console**:
  - Class: [`FinanceConsoleV1.java`](src/main/java/com/google/adk/finance/v1/FinanceConsoleV1.java)
  - Launcher: `.\test-finance-v1.bat` (or Gradle: `.\gradlew.bat runFinanceV1 --console=plain -q`)
- **Finance Agent v2 Console (State & Holdings)**:
  - Class: [`FinanceConsoleV2.java`](src/main/java/com/google/adk/finance/v2/FinanceConsoleV2.java)
  - Launcher: `.\test-finance-v2.bat` (or Gradle: `.\gradlew.bat runFinanceV2 --console=plain -q`)
  - Features: Multi-turn session state inspection (`state`), customer identification (`1001` or `1002`), SQLite queries, and contextual answers.

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

