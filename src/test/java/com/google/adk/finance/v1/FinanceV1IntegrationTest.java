package com.google.adk.finance.v1;

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.Session;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class FinanceV1IntegrationTest {

    @Test
    void testAgentConfiguration() {
        LlmAgent agent = FinanceAgentV1Factory.createFinanceAgentV1();

        assertThat(agent).isNotNull();
        assertThat(agent.name()).isEqualTo("finance_advisor_v1");
        assertThat(agent.description()).contains("financial analyst");
        assertThat(agent.instruction().toString()).contains("Finance Advisor v1");
        assertThat(agent.instruction().toString()).contains("Asset Classes");
        assertThat(agent.instruction().toString()).contains("Disclaimer");
        assertThat(agent.model()).isNotNull();
    }

    @Test
    void testSessionLifecycle() {
        LlmAgent agent = FinanceAgentV1Factory.createFinanceAgentV1();
        Runner runner = new InMemoryRunner(agent, FinanceAgentV1Factory.AGENT_NAME);

        String userId = "test-investor";
        String sessionId = "test-session-123";
        Map<String, Object> initialState = Map.of("experience_level", "intermediate");

        // Create session
        Session created = runner.sessionService()
                .createSession(runner.appName(), userId, initialState, sessionId)
                .blockingGet();

        assertThat(created).isNotNull();
        assertThat(created.userId()).isEqualTo(userId);
        assertThat(created.id()).isEqualTo(sessionId);
        assertThat(created.state()).containsEntry("experience_level", "intermediate");

        // Retrieve session
        Session retrieved = runner.sessionService()
                .getSession(runner.appName(), userId, sessionId, Optional.empty())
                .blockingGet();

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo(sessionId);
    }
}
