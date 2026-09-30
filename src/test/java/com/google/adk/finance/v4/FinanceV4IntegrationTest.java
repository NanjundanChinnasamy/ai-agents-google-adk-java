package com.google.adk.finance.v4;

import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v2.LoadCustomerPortfolioTool;
import com.google.adk.finance.v3.PortfolioMathTool;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.BaseTool;
import com.google.adk.finance.v2.CustomerPortfolioRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FinanceV4IntegrationTest {

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        File dbFile = tempDir.resolve("integration_test_v4_portfolio.db").toFile();
        CustomerPortfolioRepository.setDbPath(dbFile.getAbsolutePath());
    }

    @AfterAll
    static void tearDown() {
        CustomerPortfolioRepository.setDbPath("finance_portfolio.db");
    }

    @Test
    @DisplayName("Verify ProjectKnowledgeTool reads curated markdown documents successfully")
    void testProjectKnowledgeToolExecution() {
        ProjectKnowledgeTool tool = new ProjectKnowledgeTool(Path.of("knowledge"));

        assertThat(tool.name()).isEqualTo("read_project_knowledge");
        assertThat(tool.declaration()).isPresent();

        // 1. Read glossary
        Map<String, Object> glossaryResult = tool.runAsync(Map.of("topic", "glossary"), null).blockingGet();
        assertThat(glossaryResult.get("status")).isEqualTo("success");
        assertThat(glossaryResult.get("content").toString()).contains("Common Stock").contains("Market Capitalization");

        // 2. Read valuation principles
        Map<String, Object> valuationResult = tool.runAsync(Map.of("topic", "valuation-principles"), null).blockingGet();
        assertThat(valuationResult.get("status")).isEqualTo("success");
        assertThat(valuationResult.get("content").toString()).contains("Trailing P/E").contains("Discounted Cash Flow");

        // 3. Read risk framework
        Map<String, Object> riskResult = tool.runAsync(Map.of("topic", "risk-framework"), null).blockingGet();
        assertThat(riskResult.get("status")).isEqualTo("success");
        assertThat(riskResult.get("content").toString()).contains("Concentration Risk").contains("Systematic");

        // 4. Test unknown topic gracefully returns available topics
        Map<String, Object> unknownResult = tool.runAsync(Map.of("topic", "crypto-speculation"), null).blockingGet();
        assertThat(unknownResult.get("status")).isEqualTo("error");
        assertThat(unknownResult).containsKey("available_topics");
    }

    @Test
    @DisplayName("Verify Finance Advisor V4 Agent configuration and tool registrations")
    void testAgentConfiguration() {
        LlmAgent agent = FinanceAdvisorAgentV4Factory.createFinanceAdvisorAgentV4();

        assertThat(agent).isNotNull();
        assertThat(agent.name()).isEqualTo("finance_advisor_v4");
        assertThat(agent.description()).contains("curated project grounding knowledge");
        assertThat(agent.instruction().toString()).contains("Finance Advisor v4");
        assertThat(agent.instruction().toString()).contains("read_project_knowledge");
        assertThat(agent.instruction().toString()).contains("market_researcher");
        assertThat(agent.instruction().toString()).contains("Project Knowledge");
        assertThat(agent.instruction().toString()).contains("Current Information");
        assertThat(agent.instruction().toString()).contains("Disclaimer");

        List<BaseTool> tools = agent.tools().blockingGet();
        assertThat(tools).isNotEmpty();
        // Should contain ProjectKnowledgeTool
        assertThat(tools).anyMatch(t -> t instanceof ProjectKnowledgeTool);
        // Should contain market_researcher AgentTool
        assertThat(tools).anyMatch(t -> t.name().equals("market_researcher"));
        // Should contain SkillToolset tools (load_skill, list_skills)
        assertThat(tools).anyMatch(t -> t.name().equals("load_skill") || t.name().equals("list_skills"));
        // Should contain PortfolioMathTool from V3
        assertThat(tools).anyMatch(t -> t instanceof PortfolioMathTool);
        // Should contain LoadCustomerPortfolioTool from V2
        assertThat(tools).anyMatch(t -> t instanceof LoadCustomerPortfolioTool);
    }

    @Test
    @DisplayName("Verify Customer Portfolio Ingestion tool execution in Advisor V4")
    void testCustomerPortfolioIngestion() {
        LoadCustomerPortfolioTool tool = new LoadCustomerPortfolioTool();
        Map<String, Object> result = tool.runAsync(Map.of("customer_id", "1001"), null).blockingGet();

        assertThat(result.get("status")).isEqualTo("success");
        assertThat(result.get("customer_id")).isEqualTo("1001");
        assertThat(result.get("portfolio_id")).isEqualTo("100001");
        assertThat((Integer) result.get("holdings_count")).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Verify Session Lifecycle and Initial State Binding for Advisor V4")
    void testSessionLifecycle() {
        LlmAgent agent = FinanceAdvisorAgentV4Factory.createFinanceAdvisorAgentV4();
        Runner runner = new InMemoryRunner(agent, FinanceAdvisorAgentV4Factory.AGENT_NAME);

        String userId = "learner-user-4";
        String sessionId = "v4-test-session-" + System.currentTimeMillis();
        Map<String, Object> initialState = new HashMap<>();
        initialState.put("learning_mode", "fundamentals");

        Session session = runner.sessionService()
                .createSession(runner.appName(), userId, initialState, sessionId)
                .blockingGet();
        assertThat(session).isNotNull();
        assertThat(session.state().get("learning_mode")).isEqualTo("fundamentals");
    }

    @Test
    @DisplayName("Test 1: Concept Question - What is P/E? (Knowledge is sufficient)")
    void testConceptQuestionPe() {
        ProjectKnowledgeTool tool = new ProjectKnowledgeTool(Path.of("knowledge"));
        Map<String, Object> result = tool.runAsync(Map.of("topic", "valuation-principles"), null).blockingGet();

        assertThat(result.get("status")).isEqualTo("success");
        String content = (String) result.get("content");
        assertThat(content).contains("Price-to-Earnings");
        assertThat(content).contains("Forward P/E");
        // Enforces rule: high P/E does not automatically mean overvalued
        assertThat(content).contains("high P/E does not automatically mean a stock is overvalued");
    }

    @Test
    @DisplayName("Test 2: Risk Question - What is concentration risk? (Risk framework knowledge)")
    void testRiskQuestionConcentration() {
        ProjectKnowledgeTool tool = new ProjectKnowledgeTool(Path.of("knowledge"));
        Map<String, Object> result = tool.runAsync(Map.of("topic", "risk-framework"), null).blockingGet();

        assertThat(result.get("status")).isEqualTo("success");
        String content = (String) result.get("content");
        assertThat(content).contains("Concentration Risk");
        assertThat(content).contains("Single-Stock Concentration");
        assertThat(content).contains("Sector Concentration");
    }

    @Test
    @DisplayName("Test 3: Portfolio Principles - Asset Allocation and Diversification")
    void testPortfolioPrinciples() {
        ProjectKnowledgeTool tool = new ProjectKnowledgeTool(Path.of("knowledge"));
        Map<String, Object> result = tool.runAsync(Map.of("topic", "portfolio-principles"), null).blockingGet();

        assertThat(result.get("status")).isEqualTo("success");
        String content = (String) result.get("content");
        assertThat(content).contains("Asset Allocation");
        assertThat(content).contains("Correlation");
        assertThat(content).contains("Diversification");
    }

    @Test
    @DisplayName("Test 4: Fundamentals Baseline - What is a stock?")
    void testFundamentalsGlossaryStock() {
        ProjectKnowledgeTool tool = new ProjectKnowledgeTool(Path.of("knowledge"));
        Map<String, Object> result = tool.runAsync(Map.of("topic", "glossary"), null).blockingGet();

        assertThat(result.get("status")).isEqualTo("success");
        String content = (String) result.get("content");
        assertThat(content).contains("Common Stock");
        assertThat(content).contains("fractional ownership");
    }

    @Test
    @DisplayName("Test 5: Market Research Framework Structure")
    void testMarketResearchFramework() {
        ProjectKnowledgeTool tool = new ProjectKnowledgeTool(Path.of("knowledge"));
        Map<String, Object> result = tool.runAsync(Map.of("topic", "market-research-framework"), null).blockingGet();

        assertThat(result.get("status")).isEqualTo("success");
        String content = (String) result.get("content");
        assertThat(content).contains("Corporate Filings");
        assertThat(content).contains("Consensus Estimates");
        assertThat(content).contains("Earnings Surprise");
    }
}
