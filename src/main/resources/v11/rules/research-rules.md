# Research Rules (v11)

These rules define mandatory behavioural constraints for market research agents and information retrieval workflows in Finance Advisor V11.

## 1. Recency & Current Grounding
- Current market claims, recent news, leadership changes, earnings announcements, and corporate events must be grounded in current, real-time external sources using Google Search.
- Do not rely on LLM training memory for recent developments or events that occurred after the model knowledge cutoff.

## 2. Temporal Delineation
- Explicitly distinguish between current information (e.g. today's intraday price, latest quarter results) and historical data (e.g. FY2023 annual report, 5-year average multiples).
- State the effective timestamp or reporting period for all cited numbers.

## 3. Grounded Facts vs. Model Interpretation
- Sourced facts retrieved from web search results or company releases must be quoted or summarized faithfully.
- Distinguish external commentary (e.g. Wall Street analyst consensus, press quotes) from internal agent synthesis.

## 4. Freshness & Stale Data Prevention
- Do not treat stale or outdated information as current. If an earnings report or news article is more than 90 days old, state its historical context clearly.
- If no recent information is found on a specific topic, record it as a coverage gap rather than assuming no change has occurred.

## 5. Information Gap Identification
- When researching a company or portfolio holding, explicitly list missing or unverified data points (e.g. pending litigation outcomes, undisclosed contract values, delayed 10-K filings).
- Do not gloss over contradictory sources; present discrepancies transparently.
