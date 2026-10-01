# Workflow 3 — Iterative Research-Critic Loop (`ResearchCriticLoopWorkflowV7`)

- **Java Implementation**: [`ResearchCriticLoopWorkflowV7.java`](../../../src/main/java/com/google/adk/finance/v7/workflows/loop/ResearchCriticLoopWorkflowV7.java)
- **ADK Core Classes**: `com.google.adk.agents.LoopAgent`, `com.google.adk.tools.ExitLoopTool`, `com.google.adk.agents.SequentialAgent`

---

## 1. Architectural Topology

```
User Prompt: "Create an investment research report on Infosys and ensure important factual claims are supported by evidence."
                                             │
                                             ▼
                               +───────────────────────────+
                               |     SequentialAgent       |
                               +─────────────┬─────────────+
                                             │
                                             ▼
                              ┌─────────────────────────────┐
                              │  LoopAgent (maxIterations=3)│
                              │                             │
                              │  +───────────────────────+  │
                              │  |  Report Drafting Agent|  │  Reads: {critic_feedback?}
                              │  |  (Generates / Revises)|  │  Writes: current_draft_report
                              │  +───────────┬───────────+  │
                              │              │              │
                              │              ▼              │
                              │  +───────────────────────+  │
                              │  |  Compliance Critic    |  │  Audits: Factual grounding,
                              │  |  (ExitLoopTool)       |  │  risks, and disclaimer.
                              │  +───────────┬───────────+  │
                              │              │              │
                              │         Approved?           │
                              │         /        \          │
                              │       YES         NO        │
                              │        │           │        │
                              │   exit_loop()   Revisions   │
                              │        │         feedback   │
                              │        │           │        │
                              │        │           v        │
                              │        │      (Repeat Loop) │
                              └────────┼────────────────────┘
                                       │
                                       ▼
                         +───────────────────────────+
                         | Final Report Presenter    |
                         | (Verified Report + Audit) |
                         +───────────────────────────+
```

---

## 2. Dynamic Stopping with `ExitLoopTool`

1. **Approval Pathway**: When the draft report satisfies all empirical grounding, risk balancing, and regulatory disclaimer criteria, `ComplianceEvidenceCriticAgentV7` calls `ExitLoopTool.INSTANCE` (`exit_loop`). This sets `escalate(true)` on `toolContext.actions()`, causing `LoopAgent` to terminate.
2. **Revision Pathway**: If any criteria fail, the critic does not call `exit_loop`, but writes actionable directives to `critic_feedback`. The loop iterates back to `ReportDraftingAgentV7`.
3. **Safety Bound**: Configured with `maxIterations(3)` to prevent infinite loops.

---

## 3. Participating Agents & Tools

| Component | Class | Package | Purpose |
|---|---|---|---|
| **Workflow Tool** | [`RunCriticLoopWorkflowTool`](../../../src/main/java/com/google/adk/finance/v7/tools/RunCriticLoopWorkflowTool.java) | `com.google.adk.finance.v7.tools` | Workflow invocation wrapper called by `FinanceAdvisorAgentV7` |
| **Authoring Agent** | [`ReportDraftingAgentV7`](../../../src/main/java/com/google/adk/finance/v7/subagents/ReportDraftingAgentV7.java) | `com.google.adk.finance.v7.subagents` | Generates initial draft or incorporates critic feedback |
| **Quality Critic** | [`ComplianceEvidenceCriticAgentV7`](../../../src/main/java/com/google/adk/finance/v7/subagents/ComplianceEvidenceCriticAgentV7.java) | `com.google.adk.finance.v7.subagents` | Evaluates evidence and disclaimers; triggers `exit_loop` |
| **Presenter Agent** | [`FinalReportPresenterAgentV7`](../../../src/main/java/com/google/adk/finance/v7/subagents/FinalReportPresenterAgentV7.java) | `com.google.adk.finance.v7.subagents` | Emits verified report with compliance audit trail |
| **Shared Math** | [`PortfolioMathTool`](../../../src/main/java/com/google/adk/finance/tools/PortfolioMathTool.java) | `com.google.adk.finance.tools` | Deterministic financial math calculations |
| **Shared Knowledge** | [`ProjectKnowledgeTool`](../../../src/main/java/com/google/adk/finance/tools/ProjectKnowledgeTool.java) | `com.google.adk.finance.tools` | Curated grounding principles (`knowledge/`) |
| **ADK Loop Control** | `ExitLoopTool` | `com.google.adk.tools` | Official ADK tool setting `escalate(true)` to break loop |
