package com.google.adk.socialspark;

import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v1.FinanceAgentV1Factory;
import com.google.adk.finance.v2.FinanceAgentV2Factory;
import com.google.adk.finance.v3.agents.FinanceAgentV3Factory;
import com.google.adk.finance.v3.agents.MarketResearchAgentFactory;
import com.google.adk.socialspark.agents.DraftAgentFactory;
import com.google.adk.socialspark.agents.ResearchAgentFactory;
import com.google.adk.socialspark.agents.SocialPosterAgentFactory;
import com.google.adk.web.AdkWebServer;

/**
 * Launcher for the official Google ADK Web Developer UI (AdkWebServer).
 * Opens the interactive DAG graph, event inspector, and streaming chat at http://localhost:8080/dev-ui
 */
public final class AdkDevUiApplication {

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("   Starting Google ADK Official Web Dev UI (Port 8080)   ");
        System.out.println("   Open in browser: http://localhost:8080/dev-ui          ");
        System.out.println("=========================================================");

        // Register agents with the Dev UI
        LlmAgent rootAgent = SocialPosterAgentFactory.createRootAgent();
        LlmAgent draftAgent = DraftAgentFactory.createDraftAgent();
        LlmAgent researchAgent = ResearchAgentFactory.createResearchAgent();
        LlmAgent financeAgentV1 = FinanceAgentV1Factory.createFinanceAgentV1();
        LlmAgent financeAgentV2 = FinanceAgentV2Factory.createFinanceAgentV2();
        LlmAgent financeAgentV3 = FinanceAgentV3Factory.createFinanceAgentV3();
        LlmAgent marketResearchAgent = MarketResearchAgentFactory.createMarketResearchAgent();

        // Boot Spring Boot ADK Web Server with static agent registration
        AdkWebServer.start(rootAgent, draftAgent, researchAgent, financeAgentV1, financeAgentV2, financeAgentV3, marketResearchAgent);
    }

    private AdkDevUiApplication() {}
}
