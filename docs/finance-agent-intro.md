# Finance Portfolio Decision-Support Agent — Master Index & Architectural Reference

> [!NOTE]
> **Built with Google Agent Development Kit (ADK) for Java** (`com.google.adk:google-adk:1.4.0`, `com.google.adk:google-adk-a2a:1.4.0`, and `com.google.adk:google-adk-dev:1.4.0`).  
> This master document serves as the **central index, architectural primer, and conceptual roadmap** for the Finance Portfolio Decision-Support Agent (`FinanceAdvisorAgentV1-11`). It outlines the system's progressive design, core engineering principles, layered intelligence model, cross-cutting infrastructure, and links to all version-specific reference guides from **V1 through V11**.

---

## Table of Contents

1. [Introduction & Architectural Mission](#1-introduction-architectural-mission)
   - [1.1 The Domain Mission](#11-the-domain-mission)
   - [1.2 Why Generative AI Fails at Finance Without an Agent Framework](#12-why-generative-ai-fails-at-finance-without-an-agent-framework)
   - [1.3 The Google ADK Solution & Design Philosophy](#13-the-google-adk-solution-design-philosophy)
2. [Core Concept Taxonomy & Architectural Invariants](#2-core-concept-taxonomy-architectural-invariants)
   - [2.1 Concept Taxonomy: Skills vs. Rules vs. Knowledge vs. Hooks](#21-concept-taxonomy-skills-vs-rules-vs-knowledge-vs-hooks)
   - [2.2 The Five Analytical Pillars](#22-the-five-analytical-pillars)
   - [2.3 The Seven Architectural Invariants](#23-the-seven-architectural-invariants)
   - [2.4 The Gemini Tool Exclusivity Architectural Pattern](#24-the-gemini-tool-exclusivity-architectural-pattern)
3. [The 5-Phase Evolutionary Roadmap](#3-the-5-phase-evolutionary-roadmap)
   - [3.1 Visual Architectural Topology](#31-visual-architectural-topology)
   - [3.2 Recommended Learning Sequence (Phases 1 to 5)](#32-recommended-learning-sequence-phases-1-to-5)
4. [Version-by-Version Deep Dive (V1 through V11)](#4-version-by-version-deep-dive-v1-through-v11)
   - [Milestone 1: Foundational Agent & Session (`finance.v1`)](#milestone-1-foundational-agent-session-financev1)
   - [Milestone 2: Context & Holdings State Management (`finance.v2`)](#milestone-2-context-holdings-state-management-financev2)
   - [Milestone 3: Deterministic Java Math & Grounded Search (`finance.v3`)](#milestone-3-deterministic-java-math-grounded-search-financev3)
   - [Milestone 4: Domain Skills & Grounding Knowledge (`finance.v4`)](#milestone-4-domain-skills-grounding-knowledge-financev4)
   - [Milestone 5: Model Context Protocol (MCP) Integration (`finance.v5`)](#milestone-5-model-context-protocol-mcp-integration-financev5)
   - [Milestone 6: Specialist Multi-Agent System (`finance.v6`)](#milestone-6-specialist-multi-agent-system-financev6)
   - [Milestone 7: Deterministic Workflow Orchestration (`finance.v7`)](#milestone-7-deterministic-workflow-orchestration-financev7)
   - [Milestone 8: Safety Guardrails & Lifecycle Callbacks (`finance.v8`)](#milestone-8-safety-guardrails-lifecycle-callbacks-financev8)
   - [Milestone 9: Systematic Evaluation & Adversarial Testing (`finance.v9`)](#milestone-9-systematic-evaluation-adversarial-testing-financev9)
   - [Milestone 10: Observability, Tracing & Persistence (`finance.v10`)](#milestone-10-observability-tracing-persistence-financev10)
   - [Milestone 11: Rules-Driven & Hook-Aware Agent (`finance.v11`)](#milestone-11-rules-driven-hook-aware-agent-financev11)
5. [Shared Infrastructure & Cross-Cutting Foundations](#5-shared-infrastructure-cross-cutting-foundations)
   - [5.1 Relational State & Pre-Seeded Test Data](#51-relational-state-pre-seeded-test-data)
   - [5.2 Hybrid Model Execution (Gemini & Ollama)](#52-hybrid-model-execution-gemini-ollama)
   - [5.3 FastMCP Subprocess Architecture (Yahoo Finance)](#53-fastmcp-subprocess-architecture-yahoo-finance)
   - [5.4 Enterprise Safety Perimeter & Defense-in-Depth](#54-enterprise-safety-perimeter-defense-in-depth)
   - [5.5 Correlation Spine & OpenTelemetry Tracing](#55-correlation-spine-opentelemetry-tracing)
6. [Master Quick-Start Execution Matrix](#6-master-quick-start-execution-matrix)
7. [Testing & Verification Quick Reference](#7-testing-verification-quick-reference)
8. [Documentation Sitemap & Repository Directory](#8-documentation-sitemap-repository-directory)

---

## 1. Introduction & Architectural Mission

### 1.1 The Domain Mission

> **The Core Mandate:**  
> *"Research my portfolio, explain what changed, identify verifiable empirical evidence, compare forward-looking scenarios, and produce an institutional decision-support report."*

Financial portfolio decision support is fundamentally different from generic conversational search or chat assistance. Investors, portfolio managers, and risk analysts do not need conversational chatter; they require **deterministic arithmetic, grounded empirical facts, rigorous risk attribution, multi-scenario stress-testing, and compliance disclosures**.

The Finance Portfolio Decision-Support Agent (`FinanceAdvisorAgent`) is an autonomous, production-grade agent system built on the official **Google Agent Development Kit (ADK) for Java**. It runs alongside the Social Spark marketing agent as part of the repository's dual-domain architecture, sharing core infrastructure while addressing the strict rigor demanded by institutional finance.

---

### 1.2 Why Generative AI Fails at Finance Without an Agent Framework

Generative Large Language Models (LLMs) are **probabilistic text generators**, not computational calculators or authoritative data stores. When an raw LLM is deployed into financial workflows, it suffers from five fatal failure modes:

| Failure Mode | Raw LLM Behavior | Financial Impact | Google ADK Solution |
|---|---|---|---|
| **Arithmetic Hallucination** | Generates plausible-looking numbers for PnL, CAGR, weights, and returns based on token probability. | Flawed valuations, incorrect asset allocations, miscalculated capital gains. | Offload all math to deterministic Java tools ([`PortfolioMathTool`](../src/main/java/com/google/adk/finance/tools/PortfolioMathTool.java)). |
| **Temporal Staleness** | Relies on training weights cutoffs; cannot know current stock prices, earnings surprises, or rate decisions. | Obsolete advice based on outdated macroeconomic environments. | Live market grounding via Yahoo Finance MCP ([`YahooFinanceMcpServer`](../src/main/java/com/google/adk/mcp/yahoofinance/YahooFinanceMcpServer.java)) & Google Search. |
| **Cognitive Overload** | When given a massive monolithic prompt containing all rules, tools, formulas, and data, it forgets constraints. | Skips mandatory risk disclosures, confuses tickers, produces inconsistent reports. | Specialized sub-agent division of labor ([`FinanceAdvisorAgentV6`](../src/main/java/com/google/adk/finance/v6/FinanceAdvisorAgentV6.java)). |
| **Adversarial Vulnerability** | Prompt injection attacks can hijack instructions, leak client PII, or trigger unauthorized trade requests. | Regulatory breach, privacy violations, unauthorized transactional exposure. | Multi-stage lifecycle guardrails & blocking hooks ([`BeforeAgentGuardrail`](../src/main/java/com/google/adk/finance/v8/callbacks/BeforeAgentGuardrail.java), [`PreToolSourceValidationHook`](../src/main/java/com/google/adk/finance/v11/hooks/PreToolSourceValidationHook.java)). |
| **Black-Box Opacity** | No verifiable audit trail of how conclusions were reached, what tools ran, or what data was retrieved. | Inability to pass financial compliance audits or debug production failures. | Structured telemetry persistence ([`ExecutionRepository`](../src/main/java/com/google/adk/finance/v10/persistence/ExecutionRepository.java)) with correlation IDs. |

---

### 1.3 The Google ADK Solution & Design Philosophy

Rather than building an unstructured monolithic codebase where all features are mixed together, this repository adopts a **versioned educational progression (V1 through V11)**. Each version isolates specific Google ADK abstractions, design patterns, and domain capabilities.

This allows developers and architects to:
1. **Learn incrementally**: Master agent instantiation in V1, state in V2, tools in V3, skills in V4, MCP in V5, multi-agent in V6, pipelines in V7, guardrails in V8, evaluation in V9, tracing in V10, and rules/hooks in V11.
2. **Isolate regressions**: Compare the exact diff between versions to understand how an architectural boundary was introduced.
3. **Run side-by-side**: Each version has its own dedicated launcher script (`test-finance-v*.bat` / `.sh`) and independent automated test suites.

---

## 2. Core Concept Taxonomy & Architectural Invariants

### 2.1 Concept Taxonomy: Skills vs. Rules vs. Knowledge vs. Hooks

A frequent challenge for engineers adopting Google ADK is understanding the precise boundaries between the framework's various abstractions. The table below outlines the exact architectural taxonomy enforced across this codebase:

```
                                    +-----------------------------------------+
                                    |         User Prompt / Request           |
                                    +--------------------+--------------------+
                                                         |
                                 +-----------------------v-----------------------+
                                 |  LIFECYCLE HOOKS & GUARDRAILS (Programmatic)  |
                                 |  - BeforeAgent: PII scrub & Injection block   |
                                 |  - BeforeTool: Trading blockade               |
                                 +-----------------------+-----------------------+
                                                         |
                                 +-----------------------v-----------------------+
                                 |            ROOT AGENT ORCHESTRATOR            |
                                 |  Instructions: Persona + SCOPED RULES (.md)   |
                                 +-----------------------+-----------------------+
                                                         |
          +───────────────────────┼───────────────────────┼───────────────────────+
          |                       |                       |                       |
          v                       v                       v                       v
+───────────────────+   +───────────────────+   +───────────────────+   +───────────────────+
|     BASE TOOL     |   |    AGENT TOOL     |   |   SKILL TOOLSET   |   |  KNOWLEDGE TOOL   |
| Deterministic     |   | Sub-Agent         |   | Dynamic On-Demand |   | Authoritative     |
| Pure Java Code    |   | Delegation        |   | Markdown Workflow |   | Reference Docs    |
| (PortfolioMath)   |   | (Search/Specialist|   | (skills/finance/) |   | (knowledge/*.md)  |
+───────────────────+   +───────────────────+   +───────────────────+   +───────────────────+
```

| Concept | ADK Abstraction | Storage / Format | Enforced At | Primary Purpose | Example in Repo |
|---|---|---|---|---|---|
| **BaseTool** | `com.google.adk.tools.BaseTool` | Java class (`FunctionDeclaration`) | Tool call execution | Deterministic calculation, local DB queries, local IO. | [`PortfolioMathTool`](../src/main/java/com/google/adk/finance/tools/PortfolioMathTool.java), [`LoadCustomerPortfolioTool`](../src/main/java/com/google/adk/finance/tools/LoadCustomerPortfolioTool.java) |
| **AgentTool** | `com.google.adk.tools.AgentTool` | Wrapper over `BaseAgent` | Sub-agent delegation | Isolate specialist sub-agents or bypass Gemini tool constraints. | `AgentTool.create(searchAgent)` in V3/V6 |
| **Skill** | `LocalSkillSource`, `SkillToolset` | Markdown (`skills/finance/*/SKILL.md`) | On-demand tool invocation | Reusable step-by-step procedures loaded dynamically when needed. | `skills/finance/valuation/SKILL.md` |
| **Knowledge** | `ProjectKnowledgeTool` | Curated Markdown (`knowledge/*.md`) | Tool call reading | Authoritative reference frameworks (ratios, metrics, risk definitions). | `knowledge/valuation-principles.md` |
| **Rule** | `RuleLoader`, `ScopedRules` | Scoped Markdown (`v11/rules/*.md`) | Injected into agent instructions | Mandatory behavioral boundaries governing reasoning and output style. | `src/main/resources/v11/rules/finance-rules.md` |
| **Guardrail** | Native Callbacks (`BeforeAgent`, etc.) | Java Interceptor | Pre-model / Pre-tool boundary | Security filters preventing injection, PII leakage, or trade execution. | [`BeforeAgentGuardrail`](../src/main/java/com/google/adk/finance/v8/callbacks/BeforeAgentGuardrail.java) |
| **Hook** | `BeforeToolCallbackSync`, etc. | Java Interceptor | Invocation lifecycle boundary | Programmatic verification, telemetry logging, policy enforcement. | [`PreToolSourceValidationHook`](../src/main/java/com/google/adk/finance/v11/hooks/PreToolSourceValidationHook.java) |
| **Workflow** | `SequentialAgent`, `ParallelAgent`, `LoopAgent` | Java Agent Composition | Agent execution graph | Deterministic control-flow graphs (pipelines, fan-out, critic loops). | [`InvestmentResearchSequentialWorkflowV7`](../src/main/java/com/google/adk/finance/v7/workflows/sequential/InvestmentResearchSequentialWorkflowV7.java) |

---

### 2.2 The Five Analytical Pillars

Every version of the Finance Advisor operates across five structured analytical pillars:

1. **Portfolio Ingestion & State Tracking**: Ingest and structure current asset holdings, asset classes, target weightings, purchase prices, and current valuations from persistent relational storage.
2. **Variance & Attribution Analysis**: Explain *what changed* (PnL, cost basis, asset allocation drift, performance vs. benchmark, concentration spikes >25%).
3. **Evidence Identification & Grounding**: Retrieve verifiable real-world evidence (earnings reports, macroeconomic indicators, interest rates, SEC filings, sector news) using Google Search and Yahoo Finance MCP tools.
4. **Scenario & Stress-Testing**: Compare forward-looking scenarios (**Baseline**, **Upside Catalysts**, and **Downside Stress-Test**) to evaluate portfolio resilience and risk-reward outcomes.
5. **Decision-Support Synthesis**: Generate an institutional-grade, actionable decision-support report featuring an executive thesis, evidence matrices, scenario comparative tables, risk flags, and rebalancing trade-offs.

---

### 2.3 The Seven Architectural Invariants

Across all eleven versions, the system strictly enforces seven immutable rules:

1. **Arithmetic Invariant**: Never permit an LLM to calculate returns, cost basis, PnL, allocation percentages, or technical indicators. All math must be computed by [`PortfolioMathTool`](../src/main/java/com/google/adk/finance/tools/PortfolioMathTool.java).
2. **Trading Gate Invariant**: The agent operates strictly in **read-only decision-support mode**. Any attempt to execute trades (`execute_trade`, `place_order`, `transfer_funds`) is deterministically blocked by [`BeforeToolGuardrail`](../src/main/java/com/google/adk/finance/v8/callbacks/BeforeToolGuardrail.java) and [`PreToolSourceValidationHook`](../src/main/java/com/google/adk/finance/v11/hooks/PreToolSourceValidationHook.java).
3. **Grounding Traceability**: Every empirical quote, market capitalization, P/E ratio, and financial metric must be attributed to an authoritative source (Yahoo Finance MCP, Google Search, SEC filings).
4. **Three-Tier Scenario Completeness**: Forward-looking scenario analysis must never present a one-sided view. It must explicitly formulate **Baseline**, **Upside**, and **Downside Stress-Test** conditions.
5. **PII Redaction Invariant**: Plaintext customer identifiers (bank accounts, credit cards, tax IDs, phone numbers) must never enter model prompts, event logs, or telemetry archives.
6. **Token Accounting Safety**: If a model endpoint does not report token counts (such as some local models or test mocks), the agent records `"tokenUsage": "UNKNOWN"`. It never fabricates synthetic numbers.
7. **Mandatory Compliance Disclaimer**: Every synthesis, response, or generated report must conclude with the official non-advice regulatory disclosure.

---

### 2.4 The Gemini Tool Exclusivity Architectural Pattern

> [!IMPORTANT]
> **The Gemini Tool Exclusivity Constraint**:  
> Google Gemini's native API prohibits combining native search tools (`GoogleSearchTool.INSTANCE`) and client function declarations (`FunctionDeclaration`) within the same model generation turn. If an agent declares both, the Gemini API throws a 400 Bad Request exception.

```
WRONG ARCHITECTURE (Fails with 400 Bad Request):
+──────────────────────────────────────────────────────────+
| Root Agent                                               |
| - GoogleSearchTool.INSTANCE   (Native Search Tool)       | ──► CRASH! Cannot mix native
| - PortfolioMathTool           (Client Function Tool)     |     search with client tools.
| - LoadCustomerPortfolioTool   (Client Function Tool)     |
+──────────────────────────────────────────────────────────+

CORRECT ADK PATTERN (Used in V3-V11):
+──────────────────────────────────────────────────────────+
| Root Agent                                               |
| - AgentTool(stockmarket_researcher) (Client Function Tool)| ──► Allowed! Root agent only
| - PortfolioMathTool                 (Client Function Tool)|     sees client function
| - LoadCustomerPortfolioTool         (Client Function Tool)|     declarations.
+──────────────────────────┬───────────────────────────────+
                           │ Sub-agent invocation
                           ▼
            +─────────────────────────────+
            | stockmarket_researcher      |
            | - GoogleSearchTool.INSTANCE | (Isolated native search agent)
            +─────────────────────────────+
```

To resolve this constraint cleanly without compromising functionality, this codebase implements the **Single Responsibility Agent Architecture**:
- An isolated sub-agent (`stockmarket_researcher`) is created with **only** `GoogleSearchTool.INSTANCE`.
- The root agent wraps this sub-agent inside an [`AgentTool.create(searchAgent)`](../src/main/java/com/google/adk/finance/v3/agents/FinanceAgentV3Factory.java).
- To the root model, the search capability appears as a standard client function declaration, satisfying Gemini's API contract while granting full search grounding!

---

## 3. The 5-Phase Evolutionary Roadmap

### 3.1 Visual Architectural Topology

The system evolves progressively across five pedagogical phases:

```
PHASE 1: FOUNDATIONS (Single Agent & Memory)
[V1: Agent & Session]       ──►  [V2: Relational State & Memory]
(LlmAgent, Runner, Streaming)    (SQLite WAL, LoadCustomerPortfolioTool, {placeholder?})
                                         │
                                         ▼
PHASE 2: GROUNDING & SKILLS (Deterministic Tools & Domain Frameworks)
[V3: Java Math & Search]    ──►  [V4: Domain Skills & Knowledge]
(PortfolioMathTool, AgentTool)   (SkillToolset, LocalSkillSource, knowledge/*.md)
                                         │
                                         ▼
PHASE 3: PROTOCOLS & SPECIALIZATION (MCP & Multi-Agent Division of Labor)
[V5: Yahoo Finance MCP]     ──►  [V6: Specialist Multi-Agent Hierarchy]
(FastMCP STDIO, Subprocess)      (Director + 5 Sub-Agents: Search, Macro, Risk, etc.)
                                         │
                                         ▼
PHASE 4: DETERMINISTIC PIPELINES & SAFETY (Orchestration & Perimeter Defenses)
[V7: Workflow Orchestration]──►  [V8: Guardrails & Lifecycle Callbacks]
(Sequential, Parallel, Loop)     (BeforeAgent, BeforeTool, AfterModel, EvidenceStore)
                                         │
                                         ▼
PHASE 5: ENTERPRISE PRODUCTION RIGOR (Observability, Benchmarking & Rules)
[V9: Evaluation & Red-Team] ──►  [V10: Tracing & Telemetry] ──► [V11: Scoped Rules & Hooks]
(Faithfulness, Math Fidelity)    (executionId, SQLite Trace)   (v11/rules/, Blocking Hooks)
```

---

### 3.2 Recommended Learning Sequence (Phases 1 to 5)

If you are new to the Google Agent Development Kit or this repository, follow this systematic learning progression:

1. **Phase 1: Agent Fundamentals & Session State (V1 $\rightarrow$ V2)**
   - Understand how an agent is instantiated using `LlmAgent.builder()`.
   - Learn how `InMemoryRunner` drives conversation turns and how `SessionService` isolates client sessions.
   - Master persistent state injection using custom `BaseTool` instances and `{placeholder?}` prompt templating.
2. **Phase 2: Grounding, Tools & Domain Skills (V3 $\rightarrow$ V4)**
   - Eliminate hallucinations by offloading arithmetic to deterministic Java math tools.
   - Master the `AgentTool` pattern to isolate native Google Search grounding from client function declarations.
   - Learn dynamic on-demand domain capability loading via `SkillToolset` and curated `knowledge/*.md` repositories.
3. **Phase 3: Protocols & Multi-Agent Delegation (V5 $\rightarrow$ V6)**
   - Integrate industry-standard external tools using the **Model Context Protocol (MCP)** over STDIO transport.
   - Deconstruct complex financial problems across a hierarchy of 5 specialist sub-agents coordinated by a root Director.
4. **Phase 4: Pipelines, Parallelism & Safety Guardrails (V7 $\rightarrow$ V8)**
   - Replace probabilistic routing with deterministic workflow structures (`SequentialAgent`, `ParallelAgent`, `LoopAgent`).
   - Implement defense-in-depth security around the agent perimeter using native ADK lifecycle callbacks.
5. **Phase 5: Enterprise Production Rigor (V9 $\rightarrow$ V10 $\rightarrow$ V11)**
   - Construct an automated evaluation harness with deterministic fidelity tests and qualitative LLM-as-a-judge scoring.
   - Implement end-to-end distributed tracing with correlation IDs, PII redaction, and dual JSON/SQLite persistence.
   - Deconstruct monolithic system prompts into modular Markdown rules files and enforce security through blocking Java hooks.

---

## 4. Version-by-Version Deep Dive (V1 through V11)

---

### Milestone 1: Foundational Agent & Session (`finance.v1`)

* **Dedicated Guide**: [`finance-agent-v1.md`](finance-agent-v1.md)
* **Package Path**: `com.google.adk.finance.v1`
* **Architectural Rationale ("Why")**:  
  To establish the absolute baseline of the Google Agent Development Kit—instantiating an agent, configuring system instructions, managing multi-turn sessions, and handling real-time token streaming without the cognitive overhead of external tools or sub-agents.
* **ADK Concepts Demonstrated**:
  - `com.google.adk.agents.LlmAgent`: Core agent abstraction configured with financial persona instructions.
  - `com.google.adk.runner.InMemoryRunner`: In-memory execution runner orchestrating prompt dispatch and response generation.
  - `com.google.adk.sessions.SessionService`: Multi-turn session manager isolating conversation history by session ID.
  - `com.google.adk.agents.RunConfig`: Model generation parameters (temperature, max tokens, top-p).
  - RxJava `Flowable<Event>`: Streaming model response tokens in real-time to the terminal console.
* **Implementation Highlights**:
  - [`FinanceAgentV1Factory.java`](../src/main/java/com/google/adk/finance/v1/FinanceAgentV1Factory.java) constructs `finance_advisor_v1` using instructions that enforce analytical frameworks (Asset Allocation, Fundamental Analysis, Risk Management).
  - [`FinanceConsoleV1.java`](../src/main/java/com/google/adk/finance/v1/FinanceConsoleV1.java) provides an interactive terminal session where each turn is streamed reactively via `runner.runAsync()`.
* **Key Files**:
  | File | Relative Path | Architectural Role |
  |---|---|---|
  | `FinanceAgentV1Factory.java` | `../src/main/java/com/google/adk/finance/v1/` | Factory configuring baseline `LlmAgent` and instructions. |
  | `FinanceConsoleV1.java` | `../src/main/java/com/google/adk/finance/v1/` | Interactive CLI console managing sessions and streaming tokens. |
  | `FinanceV1IntegrationTest.java` | `../src/test/java/com/google/adk/finance/v1/` | Automated integration test verifying session execution and advice generation. |
* **Testing & Verification**:
  - Interactive CLI: `.\test-finance-v1.bat` (or `./test-finance-v1.sh`)
  - Automated Gradle Test: `./gradlew test --tests com.google.adk.finance.v1.FinanceV1IntegrationTest`
  - Sample Query: *"What are the core principles of portfolio asset allocation?"*

---

### Milestone 2: Context & Holdings State Management (`finance.v2`)

* **Dedicated Guide**: [`finance-agent-v2.md`](finance-agent-v2.md)
* **Package Path**: `com.google.adk.finance.v2`
* **Architectural Rationale ("Why")**:  
  Advisors cannot provide relevant advice without knowing the client's current portfolio holdings. V2 demonstrates how to query an embedded relational database and inject holdings into session state memory so the user is never asked for their account information twice.
* **ADK Concepts Demonstrated**:
  - Custom `com.google.adk.tools.BaseTool`: Programmatic tool querying SQLite and returning structured data.
  - `toolContext.state()`: Populating persistent session variables dynamically inside tool calls.
  - Dynamic Prompt Templating: Using `{customer_id?}`, `{portfolio_id?}`, and `{portfolio_holdings?}` placeholders in agent instructions to automatically hydrate context.
  - Multi-Turn State Preservation: State injected on Turn 1 remains active and accessible across all subsequent turns.
* **Implementation Highlights**:
  - Embedded SQLite database managed by [`CustomerPortfolioRepository.java`](../src/main/java/com/google/adk/finance/v2/CustomerPortfolioRepository.java) in WAL mode, auto-seeding Customer `1001` (Reliance & TCS) and Customer `1002` (Infosys).
  - [`LoadCustomerPortfolioTool.java`](../src/main/java/com/google/adk/finance/tools/LoadCustomerPortfolioTool.java) queries SQLite, formats holdings, and populates `toolContext.state()`.
  - Dynamic instructions in [`FinanceAgentV2Factory.java`](../src/main/java/com/google/adk/finance/v2/FinanceAgentV2Factory.java) ensure the agent asks for Customer ID if state is empty, but answers immediately from memory once loaded.
* **Key Files**:
  | File | Relative Path | Architectural Role |
  |---|---|---|
  | `CustomerPortfolioRepository.java` | `../src/main/java/com/google/adk/finance/v2/` | SQLite DAO managing schema creation, WAL mode, and auto-seeding. |
  | `PortfolioModels.java` | `../src/main/java/com/google/adk/finance/v2/` | Domain records (`CustomerRecord`, `PortfolioHoldingRecord`, `CustomerPortfolio`). |
  | `LoadCustomerPortfolioTool.java` | `../src/main/java/com/google/adk/finance/tools/` | Custom `BaseTool` injecting holdings into `toolContext.state()`. |
  | `FinanceAgentV2Factory.java` | `../src/main/java/com/google/adk/finance/v2/` | Root factory creating `finance_advisor_v2` with state placeholders. |
  | `FinanceConsoleV2.java` | `../src/main/java/com/google/adk/finance/v2/` | Interactive CLI displaying live session state transitions. |
* **Testing & Verification**:
  - Interactive CLI: `.\test-finance-v2.bat` (Type `state` to view live session state)
  - Automated Gradle Tests: `./gradlew test --tests "com.google.adk.finance.v2.*"`
  - Sample Multi-Turn Flow: Turn 1: *"What is my portfolio?"* $\rightarrow$ Turn 2: *"My customer ID is 1001"* $\rightarrow$ Turn 3: *"What is my total invested capital?"* (Answered directly from memory!)

---

### Milestone 3: Deterministic Java Math & Grounded Search (`finance.v3`)

* **Dedicated Guide**: [`finance-agent-v3.md`](finance-agent-v3.md)
* **Package Path**: `com.google.adk.finance.v3`
* **Architectural Rationale ("Why")**:  
  Solves the two most critical vulnerabilities of financial LLMs: **arithmetic hallucinations** and the **Gemini Tool Exclusivity Constraint** (which prohibits combining native search tools with client function tools).
* **ADK Concepts Demonstrated**:
  - `com.google.adk.tools.GoogleSearchTool`: Grounding responses with live web search results (news, earnings, analyst targets).
  - `com.google.adk.tools.AgentTool`: Wrapping an isolated search specialist sub-agent to expose search as a clean client function.
  - Custom `BaseTool` with typed schemas: Offloading arithmetic to deterministic Java calculations.
* **Implementation Highlights**:
  - [`MarketResearchAgentFactory.java`](../src/main/java/com/google/adk/finance/v3/agents/MarketResearchAgentFactory.java) isolates `GoogleSearchTool.INSTANCE` within `stockmarket_researcher`.
  - [`FinanceAgentV3Factory.java`](../src/main/java/com/google/adk/finance/v3/agents/FinanceAgentV3Factory.java) binds `AgentTool.create(searchAgent)`, [`PortfolioMathTool.java`](../src/main/java/com/google/adk/finance/tools/PortfolioMathTool.java), and [`LoadCustomerPortfolioTool.java`](../src/main/java/com/google/adk/finance/tools/LoadCustomerPortfolioTool.java).
  - `PortfolioMathTool` deterministically computes:
    - PnL and return percentages (`calculate_pnl`).
    - Asset allocation weights and concentration alerts >25% (`calculate_allocation`).
    - Moving averages and technical spread indicators (`calculate_technical_indicator`).
* **Key Files**:
  | File | Relative Path | Architectural Role |
  |---|---|---|
  | `PortfolioMathTool.java` | `../src/main/java/com/google/adk/finance/tools/` | Deterministic Java math tool for PnL, allocation weights, and SMAs. |
  | `MarketResearchAgentFactory.java` | `../src/main/java/com/google/adk/finance/v3/agents/` | Decoupled factory isolating Google Search in `stockmarket_researcher`. |
  | `FinanceAgentV3Factory.java` | `../src/main/java/com/google/adk/finance/v3/agents/` | Root factory coordinating search, math, and DB tools. |
  | `FinanceConsoleV3.java` | `../src/main/java/com/google/adk/finance/v3/` | Interactive CLI displaying real-time tool execution events. |
* **Testing & Verification**:
  - Interactive CLI: `.\test-finance-v3.bat`
  - Automated Gradle Tests: `./gradlew test --tests "com.google.adk.finance.v3.*"`
  - Sample Query: *"What are the latest developments and analyst price targets for Infosys?"*

---

### Milestone 4: Domain Skills & Grounding Knowledge (`finance.v4`)

* **Dedicated Guide**: [`finance-agent-v4.md`](finance-agent-v4.md)
* **Package Path**: `com.google.adk.finance.v4`
* **Architectural Rationale ("Why")**:  
  Bloating system prompts with hundreds of pages of valuation guidelines, financial ratio formulas, and risk methodologies degrades model reasoning. V4 introduces dynamic on-demand loading of domain procedures and authoritative grounding knowledge.
* **ADK Concepts Demonstrated**:
  - `com.google.adk.skills.LocalSkillSource`: Classpath and filesystem discovery of modular Markdown domain skills.
  - `com.google.adk.skills.SkillToolset`: ADK toolset exposing discovered skills dynamically to the agent.
  - Three-Tier Knowledge Hierarchy: Model Weights $\rightarrow$ Curated Project Grounding $\rightarrow$ Live Web Search.
* **Implementation Highlights**:
  - Curated grounding repository maintained in [`knowledge/`](../knowledge/) (Glossary, Valuation Multiples, Fundamentals, Risk Framework, Portfolio Principles, Market Research Framework).
  - Custom [`ProjectKnowledgeTool.java`](../src/main/java/com/google/adk/finance/tools/ProjectKnowledgeTool.java) exposes `read_project_knowledge` for on-demand inspection of Markdown files.
  - 6 modular skills maintained in [`skills/finance/`](../skills/finance/) (`finance-fundamentals`, `fundamental-analysis`, `valuation`, `risk-management`, `portfolio-analysis`, `market-research`).
  - [`FinanceAdvisorAgentV4Factory.java`](../src/main/java/com/google/adk/finance/v4/FinanceAdvisorAgentV4Factory.java) wires skills, knowledge, search, math, and DB into `finance_advisor_v4`.
* **Key Files**:
  | File | Relative Path | Architectural Role |
  |---|---|---|
  | `FinanceAdvisorAgentV4Factory.java` | `../src/main/java/com/google/adk/finance/v4/` | Root factory wiring skills, knowledge, search, math, and DB. |
  | `ProjectKnowledgeTool.java` | `../src/main/java/com/google/adk/finance/tools/` | Tool for reading curated reference documents in `knowledge/`. |
  | `FinanceConsoleV4.java` | `../src/main/java/com/google/adk/finance/v4/` | Interactive CLI with `skills` and `knowledge` listing commands. |
* **Testing & Verification**:
  - Interactive CLI: `.\test-finance-v4.bat` (Commands: `skills`, `knowledge`, `state`)
  - Automated Gradle Tests: `./gradlew test --tests "com.google.adk.finance.v4.*"`
  - Sample Query: *"Explain the difference between P/E and EV/EBITDA using our project valuation framework."*

---

### Milestone 5: Model Context Protocol (MCP) Integration (`finance.v5`)

* **Dedicated Guide**: [`finance-agent-v5.md`](finance-agent-v5.md)
* **Package Path**: `com.google.adk.finance.v5`
* **Architectural Rationale ("Why")**:  
  To integrate industry-standard external tools without writing custom code for every financial data feed. V5 connects the agent to an independent Java Yahoo Finance MCP Server over STDIO transport.
* **ADK Concepts Demonstrated**:
  - `com.google.adk.tools.mcp.McpToolset`: Official ADK toolset connecting to Model Context Protocol servers.
  - `io.modelcontextprotocol.sdk:mcp:1.1.2`: Standard MCP protocol integration.
  - `ServerParameters`: Spawning and managing external server child processes over STDIO.
  - Dynamic Tool Discovery: Tools exposed by the MCP server are discovered and registered at runtime.
* **Implementation Highlights**:
  - Standalone MCP server in [`YahooFinanceMcpServer.java`](../src/main/java/com/google/adk/mcp/yahoofinance/YahooFinanceMcpServer.java) running over `System.in`/`System.out` with logging redirected to `stderr`.
  - Exposes 4 tools: `get_stock_info`, `get_stock_actions`, `get_financial_statement`, `get_recommendations`.
  - [`YahooFinanceMcpClientManager.java`](../src/main/java/com/google/adk/finance/v5/YahooFinanceMcpClientManager.java) manages the child process lifecycle, ping verification with a 15-second timeout, and JVM shutdown hooks.
* **Key Files**:
  | File | Relative Path | Architectural Role |
  |---|---|---|
  | `YahooFinanceMcpClientManager.java` | `../src/main/java/com/google/adk/finance/v5/` | Client connection manager handling subprocess lifecycle. |
  | `YahooFinanceMcpServer.java` | `../src/main/java/com/google/adk/mcp/yahoofinance/` | Standalone Java FastMCP server over STDIO. |
  | `FinanceAdvisorAgentV5Factory.java` | `../src/main/java/com/google/adk/finance/v5/` | Root factory wiring MCP tools into `finance_advisor_v5`. |
  | `FinanceConsoleV5.java` | `../src/main/java/com/google/adk/finance/v5/` | Interactive CLI with `mcp` and `tools` inspection shortcuts. |
* **Testing & Verification**:
  - Interactive CLI: `.\test-finance-v5.bat` (Commands: `mcp`, `tools`, `skills`, `state`)
  - Automated Gradle Tests: `./gradlew test --tests com.google.adk.finance.v5.FinanceV5IntegrationTest`
  - Sample Query: *"Fetch current stock quote, market cap, and P/E ratio for AAPL using Yahoo Finance MCP."*

---

### Milestone 6: Specialist Multi-Agent System (`finance.v6`)

* **Dedicated Guide**: [`finance-agent-v6.md`](finance-agent-v6.md)
* **Package Path**: `com.google.adk.finance.v6`
* **Architectural Rationale ("Why")**:  
  As financial workflows expand, a single monolithic agent experiences cognitive degradation. V6 establishes a multi-agent hierarchy dividing labor across five specialized sub-agents.
* **ADK Concepts Demonstrated**:
  - Hierarchical Multi-Agent Architecture: Root Director delegating to specialist sub-agents.
  - Tool Ownership Isolation: Tools are bound strictly to the specialist that requires them, keeping agent context windows clean.
  - Standardized Output Contract: Specialists return structured `[SUB-AGENT REPORT: <Name>]` blocks.
* **Implementation Highlights**:
  - **Director** ([`FinanceAdvisorAgentV6.java`](../src/main/java/com/google/adk/finance/v6/FinanceAdvisorAgentV6.java)): Interprets intent, delegates tasks, and synthesizes institutional decision reports.
  - **The 5 Specialist Sub-Agents**:
    1. [`MarketResearchAgentV6.java`](../src/main/java/com/google/adk/finance/v6/subagents/MarketResearchAgentV6.java) (`stockmarket_researcher`): Real-time web news and 8-stage market research.
    2. [`ScenarioAnalystAgentV6.java`](../src/main/java/com/google/adk/finance/v6/subagents/ScenarioAnalystAgentV6.java) (`scenario_analyst`): Forward-looking 3-tier scenario modeling.
    3. [`ReportWriterAgentV6.java`](../src/main/java/com/google/adk/finance/v6/subagents/ReportWriterAgentV6.java) (`report_writer`): Synthesizes multi-agent research into executive briefings.
    4. [`FundamentalAnalysisAgentV6.java`](../src/main/java/com/google/adk/finance/v6/subagents/FundamentalAnalysisAgentV6.java) (`fundamental_analysis_agent`): Multiples, statements, and analyst targets via MCP.
    5. [`PortfolioRiskAgentV6.java`](../src/main/java/com/google/adk/finance/v6/subagents/PortfolioRiskAgentV6.java) (`portfolio_risk_agent`): Systematic risk, beta, and concentration alerts (>25%).
* **Key Files**:
  | File | Relative Path | Architectural Role |
  |---|---|---|
  | `FinanceAdvisorAgentV6.java` | `../src/main/java/com/google/adk/finance/v6/` | Root orchestrator coordinating the 5 sub-agents. |
  | `MarketResearchAgentV6.java` | `../src/main/java/com/google/adk/finance/v6/subagents/` | Google Search specialist. |
  | `ScenarioAnalystAgentV6.java` | `../src/main/java/com/google/adk/finance/v6/subagents/` | Quantitative macro scenario analyst. |
  | `FinanceConsoleV6.java` | `../src/main/java/com/google/adk/finance/v6/` | Interactive CLI with `agents` command listing the sub-agent hierarchy. |
* **Testing & Verification**:
  - Interactive CLI: `.\test-finance-v6.bat` (Command: `agents`)
  - Automated Gradle Tests: `./gradlew test --tests com.google.adk.finance.v6.FinanceV6IntegrationTest`
  - Sample Query: *"Run a fundamental analysis on AAPL and assess portfolio risk for Customer 1001."*

---

### Milestone 7: Deterministic Workflow Orchestration (`finance.v7`)

* **Dedicated Guide**: [`finance-agent-v7.md`](finance-agent-v7.md)
* **Package Path**: `com.google.adk.finance.v7`
* **Architectural Rationale ("Why")**:  
  Dynamic sub-agent routing (V6) is probabilistic and non-deterministic. For regulatory compliance and audit consistency, institutional workflows must follow code-governed, deterministic execution topologies.
* **ADK Concepts Demonstrated**:
  - `com.google.adk.agents.SequentialAgent`: Multi-step linear pipelines passing state via typed `outputKey` bindings.
  - `com.google.adk.agents.ParallelAgent`: Concurrent multi-agent execution with fan-out/fan-in aggregation.
  - `com.google.adk.agents.LoopAgent` & `ExitLoopTool`: Iterative author-critic feedback loops with maximum iteration safety bounds.
* **Implementation Highlights**:
  - **Sequential Pipeline** ([`InvestmentResearchSequentialWorkflowV7.java`](../src/main/java/com/google/adk/finance/v7/workflows/sequential/InvestmentResearchSequentialWorkflowV7.java)): Executes Research $\rightarrow$ Fundamentals $\rightarrow$ Risk $\rightarrow$ Valuation $\rightarrow$ Synthesis in strict order.
  - **Parallel Fan-Out/Fan-In** ([`PortfolioParallelResearchWorkflowV7.java`](../src/main/java/com/google/adk/finance/v7/workflows/parallel/PortfolioParallelResearchWorkflowV7.java)): Concurrently analyzes multiple tickers across threads and merges results into a comparative matrix with **partial failure resilience** (if one ticker fails, the report succeeds and notes the failure without hallucinating).
  - **Iterative Critic Loop** ([`ResearchCriticLoopWorkflowV7.java`](../src/main/java/com/google/adk/finance/v7/workflows/loop/ResearchCriticLoopWorkflowV7.java)): Drafts report $\rightarrow$ audits citations and math $\rightarrow$ loops until approved or reaches `maxIterations(3)`.
* **Key Files**:
  | File | Relative Path | Architectural Role |
  |---|---|---|
  | `FinanceAdvisorAgentV7.java` | `../src/main/java/com/google/adk/finance/v7/` | Root orchestrator routing prompts to deterministic workflows. |
  | `InvestmentResearchSequentialWorkflowV7.java` | `../src/main/java/com/google/adk/finance/v7/workflows/sequential/` | 5-stage linear research pipeline. |
  | `PortfolioParallelResearchWorkflowV7.java` | `../src/main/java/com/google/adk/finance/v7/workflows/parallel/` | Concurrent multi-ticker research with partial failure resilience. |
  | `ResearchCriticLoopWorkflowV7.java` | `../src/main/java/com/google/adk/finance/v7/workflows/loop/` | Iterative author-critic feedback loop. |
* **Testing & Verification**:
  - Interactive CLI: `.\test-finance-v7.bat`
  - Automated Gradle Tests: `./gradlew test --tests com.google.adk.finance.v7.FinanceV7IntegrationTest`
  - Sample Trigger: *"Run full investment research sequential pipeline for AAPL"* or *"Run parallel portfolio research on AAPL, MSFT, GOOGL"*

---

### Milestone 8: Safety Guardrails & Lifecycle Callbacks (`finance.v8`)

* **Dedicated Guide**: [`finance-agent-v8.md`](finance-agent-v8.md)
* **Package Path**: `com.google.adk.finance.v8`
* **Architectural Rationale ("Why")**:  
  To secure the agent against adversarial attacks (prompt injection, prompt leakage), PII exposure, unauthorized actions (e.g. trading), and hallucinated numbers using native ADK lifecycle callbacks.
* **ADK Concepts Demonstrated**:
  - `BeforeAgentCallbackSync`: Input interception halting injection attacks early via `setEndInvocation(true)`.
  - `BeforeModelCallbackSync`: Wire-level PII scrubbing from `LlmRequest` before dispatch to the model.
  - `BeforeToolCallbackSync`: Tool authorization blocking transactional tools and validating tickers.
  - `AfterToolCallbackSync`: Intercepting tool results to populate verified numbers into an [`EvidenceStore.java`](../src/main/java/com/google/adk/finance/v8/evidence/EvidenceStore.java).
  - `AfterModelCallbackSync`: Output auditing checking factual claims against the `EvidenceStore` and enforcing compliance disclaimers.
* **Implementation Highlights**:
  - [`BeforeAgentGuardrail.java`](../src/main/java/com/google/adk/finance/v8/callbacks/BeforeAgentGuardrail.java): Redacts PII to `[REDACTED_...]` tokens and aborts prompt injections early.
  - [`BeforeToolGuardrail.java`](../src/main/java/com/google/adk/finance/v8/callbacks/BeforeToolGuardrail.java): Blocks transactional tools (`execute_trade`), validates tickers, and prevents SQL/shell injection.
  - [`AfterModelGuardrail.java`](../src/main/java/com/google/adk/finance/v8/callbacks/AfterModelGuardrail.java): Enforces non-empty responses, verifies disclaimers, and audits numerical claims.
* **Key Files**:
  | File | Relative Path | Architectural Role |
  |---|---|---|
  | `FinanceAdvisorAgentV8.java` | `../src/main/java/com/google/adk/finance/v8/` | Root factory wiring all 5 lifecycle interceptor callbacks. |
  | `BeforeAgentGuardrail.java` | `../src/main/java/com/google/adk/finance/v8/callbacks/` | Input interceptor halting prompt injection and redacting PII. |
  | `BeforeToolGuardrail.java` | `../src/main/java/com/google/adk/finance/v8/callbacks/` | Tool interceptor blocking trades and validating tickers. |
  | `AfterModelGuardrail.java` | `../src/main/java/com/google/adk/finance/v8/callbacks/` | Output interceptor enforcing disclaimers and auditing facts. |
  | `EvidenceStore.java` | `../src/main/java/com/google/adk/finance/v8/evidence/` | Grounding ledger storing verified empirical numbers. |
* **Testing & Verification**:
  - Interactive CLI: `.\test-finance-v8.bat` (Test injecting credit cards, phone numbers, or prompt injection probes)
  - Automated Gradle Tests: `./gradlew test --tests "com.google.adk.finance.v8.*"` (12 canonical tests + 8 security attack vector tests)

---

### Milestone 9: Systematic Evaluation & Adversarial Testing (`finance.v9`)

* **Dedicated Guide**: [`finance-agent-v9.md`](finance-agent-v9.md)
* **Package Path**: `com.google.adk.finance.v9`
* **Architectural Rationale ("Why")**:  
  *"V8 protects the agent. V9 measures the agent."* Evaluates whether the advisor is producing factually grounded, mathematically exact, and complete decision reports using a hybrid multi-layer evaluation harness.
* **ADK Concepts Demonstrated**:
  - Hybrid Evaluation Strategy: Deterministic programmatic assertion tests + Qualitative LLM-as-a-judge scoring.
  - Adversarial Failure Testing: Systematic stress-testing against simulated tool failures, network timeouts, and attack probes.
  - Evaluation Benchmark Registry: Standardized golden evaluation test cases (`GOLDEN-001` through `GOLDEN-005`).
* **Implementation Highlights**:
  - **Deterministic Evaluators**:
    1. [`FaithfulnessEvaluator.java`](../src/main/java/com/google/adk/finance/v9/evaluation/FaithfulnessEvaluator.java): Audits factual claims against empirical records in [`EvidenceStoreV9.java`](../src/main/java/com/google/adk/finance/v9/evaluation/EvidenceStoreV9.java) ($\le 2\%$ tolerance).
    2. [`CalculationFidelityEvaluator.java`](../src/main/java/com/google/adk/finance/v9/evaluation/CalculationFidelityEvaluator.java): Validates PnL and weights against `PortfolioMathTool` formulas within $0.10$ tolerance.
    3. [`ScenarioCompletenessEvaluator.java`](../src/main/java/com/google/adk/finance/v9/evaluation/ScenarioCompletenessEvaluator.java): Enforces the presence and quantitative depth of Baseline, Upside, and Stress test tiers.
  - **Qualitative LLM-as-a-Judge** ([`LlmJudgeEvaluator.java`](../src/main/java/com/google/adk/finance/v9/evaluation/LlmJudgeEvaluator.java)): Scores evidence usage, uncertainty calibration, and reasoning coherence without overriding arithmetic.
  - **Adversarial Failure Testing** ([`FailureScenarioRunner.java`](../src/main/java/com/google/adk/finance/v9/testing/FailureScenarioRunner.java)): Injects PII, prompt overrides, invalid tickers, trading attempts, and MCP outages to prove graceful degradation.
* **Key Files**:
  | File | Relative Path | Architectural Role |
  |---|---|---|
  | `FinanceAdvisorAgentV9.java` | `../src/main/java/com/google/adk/finance/v9/` | Factory configuring `finance_advisor_v9` with evaluators. |
  | `EvidenceStoreV9.java` | `../src/main/java/com/google/adk/finance/v9/evaluation/` | Grounding ledger storing normalized evidence records. |
  | `FaithfulnessEvaluator.java` | `../src/main/java/com/google/adk/finance/v9/evaluation/` | Deterministic claim-to-evidence consistency auditor. |
  | `CalculationFidelityEvaluator.java` | `../src/main/java/com/google/adk/finance/v9/evaluation/` | Deterministic arithmetic verifier validating PnL and weights. |
  | `LlmJudgeEvaluator.java` | `../src/main/java/com/google/adk/finance/v9/evaluation/` | Qualitative evaluator scoring evidence usage and uncertainty. |
* **Testing & Verification**:
  - Interactive CLI: `.\test-finance-v9.bat` (Commands: `/cases`, `/eval GOLDEN-001`, `/fail PII_LEAK`, `/evidence`)
  - Automated Gradle Tests: `./gradlew test --tests "com.google.adk.finance.v9.*"`

---

### Milestone 10: Observability, Tracing & Persistence (`finance.v10`)

* **Dedicated Guide**: [`finance-agent-v10.md`](finance-agent-v10.md)
* **Package Path**: `com.google.adk.finance.v10`
* **Architectural Rationale ("Why")**:  
  *"What exactly happened?"* Captures end-to-end causal event streams, measures real component latencies, safely logs tokens, and persists execution traces to JSON and SQLite.
* **ADK Concepts Demonstrated**:
  - Distributed Correlation Spine: End-to-end trace propagation via collision-safe `executionId` (`exec-YYYYMMDD-<uuid8>`).
  - Structured Telemetry Stream: Strongly typed records ([`ExecutionEvent.java`](../src/main/java/com/google/adk/finance/v10/observability/ExecutionEvent.java)) emitted across agent lifecycle events.
  - Dual Persistence Architecture: Pluggable storage abstraction supporting isolated JSON files and embedded SQLite WAL persistence.
  - Non-Fabrication Token Invariant: Records `"tokenUsage": "UNKNOWN"` if tokens are omitted by endpoints; never fabricates numbers.
* **Implementation Highlights**:
  - Correlation ID is created at request entry, propagated through session state, and bound to all model calls, tool calls, and persistence records.
  - [`FinanceAgentObserver.java`](../src/main/java/com/google/adk/finance/v10/observability/FinanceAgentObserver.java) collects events, calculates component latencies, scrubs PII, and builds an immutable `ExecutionTrace`.
  - [`SqliteExecutionRepository.java`](../src/main/java/com/google/adk/finance/v10/persistence/SqliteExecutionRepository.java) persists traces to table `agent_execution_v10` with failure isolation (telemetry failures never crash business advice).
* **Key Files**:
  | File | Relative Path | Architectural Role |
  |---|---|---|
  | `FinanceAdvisorAgentV10.java` | `../src/main/java/com/google/adk/finance/v10/` | Root factory creating observable agent with tracing. |
  | `FinanceAgentObserver.java` | `../src/main/java/com/google/adk/finance/v10/observability/` | Event listener assembling the `ExecutionTrace`. |
  | `ExecutionEvent.java` | `../src/main/java/com/google/adk/finance/v10/observability/` | Strongly typed immutable record representing lifecycle events. |
  | `JsonExecutionRepository.java` | `../src/main/java/com/google/adk/finance/v10/persistence/` | File-based repository writing formatted JSON execution traces. |
  | `SqliteExecutionRepository.java`| `../src/main/java/com/google/adk/finance/v10/persistence/` | Embedded SQLite DAO persisting traces to `agent_execution_v10`. |
* **Testing & Verification**:
  - Interactive CLI: `.\test-finance-v10.bat` (Commands: `trace <id>`, `history`, `failed`, `eval-failures`, `inspect <id>`)
  - Automated Gradle Tests: `./gradlew test --tests "com.google.adk.finance.v10.*"`

---

### Milestone 11: Rules-Driven & Hook-Aware Agent (`finance.v11`)

* **Dedicated Guide**: [`finance-agent-v11.md`](finance-agent-v11.md)
* **Package Path**: `com.google.adk.finance.v11`
* **Architectural Rationale ("Why")**:  
  Resolves the fundamental architectural boundary between declarative behavioral guidance (**RULES**) and programmatic interception (**HOOKS**). Monolithic system prompts are replaced with modular Markdown rules files, while Java lifecycle hooks enforce blocking security boundaries.
* **ADK Concepts Demonstrated**:
  - Modular Scoped Rules: Declarative Markdown guidelines loaded from classpath and scoped to specific sub-agents.
  - Blocking vs. Non-Blocking Hooks: Programmatic Java interceptors declaring strict blocking or passive observation semantics.
  - Dynamic Rule Hydration: Scoped rule injection into agent instructions without code recompilation.
* **Implementation Highlights**:
  - 5 modular rules files maintained in [`src/main/resources/v11/rules/`](../src/main/resources/v11/rules/):
    - `finance-rules.md`: Mandatory financial principles and arithmetic boundaries.
    - `research-rules.md`: Grounding verification and 8-stage research methodology.
    - `source-rules.md`: Strict source hierarchy (MCP $\rightarrow$ Google Search $\rightarrow$ Curated Knowledge).
    - `risk-rules.md`: Downside stress-testing and concentration thresholds.
    - `response-rules.md`: Institutional tone, table structures, and regulatory disclosures.
  - 4 Lifecycle Hooks:
    1. [`PreToolSourceValidationHook.java`](../src/main/java/com/google/adk/finance/v11/hooks/PreToolSourceValidationHook.java) (**BLOCKING**): Halts trades and validates tickers before tool execution.
    2. [`PostToolObservationHook.java`](../src/main/java/com/google/adk/finance/v11/hooks/PostToolObservationHook.java) (**NON-BLOCKING**): Records tool execution metrics into `HookRegistry`.
    3. [`ResponseValidationHook.java`](../src/main/java/com/google/adk/finance/v11/hooks/ResponseValidationHook.java) (**BLOCKING/REMEDIATING**): Validates source citations, checks non-empty output, and injects missing disclaimers.
    4. [`PreAgentRuleEnforcementHook.java`](../src/main/java/com/google/adk/finance/v11/hooks/PreAgentRuleEnforcementHook.java) (**NON-BLOCKING**): Verifies active rules presence in session state.
* **Key Files**:
  | File | Relative Path | Architectural Role |
  |---|---|---|
  | `FinanceAdvisorAgentV11.java` | `../src/main/java/com/google/adk/finance/v11/` | Root factory creating `finance_advisor_v11` with scoped rules and hooks. |
  | `RuleLoader.java` | `../src/main/java/com/google/adk/finance/v11/ruleloader/` | Parser and loader reading rules from classpath and filesystem. |
  | `ScopedRules.java` | `../src/main/java/com/google/adk/finance/v11/ruleloader/` | Container binding targeted rule subsets to individual agents. |
  | `PreToolSourceValidationHook.java` | `../src/main/java/com/google/adk/finance/v11/hooks/` | Blocking hook intercepting tool calls to validate tickers and block trades. |
  | `PostToolObservationHook.java` | `../src/main/java/com/google/adk/finance/v11/hooks/` | Non-blocking hook capturing execution telemetry into `HookRegistry`. |
  | `ResponseValidationHook.java` | `../src/main/java/com/google/adk/finance/v11/hooks/` | Blocking/remediating hook enforcing citations and disclaimers. |
* **Testing & Verification**:
  - Interactive CLI: `.\test-finance-v11.bat` (Commands: `rules`, `hooks`, `telemetry`, `state`)
  - Automated Gradle Tests: `./gradlew test --tests "com.google.adk.finance.v11.*"`
  - Sample Query: *"Review my portfolio for Customer 1001 and propose rebalancing options."*

---

## 5. Shared Infrastructure & Cross-Cutting Foundations

### 5.1 Relational State & Pre-Seeded Test Data

The system uses an embedded SQLite database in **Write-Ahead Logging (WAL)** mode (`finance_portfolio.db`), ensuring high-concurrency read/write operations without table locking.

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

-- Table 3: agent_execution_v10 (Execution telemetry traces)
CREATE TABLE IF NOT EXISTS agent_execution_v10 (
    execution_id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    agent_version TEXT NOT NULL,
    status TEXT NOT NULL,
    prompt TEXT NOT NULL,
    response TEXT,
    duration_ms INTEGER NOT NULL,
    tool_calls_count INTEGER NOT NULL,
    token_usage TEXT NOT NULL,
    events_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Pre-Seeded Test Customer Profiles:
- **Customer `1001` (Portfolio `100001`)**:
  - **Reliance Industries (`RELIANCE`)**: Quantity: `2`, Buy Price: `1000.00 INR`, Bought: `01-01-2025`
  - **Tata Consultancy Services (`TCS`)**: Quantity: `2`, Buy Price: `1000.00 INR`, Bought: `01-01-2025`
  - Total Invested Capital: `4000.00 INR` (Balanced 50/50 allocation).
- **Customer `1002` (Portfolio `100002`)**:
  - **Infosys (`INFY`)**: Quantity: `10`, Buy Price: `1500.00 INR`, Bought: `10-08-2026`
  - Total Invested Capital: `15000.00 INR` (100% single-stock concentration risk).

---

### 5.2 Hybrid Model Execution (Gemini & Ollama)

The application supports hybrid model execution managed by [`AppConfig.createModel()`](../src/main/java/com/google/adk/socialspark/config/AppConfig.java):

```
                                      +-------------------------------+
                                      |     AppConfig.createModel()   |
                                      +---------------+---------------+
                                                      |
                                    +-----------------+-----------------+
                                    |                                   |
                                    v                                   v
                     +-----------------------------+     +-----------------------------+
                     |        Google Gemini        |     |        Local Ollama         |
                     | Provider: GEMINI / GOOGLE   |     | Provider: OLLAMA / OPENAI   |
                     | Model: gemini-2.5-flash     |     | Model: gemma4:31b           |
                     | Env: GEMINI_API_KEY         |     | Env: OLLAMA_BASE_URL        |
                     +-----------------------------+     +-----------------------------+
```

To switch between providers, configure your environment variables:
```bash
# Option A: Google Gemini GenAI (Default)
export MODEL_PROVIDER=gemini
export GEMINI_API_KEY="your-api-key"
export GEMINI_MODEL="gemini-2.5-flash"

# Option B: Local Ollama / Gemma
export MODEL_PROVIDER=ollama
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_MODEL="gemma4:31b"
```

---

### 5.3 FastMCP Subprocess Architecture (Yahoo Finance)

The Yahoo Finance Model Context Protocol integration uses a standalone Java FastMCP server executing over standard I/O (STDIO):

```
+───────────────────────────────────+             STDIO Pipe            +───────────────────────────────────+
|         Parent Application        |   (System.in / System.out)        |     YahooFinanceMcpServer         |
|  YahooFinanceMcpClientManager     | ────────────────────────────────► |  Standalone Child JVM Process     |
|  - Spawns subprocess              | ◄──────────────────────────────── |  - Handles MCP JSON-RPC protocol  |
|  - 15-second ping timeout         |                                   |  - Exposes 4 market data tools    |
|  - JVM shutdown hook cleanup      |                                   |  - Redirects logs to stderr       |
+───────────────────────────────────+                                   +───────────────────────────────────+
```

The 4 tools exposed dynamically:
1. `get_stock_info(symbol)`: Real-time price quotes, market capitalization, 52-week ranges, forward P/E.
2. `get_stock_actions(symbol)`: Historical dividend payouts and stock split events.
3. `get_financial_statement(symbol, statementType)`: Income statements, balance sheets, and cash flows.
4. `get_recommendations(symbol)`: Wall Street consensus ratings and target price ranges.

---

### 5.4 Enterprise Safety Perimeter & Defense-in-Depth

The safety architecture surrounds the model with five coordinated defense layers:

```
User Input ──► [Layer 1: BeforeAgentGuardrail] (Scrub PII & Halt Prompt Injection)
                       │
                       ▼
               [Layer 2: BeforeModelGuardrail] (Wire-Level Request Sanitization)
                       │
                       ▼
               [Layer 3: BeforeToolGuardrail / PreToolHook] (Block Trades & Validate Tickers)
                       │
                       ▼
               [Layer 4: AfterToolEvidenceCapture] (Store Verified Quotes in EvidenceStore)
                       │
                       ▼
               [Layer 5: AfterModelGuardrail / ResponseHook] (Enforce Disclaimers & Audit Numbers)
                       │
                       ▼
               Verified, Grounded Response to User
```

---

### 5.5 Correlation Spine & OpenTelemetry Tracing

Every transaction generates an immutable `executionId` (`exec-YYYYMMDD-<uuid8>`). This correlation identifier is passed through:
- **`ExecutionEvent`**: Strongly typed lifecycle records recording timestamps, component names, and durations.
- **`ExecutionTrace`**: Aggregated audit snapshot containing full causal streams, token usage, and status.
- **Persistence Storage**: Structured JSON trace files in `data/traces/` and rows in table `agent_execution_v10`.

---

## 6. Master Quick-Start Execution Matrix

| Version | Milestone Domain Focus | Core Google ADK Concept | CLI Launcher Script | Automated Test Command | Dedicated Reference Guide |
|:---:|---|---|---|---|:---:|
| **V1** | Foundational Analyst Persona | `LlmAgent`, `InMemoryRunner`, `SessionService`, `RunConfig` | `.\test-finance-v1.bat` | `./gradlew test --tests com.google.adk.finance.v1.FinanceV1IntegrationTest` | [`finance-agent-v1.md`](finance-agent-v1.md) |
| **V2** | SQLite State & Holdings Memory | Custom `BaseTool`, `toolContext.state()`, `{placeholder?}` templates | `.\test-finance-v2.bat` | `./gradlew test --tests "com.google.adk.finance.v2.*"` | [`finance-agent-v2.md`](finance-agent-v2.md) |
| **V3** | Deterministic Math & Search | `AgentTool.create(...)`, `GoogleSearchTool.INSTANCE`, Pure Java math | `.\test-finance-v3.bat` | `./gradlew test --tests "com.google.adk.finance.v3.*"` | [`finance-agent-v3.md`](finance-agent-v3.md) |
| **V4** | Domain Skills & Grounding Knowledge | `SkillToolset`, `LocalSkillSource`, Three-Tier Knowledge Hierarchy | `.\test-finance-v4.bat` | `./gradlew test --tests "com.google.adk.finance.v4.*"` | [`finance-agent-v4.md`](finance-agent-v4.md) |
| **V5** | Yahoo Finance MCP Server (STDIO) | `McpToolset`, `ServerParameters`, Dynamic MCP Tool Discovery | `.\test-finance-v5.bat` | `./gradlew test --tests com.google.adk.finance.v5.FinanceV5IntegrationTest` | [`finance-agent-v5.md`](finance-agent-v5.md) |
| **V6** | 5 Specialist Sub-Agents | Multi-Agent Delegation, Isolated Tool Ownership, Standardized Contract | `.\test-finance-v6.bat` | `./gradlew test --tests com.google.adk.finance.v6.FinanceV6IntegrationTest` | [`finance-agent-v6.md`](finance-agent-v6.md) |
| **V7** | Sequential, Parallel & Critic Pipelines| `SequentialAgent`, `ParallelAgent`, `LoopAgent`, `ExitLoopTool` | `.\test-finance-v7.bat` | `./gradlew test --tests com.google.adk.finance.v7.FinanceV7IntegrationTest` | [`finance-agent-v7.md`](finance-agent-v7.md) |
| **V8** | Safety Guardrails & Callbacks | `BeforeAgent`, `BeforeModel`, `BeforeTool`, `AfterTool`, `AfterModel` | `.\test-finance-v8.bat` | `./gradlew test --tests "com.google.adk.finance.v8.*"` | [`finance-agent-v8.md`](finance-agent-v8.md) |
| **V9** | Evaluation, Evidence & LLM Judge | `FaithfulnessEvaluator`, `CalculationFidelity`, Red-Teaming Failures | `.\test-finance-v9.bat` | `./gradlew test --tests "com.google.adk.finance.v9.*"` | [`finance-agent-v9.md`](finance-agent-v9.md) |
| **V10** | Tracing, Observability & Persistence | `executionId` spine, `ExecutionTrace`, JSON & SQLite WAL persistence | `.\test-finance-v10.bat` | `./gradlew test --tests "com.google.adk.finance.v10.*"` | [`finance-agent-v10.md`](finance-agent-v10.md) |
| **V11** | Scoped Rules & Programmatic Hooks | Scoped Markdown rules files, Blocking vs. Non-blocking Hook Policies | `.\test-finance-v11.bat` | `./gradlew test --tests "com.google.adk.finance.v11.*"` | [`finance-agent-v11.md`](finance-agent-v11.md) |

---

## 7. Testing & Verification Quick Reference

### 7.1 Interactive CLI Shortcuts Cheat Sheet

When running any interactive console launcher (`.\test-finance-v*.bat`), use these built-in inspection commands:

| Command | Available In | Action Performed |
|---|---|---|
| `help` | All Consoles | Displays available commands and keyboard shortcuts. |
| `state` | V2 to V11 | Prints live session state variables (`customer_id`, `portfolio_holdings`). |
| `skills` | V4, V5, V11 | Lists all dynamically loaded skills from `skills/finance/`. |
| `knowledge` | V4 | Lists curated reference frameworks available in `knowledge/`. |
| `mcp` / `tools`| V5 | Tests Yahoo Finance MCP connectivity and lists discovered tools. |
| `agents` | V6, V7 | Displays the active multi-agent hierarchy and specialist roles. |
| `/cases` | V9 | Lists standardized golden benchmark test cases. |
| `/eval <id>`| V9 | Runs the evaluation harness against a specific golden case (e.g. `/eval GOLDEN-001`). |
| `/fail <id>`| V9 | Runs an adversarial failure probe (e.g. `/fail PII_LEAK` or `/fail TRADE_BLOCK`). |
| `history` | V10 | Displays recent execution traces stored in SQLite. |
| `trace <id>` | V10 | Inspects full causal telemetry event stream for a specific execution ID. |
| `rules` | V11 | Displays active scoped Markdown rules loaded for the current agent. |
| `hooks` | V11 | Displays registered lifecycle hooks and their blocking/non-blocking policies. |
| `telemetry` | V11 | Displays real-time observation metrics captured by `PostToolObservationHook`. |
| `exit` / `quit`| All Consoles | Gracefully closes the session, terminates MCP child processes, and exits. |

---

### 7.2 Running All Automated Verification Tests

To verify the entire Finance Portfolio Agent test suite across all versions in a single command:

```bash
# Windows
.\gradlew.bat test --tests "com.google.adk.finance.*"

# Linux / macOS
./gradlew test --tests "com.google.adk.finance.*"
```

---

## 8. Documentation Sitemap & Repository Directory

```
docs/
├── finance-agent-intro.md   ◄── [YOU ARE HERE: Master Index & Architectural Overview]
├── finance-agent-v1.md      ◄── Foundational Agent & Session Management
├── finance-agent-v2.md      ◄── SQLite Relational State & Context Injection
├── finance-agent-v3.md      ◄── Deterministic Java Math & Grounded Google Search
├── finance-agent-v4.md      ◄── Domain Skills & Curated Project Knowledge
├── finance-agent-v5.md      ◄── Model Context Protocol (MCP) STDIO Integration
├── finance-agent-v6.md      ◄── Specialist Multi-Agent System & Division of Labor
├── finance-agent-v7.md      ◄── Deterministic Workflow Orchestration (Sequential, Parallel, Loop)
├── finance-agent-v8.md      ◄── Perimeter Safety Guardrails & Lifecycle Callbacks
├── finance-agent-v9.md      ◄── Systematic Evaluation Harness & Adversarial Failure Testing
├── finance-agent-v10.md     ◄── Observability, Tracing, Metrics & Telemetry Persistence
└── finance-agent-v11.md     ◄── Rules-Driven & Hook-Aware Multi-Agent Architecture
```

### Essential Repository Resources:
- [`README.md`](../README.md): High-level repository overview, quickstart instructions, and dual-domain architecture.
- [`AGENTS.md`](../AGENTS.md): Code-level implementation reference and dual-domain architectural blueprint.
- [`skills/finance/`](../skills/finance/): Modular Markdown domain procedures for corporate finance, valuation, and analysis.
- [`knowledge/`](../knowledge/): Curated grounding documents for ratios, multiples, and risk frameworks.
- [`src/main/resources/v11/rules/`](../src/main/resources/v11/rules/): Persistent Markdown rules files scoped to V11 agents.
