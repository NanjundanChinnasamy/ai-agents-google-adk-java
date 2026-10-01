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
