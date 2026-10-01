# Specialized Sub-Agents Catalog (Finance Advisor V7)

Finance Advisor V7 organizes its division of labor across **10 specialized sub-agents** grouped by workflow:

---

## 1. Sequential Workflow Sub-Agents (`workflows/sequential/`)

| Specialist Sub-Agent | Class | Responsibility | Tools Used | Output Key |
|---|---|---|---|---|
| **Company Research** | [`CompanyResearchAgentV7.java`](../../src/main/java/com/google/adk/finance/v7/subagents/CompanyResearchAgentV7.java) | Real-time web developments, announcements, C-suite changes | `GoogleSearchTool.INSTANCE` | `company_research_output` |
| **Fundamental Analysis** | [`FundamentalAnalysisAgentV7.java`](../../src/main/java/com/google/adk/finance/v7/subagents/FundamentalAnalysisAgentV7.java) | Quantitative company financials, margins, debt/equity, statements | Yahoo Finance MCP (`get_stock_info`, `get_financial_statement`) + Skills + Knowledge | `fundamental_analysis_output` |
| **Risk Analysis** | [`RiskAnalysisAgentV7.java`](../../src/main/java/com/google/adk/finance/v7/subagents/RiskAnalysisAgentV7.java) | Systematic market beta, 52-week spread, leverage risk | Yahoo Finance MCP + `PortfolioMathTool` + Skills | `risk_analysis_output` |
| **Valuation Analysis** | [`ValuationAnalysisAgentV7.java`](../../src/main/java/com/google/adk/finance/v7/subagents/ValuationAnalysisAgentV7.java) | Grounded multiples (P/E, PEG, EV/EBITDA); no hallucinated figures | Yahoo Finance MCP (`get_stock_info`) + Skills | `valuation_analysis_output` |
| **Report Synthesis** | [`SequentialReportSynthesisAgentV7.java`](../../src/main/java/com/google/adk/finance/v7/subagents/SequentialReportSynthesisAgentV7.java) | Synthesizes all 4 upstream outputs into 7-section institutional report | Knowledge + Skills | `investment_research_report` |

---

## 2. Parallel Workflow Sub-Agents (`workflows/parallel/`)

| Specialist Sub-Agent | Class | Responsibility | Tools Used | Output Key |
|---|---|---|---|---|
| **Parallel Research Worker** | [`CompanyParallelResearchWorkerV7.java`](../../src/main/java/com/google/adk/finance/v7/subagents/CompanyParallelResearchWorkerV7.java) | Concurrently executes company profile, MCP fundamentals, and risk | Yahoo Finance MCP + `PortfolioMathTool` + Knowledge | `parallel_research_<symbol>` |
| **Portfolio Comparator (Fan-In)** | [`ParallelPortfolioComparisonAgentV7.java`](../../src/main/java/com/google/adk/finance/v7/subagents/ParallelPortfolioComparisonAgentV7.java) | Fan-In aggregator producing comparative matrix and auditing partial failures | Knowledge + Skills | `portfolio_comparison_report` |

---

## 3. Loop Workflow Sub-Agents (`workflows/loop/`)

| Specialist Sub-Agent | Class | Responsibility | Tools Used | Output Key |
|---|---|---|---|---|
| **Report Drafter** | [`ReportDraftingAgentV7.java`](../../src/main/java/com/google/adk/finance/v7/subagents/ReportDraftingAgentV7.java) | Drafts initial report or incorporates `{critic_feedback?}` iteratively | Yahoo Finance MCP + Google Search + Knowledge | `current_draft_report` |
| **Compliance Critic** | [`ComplianceEvidenceCriticAgentV7.java`](../../src/main/java/com/google/adk/finance/v7/subagents/ComplianceEvidenceCriticAgentV7.java) | Evaluates grounding, balance, disclaimer; triggers `exit_loop()` when approved | `ExitLoopTool.INSTANCE` + Knowledge | `critic_feedback` |
| **Report Presenter** | [`FinalReportPresenterAgentV7.java`](../../src/main/java/com/google/adk/finance/v7/subagents/FinalReportPresenterAgentV7.java) | Presents approved investment report alongside compliance audit stamp | - | `final_verified_report` |
