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

| Tool Name | Class | Purpose |
|---|---|---|
| `run_sequential_research_workflow` | `RunSequentialWorkflowTool` | Launches 5-stage sequential research pipeline |
| `run_parallel_portfolio_research_workflow` | `RunParallelWorkflowTool` | Launches concurrent fan-out / fan-in multi-company research |
| `run_critic_loop_research_workflow` | `RunCriticLoopWorkflowTool` | Launches iterative authoring & compliance critic loop |
| `load_customer_portfolio` | `LoadCustomerPortfolioTool` | Queries SQLite holdings and sets session state |
| `portfolio_math` | `PortfolioMathTool` | Deterministic PnL, allocation, and concentration calculations |
| `read_project_knowledge` | `ProjectKnowledgeTool` | Reads curated valuation, risk, and portfolio markdown guidelines |
