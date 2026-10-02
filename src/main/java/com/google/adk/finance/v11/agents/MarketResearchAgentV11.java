package com.google.adk.finance.v11.agents;

import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v11.hooks.PostToolObservationHook;
import com.google.adk.finance.v11.hooks.PreToolSourceValidationHook;
import com.google.adk.finance.v11.ruleloader.RuleLoader;
import com.google.adk.finance.v11.ruleloader.ScopedRules;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.GoogleSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Specialized Market Research Sub-Agent for V11.
 * <p>
 * Scoped with:
 * <ul>
 *   <li>{@code research-rules.md}: Current grounding, recency, temporal delineation</li>
 *   <li>{@code source-rules.md}: Google Search for public web news</li>
 *   <li>{@code finance-rules.md}: Non-fabrication & empirical truthfulness</li>
 * </ul>
 * Protected by {@link PreToolSourceValidationHook} and {@link PostToolObservationHook}.
 */
public final class MarketResearchAgentV11 {
    private static final Logger logger = LoggerFactory.getLogger(MarketResearchAgentV11.class);

    public static final String AGENT_NAME = "market_researcher_v11";

    public static final String BASE_INSTRUCTION = """
            You are market_researcher_v11, a specialized autonomous market research agent.
            Your role is to retrieve current, verifiable public information (news, leadership updates, earnings releases,
            analyst ratings, and regulatory disclosures) using Google Search.

            OPERATIONAL MANDATES:
            1. Use GoogleSearchTool to investigate recent corporate developments and announcements.
            2. Never invent press releases, earnings numbers, or dates.
            3. Delineate between recent (last 90 days) events and older historical context.
            4. State specific source URLs and publication dates where available.
            """;

    public static LlmAgent create() {
        return create(RuleLoader.getInstance());
    }

    public static LlmAgent create(RuleLoader ruleLoader) {
        String modelName = AppConfig.get("RESEARCH_MODEL", AppConfig.RESEARCH_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);

        ScopedRules scopedRules = ruleLoader.forMarketResearchAgent();
        String fullInstruction = scopedRules.applyToInstruction(BASE_INSTRUCTION);

        logger.info("Initializing {} with model: {} and scoped rules: {}",
                AGENT_NAME, model.model(), scopedRules.ruleFileNames());

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("V11 Market Researcher - Grounds claims in current external public web data via Google Search.")
                .model(model)
                .instruction(fullInstruction)
                .tools(List.of(GoogleSearchTool.INSTANCE))
                .beforeToolCallbackSync(new PreToolSourceValidationHook())
                .afterToolCallbackSync(new PostToolObservationHook())
                .build();
    }

    private MarketResearchAgentV11() {}
}
