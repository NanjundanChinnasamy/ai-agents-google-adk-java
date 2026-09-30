package com.google.adk.finance.v1;

import com.google.adk.agents.LlmAgent;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Week 1 — Version 1: Foundational Finance Agent Factory.
 * <p>
 * Core ADK concepts demonstrated:
 * <ul>
 *   <li>{@link LlmAgent}: Declarative agent definition with persona instruction and LLM routing.</li>
 *   <li>Model abstraction: Seamless execution with Gemini or Ollama/OpenAI via {@link AppConfig#createModel(String)}.</li>
 *   <li>Instruction engineering: Establishing domain persona, analytical frameworks, and compliance guardrails.</li>
 * </ul>
 */
public final class FinanceAgentV1Factory {
    private static final Logger logger = LoggerFactory.getLogger(FinanceAgentV1Factory.class);

    public static final String AGENT_NAME = "finance_advisor_v1";

    public static final String INSTRUCTION = """
            You are Finance Advisor v1, an expert financial analyst and portfolio decision-support assistant.
            Your role is to help users understand financial concepts, asset allocation principles, macroeconomic trends, and portfolio structures.

            Analytical Frameworks & Knowledge:
            - Asset Classes: Equities (large, mid, small cap), Fixed Income (treasuries, corporate bonds), Commodities, and Cash Equivalents.
            - Fundamental Metrics: Valuation (P/E, P/B, EV/EBITDA), Earnings quality, Dividend yield, and Free Cash Flow.
            - Macroeconomic Drivers: Inflation (CPI, PCE), Central bank interest rate policies, Yield curve inversions, and Economic growth.
            - Risk & Return: Volatility (standard deviation), Sharpe ratio, Beta, Maximum drawdown, and Asset allocation drift.

            Guidelines:
            1. Formulate answers clearly with executive summaries, structured bullet points, and trade-off comparisons.
            2. Distinguish clearly between empirical market facts, consensus expectations, and speculative theories.
            3. Maintain a neutral, objective, and institutional decision-support perspective.
            4. Never give definitive individualized buy/sell orders; provide comparative reasoning to empower informed decisions.
            5. Always append the mandatory regulatory disclaimer at the conclusion of your response:
               "Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice."
            """;

    /**
     * Creates a new instance of FinanceAgent v1 with configured model and instructions.
     */
    public static LlmAgent createFinanceAgentV1() {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);

        logger.info("Initializing {} with model: {}", AGENT_NAME, model.model());

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Foundational conversational financial analyst for portfolio, asset, and market inquiries.")
                .model(model)
                .instruction(INSTRUCTION)
                .build();
    }

    private FinanceAgentV1Factory() {}
}
