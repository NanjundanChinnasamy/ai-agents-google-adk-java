package com.google.adk.finance.v6;

import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v2.CustomerPortfolioRepository;
import com.google.adk.finance.tools.LoadCustomerPortfolioTool;
import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.adk.finance.v5.YahooFinanceMcpClientManager;
import com.google.adk.finance.v6.subagents.FundamentalAnalysisAgentV6;
import com.google.adk.finance.v6.subagents.MarketResearchAgentV6;
import com.google.adk.finance.v6.subagents.PortfolioRiskAgentV6;
import com.google.adk.finance.v6.subagents.ReportWriterAgentV6;
import com.google.adk.finance.v6.subagents.ScenarioAnalystAgentV6;
import com.google.adk.tools.AgentTool;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.GoogleSearchTool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FinanceV6IntegrationTest {

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        File dbFile = tempDir.resolve("integration_test_v6_portfolio.db").toFile();
        CustomerPortfolioRepository.setDbPath(dbFile.getAbsolutePath());
    }

    @AfterAll
    static void tearDown() {
        CustomerPortfolioRepository.setDbPath("finance_portfolio.db");
    }

    @Test
    @DisplayName("Test 1: Verify MarketResearchAgentV6 (stockmarket_researcher) Sub-Agent Configuration & Tool Ownership")
    void testMarketResearchSubAgentConfiguration() {
        LlmAgent agent = MarketResearchAgentV6.create();

        assertThat(agent).isNotNull();
        assertThat(agent.name()).isEqualTo("stockmarket_researcher");
        assertThat(agent.description()).contains("Google Search");
        assertThat(agent.instruction().toString()).contains("MarketResearchAgentV6");
        assertThat(agent.instruction().toString()).contains("SUB-AGENT REPORT: MarketResearchAgentV6");
        assertThat(agent.instruction().toString()).contains("Google Search");
        assertThat(agent.outputKey()).hasValue("market_research_report");

        List<BaseTool> tools = agent.tools().blockingGet();
        assertThat(tools).hasSize(1);
        assertThat(tools.get(0)).isSameAs(GoogleSearchTool.INSTANCE);
    }

    @Test
    @DisplayName("Test 2: Verify FundamentalAnalysisAgentV6 Sub-Agent Configuration & MCP Tool Ownership")
    void testFundamentalAnalysisSubAgentConfiguration() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();
            LlmAgent agent = FundamentalAnalysisAgentV6.create(mcpManager.getMcpToolset());

            assertThat(agent).isNotNull();
            assertThat(agent.name()).isEqualTo("fundamental_analysis_agent");
            assertThat(agent.description()).contains("fundamentals");
            assertThat(agent.instruction().toString()).contains("FundamentalAnalysisAgentV6");
            assertThat(agent.instruction().toString()).contains("SUB-AGENT REPORT: FundamentalAnalysisAgentV6");
            assertThat(agent.instruction().toString()).contains("get_stock_info");
            assertThat(agent.instruction().toString()).contains("get_financial_statement");
            assertThat(agent.instruction().toString()).contains("get_recommendations");
            assertThat(agent.outputKey()).hasValue("fundamental_analysis_report");

            List<BaseTool> tools = agent.tools().blockingGet();
            List<String> toolNames = tools.stream().map(BaseTool::name).toList();
            assertThat(toolNames).contains(
                    "get_stock_info",
                    "get_financial_statement",
                    "get_stock_actions",
                    "get_recommendations",
                    "read_project_knowledge"
            );
        }
    }

    @Test
    @DisplayName("Test 3: Verify PortfolioRiskAgentV6 Sub-Agent Configuration & Risk Capabilities")
    void testPortfolioRiskSubAgentConfiguration() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();
            LlmAgent agent = PortfolioRiskAgentV6.create(mcpManager.getMcpToolset());

            assertThat(agent).isNotNull();
            assertThat(agent.name()).isEqualTo("portfolio_risk_agent");
            assertThat(agent.description()).contains("risk");
            assertThat(agent.instruction().toString()).contains("PortfolioRiskAgentV6");
            assertThat(agent.instruction().toString()).contains("SUB-AGENT REPORT: PortfolioRiskAgentV6");
            assertThat(agent.instruction().toString()).contains("Beta");
            assertThat(agent.instruction().toString()).contains("portfolio_math");
            assertThat(agent.instruction().toString()).contains("read_project_knowledge");
            assertThat(agent.outputKey()).hasValue("portfolio_risk_report");

            List<BaseTool> tools = agent.tools().blockingGet();
            List<String> toolNames = tools.stream().map(BaseTool::name).toList();
            assertThat(toolNames).contains("get_stock_info", "read_project_knowledge", "portfolio_math");
        }
    }

    @Test
    @DisplayName("Test 4: Verify ScenarioAnalystAgentV6 Sub-Agent Configuration & Stress-Testing Capabilities")
    void testScenarioAnalystSubAgentConfiguration() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();
            LlmAgent agent = ScenarioAnalystAgentV6.create(mcpManager.getMcpToolset());

            assertThat(agent).isNotNull();
            assertThat(agent.name()).isEqualTo("scenario_analyst");
            assertThat(agent.description()).contains("scenario");
            assertThat(agent.instruction().toString()).contains("ScenarioAnalystAgentV6");
            assertThat(agent.instruction().toString()).contains("SUB-AGENT REPORT: ScenarioAnalystAgentV6");
            assertThat(agent.instruction().toString()).contains("Baseline Case");
            assertThat(agent.instruction().toString()).contains("Bull Case");
            assertThat(agent.instruction().toString()).contains("Bear Case");
            assertThat(agent.outputKey()).hasValue("scenario_analysis_report");

            List<BaseTool> tools = agent.tools().blockingGet();
            List<String> toolNames = tools.stream().map(BaseTool::name).toList();
            assertThat(toolNames).contains("get_stock_info", "read_project_knowledge", "portfolio_math");
        }
    }

    @Test
    @DisplayName("Test 5: Verify ReportWriterAgentV6 Sub-Agent Configuration & Report Synthesis Capabilities")
    void testReportWriterSubAgentConfiguration() {
        LlmAgent agent = ReportWriterAgentV6.create();

        assertThat(agent).isNotNull();
        assertThat(agent.name()).isEqualTo("report_writer");
        assertThat(agent.description()).contains("report");
        assertThat(agent.instruction().toString()).contains("ReportWriterAgentV6");
        assertThat(agent.instruction().toString()).contains("SUB-AGENT REPORT: ReportWriterAgentV6");
        assertThat(agent.instruction().toString()).contains("Executive Summary");
        assertThat(agent.instruction().toString()).contains("Empirical Evidence Matrix");
        assertThat(agent.outputKey()).hasValue("executive_decision_report");

        List<BaseTool> tools = agent.tools().blockingGet();
        List<String> toolNames = tools.stream().map(BaseTool::name).toList();
        assertThat(toolNames).contains("read_project_knowledge");
    }

    @Test
    @DisplayName("Test 6: Verify Portfolio Director (FinanceAdvisorAgentV6) Hierarchy & 5-Specialist Delegation")
    void testParentAgentWiringAndDelegationArchitecture() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();
            LlmAgent parentAgent = FinanceAdvisorAgentV6.createFinanceAdvisorAgentV6(mcpManager.getMcpToolset());

            assertThat(parentAgent).isNotNull();
            assertThat(parentAgent.name()).isEqualTo("finance_advisor_v6");
            assertThat(parentAgent.instruction().toString()).contains("portfolio_director");
            assertThat(parentAgent.instruction().toString()).contains("Specialized Sub-Agent Delegation Matrix");
            assertThat(parentAgent.instruction().toString()).contains("stockmarket_researcher");
            assertThat(parentAgent.instruction().toString()).contains("scenario_analyst");
            assertThat(parentAgent.instruction().toString()).contains("report_writer");
            assertThat(parentAgent.instruction().toString()).contains("fundamental_analysis_agent");
            assertThat(parentAgent.instruction().toString()).contains("portfolio_risk_agent");
            assertThat(parentAgent.instruction().toString()).contains("Multi-Specialist Queries");
            assertThat(parentAgent.instruction().toString()).contains("Failure Isolation & Graceful Degradation");
            assertThat(parentAgent.instruction().toString()).contains("Disclaimer");

            List<BaseTool> tools = parentAgent.tools().blockingGet();
            List<String> toolNames = tools.stream().map(BaseTool::name).toList();

            // 1. Parent MUST have all 5 sub-agents wrapped as AgentTool
            assertThat(toolNames).contains(
                    "stockmarket_researcher",
                    "scenario_analyst",
                    "report_writer",
                    "fundamental_analysis_agent",
                    "portfolio_risk_agent"
            );

            // 2. Parent MUST have custom portfolio loading and math tools
            assertThat(toolNames).contains("load_customer_portfolio", "portfolio_math");

            // Verify all 5 sub-agents are instances of AgentTool
            long agentToolCount = tools.stream().filter(t -> t instanceof AgentTool).count();
            assertThat(agentToolCount).isEqualTo(5);
        }
    }

    @Test
    @DisplayName("Test 7: Verify Multi-Specialist Analysis Execution (MCP Quote + Risk + Research Tools)")
    void testMultiSpecialistSubAgentExecution() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();

            // 1. Verify Fundamental Specialist MCP tool executes live quote lookup
            BaseTool stockInfoTool = mcpManager.getDiscoveredTools().stream()
                    .filter(t -> t.name().equals("get_stock_info"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("get_stock_info tool missing"));

            Map<String, Object> quoteResult = stockInfoTool.runAsync(Map.of("ticker", "INFY"), null).blockingGet();
            assertThat(quoteResult).isNotNull();
            assertThat(quoteResult.toString()).containsIgnoringCase("INFY");

            // 2. Verify Risk Specialist math tool computes concentration weighting
            PortfolioMathTool mathTool = new PortfolioMathTool();
            List<Map<String, Object>> positions = List.of(
                    Map.of("symbol", "INFY", "value", 3500.0),
                    Map.of("symbol", "TCS", "value", 6500.0)
            );
            Map<String, Object> allocArgs = Map.of(
                    "operation", "calculate_allocation",
                    "positions", positions
            );
            Map<String, Object> allocResult = mathTool.runAsync(allocArgs, null).blockingGet();
            assertThat(allocResult.get("status")).isEqualTo("success");
            assertThat((Boolean) allocResult.get("concentration_risk_flag")).isTrue();
            assertThat((Double) allocResult.get("max_concentration_pct")).isEqualTo(65.0);
        }
    }

    @Test
    @DisplayName("Test 8: Verify Failure Isolation & Graceful Handling when MCP is unavailable")
    void testSpecialistFailureIsolation() {
        // Parent agent handles missing or null MCP toolsets gracefully without crashing
        LlmAgent degradedParent = FinanceAdvisorAgentV6.createFinanceAdvisorAgentV6(null);
        assertThat(degradedParent).isNotNull();
        assertThat(degradedParent.name()).isEqualTo("finance_advisor_v6");

        List<BaseTool> tools = degradedParent.tools().blockingGet();
        assertThat(tools).isNotEmpty();
        // Even when MCP is absent, all 5 sub-agent tools remain registered to explain limitations to user
        assertThat(tools.stream().map(BaseTool::name).toList()).contains(
                "stockmarket_researcher",
                "scenario_analyst",
                "report_writer",
                "fundamental_analysis_agent",
                "portfolio_risk_agent"
        );
    }

    @Test
    @DisplayName("Test 9: Verify SQLite Portfolio Ingestion & Session State Integration in V6")
    void testPortfolioIngestionInV6() {
        LoadCustomerPortfolioTool portfolioTool = new LoadCustomerPortfolioTool();
        Map<String, Object> result = portfolioTool.runAsync(Map.of("customer_id", "1001"), null).blockingGet();
        assertThat(result.get("status")).isEqualTo("success");
        assertThat(result.get("customer_id")).isEqualTo("1001");
        assertThat(result.get("summary").toString()).contains("RELIANCE").contains("TCS");
    }
}
