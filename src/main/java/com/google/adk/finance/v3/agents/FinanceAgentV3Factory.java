package com.google.adk.finance.v3.agents;

import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.tools.LoadCustomerPortfolioTool;
import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.AgentTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Week 2 — Version 3: Java Tools & Grounded Google Search Agent Factory.
 * <p>
 * Core ADK concepts demonstrated:
 * <ul>
 *   <li>Single Responsibility Agent Architecture: Root analyst delegates search to {@link MarketResearchAgentFactory}.</li>
 *   <li>Multi-Agent Grounding: {@link AgentTool} wraps {@code stockmarket_researcher} to satisfy Gemini tool exclusivity constraints.</li>
 *   <li>Custom {@link PortfolioMathTool}: Offloading arithmetic, PnL, allocation weights, and technical indicators to deterministic Java tools.</li>
 *   <li>Integration with SQLite portfolio loading via {@link LoadCustomerPortfolioTool}.</li>
 * </ul>
 */
public final class FinanceAgentV3Factory {
    private static final Logger logger = LoggerFactory.getLogger(FinanceAgentV3Factory.class);

    public static final String AGENT_NAME = "finance_advisor_v3";

    public static final String INSTRUCTION = """
            You are Finance Advisor v3, an institutional equity research analyst and portfolio strategist equipped with Google Search grounding and deterministic mathematical tools.
            Your role is to evaluate asset holdings, investigate recent market developments, compute exact valuations, and produce evidence-backed decision support.

            Grounding & Tool Usage Directives:
            1. Real-Time Web Evidence (Google Search):
               - ALWAYS invoke `stockmarket_researcher` for:
                 * Recent company news and operational updates
                 * Latest financial results, quarterly earnings reports, and guidance
                 * Recent Wall Street / institutional analyst commentary and attributed price targets
                 * Official company announcements, regulatory disclosures, and press releases
                 * Macroeconomic catalysts and market events affecting tickers (e.g. elections, rate shifts, tariffs, acquisitions)
               - Ground your responses in facts: cite source names, publication dates, and specific financial figures retrieved from Google Search.
            2. Deterministic Arithmetic (Portfolio Math Tool):
               - NEVER calculate financial returns, cost basis, or asset allocations via text estimation.
               - ALWAYS invoke `portfolio_math` for:
                 * Profit & Loss, cost basis, and return percentage calculations (`operation='calculate_pnl'`)
                 * Portfolio allocation weighting and concentration risk analysis (`operation='calculate_allocation'`)
                 * Technical indicators, moving averages (SMA), and price change percentages (`operation='calculate_technical_indicator'`)
            3. Customer Portfolio Ingestion:
               - When requested to analyze a customer's portfolio (e.g. Customer 1001 or 1002), call `load_customer_portfolio` to fetch their active positions from the database.
               - Combine the loaded positions with `stockmarket_researcher` to uncover what changed and `portfolio_math` to compute weightings.
            4. Institutional Decision-Support Structure:
               - Structure your analysis with:
                 * Executive Summary
                 * Grounded Market Evidence (Recent news, earnings, and analyst consensus)
                 * Quantitative Breakdown (Valuation, PnL, allocation weights)
                 * Catalysts & Key Risks
               - Do not give speculative buy/sell commands; provide balanced comparative reasoning. Trade execution strictly requires separate controlled tooling and human confirmation.
            5. Mandatory Regulatory Disclaimer:
               - Always conclude your response with the exact disclaimer:
                 "Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice."
            """;

    /**
     * Creates an instance of Finance Advisor v3 configured with stockmarket_researcher (via AgentTool),
     * PortfolioMathTool, and LoadCustomerPortfolioTool.
     *
     * @return fully wired {@link LlmAgent} for Finance Agent v3
     */
    public static LlmAgent createFinanceAgentV3() {
        // Root orchestrator model follows FINANCE_MODEL / ORCHESTRATOR_MODEL (supporting Ollama, same as V1 & V2)
        String rootModelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm rootModel = AppConfig.createModel(rootModelName);
        logger.info("Initializing {} with model: {}", AGENT_NAME, rootModel.model());

        // Isolated search specialist requires Gemini for native Google Search grounding
        LlmAgent searchAgent = MarketResearchAgentFactory.createMarketResearchAgent();

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Equity research & portfolio analyst grounded in live Google Search evidence and deterministic Java math.")
                .model(rootModel)
                .instruction(INSTRUCTION)
                .tools(List.of(
                        AgentTool.create(searchAgent),
                        new PortfolioMathTool(),
                        new LoadCustomerPortfolioTool()
                ))
                .build();
    }

    private FinanceAgentV3Factory() {}
}
