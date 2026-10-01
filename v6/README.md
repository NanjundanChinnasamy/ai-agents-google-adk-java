# Finance Advisor V6 — Sub-Agents & Multi-Agent Delegation

This directory documents **Finance Advisor V6**, which introduces **specialized sub-agents** and **parent orchestrator delegation** using the official Google Agent Development Kit (ADK) for Java.

---

## 1. Directory Structure

```
v6/
├── README.md                          <- This documentation reference
├── sub-agents/                        <- Architectural guide for specialized sub-agents
│   └── README.md
│
Java Source Implementations (Package: com.google.adk.finance.v6):
src/main/java/com/google/adk/finance/v6/
├── FinanceAdvisorAgentV6.java         <- Root Orchestrator Agent (portfolio_director)
├── FinanceConsoleV6.java              <- Interactive CLI Runner with Delegation Observability
└── subagents/                         <- Specialized Sub-Agents (Narrow responsibilities)
    ├── MarketResearchAgentV6.java     <- stockmarket_researcher: Google Search web grounding & news
    ├── ScenarioAnalystAgentV6.java    <- scenario_analyst: Bull, Bear, Baseline macro scenarios & stress-testing
    ├── ReportWriterAgentV6.java       <- report_writer: Synthesizes findings into institutional decision report
    ├── FundamentalAnalysisAgentV6.java<- fundamental_analysis_agent: Yahoo Finance MCP fundamentals & ratios
    └── PortfolioRiskAgentV6.java      <- portfolio_risk_agent: Volatility, beta, concentration & risk

Integration Test Suite:
src/test/java/com/google/adk/finance/v6/
└── FinanceV6IntegrationTest.java      <- 9 Comprehensive Integration Tests covering all specialists & hierarchy
```

---

## 2. Learning Progression: V1 through V6

| Version | Focus | Core Concept |
|---|---|---|
| **V1** | Baseline Agent | `LlmAgent`, `InMemoryRunner`, `SessionService` |
| **V2** | Context & State | SQLite persistence, `LoadCustomerPortfolioTool`, prompt templating |
| **V3** | Tools & Search | `PortfolioMathTool`, `stockmarket_researcher` via `AgentTool` |
| **V4** | Skills & Knowledge | `SkillToolset`, `ProjectKnowledgeTool`, curated valuation/risk markdown |
| **V5** | MCP Integration | Yahoo Finance MCP Server over stdio, `McpToolset` dynamic tool discovery |
| **V6** | **Specialist Multi-Agent System**| **Parent orchestrator (`portfolio_director`) delegating to 5 specialized sub-agents (`stockmarket_researcher`, `scenario_analyst`, `report_writer`, `fundamental_analysis_agent`, `portfolio_risk_agent`) with standardized output contracts, isolated toolsets, failure isolation, and multi-specialist synthesis.** |

### What V6 Deliberately Does NOT Introduce
- Sequential pipelines or deterministic workflows (Scheduled for V7)
- Parallel fan-out/fan-in asynchronous processing (Scheduled for V7)
- Loop / critic / reflection agents (Scheduled for V7)
- Guardrails or PII compliance filters (Scheduled for V8)
- Autonomous trading or portfolio execution

---

## 3. Quickstart

Run the interactive CLI console:
```bash
# Windows
test-finance-v6.bat

# Linux / macOS
./test-finance-v6.sh
```

Run V6 integration tests:
```bash
gradlew test --tests com.google.adk.finance.v6.FinanceV6IntegrationTest
```
