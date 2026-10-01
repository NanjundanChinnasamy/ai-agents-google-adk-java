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
