package com.google.adk.socialspark.agents;

import com.google.adk.a2a.agent.RemoteA2AAgent;
import com.google.adk.agents.BaseAgent;
import com.google.adk.socialspark.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MemoryAgentFactory {
    private static final Logger logger = LoggerFactory.getLogger(MemoryAgentFactory.class);

    public static BaseAgent createMemoryAgent() {
        String cardUrl = AppConfig.MEMORY_AGENT_CARD_URL;
        if (cardUrl == null || cardUrl.isBlank()) {
            return null;
        }

        try {
            logger.info("Configuring RemoteA2AAgent for memory from {}", cardUrl);
            // Constructing RemoteA2AAgent with card URL
            return RemoteA2AAgent.builder()
                    .name("memory_agent")
                    .description("Knows the user's past posts. Consult for their usual topics, tone, and phrasing before drafting.")
                    .build();
        } catch (Exception e) {
            logger.warn("Could not initialize RemoteA2AAgent from {}: {}", cardUrl, e.getMessage());
            return null;
        }
    }

    private MemoryAgentFactory() {}
}
