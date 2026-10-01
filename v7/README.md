# Finance Advisor V7 — Workflow Orchestration

This directory documents **Finance Advisor V7**, which introduces **deterministic workflow orchestration** into the Finance Advisor application using the official **Google Agent Development Kit (ADK) for Java** (`com.google.adk:google-adk:1.4.0`).

---

## 1. Directory Structure

```
v7/
├── README.md                          <- Main architectural documentation for V7
│
├── workflows/
│   ├── sequential/
│   │   └── README.md                  <- Specification of Sequential Research Pipeline
│   ├── parallel/
│   │   └── README.md                  <- Specification of Parallel Fan-Out/In Workflow & Partial Failure
│   └── loop/
│       └── README.md                  <- Specification of Iterative Research-Critic Loop & ExitLoopTool
│
├── agents/
│   └── README.md                      <- Specification of Root Orchestrator (FinanceAdvisorAgentV7)
│
└── sub-agents/
    └── README.md                      <- Catalog of 10 specialized sub-agents across the 3 workflows

Java Source Implementations (Package: com.google.adk.finance.v7):
src/main/java/com/google/adk/finance/v7/
├── FinanceAdvisorAgentV7.java         <- Root Orchestrator Agent (workflow_director)
├── FinanceConsoleV7.java              <- Interactive CLI Runner with Workflow Observability
│
├── workflows/
│   ├── sequential/
│   │   └── InvestmentResearchSequentialWorkflowV7.java  <- 5-Stage Sequential Pipeline (SequentialAgent)
│   ├── parallel/
│   │   └── PortfolioParallelResearchWorkflowV7.java     <- Concurrent Fan-Out / Fan-In with failure isolation
│   └── loop/
│       └── ResearchCriticLoopWorkflowV7.java            <- Iterative Critic Loop (LoopAgent + ExitLoopTool)
│
└── subagents/
    ├── CompanyResearchAgentV7.java             <- Stage 1: Google Search grounding
    ├── FundamentalAnalysisAgentV7.java         <- Stage 2: Yahoo Finance MCP fundamentals
    ├── RiskAnalysisAgentV7.java                <- Stage 3: Volatility, Beta & Leverage
    ├── ValuationAnalysisAgentV7.java           <- Stage 4: Grounded valuation multiples
    ├── SequentialReportSynthesisAgentV7.java   <- Stage 5: Institutional 7-section report synthesis
    ├── CompanyParallelResearchWorkerV7.java    <- Parallel worker for concurrent company research
    ├── ParallelPortfolioComparisonAgentV7.java <- Fan-in comparison aggregator with failure audit
    ├── ReportDraftingAgentV7.java              <- Author/reviser in iterative critic loop
    ├── ComplianceEvidenceCriticAgentV7.java    <- Quality critic with ExitLoopTool termination
    └── FinalReportPresenterAgentV7.java        <- Final presenter of audited report and verification seal

Integration Test Suite:
src/test/java/com/google/adk/finance/v7/
└── FinanceV7IntegrationTest.java      <- Comprehensive test suite validating all 3 workflows & failure isolation
```

---

## 2. Learning Progression: V1 through V7

| Version | Focus | Core Concept |
|---|---|---|
| **V1** | Baseline Agent | `LlmAgent`, `InMemoryRunner`, `SessionService` |
| **V2** | Context & State | SQLite persistence, `LoadCustomerPortfolioTool`, prompt templating |
| **V3** | Tools & Search | `PortfolioMathTool`, `stockmarket_researcher` via `AgentTool` |
| **V4** | Skills & Knowledge | `SkillToolset`, `ProjectKnowledgeTool`, curated valuation/risk markdown |
| **V5** | MCP Integration | Yahoo Finance MCP Server over stdio, `McpToolset` dynamic tool discovery |
| **V6** | Sub-Agents | Dynamic multi-agent delegation: parent LLM selects from 5 specialists |
| **V7** | **Workflow Orchestration** | **Deterministic pipelines: (1) Sequential 5-stage pipeline, (2) Parallel fan-out/fan-in with failure isolation, (3) Iterative critic loop with `ExitLoopTool` termination.** |

### The Core Shift: V6 vs V7

- **In V6 (Dynamic Delegation)**:
  ```
       Parent Agent (portfolio_director)
             │
             ├──> Sub-Agent A (Decided by LLM turn-by-turn)
             ├──> Sub-Agent B (Decided by LLM turn-by-turn)
             └──> Sub-Agent C (Decided by LLM turn-by-turn)
  ```
  The parent LLM dynamically guesses which specialist to call.

- **In V7 (Deterministic Workflow Orchestration)**:
  ```
       Deterministic Workflow Engine
             │
             ├──> Step A (Strictly precedes Step B)
             │
             ├──> Step B (Consumes Step A context via outputKey)
             │
             └──> Step C (Synthesizes final institutional brief)
  ```
  Execution order, concurrency, and repetition are governed deterministically by code structures (`SequentialAgent`, `ParallelAgent`, `LoopAgent`), eliminating model guessing.

---

## 3. Quickstart

Run the interactive CLI console:
```bash
# Windows
test-finance-v7.bat

# Linux / macOS
./test-finance-v7.sh
```

Run V7 integration tests:
```bash
gradlew test --tests com.google.adk.finance.v7.FinanceV7IntegrationTest
```
