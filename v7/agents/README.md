# Root Orchestrator: FinanceAdvisorAgentV7 (`workflow_director`)

- **Java Implementation**: [`FinanceAdvisorAgentV7.java`](../../src/main/java/com/google/adk/finance/v7/FinanceAdvisorAgentV7.java)
- **Role Name**: `workflow_director`
- **Agent Name**: `finance_advisor_v7`

---

## 1. Architectural Responsibility

The root `FinanceAdvisorAgentV7` is responsible for:
1. **Inquiry Classification & Workflow Routing**: Analyzing user intent and selecting among the 3 deterministic workflow engines:
   - `run_sequential_research_workflow`
   - `run_parallel_portfolio_research_workflow`
   - `run_critic_loop_research_workflow`
2. **Context & Persistence Management**: Loading customer portfolio holdings via `load_customer_portfolio` and performing exact arithmetic via `portfolio_math`.
3. **Institutional Quality & Compliance**: Ensuring all outputs include evidence grounding and the mandatory regulatory disclaimer.

---

## 2. Tools Registered

| Tool Name | Class | Package | Purpose |
|---|---|---|---|
| `run_sequential_research_workflow` | [`RunSequentialWorkflowTool`](../../src/main/java/com/google/adk/finance/v7/tools/RunSequentialWorkflowTool.java) | `com.google.adk.finance.v7.tools` | Launches 5-stage sequential research pipeline |
| `run_parallel_portfolio_research_workflow` | [`RunParallelWorkflowTool`](../../src/main/java/com/google/adk/finance/v7/tools/RunParallelWorkflowTool.java) | `com.google.adk.finance.v7.tools` | Launches concurrent fan-out / fan-in multi-company research |
| `run_critic_loop_research_workflow` | [`RunCriticLoopWorkflowTool`](../../src/main/java/com/google/adk/finance/v7/tools/RunCriticLoopWorkflowTool.java) | `com.google.adk.finance.v7.tools` | Launches iterative authoring & compliance critic loop |
| `load_customer_portfolio` | [`LoadCustomerPortfolioTool`](../../src/main/java/com/google/adk/finance/tools/LoadCustomerPortfolioTool.java) | `com.google.adk.finance.tools` | Queries SQLite holdings and sets session state |
| `portfolio_math` | [`PortfolioMathTool`](../../src/main/java/com/google/adk/finance/tools/PortfolioMathTool.java) | `com.google.adk.finance.tools` | Deterministic PnL, allocation, and concentration calculations |
| `read_project_knowledge` | [`ProjectKnowledgeTool`](../../src/main/java/com/google/adk/finance/tools/ProjectKnowledgeTool.java) | `com.google.adk.finance.tools` | Reads curated valuation, risk, and portfolio markdown guidelines |
