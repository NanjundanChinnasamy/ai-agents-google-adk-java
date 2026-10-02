# Finance Advisor V6 — Specialist Multi-Agent System (`finance.v6`)

> **Milestone 6 Objective**: Introduce **specialized sub-agents / multi-agent delegation** into the Finance Advisor application using Google ADK Java. Learn how a root orchestrator agent (`portfolio_director` / [`FinanceAdvisorAgentV6.java`](../src/main/java/com/google/adk/finance/v6/FinanceAdvisorAgentV6.java)) delegates specialized financial tasks to independent sub-agents (`stockmarket_researcher`, `scenario_analyst`, `report_writer`, `fundamental_analysis_agent`, and `portfolio_risk_agent`), enforces narrow specialist responsibilities, isolates tool ownership, handles partial specialist failures, and synthesizes multi-specialist findings into an institutional decision-support answer.

---

## 1. High-Level Sub-Agent Architecture

```
                                       User Inquiry
                                            │
                                            ▼
                             +──────────────────────────────+
                             |      portfolio_director      |
                             |    (FinanceAdvisorAgentV6)   |
                             |   Understands Intent & Routes|
                             +──────────────┬───────────────+
                                            │
        ┌────────────────────┬──────────────┼──────────────┬────────────────────┐
        │                    │              │              │                    │
[Delegation 1]        [Delegation 2] [Delegation 3] [Delegation 4]        [Delegation 5]
        │                    │              │              │                    │
        ▼                    ▼              ▼              ▼                    ▼
+───────────────────+ +─────────────────+ +────────────────+ +──────────────────+ +──────────────────+
|stockmarket_res.   | |fundamental_agent| |portfolio_risk  | |scenario_analyst  | |report_writer     |
|"Why did asset     | |"Quantitative    | |"Identify beta, | |"Model Bull/Bear  | |"Synthesize       |
| prices change?"   | | fundamentals &  | | volatility &   | | scenarios &      | | executive        |
|                   | | multiples"      | | concentration" | | stress-test"     | | decision report" |
+─────────┬─────────+ +────────┬────────+ +────────┬───────+ +────────┬─────────+ +────────┬─────────+
        │                    │                   │                  │                    │
        ▼                    ▼                   ▼                  ▼                    ▼
+───────────────────+ +─────────────────+ +────────────────+ +──────────────────+ +──────────────────+
| GoogleSearchTool  | |Yahoo Finance MCP| |Yahoo Finance   | |Yahoo Finance MCP | |Project Knowledge |
| (Live Web News &  | |(Stock info,     | |(Beta, 52-wk)   | |(Beta & Multiples)| |(Curated Briefing |
|  attributions)    | | statements,     | |+ PortfolioMath | |+ PortfolioMath   | | Frameworks)      |
| + 8-Stage Research| | recommendations)| |+ Risk Framework| |+ Risk Framework  | |+ Domain Skills   |
+─────────┬─────────+ +────────┬────────+ +────────┬───────+ +────────┬─────────+ +────────┬─────────+
        │                    │                   │                  │                    │
        └────────────────────┴──────────────┬────┴──────────────────┴────────────────────┘
                                            │ Standardized Sub-Agent Reports
                                            ▼
                             +──────────────────────────────+
                             |  portfolio_director Synthesis|
                             | - Executive Summary          |
                             | - Multi-Specialist Evidence  |
                             | - Macro Scenario Matrix      |
                             | - Limitations & Warnings     |
                             | - Mandatory Disclaimer       |
                             +──────────────┬───────────────+
                                            │
                                            ▼
                                       Final Answer
```

---

## 2. Evolutionary Comparison (V1 through V6)

| Milestone | Core ADK & Architectural Focus | What Changed / Capability Added |
|---|---|---|
| **V1** | Baseline `LlmAgent`, `InMemoryRunner`, `SessionService` | Foundational conversational assistant with multi-turn memory. |
| **V2** | Relational Context & State Management | Ingests SQLite portfolio state dynamically via `LoadCustomerPortfolioTool` and `{placeholder?}` templates. |
| **V3** | Java Tools & Google Search Grounding | Deterministic arithmetic via `PortfolioMathTool` + live web news via `stockmarket_researcher` (`AgentTool`). |
| **V4** | Domain Skills & Project Grounding Knowledge | Curated valuation multiples, accounting frameworks, and Markdown procedures loaded dynamically via `SkillToolset` and `ProjectKnowledgeTool`. |
| **V5** | Model Context Protocol (MCP) | External stdio integration to standalone Java Yahoo Finance MCP Server exposing 4 structured market tools without custom code. |
| **V6** | **Specialist Multi-Agent System** | **Parent-to-sub-agent delegation (`AgentTool`), 5 specialist sub-agents (`stockmarket_researcher`, `scenario_analyst`, `report_writer`, `fundamental_analysis_agent`, `portfolio_risk_agent`), isolated tool ownership, standardized reporting contracts, failure isolation, and multi-specialist synthesis.** |

### What V6 Deliberately Does NOT Introduce
To keep V6 focused strictly on understanding **Sub-Agents**, the following advanced topics are deferred to future milestones:
- ❌ **Sequential Workflows & Pipelines** (Scheduled for V7)
- ❌ **Parallel Fan-out / Fan-in Engines** (Scheduled for V7)
- ❌ **Loop / Critic / Reflection Agents** (Scheduled for V7)
- ❌ **Guardrail Callbacks & PII Redaction** (Scheduled for V8)
- ❌ **Agent-to-Agent (A2A) Remote Protocol**
- ❌ **Automated LLM-as-a-Judge Evaluation Benchmarks** (Scheduled for V9)
- ❌ **OpenTelemetry Tracing & Export** (Scheduled for V10)
- ❌ **Autonomous Trading or Portfolio Execution**

---

## 3. The 5 Specialized Sub-Agents

Every sub-agent in V6 has a narrow, explicit responsibility and isolated toolset:

### 3.1 MarketResearchAgentV6 (`stockmarket_researcher` / `market_research_agent`)
- **Package**: `com.google.adk.finance.v6.subagents`
- **Class**: [`MarketResearchAgentV6.java`](../src/main/java/com/google/adk/finance/v6/subagents/MarketResearchAgentV6.java)
- **Responsibility**: Investigates what is happening **now** on the public web and explains why asset prices changed.
- **Typical Questions**:
  - *"What happened with Infosys this week?"*
  - *"Why did TCS drop after earnings?"*
  - *"What are the latest corporate announcements or C-suite changes?"*
- **Tool Ownership**:
  - `GoogleSearchTool.INSTANCE` (server-side search grounding).
- **Domain Framework**:
  - Embedded 8-stage market research methodology: Company announcements, quarterly earnings surprises, regulatory inquiries, competitor dynamics, macro factors, analyst revisions, market sentiment, and key catalysts.

### 3.2 ScenarioAnalystAgentV6 (`scenario_analyst`)
- **Package**: `com.google.adk.finance.v6.subagents`
- **Class**: [`ScenarioAnalystAgentV6.java`](../src/main/java/com/google/adk/finance/v6/subagents/ScenarioAnalystAgentV6.java)
- **Responsibility**: Models forward-looking macro scenarios and stress-tests portfolio sensitivity.
- **Typical Questions**:
  - *"Model bull and bear scenarios for Infosys."*
  - *"What happens if interest rates rise 50 bps?"*
  - *"Stress-test my portfolio against a 20% tech sector correction."*
- **Tool Ownership**:
  - Yahoo Finance MCP (`get_stock_info` for beta and multiples), `PortfolioMathTool` (allocation & return simulation), `ProjectKnowledgeTool` (portfolio principles & risk framework).

### 3.3 ReportWriterAgentV6 (`report_writer`)
- **Package**: `com.google.adk.finance.v6.subagents`
- **Class**: [`ReportWriterAgentV6.java`](../src/main/java/com/google/adk/finance/v6/subagents/ReportWriterAgentV6.java)
- **Responsibility**: Synthesizes multi-agent research notes into an institutional decision-support report.
- **Typical Questions**:
  - *"Synthesize an executive decision report for my portfolio."*
  - *"Compile our research, fundamentals, and scenario findings into an investment brief."*
- **Tool Ownership**:
  - `ProjectKnowledgeTool` (briefing templates), `SkillToolset`.

### 3.4 FundamentalAnalysisAgentV6 (`fundamental_analysis_agent`)
- **Package**: `com.google.adk.finance.v6.subagents`
- **Class**: [`FundamentalAnalysisAgentV6.java`](../src/main/java/com/google/adk/finance/v6/subagents/FundamentalAnalysisAgentV6.java)
- **Responsibility**: Analyzes structured company fundamentals, ratios, and filings.
- **Typical Questions**:
  - *"What are Infosys' latest valuation multiples and profit margins?"*
  - *"Compare balance sheet debt-to-equity and cash position."*
- **Tool Ownership**:
  - Yahoo Finance MCP tools (`get_stock_info`, `get_financial_statement`, `get_stock_actions`, `get_recommendations`), `ProjectKnowledgeTool`.

### 3.5 PortfolioRiskAgentV6 (`portfolio_risk_agent`)
- **Package**: `com.google.adk.finance.v6.subagents`
- **Class**: [`PortfolioRiskAgentV6.java`](../src/main/java/com/google/adk/finance/v6/subagents/PortfolioRiskAgentV6.java)
- **Responsibility**: Evaluates investment hazards, systematic risk, and portfolio concentration.
- **Typical Questions**:
  - *"What are the risks of investing in Reliance?"*
  - *"Does my portfolio violate concentration thresholds (>25%)?"*
- **Tool Ownership**:
  - Yahoo Finance MCP (Beta, 52-week trading spread), `PortfolioMathTool` (concentration math), `ProjectKnowledgeTool`.

---

## 4. Standardized Output Contract

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

---

## 5. Key Files & Implementation Reference

| File | Package Path | Role & Purpose |
|---|---|---|
| `FinanceAdvisorAgentV6.java` | `src/main/java/com/google/adk/finance/v6/` | Root orchestrator factory (`portfolio_director`) coordinating the 5 sub-agents via `AgentTool`. |
| `MarketResearchAgentV6.java` | `src/main/java/com/google/adk/finance/v6/subagents/` | Search specialist (`stockmarket_researcher`) dedicated to Google Search and 8-stage market research. |
| `ScenarioAnalystAgentV6.java` | `src/main/java/com/google/adk/finance/v6/subagents/` | Macro scenario analyst (`scenario_analyst`) modeling bull/bear cases and stress-testing. |
| `ReportWriterAgentV6.java` | `src/main/java/com/google/adk/finance/v6/subagents/` | Report synthesis specialist (`report_writer`) compiling executive decision-support documents. |
| `FundamentalAnalysisAgentV6.java` | `src/main/java/com/google/adk/finance/v6/subagents/` | Financial statement specialist (`fundamental_analysis_agent`) analyzing multiples and balance sheets. |
| `PortfolioRiskAgentV6.java` | `src/main/java/com/google/adk/finance/v6/subagents/` | Risk analyst (`portfolio_risk_agent`) computing beta, volatility, and concentration alerts. |
| `FinanceConsoleV6.java` | `src/main/java/com/google/adk/finance/v6/` | Interactive CLI with sub-agent routing, real-time event logs, and status inspection. |
| `FinanceV6IntegrationTest.java` | `src/test/java/com/google/adk/finance/v6/` | Test suite covering all 9 V6 integration tests: sub-agent creation, delegation, failure isolation, and synthesis. |

---

## 6. How to Run & Test Finance Advisor V6

### 6.1 Interactive CLI Console
Launch the interactive terminal:
```bash
# Windows
test-finance-v6.bat

# Linux / macOS
./test-finance-v6.sh
```

Within the console, use shortcut commands:
- `agents`    : Displays all 5 registered specialist sub-agents and their descriptions.
- `state`     : Inspects current customer portfolio session state.
- `help`      : Displays sample multi-agent delegation questions.
- `exit`      : Terminates the session cleanly.

### 6.2 Executing Automated Tests
Run the complete V6 integration test suite:
```bash
# Windows
gradlew.bat test --tests com.google.adk.finance.v6.FinanceV6IntegrationTest

# Linux / macOS
./gradlew test --tests com.google.adk.finance.v6.FinanceV6IntegrationTest
```
