package com.google.adk.finance.v3.agents;

import com.google.adk.agents.LlmAgent;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.GoogleSearchTool;

import java.util.List;

/**
 * Atomic factory for the isolated Market Research Agent grounded strictly in {@link GoogleSearchTool#INSTANCE}.
 * <p>
 * Architectural Rationale:
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP)</b>: Encapsulates solely market research prompting and web search grounding.</li>
 *   <li><b>Gemini API Constraint</b>: The Gemini API does not allow mixing server-side Google Search grounding with client-side
 *       function declarations in the same generation step ({@code "Multiple tools are supported only when they are all search tools"}).
 *       Additionally, {@code "google_search"} is a reserved keyword in Gemini function calling. Isolating this agent with the
 *       name {@code "stockmarket_researcher"} satisfies both invariants.</li>
 * </ul>
 */
public final class MarketResearchAgentFactory {

    public static final String AGENT_NAME = "stockmarket_researcher";

    public static final String INSTRUCTION = """
            You are stockmarket_researcher, a specialized financial equity and stock market research agent.
            Perform Google searches to retrieve up-to-date stock market facts, company earnings reports, analyst ratings, SEC filings, and corporate news.
            Provide concise, grounded, factual findings citing dates, sources, and specific numbers.
            """;

    /**
     * Creates an instance of the market research agent using the specified model.
     *
     * @param model the Gemini language model instance
     * @return an {@link LlmAgent} configured strictly with GoogleSearchTool
     */
    public static LlmAgent createMarketResearchAgent(BaseLlm model) {
        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Grounds stock market and equity queries in live web search results via Google Search: recent company news, quarterly earnings/filings, analyst commentary/ratings, and macro events.")
                .model(model)
                .instruction(INSTRUCTION)
                .tools(List.of(GoogleSearchTool.INSTANCE))
                .outputKey("research_findings")
                .build();
    }

    /**
     * Creates an instance of the market research agent using the default configured Gemini model.
     *
     * @return an {@link LlmAgent} configured strictly with GoogleSearchTool
     */
    public static LlmAgent createMarketResearchAgent() {
        String modelName = AppConfig.get("FINANCE_SEARCH_MODEL", AppConfig.get("RESEARCH_MODEL", "gemini-2.5-flash"));
        if (modelName == null || (!modelName.startsWith("gemini-2") && !modelName.startsWith("gemini-3"))) {
            modelName = "gemini-2.5-flash";
        }
        return createMarketResearchAgent(AppConfig.createModel(modelName));
    }

    private MarketResearchAgentFactory() {}
}
