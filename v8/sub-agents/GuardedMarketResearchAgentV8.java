package com.google.adk.finance.v8.subagents;

import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v8.callbacks.BeforeAgentGuardrail;
import com.google.adk.finance.v8.callbacks.BeforeModelGuardrail;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.GoogleSearchTool;

import java.util.List;

/**
 * Isolated market research sub-agent grounded strictly in {@link GoogleSearchTool#INSTANCE},
 * protected by V8 input and model-level guardrails.
 */
public final class GuardedMarketResearchAgentV8 {
    public static final String AGENT_NAME = "stockmarket_researcher";

    public static final String INSTRUCTION = """
            You are stockmarket_researcher, a specialized financial equity and market research agent.
            Perform Google searches to retrieve real-time stock market facts, company earnings reports,
            analyst ratings, SEC filings, and corporate developments.
            Provide concise, factual findings citing dates, sources, and verified numbers.
            """;

    public static LlmAgent createGuardedMarketResearchAgent(BaseLlm model) {
        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Grounds stock market and equity queries in live web search results via Google Search: recent company news, quarterly earnings/filings, analyst commentary/ratings, and macro events.")
                .model(model)
                .instruction(INSTRUCTION)
                .tools(List.of(GoogleSearchTool.INSTANCE))
                .beforeAgentCallbackSync(new BeforeAgentGuardrail())
                .beforeModelCallbackSync(new BeforeModelGuardrail())
                .outputKey("research_findings")
                .build();
    }

    public static LlmAgent createGuardedMarketResearchAgent() {
        String modelName = AppConfig.get("FINANCE_SEARCH_MODEL", AppConfig.get("RESEARCH_MODEL", "gemini-2.5-flash"));
        if (modelName == null || (!modelName.startsWith("gemini-2") && !modelName.startsWith("gemini-3"))) {
            modelName = "gemini-2.5-flash";
        }
        return createGuardedMarketResearchAgent(AppConfig.createModel(modelName));
    }

    private GuardedMarketResearchAgentV8() {}
}
