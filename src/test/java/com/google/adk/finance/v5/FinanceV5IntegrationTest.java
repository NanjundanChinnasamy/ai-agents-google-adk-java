package com.google.adk.finance.v5;

import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v2.CustomerPortfolioRepository;
import com.google.adk.finance.v2.LoadCustomerPortfolioTool;
import com.google.adk.finance.v3.PortfolioMathTool;
import com.google.adk.finance.v4.ProjectKnowledgeTool;
import com.google.adk.tools.BaseTool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FinanceV5IntegrationTest {

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        File dbFile = tempDir.resolve("integration_test_v5_portfolio.db").toFile();
        CustomerPortfolioRepository.setDbPath(dbFile.getAbsolutePath());
    }

    @AfterAll
    static void tearDown() {
        CustomerPortfolioRepository.setDbPath("finance_portfolio.db");
    }

    @Test
    @DisplayName("Test 1: Verify Yahoo Finance MCP Server Startup, Readiness & Dynamic Tool Discovery")
    void testMcpServerStartupAndToolDiscovery() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();

            assertThat(mcpManager.isInitialized()).isTrue();
            List<BaseTool> tools = mcpManager.getDiscoveredTools();
            assertThat(tools).isNotEmpty();
            assertThat(tools).hasSizeGreaterThanOrEqualTo(4);

            List<String> toolNames = tools.stream().map(BaseTool::name).toList();
            assertThat(toolNames).contains(
                    "get_stock_info",
                    "get_stock_actions",
                    "get_financial_statement",
                    "get_recommendations"
            );

            // Verify each tool has an informative description
            for (BaseTool tool : tools) {
                assertThat(tool.description()).isNotBlank();
            }
        }
    }

    @Test
    @DisplayName("Test 2: Verify MCP Price & Stock Info Lookup (get_stock_info)")
    void testMcpStockInfoLookup() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();

            BaseTool stockInfoTool = mcpManager.getDiscoveredTools().stream()
                    .filter(t -> t.name().equals("get_stock_info"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("get_stock_info not found"));

            Map<String, Object> result = stockInfoTool.runAsync(Map.of("ticker", "INFY"), null).blockingGet();
            assertThat(result).isNotNull();
            // Result is a map containing structured quote details or content
            assertThat(result.toString()).containsIgnoringCase("INFY");
        }
    }

    @Test
    @DisplayName("Test 3: Verify MCP Historical Data / Actions Lookup (get_stock_actions)")
    void testMcpStockActionsLookup() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();

            BaseTool actionsTool = mcpManager.getDiscoveredTools().stream()
                    .filter(t -> t.name().equals("get_stock_actions"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("get_stock_actions not found"));

            Map<String, Object> result = actionsTool.runAsync(Map.of("ticker", "AAPL"), null).blockingGet();
            assertThat(result).isNotNull();
            assertThat(result.toString()).contains("Dividends").contains("Stock Splits");
        }
    }

    @Test
    @DisplayName("Test 4: Verify Google Search Agent Tool is properly wired in Advisor V5")
    void testGoogleSearchWiringInV5() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();
            LlmAgent agent = FinanceAdvisorAgentV5Factory.createFinanceAdvisorAgentV5(mcpManager.getMcpToolset());

            List<BaseTool> tools = agent.tools().blockingGet();
            assertThat(tools).anyMatch(t -> t.name().equals("stockmarket_researcher"));
        }
    }

    @Test
    @DisplayName("Test 5: Combined Toolset Verification (MCP + Search + Skills + Knowledge + Math + Portfolio)")
    void testCombinedToolsetVerification() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();
            LlmAgent agent = FinanceAdvisorAgentV5Factory.createFinanceAdvisorAgentV5(mcpManager.getMcpToolset());

            assertThat(agent.name()).isEqualTo("finance_advisor_v5");
            assertThat(agent.instruction().toString()).contains("Finance Advisor v5");
            assertThat(agent.instruction().toString()).contains("get_stock_info");
            assertThat(agent.instruction().toString()).contains("stockmarket_researcher");
            assertThat(agent.instruction().toString()).contains("load_customer_portfolio");
            assertThat(agent.instruction().toString()).contains("portfolio_math");
            assertThat(agent.instruction().toString()).contains("read_project_knowledge");
            assertThat(agent.instruction().toString()).contains("Disclaimer");

            List<BaseTool> tools = agent.tools().blockingGet();
            List<String> names = tools.stream().map(BaseTool::name).toList();

            // 1. Yahoo Finance MCP tools
            assertThat(names).contains("get_stock_info", "get_stock_actions", "get_financial_statement", "get_recommendations");

            // 2. Google Search sub-agent tool
            assertThat(names).contains("stockmarket_researcher");

            // 3. Domain skills
            assertThat(names).contains("load_skill", "list_skills");

            // 4. Grounding knowledge
            assertThat(names).contains("read_project_knowledge");

            // 5. Custom Java tools (kept as requested)
            assertThat(names).contains("portfolio_math", "load_customer_portfolio");
        }
    }

    @Test
    @DisplayName("Test 6: Verify MCP Failure Handling on Missing Jar or Timeout")
    void testMcpFailureHandling() {
        Path invalidJar = Path.of("mcp", "non_existent_server.jar");
        YahooFinanceMcpClientManager manager = new YahooFinanceMcpClientManager(invalidJar, Duration.ofSeconds(2));

        assertThatThrownBy(manager::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Yahoo Finance MCP jar not found");
    }

    @Test
    @DisplayName("Test 7: Verify SQLite Portfolio Ingestion + Math Tool Integration in V5")
    void testPortfolioIngestionAndMath() {
        LoadCustomerPortfolioTool portfolioTool = new LoadCustomerPortfolioTool();
        Map<String, Object> portfolioResult = portfolioTool.runAsync(Map.of("customer_id", "1001"), null).blockingGet();
        assertThat(portfolioResult.get("status")).isEqualTo("success");
        assertThat(portfolioResult.get("customer_id")).isEqualTo("1001");

        PortfolioMathTool mathTool = new PortfolioMathTool();
        Map<String, Object> mathArgs = Map.of(
                "operation", "calculate_pnl",
                "symbol", "TEST",
                "quantity", 2.0,
                "buy_price", 1000.0,
                "current_price", 1250.0
        );
        Map<String, Object> mathResult = mathTool.runAsync(mathArgs, null).blockingGet();
        assertThat(mathResult.get("status")).isEqualTo("success");
        assertThat((Double) mathResult.get("unrealized_pnl")).isEqualTo(500.0);
        assertThat((Double) mathResult.get("return_pct")).isEqualTo(25.0);
    }

    @Test
    @DisplayName("Test 8: Verify Graceful Shutdown terminates MCP resources cleanly")
    void testGracefulShutdown() {
        YahooFinanceMcpClientManager manager = new YahooFinanceMcpClientManager();
        manager.start();
        assertThat(manager.isInitialized()).isTrue();

        manager.close();
        assertThat(manager.isInitialized()).isFalse();
    }
}
