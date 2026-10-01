# Specialized Sub-Agents Specification (Finance Advisor V6)

All V6 sub-agents have narrow, explicitly scoped responsibilities, isolated toolsets, and standardized reporting contracts.

---

## 1. Specialist Sub-Agent Breakdown

### 1. MarketResearchAgentV6 (`stockmarket_researcher` / `market_research_agent`)
- **Java Class**: [`MarketResearchAgentV6.java`](../../src/main/java/com/google/adk/finance/v6/subagents/MarketResearchAgentV6.java)
- **Agent Name**: `stockmarket_researcher` (Alias: `market_research_agent`)
- **Output Key**: `market_research_report`
- **Primary Tool**: `GoogleSearchTool.INSTANCE`
- **Responsibility**: "Investigate what is happening now and identify why asset prices changed."
- **Capabilities**: Real-time breaking corporate news, quarterly earnings surprises, C-suite transitions, M&A announcements, regulatory inquiries, analyst upgrades/downgrades, and macro catalysts using the structured 8-stage market research framework.
- **API Resolution**: Resolves the Gemini API constraint prohibiting client function declarations from mixing with server-side Google Search within the same agent step by isolating search grounding in its own sub-agent turn.

### 2. ScenarioAnalystAgentV6 (`scenario_analyst`)
- **Java Class**: [`ScenarioAnalystAgentV6.java`](../../src/main/java/com/google/adk/finance/v6/subagents/ScenarioAnalystAgentV6.java)
- **Agent Name**: `scenario_analyst`
- **Output Key**: `scenario_analysis_report`
- **Primary Tools**: Yahoo Finance MCP (`get_stock_info`), `PortfolioMathTool` (allocation & return math), `ProjectKnowledgeTool` (portfolio principles & risk framework), `SkillToolset`
- **Responsibility**: "Model forward-looking macro scenarios and stress-test portfolio allocations."
- **Capabilities**: Quantitative modeling of Baseline, Bull, and Bear cases; evaluating portfolio sensitivity against interest rate shocks, tech corrections, and multiple compression; stress-testing single-stock concentration hazards (>25%).

### 3. ReportWriterAgentV6 (`report_writer`)
- **Java Class**: [`ReportWriterAgentV6.java`](../../src/main/java/com/google/adk/finance/v6/subagents/ReportWriterAgentV6.java)
- **Agent Name**: `report_writer`
- **Output Key**: `executive_decision_report`
- **Primary Tools**: `ProjectKnowledgeTool`, `SkillToolset`
- **Responsibility**: "Synthesize multi-agent research notes into an institutional decision-support report."
- **Capabilities**: Formats triangulated findings into a 6-part institutional brief:
  1. Executive Summary & Core Strategic Thesis
  2. Empirical Evidence Matrix (News catalysts, quarterly earnings, operating margins, solvency)
  3. Comparative Macro Scenarios (Baseline, Bull, Bear outcomes)
  4. Risk Taxonomy & Trade-Offs (Beta, leverage, concentration flags)
  5. Rebalancing & Portfolio Considerations (Non-discretionary)
  6. Mandatory Regulatory Compliance Disclaimer

### 4. FundamentalAnalysisAgentV6 (`fundamental_analysis_agent`)
- **Java Class**: [`FundamentalAnalysisAgentV6.java`](../../src/main/java/com/google/adk/finance/v6/subagents/FundamentalAnalysisAgentV6.java)
- **Agent Name**: `fundamental_analysis_agent`
- **Output Key**: `fundamental_analysis_report`
- **Primary Tools**: Yahoo Finance MCP tools (`get_stock_info`, `get_financial_statement`, `get_stock_actions`, `get_recommendations`), `ProjectKnowledgeTool`, `SkillToolset`
- **Responsibility**: "Analyse structured company fundamentals and financial statements."
- **Capabilities**: Empirical valuation multiples (trailing/forward P/E, PEG, P/B, EV/EBITDA), margins (gross, operating, net), return on equity/capital (ROE, ROCE), balance sheet cash & debt ratios, historical financial statements, and Wall Street analyst price target trends.

### 5. PortfolioRiskAgentV6 (`portfolio_risk_agent`)
- **Java Class**: [`PortfolioRiskAgentV6.java`](../../src/main/java/com/google/adk/finance/v6/subagents/PortfolioRiskAgentV6.java)
- **Agent Name**: `portfolio_risk_agent`
- **Output Key**: `portfolio_risk_report`
- **Primary Tools**: Yahoo Finance MCP (Beta, 52-week spread), `PortfolioMathTool` (concentration math), `ProjectKnowledgeTool` (risk framework), `SkillToolset`
- **Responsibility**: "Identify investment hazards, market sensitivity, and portfolio risk."
- **Capabilities**: Systematic market beta ($\beta$), price drawdown spread, capital structure leverage risk, and single-stock portfolio concentration flags (>25%).

---

## 2. Standardized Sub-Agent Output Contract

Every specialist in V6 adheres strictly to this output contract:

```markdown
[SUB-AGENT REPORT: <SpecialistName> (<agent_name>)]
• Specialist: <SpecialistName> (<agent_name>)
• Task: <Brief summary of research or analysis task>
• Key Findings:
  - <Specific finding 1 with attributed metrics, dates, or sources>
  - <Specific finding 2 with attributed metrics, dates, or sources>
  - <Specific finding 3 with attributed metrics, dates, or sources>
• Data Sources & Capabilities: <Google Search | Yahoo Finance MCP | Curated Frameworks | Math Tool>
• Limitations & Scope: <Explicit boundaries of what this specialist does and does not cover>
```
