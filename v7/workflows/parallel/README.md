# Workflow 2 — Parallel Fan-Out / Fan-In (`PortfolioParallelResearchWorkflowV7`)

- **Java Implementation**: [`PortfolioParallelResearchWorkflowV7.java`](../../../src/main/java/com/google/adk/finance/v7/workflows/parallel/PortfolioParallelResearchWorkflowV7.java)
- **Concurrency Safety Wrapper**: [`ThreadSafeMcpToolset.java`](../../../src/main/java/com/google/adk/finance/v7/workflows/parallel/ThreadSafeMcpToolset.java)
- **ADK Core Classes**: `com.google.adk.agents.ParallelAgent`, `com.google.adk.agents.SequentialAgent`

---

## 1. Architectural Topology

```
User Prompt: "Analyse Infosys, HDFC Bank and Reliance and give me a comparable research summary."
                                             │
                                             ▼
                               +───────────────────────────+
                               |     FAN-OUT EXECUTION     |
                               +─────────────┬─────────────+
                                             │
                 ┌───────────────────────────┼───────────────────────────┐
                 │                           │                           │
                 ▼                           ▼                           ▼
    +─────────────────────────+ +─────────────────────────+ +─────────────────────────+
    |  Infosys Research Task  | | HDFC Bank Research Task | | Reliance Research Task  |
    |  - Business overview    | | - Business overview     | | - Business overview     |
    |  - MCP Fundamentals     | | - MCP Fundamentals      | | - MCP Fundamentals      |
    |  - Beta & Volatility    | | - Beta & Volatility     | | - Beta & Volatility     |
    +────────────┬────────────+ +────────────┬────────────+ +────────────┬────────────+
                 │                           │                           │
                 └───────────────────────────┼───────────────────────────┘
                                             │
                                             ▼
                               +───────────────────────────+
                               |      FAN-IN JOIN GATE     |
                               | (CompletableFuture.allOf) |
                               +─────────────┬─────────────+
                                             │
                                             ▼
                               +───────────────────────────+
                               | Parallel Portfolio        |
                               | Comparison Agent          |
                               | - Execution Audit (Pass/F)|
                               | - Comparative Matrix      |
                               | - Limitations & Scope     |
                               | - Mandatory Disclaimer    |
                               +───────────────────────────+
```

---

## 2. Concurrency Implementation & Real Logging

Tasks execute concurrently rather than sequentially. Real event logging records:
```
[PARALLEL] Starting Infosys research
[PARALLEL] Starting HDFC Bank research
[PARALLEL] Starting Reliance research
[PARALLEL] Infosys research completed
[PARALLEL] HDFC Bank research completed
[PARALLEL] Reliance research completed
```

---

## 3. Resilient Partial Failure Handling

If a task fails (e.g. invalid ticker, network timeout):
```
[PARALLEL] Starting Infosys research
[PARALLEL] Starting FAIL research
[PARALLEL] Infosys research completed
[PARALLEL] FAIL research FAILED: Simulated connection failure / data unavailable
```

The Fan-In aggregator synthesizes:
1. **Successful Analyses**: Infosys
2. **Failed Analyses**: FAIL (Simulated connection failure)
3. **Missing Information**: Quantitative metrics omitted for failed companies.
4. **Data Integrity Rule**: Missing company data is NEVER fabricated.

---

## 4. Participating Agents & Tools

| Component | Class | Package | Purpose |
|---|---|---|---|
| **Workflow Tool** | [`RunParallelWorkflowTool`](../../../src/main/java/com/google/adk/finance/v7/tools/RunParallelWorkflowTool.java) | `com.google.adk.finance.v7.tools` | Workflow invocation wrapper called by `FinanceAdvisorAgentV7` |
| **Worker Sub-Agent** | [`CompanyParallelResearchWorkerV7`](../../../src/main/java/com/google/adk/finance/v7/subagents/CompanyParallelResearchWorkerV7.java) | `com.google.adk.finance.v7.subagents` | Concurrent company researcher executed in parallel tasks |
| **Comparator Sub-Agent** | [`ParallelPortfolioComparisonAgentV7`](../../../src/main/java/com/google/adk/finance/v7/subagents/ParallelPortfolioComparisonAgentV7.java) | `com.google.adk.finance.v7.subagents` | Fan-in aggregator synthesizing comparative matrices and auditing failures |
| **Concurrency Wrapper** | [`ThreadSafeMcpToolset`](../../../src/main/java/com/google/adk/finance/v7/workflows/parallel/ThreadSafeMcpToolset.java) | `com.google.adk.finance.v7.workflows.parallel` | Synchronized MCP tool wrapper ensuring thread safety across concurrent workers |
| **Shared Math** | [`PortfolioMathTool`](../../../src/main/java/com/google/adk/finance/tools/PortfolioMathTool.java) | `com.google.adk.finance.tools` | Deterministic PnL, allocation weights & concentration risk |
| **Shared Knowledge** | [`ProjectKnowledgeTool`](../../../src/main/java/com/google/adk/finance/tools/ProjectKnowledgeTool.java) | `com.google.adk.finance.tools` | Curated grounding principles (`knowledge/`) |
