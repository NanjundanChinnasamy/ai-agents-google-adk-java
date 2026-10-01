# Finance Advisor V8 — Specialized Sub-Agents

This directory documents specialized sub-agents implemented for Milestone 8:

---

## 1. `GuardedMarketResearchAgentV8`
- **File**: [`GuardedMarketResearchAgentV8.java`](GuardedMarketResearchAgentV8.java)
- **Agent Name**: `stockmarket_researcher`
- **Purpose**: Performs grounded live web search using Google Search (`GoogleSearchTool.INSTANCE`) to retrieve recent company announcements, quarterly results, SEC filings, and analyst ratings.
- **Safety Boundary**: Wrapped with dedicated `BeforeAgentGuardrail` and `BeforeModelGuardrail` to ensure untrusted search prompts are screened for PII and prompt injection prior to executing external queries.

---

## 2. `MockTradingAgentV8`
- **File**: [`MockTradingAgentV8.java`](MockTradingAgentV8.java)
- **Tool Exposed**: `execute_trade` (arguments: `symbol`, `action`, `quantity`)
- **Purpose**: Serves as a pedagogical verification testbed. While the mock tool contains dummy execution logic, the V8 `BeforeToolGuardrail` and `ToolOperationGuard` intercept any invocation before it can execute, returning an explicit authorization block.
- **Invariant**: The Finance Advisor is strictly an advisory, analytical, and research assistant. Financial transactions, order placement, and money movements are forbidden.
