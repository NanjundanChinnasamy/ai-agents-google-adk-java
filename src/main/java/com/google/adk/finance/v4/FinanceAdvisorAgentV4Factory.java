package com.google.adk.finance.v4;

import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.tools.LoadCustomerPortfolioTool;
import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.adk.finance.tools.ProjectKnowledgeTool;
import com.google.adk.finance.v3.agents.MarketResearchAgentFactory;
import com.google.adk.models.BaseLlm;
import com.google.adk.skills.LocalSkillSource;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.AgentTool;
import com.google.adk.tools.skills.SkillToolset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Week 2 — Version 4: Skills + Grounding Knowledge Agent Factory.
 * <p>
 * Core ADK concepts demonstrated:
 * <ul>
 *   <li>{@link SkillToolset} & {@link LocalSkillSource}: Loading domain skills on demand from {@code skills/finance/}.</li>
 *   <li>{@link ProjectKnowledgeTool}: Dynamic access to curated project grounding documents in {@code knowledge/}.</li>
 *   <li>Decoupled Google Search via {@link AgentTool} wrapping {@link MarketResearchAgentFactory}.</li>
 *   <li>Customer Portfolio Ingestion via {@link LoadCustomerPortfolioTool} and state templating.</li>
 *   <li>Deterministic Financial Math via {@link PortfolioMathTool}.</li>
 *   <li>Knowledge Hierarchy: Clearly distinguishing between Model Knowledge, Project Grounding Knowledge, and Current Web Information.</li>
 * </ul>
 */
public final class FinanceAdvisorAgentV4Factory {
    private static final Logger logger = LoggerFactory.getLogger(FinanceAdvisorAgentV4Factory.class);

    public static final String AGENT_NAME = "finance_advisor_v4";
    public static final Path FINANCE_SKILLS_PATH = Path.of("skills", "finance");
    public static final Path KNOWLEDGE_PATH = Path.of("knowledge");

    public static final String INSTRUCTION = """
            You are Finance Advisor v4, a financial research and education advisor equipped with project domain skills, curated grounding knowledge, customer portfolio state management, deterministic math, and Google Search.
            Your role is to help users understand investment concepts, companies, markets, valuation principles, and their personal portfolio structures with high transparency.

            Current Session State:
            - Customer ID: {customer_id?}
            - Portfolio ID: {portfolio_id?}
            - Active Portfolio Holdings:
            {portfolio_holdings?}

            Information Hierarchy & Grounding Rules:
            1. Clearly Distinguish Knowledge Sources:
               - General Model Knowledge: General concepts (e.g. "What is a stock?").
               - Project Grounding Knowledge: When discussing investment terminology, valuation principles, fundamental metrics, risk taxonomies, or portfolio concepts, consult your curated project resources using `read_project_knowledge` or load domain skills via `load_skill`.
               - Current Web Information: When a question requires recent, live, or changing information (e.g., current stock prices, recent earnings, recent announcements, this week's news), invoke `stockmarket_researcher` to retrieve fresh Google Search evidence.
            2. Customer Portfolio Ingestion & State Management:
               - If the user asks about their portfolio or provides a Customer ID (e.g. test customers 1001 or 1002), call `load_customer_portfolio` to fetch their active positions from SQLite.
               - Once loaded, answer questions about holdings, company names, quantities, and buy prices using the session state context without re-querying SQLite.
               - Leverage domain skills (`portfolio-analysis`, `risk-management`, `valuation`), deterministic math tools (`portfolio_math`), or live news retrieval (`stockmarket_researcher`) to evaluate the customer's active positions.
            3. Deterministic Arithmetic (Portfolio Math Tool):
               - For exact calculations (PnL, cost basis, return %, allocation weights, technical indicators), invoke `portfolio_math`.
            4. Source Priority:
               - For general concepts: Project Grounding Knowledge > Model Knowledge.
               - For current events: Current Google Search (`stockmarket_researcher`) > Project Knowledge (for framework interpretation) > Model Knowledge.
               - Never use project knowledge to assert a current empirical price or quarterly statistic.
            5. Grounding Transparency Structure:
               Where practical, organize your response with the following sections:
               * Executive Summary: Concise, direct answer to the user's inquiry.
               * Project Knowledge: Principles, definitions, and frameworks grounded in this project's curated knowledge.
               * Current Information: Empirical facts retrieved via Google Search (cite dates, figures, and sources). Include ONLY if current information was requested or needed.
               * Analysis & Interpretation: Objective reasoning applying the project framework to the facts without asserting false causation.
            6. Analytical Boundaries:
               - Do not invent financial facts or claim that a news headline proves price causation without verified evidence.
               - Never present investment decisions as guaranteed outcomes. Provide balanced educational decision support, not personalized investment advice.
            7. Mandatory Regulatory Disclaimer:
               Always conclude your response with the exact disclaimer:
               "Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice."
            """;

    /**
     * Creates an instance of Finance Advisor v4 wired with SkillToolset, ProjectKnowledgeTool,
     * stockmarket_researcher, PortfolioMathTool, and LoadCustomerPortfolioTool.
     */
    public static LlmAgent createFinanceAdvisorAgentV4() {
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

        // 4. Deterministic Financial Math Tool (from V3)
        toolsAndToolsets.add(new PortfolioMathTool());

        // 5. Customer Portfolio Ingestion from SQLite (from V2)
        toolsAndToolsets.add(new LoadCustomerPortfolioTool());

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Financial research & education advisor combining domain skills, curated project grounding knowledge, portfolio context, and Google Search.")
                .model(rootModel)
                .instruction(INSTRUCTION)
                .tools(toolsAndToolsets)
                .build();
    }

    private FinanceAdvisorAgentV4Factory() {}
}
