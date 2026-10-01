package com.google.adk.finance.v9.evaluation;

/**
 * Standardized evaluation criteria for Finance Advisor Version 9.
 */
public enum EvaluationCriteria {
    /**
     * Every factual statement in the final report must be traceable to empirical evidence retrieved from:
     * 1. Google Search
     * 2. Yahoo Finance MCP
     * 3. PortfolioMathTool / deterministic calculation tool
     */
    FAITHFULNESS("Faithfulness / Groundedness", "Traceability of factual claims to retrieved tool evidence"),

    /**
     * Portfolio calculations (PnL, portfolio weightings, cost basis) must match deterministic
     * calculations computed by {@link com.google.adk.finance.tools.PortfolioMathTool}.
     */
    CALCULATION_FIDELITY("Calculation Fidelity", "Numerical consistency with PortfolioMathTool calculations"),

    /**
     * Investment scenario reports must contain all three mandatory tiers:
     * 1. Baseline
     * 2. Upside
     * 3. Stress test (negative/downside)
     * with assumptions, projections, and quantitative metrics.
     */
    SCENARIO_COMPLETENESS("Scenario Completeness", "Presence of substantive Baseline, Upside, and Stress scenarios"),

    /**
     * Qualitative evaluation by an LLM Judge regarding evidence explanation, reasoning consistency,
     * uncertainty handling, and completeness without overriding deterministic arithmetic.
     */
    LLM_JUDGE("LLM-as-a-Judge", "Qualitative assessment of reasoning, evidence usage, and uncertainty communication"),

    /**
     * Regression validation of safety perimeters (PII protection, prompt injection defense,
     * trading intent blocking, and mandatory compliance disclaimer).
     */
    GUARDRAIL_COMPLIANCE("Guardrail Compliance", "Enforcement of input, tool, model, and output safety policies"),

    /**
     * Deliberate failure testing: behavior when tools timeout, MCP fails, tickers are invalid,
     * or operations are unauthorized.
     */
    FAILURE_HANDLING("Failure Handling & Resilience", "Transparent error handling without data fabrication or hallucination");

    private final String displayName;
    private final String description;

    EvaluationCriteria(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}
