# Finance Advisor V7 — Workflow Orchestration (`finance.v7`)

> **Milestone 7 Objective**: Introduce **deterministic workflow orchestration** into the Finance Advisor application using Google ADK Java. Learn how multiple agents and sub-agents are orchestrated through deterministic workflow structures (Sequential Pipelines, Parallel Fan-Out/Fan-In, and Iterative Critic Loops) rather than relying only on the parent LLM to dynamically guess what to do.

---

## 1. High-Level Workflow Topologies

```
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                    FINANCE ADVISOR V7                                       |
|                               (com.google.adk.finance.v7)                                   |
+──────────────────────────────────────────────┬──────────────────────────────────────────────+
                                               │
               ┌───────────────────────────────┼───────────────────────────────┐
               ▼                               ▼                               ▼
+─────────────────────────────+ +─────────────────────────────+ +─────────────────────────────+
|     WORKFLOW 1: SEQUENTIAL  | |     WORKFLOW 2: PARALLEL    | |      WORKFLOW 3: LOOP       |
|                             | |                             | |                             |
| Stage 1: Company Research   | |           Portfolio         | |       Generate Report       |
|    (GoogleSearchTool)       | |               │             | |              ↓              |
|            ↓                | |            FAN-OUT          | | Compliance / Evidence Critic|
| Stage 2: Fundamentals       | |         /     |     \       | |              ↓              |
|    (Yahoo Finance MCP)      | |        v      v      v      | |          Approved?          |
|            ↓                | |     Infosys  HDFC   Reliance| |          /       \          |
| Stage 3: Risk Analysis      | |     Worker  Worker   Worker | |        YES        NO        |
|    (Beta & Volatility)      | |        \      |      /      | |         │          │        |
|            ↓                | |         v     v     v       | |         v          v        |
| Stage 4: Valuation          | |            FAN-IN           | |    exit_loop()   Revise Draft|
|    (Grounded Multiples)     | |               │             | |         │          │        |
|            ↓                | |       Comparison Report     | |         v          v        |
| Stage 5: Synthesis Report   | | (Partial Failure Resilient) | |     Final Report Critic Loop|
+─────────────────────────────+ +─────────────────────────────+ +─────────────────────────────+
```

---

## 2. Detailed Workflow Specifications

### Workflow 1: Sequential Investment Research Pipeline (`InvestmentResearchSequentialWorkflowV7`)
- **Package**: `com.google.adk.finance.v7.workflows.sequential`
- **ADK Core Class**: `SequentialAgent`
- **Use Case**: *"Prepare a structured investment research report on Infosys."*
- **Execution Model**: Strictly linear execution where output of Stage $N$ is published to `outputKey` and consumed by Stage $N+1$ via `{outputKey}` prompt bindings:
  1. `company_research_step` publishes to `company_research_output`.
  2. `fundamental_analysis_step` reads `{company_research_output?}` and publishes to `fundamental_analysis_output`.
  3. `risk_analysis_step` reads both and publishes to `risk_analysis_output`.
  4. `valuation_analysis_step` evaluates grounded multiples without hallucinating missing values and publishes to `valuation_analysis_output`.
  5. `sequential_synthesis_step` produces the structured 7-section institutional brief with the mandatory disclaimer.

---

### Workflow 2: Parallel Portfolio Research Fan-Out / Fan-In (`PortfolioParallelResearchWorkflowV7`)
- **Package**: `com.google.adk.finance.v7.workflows.parallel`
- **ADK Core Classes**: `ParallelAgent`, `SequentialAgent`
- **Use Case**: *"Analyse Infosys, HDFC Bank and Reliance and give me a comparable research summary."*
- **Execution Model**:
  - Independent company research workers execute concurrently across threads (`CompletableFuture.allOf` / Virtual Threads / RxJava).
  - Actual concurrency logging verifies non-blocking execution:
    ```
    [PARALLEL] Starting Infosys research
    [PARALLEL] Starting HDFC Bank research
    [PARALLEL] Starting Reliance research
    [PARALLEL] Infosys research completed
    [PARALLEL] HDFC Bank research completed
    [PARALLEL] Reliance research completed
    ```
  - **Partial Failure Resilience**: If one stock fails (e.g. invalid symbol or network error), the fan-in aggregator still produces the comparative brief, explicitly identifying:
    - Successful analyses
    - Failed analyses & error reasons
    - Missing information
    - Analytical limitations
    - Never fabricates missing company figures.

---

### Workflow 3: Iterative Research-Critic Loop (`ResearchCriticLoopWorkflowV7`)
- **Package**: `com.google.adk.finance.v7.workflows.loop`
- **ADK Core Classes**: `LoopAgent`, `ExitLoopTool`, `SequentialAgent`
- **Use Case**: *"Create an investment research report on Infosys and ensure important factual claims are supported by evidence."*
- **Execution Model**:
  - `report_drafter` generates or revises the report.
  - `compliance_evidence_critic` audits the draft for empirical grounding, balanced risks, and regulatory compliance.
  - If approved, the critic invokes `exit_loop`, setting `escalate(true)` and terminating the loop.
  - If rejected, the critic leaves `exit_loop` uncalled and emits revision directives for the next iteration.
  - Safety bound with `maxIterations(3)`.

---

## 3. Directory Layout

```
src/main/java/com/google/adk/finance/v7/
├── FinanceAdvisorAgentV7.java
├── FinanceConsoleV7.java
├── workflows/
│   ├── sequential/
│   │   └── InvestmentResearchSequentialWorkflowV7.java
│   ├── parallel/
│   │   └── PortfolioParallelResearchWorkflowV7.java
│   └── loop/
│       └── ResearchCriticLoopWorkflowV7.java
└── subagents/
    ├── CompanyResearchAgentV7.java
    ├── FundamentalAnalysisAgentV7.java
    ├── RiskAnalysisAgentV7.java
    ├── ValuationAnalysisAgentV7.java
    ├── SequentialReportSynthesisAgentV7.java
    ├── CompanyParallelResearchWorkerV7.java
    ├── ParallelPortfolioComparisonAgentV7.java
    ├── ReportDraftingAgentV7.java
    ├── ComplianceEvidenceCriticAgentV7.java
    └── FinalReportPresenterAgentV7.java
```

---

## 4. Verification & Testing

Launch interactive console:
```bash
# Windows
test-finance-v7.bat

# Linux / macOS
./test-finance-v7.sh
```

Execute integration tests:
```bash
gradlew test --tests com.google.adk.finance.v7.FinanceV7IntegrationTest
```
