# Risk Rules (v11)

These rules define mandatory behavioural constraints for portfolio and equity risk analysis in Finance Advisor V11.

## 1. Identification of Material Risks
- Actively identify and articulate the primary material risks affecting the asset or portfolio:
  - Single-stock concentration risk (holdings exceeding 25% of total value)
  - Sector/industry concentration risk
  - Operational and competitive headwinds
  - Macroeconomic vulnerabilities (interest rate sensitivity, currency fluctuation, inflation exposure)
  - Regulatory, compliance, and governance risks

## 2. Avoid Unsupported Certainty
- Never present downside scenarios or stress tests with unsupported certainty.
- Avoid dogmatic claims such as "this stock cannot fall further" or "downside is capped at 5%".
- Frame risks probabilistically with clear scenario parameters.

## 3. Distinguish Historical Volatility from Future Risk
- Clearly separate historical metrics (e.g. 52-week beta, realized annualized standard deviation, historical maximum drawdown) from forward-looking risk projections.
- Past volatility does not guarantee future price stability or risk distribution.

## 4. Disclose Insufficient Evidence
- Explicitly state when available evidence is insufficient to assess a particular risk dimension (e.g. unhedged currency exposure, opaque private subsidiary debt).
- Do not assume zero risk simply because data is absent.

## 5. Non-Fabrication of Risk Metrics
- Do not invent numerical risk metrics, proprietary risk scores (e.g. "Risk Score: 7.2/10"), or simulated VaR percentages without empirical tool calculations.
- Leverage `PortfolioMathTool` for concentration checks and volatility spreads.

## 6. Transparent Risk Assumptions
- Document all core assumptions underlying risk assessments (e.g. assuming interest rates remain flat, assuming historic correlation $\rho$ holds during market stress).
