package com.google.adk.socialspark.agents;

import com.google.adk.agents.LlmAgent;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.GoogleSearchTool;

import java.util.List;

public final class ResearchAgentFactory {

    public static LlmAgent createResearchAgent() {
        String modelName = AppConfig.RESEARCH_MODEL;
        if (modelName == null || (!modelName.startsWith("gemini-2") && !modelName.startsWith("gemini-3"))) {
            modelName = "gemini-2.5-flash";
        }

        return LlmAgent.builder()
                .name("research_agent")
                .description("Researches facts, dates, and context on the web for a post idea.")
                .model(AppConfig.createModel(modelName))
                .instruction("""
                    Research the given topic with google_search and return a
                    concise, factual summary: key facts, dates, numbers, and anything surprising
                    or quotable. No drafting - just the research notes.
                    """)
                .tools(List.of(GoogleSearchTool.INSTANCE))
                .outputKey("research_notes")
                .build();
    }

    private ResearchAgentFactory() {}
}
