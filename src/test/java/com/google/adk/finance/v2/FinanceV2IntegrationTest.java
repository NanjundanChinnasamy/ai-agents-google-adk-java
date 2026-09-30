package com.google.adk.finance.v2;

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.Session;
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

public class FinanceV2IntegrationTest {

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        File dbFile = tempDir.resolve("integration_test_portfolio.db").toFile();
        CustomerPortfolioRepository.setDbPath(dbFile.getAbsolutePath());
    }

    @Test
    void testAgentConfiguration() {
        LlmAgent agent = FinanceAgentV2Factory.createFinanceAgentV2();

        assertThat(agent).isNotNull();
        assertThat(agent.name()).isEqualTo("finance_advisor_v2");
        assertThat(agent.description()).contains("holdings state management");
        assertThat(agent.instruction().toString()).contains("Finance Advisor v2");
        assertThat(agent.instruction().toString()).contains("{customer_id?}");
        assertThat(agent.instruction().toString()).contains("{portfolio_id?}");
        assertThat(agent.instruction().toString()).contains("{portfolio_holdings?}");
        assertThat(agent.instruction().toString()).contains("load_customer_portfolio");
        assertThat(agent.instruction().toString()).contains("Disclaimer");
        List<com.google.adk.tools.BaseTool> tools = agent.tools().blockingGet();
        assertThat(tools).isNotEmpty();
        assertThat(tools).anyMatch(t -> t instanceof LoadCustomerPortfolioTool);
    }

    @Test
    void testSessionStateLifecycleWithPortfolioContext() {
        LlmAgent agent = FinanceAgentV2Factory.createFinanceAgentV2();
        Runner runner = new InMemoryRunner(agent, FinanceAgentV2Factory.AGENT_NAME);

        String userId = "investor-test-v2";
        String sessionId = "finance-v2-test-session";

        Optional<PortfolioModels.CustomerPortfolio> portfolioOpt = CustomerPortfolioRepository.findPortfolioByCustomerId("1001");
        assertThat(portfolioOpt).isPresent();
        PortfolioModels.CustomerPortfolio portfolio = portfolioOpt.get();

        Map<String, Object> sessionState = new HashMap<>();
        sessionState.put("customer_id", portfolio.customerId());
        sessionState.put("portfolio_id", portfolio.portfolioId());
        sessionState.put("portfolio_holdings", portfolio.toFormattedSummary());
        sessionState.put("total_invested", portfolio.totalInvested());
        sessionState.put("portfolio_loaded", true);

        Session session = runner.sessionService()
                .createSession(runner.appName(), userId, sessionState, sessionId)
                .blockingGet();

        assertThat(session).isNotNull();
        assertThat(session.id()).isEqualTo(sessionId);

        // Verify retrieval of persistent session state
        Session retrieved = runner.sessionService()
                .getSession(runner.appName(), userId, sessionId, Optional.empty())
                .blockingGet();

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.state()).containsEntry("customer_id", "1001");
        assertThat(retrieved.state()).containsEntry("portfolio_id", "100001");
        assertThat(retrieved.state()).containsEntry("total_invested", 4000.0);
        assertThat(retrieved.state().get("portfolio_holdings").toString()).contains("RELIANCE");
        assertThat(retrieved.state().get("portfolio_holdings").toString()).contains("TCS");
    }
}
