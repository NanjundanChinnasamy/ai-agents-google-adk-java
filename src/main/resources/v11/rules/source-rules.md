# Source Rules (v11)

These rules define authoritative source selection, priority mapping, and fallback boundaries for tools and MCP servers in Finance Advisor V11.

## 1. Source-Selection Mapping
The following operational mapping is strictly enforced across all agents:

| Domain Need | Authoritative Source | Primary Mechanism | Fallback / Limitation |
|---|---|---|---|
| **Structured Market & Financial Data** | Yahoo Finance MCP | `get_stock_info`, `get_financial_statement`, `get_stock_actions` | If MCP unavailable, acknowledge offline status; do not fabricate quotes |
| **Current Public-Web Information & News** | Google Search | `stockmarket_researcher` / `GoogleSearchTool` | If search yields no results, note information gap; do not extrapolate |
| **Portfolio Holdings & Transactions** | Relational Database | `LoadCustomerPortfolioTool` (SQLite) | Customer ID required; do not assume default assets |
| **Deterministic Arithmetic & Math** | Java Portfolio Math | `PortfolioMathTool` | Cost basis, PnL, allocation weights, concentration risk, SMA |

## 2. No Unauthorized Source Substitution
- The agent must not substitute one source for another without explicit rationale.
  - Example violation: Querying Google Search for real-time bid/ask or P/E multiple when Yahoo Finance MCP is available.
  - Example violation: Asking the LLM to calculate portfolio return percentage instead of invoking `portfolio_math`.
- If an agent intentionally falls back to a secondary source (e.g. Yahoo Finance MCP is offline so company investor relations page is checked via Search), it must state this substitution explicitly.

## 3. Graceful Handling of Unavailable Sources
- Where the designated source is unavailable or returns an error, the agent must acknowledge the operational limitation directly.
- The agent must never invent plausible numbers to cover for an unavailable source or network outage.

## 4. Source Metadata Integrity
- Tool outputs must retain their provenance metadata throughout the research and synthesis pipeline.
- Raw API keys, credentials, or internal endpoint paths must never be emitted into end-user prompts or responses.
