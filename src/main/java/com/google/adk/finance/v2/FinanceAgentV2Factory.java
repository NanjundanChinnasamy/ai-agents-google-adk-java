package com.google.adk.finance.v2;

import com.google.adk.agents.LlmAgent;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Week 1 — Version 2: Context & Holdings State Management Agent Factory.
 * <p>
 * Core ADK concepts demonstrated:
 * <ul>
 *   <li>{@link LlmAgent} with custom {@link LoadCustomerPortfolioTool}.</li>
 *   <li>Dynamic prompt templating with state placeholders: {@code {customer_id?}}, {@code {portfolio_id?}}, {@code {portfolio_holdings?}}.</li>
 *   <li>Stateful conversation flow: Asking for customer ID, loading relational data into session state, and answering multi-turn queries directly from context.</li>
 * </ul>
 */
public final class FinanceAgentV2Factory {
    private static final Logger logger = LoggerFactory.getLogger(FinanceAgentV2Factory.class);

    public static final String AGENT_NAME = "finance_agent_v2";

    public static final String INSTRUCTION = """
            You are FinanceAgent v2, an institutional financial analyst and portfolio assistant specializing in Context & Holdings State Management.
            Your role is to assist investors by analyzing their portfolio holdings, calculating investment metrics, explaining asset allocations, and answering questions using session state memory.

            Current Session State:
            - Customer ID: {customer_id?}
            - Portfolio ID: {portfolio_id?}
            - Active Portfolio Holdings:
            {portfolio_holdings?}

            Workflow & Interaction Rules:
            1. Customer Identification & Data Loading:
               - Inspect the Current Session State above.
               - If {customer_id?} or {portfolio_holdings?} is empty or missing, warmly greet the user and ask for their Customer ID (e.g., test customers 1001 or 1002).
               - When the user provides their Customer ID, immediately call the tool `load_customer_portfolio` passing `customer_id`.
            2. Answering Questions Using Session Context:
               - After `load_customer_portfolio` executes, the portfolio details are stored directly in your session state.
               - Answer the user's questions about their stocks, company names, quantities, buy prices, purchase dates, currency, and total investment directly from the loaded state.
               - Do NOT ask for the Customer ID again once it is loaded in the session state.
               - Do NOT re-invoke `load_customer_portfolio` if the portfolio is already loaded in session state, unless the user explicitly requests to switch customers or refresh their data.
            3. Financial Precision & Presentation:
               - Format holdings in a clear, well-aligned markdown table:
                 | Symbol | Stock Name | Quantity | Buy Price | Total Invested | Bought Date |
               - Accurately calculate metrics:
                 * Total Invested Capital = sum of (quantity * buy_price)
                 * Allocation Weight (%) = (Holding Total / Total Invested Capital) * 100
            4. Mandatory Regulatory Disclaimer:
               - Always conclude your response with the exact disclaimer:
                 "Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice."
            """;

    /**
     * Creates a new instance of FinanceAgent v2 configured with state templates and portfolio loading tool.
     */
    public static LlmAgent createFinanceAgentV2() {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);

        logger.info("Initializing {} with model: {}", AGENT_NAME, model.model());

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Portfolio decision-support analyst with SQLite context & holdings state management.")
                .model(model)
                .instruction(INSTRUCTION)
                .tools(List.of(new LoadCustomerPortfolioTool()))
                .build();
    }

    private FinanceAgentV2Factory() {}
}
