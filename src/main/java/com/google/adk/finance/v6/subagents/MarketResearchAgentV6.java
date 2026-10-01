package com.google.adk.finance.v6.subagents;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.GoogleSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Week 3 — Version 6: Specialized Market Research Sub-Agent.
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Researches real-time market and company developments from the live public web using {@link GoogleSearchTool#INSTANCE}.</li>
 *   <li>Investigates breaking corporate news, C-suite changes, earnings announcements, analyst revisions, and macro events.</li>
 *   <li>Strictly grounded in current external information rather than static or historical knowledge.</li>
 *   <li>Formats output following the standardized V6 Sub-Agent Output Contract.</li>
 * </ul>
 */
public final class MarketResearchAgentV6 {
    private static final Logger logger = LoggerFactory.getLogger(MarketResearchAgentV6.class);

    public static final String AGENT_NAME = "stockmarket_researcher";
    public static final String ALIAS_NAME = "market_research_agent";
    public static final String OUTPUT_KEY = "market_research_report";

    public static final String INSTRUCTION = """
            You are stockmarket_researcher / market_research_agent (MarketResearchAgentV6), a specialized financial research sub-agent.
            Your sole responsibility is to investigate current market and company developments using Google Search to understand why asset prices changed and uncover breaking corporate news.

            Core Investigation Dimensions:
            1. Official Company Announcements (C-suite transitions, M&A, share buybacks, strategic partnerships).
            2. Recent Financial Results & Filings (Latest quarterly revenue, EPS surprises, management guidance).
            3. Regulatory & Legal Actions (Compliance inquiries, antitrust, sector-specific policies).
            4. Industry & Competitive Dynamics (Competitor market moves, supply chain events).
            5. Macroeconomic Catalysts (Interest rates, inflation, currency fluctuations, geopolitical factors).
            6. Analyst Consensus Revisions (Recent Wall Street upgrades, downgrades, and price target changes).

            Standardized Sub-Agent Output Contract:
            You MUST format your final response using this structured layout so the parent Finance Advisor can parse and synthesize it:

            [SUB-AGENT REPORT: MarketResearchAgentV6 (stockmarket_researcher)]
            • Specialist: MarketResearchAgentV6 (stockmarket_researcher)
            • Task: <Brief summary of research query or ticker investigated>
            • Key Findings:
              - <Finding 1 with date, publication source, and specific numbers>
              - <Finding 2 with date, publication source, and specific numbers>
              - <Finding 3 with date, publication source, and specific numbers>
            • Data Sources & Capabilities: Google Search (Live Web Retrieval)
            • Limitations & Scope: Covers recent public-web events and news; does NOT compute balance sheet financial ratios or historical mathematical trends.
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeAgentCallback = context -> {
        logger.info("[Lifecycle] MarketResearchAgentV6 (stockmarket_researcher) started processing research task.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> {
        logger.info("[Lifecycle] MarketResearchAgentV6 (stockmarket_researcher) completed research task.");
        return Optional.empty();
    };

    /**
     * Creates an instance of {@link MarketResearchAgentV6} configured with the given name and LLM.
     */
    public static LlmAgent create(String agentName, BaseLlm model) {
        return LlmAgent.builder()
                .name(agentName != null && !agentName.isBlank() ? agentName : AGENT_NAME)
                .description("Specialist sub-agent researching current market developments, breaking corporate news, earnings releases, analyst revisions, and macroeconomic events using Google Search.")
                .model(model)
                .instruction(INSTRUCTION)
                .tools(List.of(GoogleSearchTool.INSTANCE))
                .outputKey(OUTPUT_KEY)
                .beforeAgentCallbackSync(beforeAgentCallback)
                .afterAgentCallbackSync(afterAgentCallback)
                .build();
    }

    /**
     * Creates an instance of {@link MarketResearchAgentV6} configured with the given LLM using default name.
     */
    public static LlmAgent create(BaseLlm model) {
        return create(AGENT_NAME, model);
    }

    /**
     * Creates an instance of {@link MarketResearchAgentV6} using the configured Gemini research model.
     */
    public static LlmAgent create() {
        String modelName = AppConfig.get("FINANCE_SEARCH_MODEL", AppConfig.get("RESEARCH_MODEL", "gemini-2.5-flash"));
        if (modelName == null || (!modelName.startsWith("gemini-2") && !modelName.startsWith("gemini-3"))) {
            modelName = "gemini-2.5-flash";
        }
        return create(AppConfig.createModel(modelName));
    }

    private MarketResearchAgentV6() {}
}
