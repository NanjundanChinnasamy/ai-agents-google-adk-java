package com.google.adk.finance.v3;

import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v2.CustomerPortfolioRepository;
import com.google.adk.finance.v2.LoadCustomerPortfolioTool;
import com.google.adk.finance.v3.agents.FinanceAgentV3Factory;
import com.google.adk.finance.v3.agents.MarketResearchAgentFactory;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.GoogleSearchTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class FinanceV3IntegrationTest {

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        File dbFile = tempDir.resolve("integration_test_v3_portfolio.db").toFile();
        CustomerPortfolioRepository.setDbPath(dbFile.getAbsolutePath());
    }

    @Test
    void testMarketResearchAgentConfiguration() {
        LlmAgent searchAgent = MarketResearchAgentFactory.createMarketResearchAgent();

        assertThat(searchAgent).isNotNull();
        assertThat(searchAgent.name()).isEqualTo("market_researcher");
        assertThat(searchAgent.tools().blockingGet()).hasSize(1);
        assertThat(searchAgent.tools().blockingGet().get(0)).isSameAs(GoogleSearchTool.INSTANCE);
        assertThat(searchAgent.outputKey()).isEqualTo(Optional.of("research_findings"));
    }

    @Test
    void testAgentConfiguration() {
        LlmAgent agent = FinanceAgentV3Factory.createFinanceAgentV3();

        assertThat(agent).isNotNull();
        assertThat(agent.name()).isEqualTo("finance_agent_v3");
        assertThat(agent.description()).contains("Google Search evidence");
        assertThat(agent.instruction().toString()).contains("FinanceAgent v3");
        assertThat(agent.instruction().toString()).contains("market_researcher");
        assertThat(agent.instruction().toString()).contains("portfolio_math");
        assertThat(agent.instruction().toString()).contains("load_customer_portfolio");
        assertThat(agent.instruction().toString()).contains("Disclaimer");

        List<BaseTool> tools = agent.tools().blockingGet();
        assertThat(tools).isNotEmpty();
        assertThat(tools).anyMatch(t -> t.name().equals("market_researcher"));
        assertThat(tools).anyMatch(t -> t instanceof PortfolioMathTool);
        assertThat(tools).anyMatch(t -> t instanceof LoadCustomerPortfolioTool);
    }

    @Test
    void testSessionLifecycle() {
        LlmAgent agent = FinanceAgentV3Factory.createFinanceAgentV3();
        Runner runner = new InMemoryRunner(agent, FinanceAgentV3Factory.AGENT_NAME);

        String userId = "analyst-user-3";
        String sessionId = "v3-session-" + System.currentTimeMillis();
        Map<String, Object> initialState = new HashMap<>();
        initialState.put("focus_ticker", "RELIANCE");

        Session session = runner.sessionService()
                .createSession(runner.appName(), userId, initialState, sessionId)
                .blockingGet();

        assertThat(session).isNotNull();
        assertThat(session.id()).isEqualTo(sessionId);
        assertThat(session.state()).containsEntry("focus_ticker", "RELIANCE");

        Session retrieved = runner.sessionService()
                .getSession(runner.appName(), userId, sessionId, Optional.empty())
                .blockingGet();

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.state()).containsEntry("focus_ticker", "RELIANCE");
    }
}
