# V11 Sub-Agents & Rule Scoping

In Finance Advisor V11, specialized sub-agents are governed by **Scoped Rules**:

| Sub-Agent | Class | Scoped Rules Applied | Primary Responsibility |
|---|---|---|---|
| **Market Researcher** | `MarketResearchAgentV11` | `research-rules.md`<br>`source-rules.md`<br>`finance-rules.md` | Public web news grounding via Google Search |
| **Fundamental Analyst** | `FundamentalAnalysisAgentV11` | `source-rules.md`<br>`finance-rules.md` | Structured balance sheet and valuation multiples via Yahoo Finance MCP |
| **Portfolio Risk Analyst** | `PortfolioRiskAgentV11` | `risk-rules.md`<br>`finance-rules.md` | Concentration risk, volatility spread, deterministic math |
| **Response Synthesizer** | `ResponseSynthesisAgentV11` | `response-rules.md`<br>`finance-rules.md` | Multi-source assembly, limitation disclosure, regulatory disclaimer |

### Lifecycle Hook Protection
All sub-agents executing external tools are intercepted by:
- `PreToolSourceValidationHook` (BLOCKING): Validates ticker syntax and enforces source selection rules.
- `PostToolObservationHook` (NON-BLOCKING): Observes execution duration, payload size, and status without credentials.
