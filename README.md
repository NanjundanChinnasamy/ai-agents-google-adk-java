# Dual-Domain Multi-Agent AI System: Social Spark & Finance Portfolio (Google ADK Java)

A production-grade, multi-domain AI backend built in **Java 21** using the official **[Google Agent Development Kit (ADK) for Java](https://adk.dev/)** (`com.google.adk:google-adk:1.4.0`, `com.google.adk:google-adk-a2a:1.4.0`, and `com.google.adk:google-adk-dev:1.4.0`).

The application accommodates two complementary autonomous multi-agent systems sharing a common core foundation of hybrid model execution, dynamic skills, Model Context Protocol (MCP) toolsets, embedded SQLite persistence, and reactive Server-Sent Events (SSE) streaming via the **AG-UI Protocol**:
1. **Domain 1: Social Spark**: Social media post generator, real-time web researcher, multimodal image creator, human approval gate, and publisher (LinkedIn & Buffer MCPs) integrated with `@ag-ui/client` and CopilotKit Next.js frontend.
2. **Domain 2: Finance Portfolio Decision-Support Agent**: A progressive 6-week curriculum (`v1` through `v10` and `prod`) answering *"Research my portfolio, explain what changed, identify evidence, compare scenarios, and produce a decision-support report."*

> [!IMPORTANT]
> **Documentation Constraint — Relative Paths Only**:
> All file, class, script, and directory references across this repository's documentation (including `README.md`, `AGENTS.md`, and `skills/`) must use **relative file paths** (e.g., `src/main/java/...`, `skills/...`, `mcp/...`). Absolute local machine paths (such as `file:///c:/...` or `/home/...`) are strictly prohibited to preserve documentation portability across operating systems and developer environments.

---

## Dual-Domain Architecture Overview

```
                                      +---------------------------------------------+
                                      |            Frontend Interfaces              |
                                      | - Next.js CopilotKit UI (Port 3000)        |
                                      | - Google ADK Official Dev UI (Port 8080)   |
                                      | - Direct CLI Consoles (Interactive Terminal)|
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

## Key Features Across Domains

### Domain 1: Social Spark
- **Google ADK Java Native Core**: Built directly against official Google ADK Java packages (`com.google.adk:google-adk:1.4.0`, `google-adk-a2a:1.4.0`).
- **Specialist Multi-Agent System**:
  - `social_poster`: Root orchestrator coordinating research, drafting, user approvals, and publishing.
  - `research_agent`: Isolated web search using `GoogleSearchTool.INSTANCE` (strictly isolated as required by Gemini Agent Platform).
  - `draft_agent`: Loads Agent Skills (`brand-voice`, `platform-style`, `post-formatter`, `poster-style`) via `SkillToolset` and `LocalSkillSource`.
  - `memory_agent`: Remote Agent-to-Agent (A2A) integration for querying user post history and brand voice.
- **Multimodal Image Generation**: Directly calls `gemini-3.1-flash-image` with automated fallbacks, image extraction, and local staging in `gallery/`.
- **Model Context Protocol (MCP)**:
  - **LinkedIn MCP**: Subprocess execution with auto-detection of `uv`/python with `fastmcp` and `httpx`.
  - **Buffer MCP**: Remote Streamable HTTP MCP connection to `https://mcp.buffer.com/mcp` with automated review delay scheduling.
  - **Human Confirmation Gate**: All publishing tools require explicit user approval.
- **Embedded Durable SQLite**: WAL-mode SQLite database `social_spark.db` preserving post history.
- **AG-UI Protocol Engine**: Full Server-Sent Events (SSE) streaming bridging ADK `Flowable<Event>` to `@ag-ui/client` and CopilotKit.

### Domain 2: Finance Portfolio Decision-Support Agent
- **6-Week Versioned Curriculum**: Strict milestone progression from foundational agent (`v1`) to production cloud deployment (`prod`).
- **Week 1 — Version 1 (`finance.v1`)**:
  - Foundational conversational financial analyst with domain instruction engineering (asset classes, valuation multiples, macro drivers, risk/return ratios).
  - `LlmAgent`, `AppConfig.createModel()`, `InMemoryRunner`, and session lifecycle management.
- **Week 1 — Version 2 (`finance.v2`)**:
  - **SQLite Relational Persistence**: Composite primary key `customer (customer_id, portfolio_id)` and `portfolio_holding` table with auto-seeding.
  - **Custom Tool Integration**: `LoadCustomerPortfolioTool` (`load_customer_portfolio`) querying SQLite and injecting holdings into active session state via `toolContext.state()`.
  - **Dynamic Prompt Templating**: Embeds `{customer_id?}`, `{portfolio_id?}`, and `{portfolio_holdings?}` placeholders into agent instructions.
  - **Multi-Turn State Preservation**: Asks for Customer ID once, retrieves portfolio from SQLite, and retains state across turns to answer inquiries directly without re-querying.
  - **Regulatory Compliance Guardrail**: Automatically appends mandatory institutional disclaimers.

---

## Project Structure

```
ai-agents-google-adk-java/
├── build.gradle.kts           # Gradle configuration with Java 21 toolchain
├── pom.xml                    # Maven configuration for alternate builds
├── settings.gradle.kts        # Project settings
├── gradlew.bat / gradlew      # Gradle wrapper scripts
├── run-backend.bat / .sh      # Javalin AG-UI server launcher (Port 8000)
├── run-adk-web.bat / .sh      # Official Google ADK Web Dev UI launcher (Port 8080)
├── test-agent.bat / .sh       # Direct CLI agent test console (Social Spark & Finance)
├── test-finance-v1.bat / .sh  # Dedicated CLI runner for Finance Agent v1
├── test-finance-v2.bat / .sh  # Dedicated CLI runner for Finance Agent v2
├── .env.example               # Environment variables template
├── skills/                    # Agent Skills (brand-voice, platform-style, etc.)
├── mcp/                       # Local MCP servers (linkedin_server.py)
└── src/
    ├── main/
    │   ├── java/com/google/adk/
    │   │   ├── socialspark/                           # DOMAIN 1: Social Spark
    │   │   │   ├── Application.java                   # Main entrypoint & Javalin web server (Port 8000)
    │   │   │   ├── AdkDevUiApplication.java           # Official Google ADK Web Dev UI (Port 8080)
    │   │   │   ├── AgentConsoleRunner.java            # Interactive CLI runner & one-shot agent tester
    │   │   │   ├── config/AppConfig.java              # Environment & model configuration
    │   │   │   ├── db/
    │   │   │   │   ├── PostRecord.java                # Post DTO
    │   │   │   │   └── PostRepository.java            # SQLite repository (WAL mode)
    │   │   │   ├── tools/
    │   │   │   │   ├── CheckTextLengthTool.java       # Character counter tool
    │   │   │   │   ├── GenerateImageTool.java         # Gemini multimodal image generation
    │   │   │   │   ├── UploadImageTool.java           # GCS signed/public URL uploader
    │   │   │   │   └── UseProvidedImageUrlTool.java    # Image URL validator
    │   │   │   ├── agents/
    │   │   │   │   ├── ResearchAgentFactory.java      # Isolated search specialist
    │   │   │   │   ├── DraftAgentFactory.java         # Skill-based drafting specialist
    │   │   │   │   ├── MemoryAgentFactory.java        # Remote A2A memory agent
    │   │   │   │   ├── PostingToolsetsFactory.java    # LinkedIn & Buffer MCP toolsets
    │   │   │   │   └── SocialPosterAgentFactory.java  # Orchestrator & callbacks
    │   │   │   └── agui/
    │   │   │       ├── AgUiModels.java                # AG-UI protocol DTOs
    │   │   │       ├── SessionStore.java              # Thread session storage
    │   │   │       └── AgUiEventTranslator.java       # Reactive ADK Event -> SSE translator
    │   │   │
    │   │   └── finance/                               # DOMAIN 2: Finance Portfolio Decision-Support
    │   │       ├── v1/                                # Week 1: Foundational Agent & Session
    │   │       │   ├── FinanceAgentV1Factory.java     # LlmAgent factory & instruction engineering
    │   │       │   └── FinanceConsoleV1.java          # Dedicated interactive CLI console
    │   │       │
    │   │       └── v2/                                # Week 1: Context & Holdings State Management
    │   │           ├── PortfolioModels.java           # Customer & holding domain records
    │   │           ├── CustomerPortfolioRepository.java # SQLite DAO with WAL mode & auto-seed
    │   │           ├── LoadCustomerPortfolioTool.java # BaseTool injecting state via ToolContext
    │   │           ├── FinanceAgentV2Factory.java     # Agent factory with dynamic prompt templates
    │   │           └── FinanceConsoleV2.java          # Multi-turn stateful CLI console
    │   │
    │   └── resources/logback.xml                      # Logging configuration
    │
    └── test/
        └── java/com/google/adk/
            ├── socialspark/                           # Social Spark test suite
            │   ├── SocialSparkIntegrationTest.java
            │   ├── PostRepositoryTest.java
            │   └── CheckTextLengthToolTest.java
            └── finance/                               # Finance Agent test suite
                ├── v1/
                │   └── FinanceV1IntegrationTest.java
                └── v2/
                    ├── CustomerPortfolioRepositoryTest.java
                    ├── LoadCustomerPortfolioToolTest.java
                    └── FinanceV2IntegrationTest.java
```

---

## Prerequisites

- **Java JDK 21+** (e.g. Eclipse Temurin or Oracle JDK 21)
- **uv** (recommended for LinkedIn MCP execution) or **Python 3.10+**
- **Google GenAI API Key** or **Vertex AI Project**:
  - `GOOGLE_API_KEY` / `GEMINI_API_KEY`, OR
  - `GOOGLE_GENAI_USE_VERTEXAI=TRUE` with `GOOGLE_CLOUD_PROJECT` (e.g. in `us-central1`)

---

## Configuration (`.env`)

Copy `.env.example` to `.env` and configure your settings:

```ini
# --- Option A: Google AI Studio (API Key) ---
GEMINI_API_KEY=your-api-key-here

# --- Option B: Google Cloud Vertex AI ---
GOOGLE_GENAI_USE_VERTEXAI=TRUE
GOOGLE_CLOUD_PROJECT=your-gcp-project-id
GOOGLE_CLOUD_LOCATION=us-central1

# Models (ADK GoogleSearchTool requires gemini-2.x or gemini-3.x)
RESEARCH_MODEL=gemini-2.5-flash
DRAFT_MODEL=gemini-2.5-flash
ORCHESTRATOR_MODEL=gemini-2.5-flash
IMAGE_MODEL_ID=gemini-3.1-flash-image

# Server & Execution
PORT=8000
DRY_RUN=true
BACKEND_PUBLIC_ORIGIN=http://localhost:8000
```

---

## Testing & Execution Methods

You can test and run the agents across both domains through several flexible interfaces:

---

### Method 1: Official Google ADK Developer Web UI (Recommended Visual Tool)

The project includes the official **Google ADK Developer Web UI** powered by `com.google.adk:google-adk-dev`. It provides an interactive DAG visualization of the agent hierarchy, real-time token streaming, tool call inspection, and session management.

#### 1. Start the Dev UI Server
```bash
# Windows
.\run-adk-web.bat

# macOS / Linux
./run-adk-web.sh

# Or directly with Gradle:
.\gradlew.bat runDevUi --console=plain
```

#### 2. Open in Your Browser
Navigate directly to:
👉 **`http://localhost:8080/dev-ui`**

#### 3. Features & How to Use
- **Agent Switcher (Top Header)**:
  - `social_poster`: Test the root multi-agent orchestrator coordinating research, drafting, and MCP publishing.
  - `draft_agent`: Test the copywriting specialist in isolation with its Agent Skills.
  - `research_agent`: Test the Google Search grounding specialist.
  - `finance_agent_v1`: Test the foundational financial analyst for asset allocations, valuation metrics, and macro drivers.
  - `finance_agent_v2`: Test the state-managed portfolio analyst with SQLite holdings persistence and `load_customer_portfolio` tool.
- **Visual DAG Graph**: Displays the live execution tree connecting the orchestrator to its sub-agents (`research_agent`, `draft_agent`) and registered tools.
- **Tool Inspection**: Click on any executed tool chip (e.g. `google_search`, `read_skill_content`, `load_customer_portfolio`) in the chat to view the exact function call parameters, input arguments, and model output in the left inspector drawer.
- **Session Management**: Create new sessions via `+ New Session` or inspect past event steps sequentially (`Event 1 of N`, `Request`, `Response`).

---

### Method 2: Interactive Terminal Console (Fast Direct CLI Testing)

Test the agents directly from your terminal without launching any web browser or frontend.

#### 1. Start the Interactive CLI
```bash
# Windows
.\test-agent.bat

# macOS / Linux
./test-agent.sh

# Or directly with Gradle:
.\gradlew.bat runAgent --console=plain -q
```

#### 2. Interactive Console Commands
Once inside the prompt (`User > `):
- **Chat directly with the Root Agent (`social_poster`)**:
  ```text
  User > Write a post about Java 21 Virtual Threads
  ```
  Watch the orchestrator delegate to `research_agent`, invoke Google Search, transition stages (`[Stage changed -> RESEARCH]`, `[Stage changed -> DRAFT]`), and stream the finalized post.
- **Test `draft_agent` in isolation**:
  ```text
  User > draft_only Write a tweet about modern Java features
  ```
  Bypasses research and runs only the drafting specialist with local skill loading.
- **Test `finance_agent_v1` directly**:
  ```text
  User > finance_v1 Explain the difference between growth and value investing
  ```
  Runs the conversational financial analyst answering asset allocation, macro, and valuation inquiries.
- **Test `finance_agent_v2` directly**:
  ```text
  User > finance_v2 Can you review my portfolio?
  ```
  Runs the state-managed portfolio analyst, prompts for customer ID, loads SQLite holdings, and maintains session context.
- **Inspect Session State**:
  ```text
  User > state
  ```
  Prints current in-memory session variables (such as `pipeline_stage`, `post_idea`, `customer_id`, `portfolio_id`).
- **Exit**:
  ```text
  User > exit
  ```

#### 3. One-Shot Command Execution
You can also run one-off prompts directly without entering the interactive loop:
```bash
# Test draft agent directly
.\test-agent.bat "draft_only Write a one-liner about microservices"

# Test full orchestrator
.\test-agent.bat "Write an announcement about Java 21 release"

# Test finance agent v1
.\test-agent.bat "finance_v1 What is portfolio drift?"

# Test finance agent v2
.\test-agent.bat "finance_v2 Review my portfolio"
```

---

### Method 3: Full AG-UI Server & CopilotKit Frontend

Run the full production setup with the Javalin backend server and Next.js CopilotKit UI.

#### 1. Start the Java Backend (Port 8000)
```bash
# Windows
.\run-backend.bat

# macOS / Linux
./run-backend.sh

# Or directly with Gradle:
.\gradlew.bat run
```

Endpoints available on `http://localhost:8000`:
- `GET /healthz` - Health probe (`{"status": "ok"}`)
- `GET /api/posts` - Historical post records for `PostGallery.tsx`
- `POST /agents/state` - AG-UI session state rehydration
- `POST /api/adk` - AG-UI Server-Sent Events (SSE) execution stream
- `GET /outputs/{filename}` - Gallery static image preview handler

#### 2. Start the Next.js Frontend (Port 3000)
In a separate terminal, start the Next.js frontend (e.g. from `ai-devcamp-labs/frontend`):
```bash
cd <LOCAL-SYSTEM-PATH>\ai-devcamp-labs\frontend
npm run dev
```
Open **`http://localhost:3000`** to interact with the full web application.

### Method 4: Finance Portfolio Decision-Support Agent CLI

The multi-domain portfolio agent is organized into versioned milestones under `src/main/java/com/google/adk/finance/`.

#### Week 1 — Version 1: Foundational Analyst (`finance.v1`)
Run the standalone conversational analyst for market Q&A, asset allocations, and financial frameworks:
```bash
# Windows
.\test-finance-v1.bat

# macOS / Linux
./test-finance-v1.sh

# One-shot query:
.\test-finance-v1.bat "Explain the difference between growth and value investing strategies."
```

#### Week 1 — Version 2: Context & Holdings State Management (`finance.v2`)
Run the stateful portfolio analyst with SQLite persistence, custom tool execution (`load_customer_portfolio`), and multi-turn state preservation:
```bash
# Windows
.\test-finance-v2.bat

# macOS / Linux
./test-finance-v2.sh

# Or directly with Gradle:
.\gradlew.bat runFinanceV2 --console=plain -q
```

**Seed Test Portfolios Available in SQLite (`finance_portfolio.db`):**
- **Customer 1001** (Portfolio `100001`): Reliance (2 shares @ 1000 INR on 01-01-2025) & TCS (2 shares @ 1000 INR on 01-01-2025) -> Total: 4,000 INR
- **Customer 1002** (Portfolio `100002`): Infosys (10 shares @ 1500 INR on 10-08-2026) -> Total: 15,000 INR

**Interactive Dialogue Flow:**
1. **Turn 1**: *"Can you review my portfolio?"* -> Agent detects missing context and asks for Customer ID (`1001` or `1002`).
2. **Turn 2**: *"My customer ID is 1001"* -> Agent calls `load_customer_portfolio` tool, queries SQLite, populates session state, and displays Reliance & TCS positions.
3. **Turn 3**: *"What is my total invested capital?"* -> Agent answers directly from session state context without re-querying SQLite or prompting for customer ID.

---

### 5. Running Unit & Integration Tests
```bash
# Windows
.\gradlew.bat test

# macOS / Linux
./gradlew test
```

