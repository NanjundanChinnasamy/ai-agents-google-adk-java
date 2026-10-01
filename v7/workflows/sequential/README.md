# Workflow 1 — Sequential Research Pipeline (`InvestmentResearchSequentialWorkflowV7`)

- **Java Implementation**: [`InvestmentResearchSequentialWorkflowV7.java`](../../../src/main/java/com/google/adk/finance/v7/workflows/sequential/InvestmentResearchSequentialWorkflowV7.java)
- **ADK Core Class**: `com.google.adk.agents.SequentialAgent`

---

## 1. Architectural Topology

```
                  User Prompt: "Prepare a structured investment research report on Infosys."
                                             │
                                             ▼
                               +───────────────────────────+
                               |     SequentialAgent       |
                               | (5 Deterministic Stages)  |
                               +─────────────┬─────────────+
                                             │
                                             ▼
                               +───────────────────────────+
                               | Stage 1: Company Research |  Output: company_research_output
                               | Tool: GoogleSearchTool    |  (Recent news, management changes, catalysts)
                               +─────────────┬─────────────+
                                             │
                                             ▼
                               +───────────────────────────+
                               | Stage 2: Fundamentals     |  Output: fundamental_analysis_output
                               | Tool: Yahoo Finance MCP   |  (Revenue, margins, balance sheet solvency)
                               +─────────────┬─────────────+
                                             │
                                             ▼
                               +───────────────────────────+
                               | Stage 3: Risk Analysis    |  Output: risk_analysis_output
                               | Tools: MCP + Math + Skill |  (Market Beta β, 52-week spread, leverage)
                               +─────────────┬─────────────+
                                             │
                                             ▼
                               +───────────────────────────+
                               | Stage 4: Valuation        |  Output: valuation_analysis_output
                               | Tool: MCP (Grounded P/E)  |  (Trailing/Forward P/E, PEG, EV/EBITDA)
                               +─────────────┬─────────────+
                                             │
                                             ▼
                               +───────────────────────────+
                               | Stage 5: Synthesis        |  Output: investment_research_report
                               | Institutional 7-Section   |  (Grounded synthesis & mandatory disclaimer)
                               +───────────────────────────+
```

---

## 2. ADK Context Chaining Pattern

In Google ADK Java, stages communicate downstream using explicit state keys:
1. Stage 1 configures `.outputKey("company_research_output")`.
2. Stage 2 includes `{company_research_output?}` in its `.instruction(...)` and configures `.outputKey("fundamental_analysis_output")`.
3. Stage 3 includes both `{company_research_output?}` and `{fundamental_analysis_output?}`.
4. Stage 4 includes `{fundamental_analysis_output?}` and `{risk_analysis_output?}`.
5. Stage 5 includes all 4 previous outputs and produces the final institutional report.

This guarantees deterministic pipeline dependencies without any model guessing.
