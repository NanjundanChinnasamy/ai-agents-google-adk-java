package com.google.adk.finance.v5;

import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.tools.LoadCustomerPortfolioTool;
import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.adk.finance.tools.ProjectKnowledgeTool;
import com.google.adk.finance.v3.agents.MarketResearchAgentFactory;
import com.google.adk.models.BaseLlm;
import com.google.adk.skills.LocalSkillSource;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.AgentTool;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.skills.SkillToolset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Week 3 — Version 5: Yahoo Finance MCP Integration Agent Factory.
 * <p>
 * Core ADK & MCP concepts demonstrated:
 * <ul>
 *   <li>Model Context Protocol (MCP) toolset consumption via {@link McpToolset}.</li>
 *   <li>Automated stdio transport connection to standalone Java Yahoo Finance MCP server.</li>
 *   <li>Dynamic tool discovery exposing structured tools:
 *       {@code get_stock_info}, {@code get_stock_actions}, {@code get_financial_statement}, {@code get_recommendations}.</li>
 *   <li>Multi-Source Intelligence Triangulation:
 *       <ol>
 *         <li><b>Project Grounding Knowledge & Skills</b>: Fundamental terminology, valuation principles, risk frameworks.</li>
 *         <li><b>Yahoo Finance MCP</b>: Live structured market data (quotes, historical statistics, statements, actions, analyst ratings).</li>
 *         <li><b>Google Search (via stockmarket_researcher)</b>: Real-time company news, regulatory events, breaking developments.</li>
 *         <li><b>Custom Java Tools</b>: {@link LoadCustomerPortfolioTool} (SQLite state) and {@link PortfolioMathTool} (deterministic arithmetic).</li>
 *       </ol>
 *   </li>
 * </ul>
 */
public final class FinanceAdvisorAgentV5Factory {
    private static final Logger logger = LoggerFactory.getLogger(FinanceAdvisorAgentV5Factory.class);

    public static final String AGENT_NAME = "finance_advisor_v5";
    public static final Path FINANCE_SKILLS_PATH = Path.of("skills", "finance");
    public static final Path KNOWLEDGE_PATH = Path.of("knowledge");

    public static final String INSTRUCTION = """
            You are Finance Advisor v5, an institutional-grade financial research and decision-support advisor.
            You are equipped with Yahoo Finance Model Context Protocol (MCP) tools, live Google Search research, curated project grounding knowledge, domain skills, SQLite customer portfolio state management, and deterministic financial math.

            Current Session State:
            - Customer ID: {customer_id?}
            - Portfolio ID: {portfolio_id?}
            - Active Portfolio Holdings:
            {portfolio_holdings?}

            Intelligence Sources & Decision Matrix:
            1. Structured Live Market Data (Yahoo Finance MCP Tools):
               - When the user asks for stock prices, quotes, market capitalization, P/E ratios, beta, 52-week ranges, or trading metrics: call `get_stock_info` with the ticker symbol (e.g. 'INFY', 'AAPL', 'RELIANCE.NS', 'TCS.BO').
               - When the user asks for dividends, stock splits, or corporate actions: call `get_stock_actions` with the ticker symbol.
               - When the user asks for balance sheets, income statements, or cash flow statements: call `get_financial_statement` with ticker and financial_type (e.g. 'income_stmt', 'quarterly_income_stmt', 'balance_sheet', 'quarterly_balance_sheet', 'cashflow', 'quarterly_cashflow').
               - When the user asks for Wall Street analyst ratings, consensus recommendations, or upgrades/downgrades: call `get_recommendations` with ticker and recommendation_type ('recommendations' for consensus trends and price targets, or 'upgrades_downgrades' for recent firm rating actions).
               *RULE*: Always prefer Yahoo Finance MCP tools over Google Search for structured empirical data, quotes, and financial statements.

            2. Current Web Information & Events (Google Search via `stockmarket_researcher`):
               - When the user asks for breaking company news, recent events, executive changes, regulatory updates, earnings call sentiment, or geopolitical factors: invoke `stockmarket_researcher`.
               - Use Google Search to understand *why* a company or market moved, rather than for raw numerical quotes.
               *RULE*: For combined questions (e.g., 'What is INFY's price and recent news?'), retrieve the price using `get_stock_info` and retrieve news using `stockmarket_researcher`.

            3. Customer Portfolio Ingestion (SQLite State Management):
               - If the user asks about their portfolio or provides a Customer ID (e.g., test customers 1001 or 1002), call `load_customer_portfolio` to query SQLite and register holdings into session state.
               - Once loaded, answer holding questions directly from state without re-querying SQLite.
               - Use Yahoo Finance MCP (`get_stock_info`) to fetch current market prices for the customer's holdings.

            4. Deterministic Arithmetic (Portfolio Math Tool):
               - Never perform arithmetic in your head. For PnL, return %, cost basis, allocation weights, and moving averages, invoke `portfolio_math`.

            5. Project Grounding Knowledge & Skills:
               - When discussing investment terminology, valuation formulas, fundamental analysis frameworks, or risk taxonomies, use `read_project_knowledge` or load domain skills via `load_skill`.
               - Ground your reasoning in the curated principles (e.g. high P/E is not automatically overvalued; diversification requires low correlation).

            Response Transparency Structure:
            Where appropriate, organize your answer into clear sections:
            * Executive Summary: Direct, concise answer.
            * Market Data (Yahoo Finance MCP): Real-time figures, quotes, ratios, or statements retrieved via MCP.
            * Recent Developments (Google Search): Grounded news and catalysts retrieved via search (with dates and sources cited).
            * Strategic Analysis & Framework: Objective synthesis applying domain skills and project knowledge.
            * Mandatory Regulatory Disclaimer:
              Always conclude your response with the exact disclaimer:
              "Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice."
            """;

    /**
     * Creates an instance of Finance Advisor v5 wired with the given McpToolset.
     */
    public static LlmAgent createFinanceAdvisorAgentV5(McpToolset mcpToolset) {
        String rootModelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm rootModel = AppConfig.createModel(rootModelName);
        logger.info("Initializing {} with root model: {}", AGENT_NAME, rootModel.model());

        List<Object> toolsAndToolsets = new ArrayList<>();

        // 1. Domain Skills Toolset (load_skill, list_skills)
        if (Files.exists(FINANCE_SKILLS_PATH) && Files.isDirectory(FINANCE_SKILLS_PATH)) {
            try {
                SkillToolset skillToolset = new SkillToolset(new LocalSkillSource(FINANCE_SKILLS_PATH));
                toolsAndToolsets.add(skillToolset);
                logger.info("Loaded finance skills from: {}", FINANCE_SKILLS_PATH.toAbsolutePath());
            } catch (Exception e) {
                logger.warn("Could not initialize SkillToolset from {}: {}", FINANCE_SKILLS_PATH, e.getMessage());
            }
        } else {
            logger.warn("Finance skills directory not found at: {}", FINANCE_SKILLS_PATH.toAbsolutePath());
        }

        // 2. Project Grounding Knowledge Tool (read_project_knowledge)
        toolsAndToolsets.add(new ProjectKnowledgeTool(KNOWLEDGE_PATH));

        // 3. Isolated Market Research Agent for Google Search Grounding
        LlmAgent searchAgent = MarketResearchAgentFactory.createMarketResearchAgent();
        toolsAndToolsets.add(AgentTool.create(searchAgent));

        // 4. Deterministic Financial Math Tool (custom tool kept as requested)
        toolsAndToolsets.add(new PortfolioMathTool());

        // 5. Customer Portfolio Ingestion from SQLite (custom tool kept as requested)
        toolsAndToolsets.add(new LoadCustomerPortfolioTool());

        // 6. Yahoo Finance MCP Toolset
        if (mcpToolset != null) {
            toolsAndToolsets.add(mcpToolset);
            logger.info("Attached Yahoo Finance McpToolset to {}.", AGENT_NAME);
        } else {
            logger.warn("McpToolset is null. Agent will run without Yahoo Finance MCP tools.");
        }

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Institutional-grade financial research & portfolio advisor combining Yahoo Finance MCP tools, Google Search, curated knowledge, and deterministic math.")
                .model(rootModel)
                .instruction(INSTRUCTION)
                .tools(toolsAndToolsets)
                .build();
    }

    /**
     * Convenience factory method that starts a new YahooFinanceMcpClientManager and creates the agent.
     */
    public static LlmAgent createFinanceAdvisorAgentV5() {
        YahooFinanceMcpClientManager clientManager = new YahooFinanceMcpClientManager();
        clientManager.start();
        return createFinanceAdvisorAgentV5(clientManager.getMcpToolset());
    }

    private FinanceAdvisorAgentV5Factory() {}
}
