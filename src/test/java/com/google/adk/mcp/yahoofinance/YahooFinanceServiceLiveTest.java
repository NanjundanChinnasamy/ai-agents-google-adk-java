package com.google.adk.mcp.yahoofinance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YahooFinanceServiceLiveTest {
    private static YahooFinanceService service;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        service = new YahooFinanceService();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Tool 1: get_stock_info should return rich stock metrics for AAPL")
    void testGetStockInfo() throws Exception {
        String json = service.getStockInfo("AAPL");
        assertThat(json).isNotNull();
        assertThat(json).doesNotStartWith("Error:");
        assertThat(json).doesNotStartWith("No stock info found");

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.has("symbol")).isTrue();
        assertThat(root.get("symbol").asText()).isEqualTo("AAPL");
        assertThat(root.has("shortName")).isTrue();
        assertThat(root.has("currency")).isTrue();
        assertThat(root.get("currency").asText()).isEqualTo("USD");
        assertThat(root.has("regularMarketPrice")).isTrue();
        assertThat(root.get("regularMarketPrice").asDouble()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Tool 2: get_stock_actions should return dividends and splits history")
    void testGetStockActions() throws Exception {
        String json = service.getStockActions("AAPL");
        assertThat(json).isNotNull();
        assertThat(json).doesNotStartWith("Error:");

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.isArray()).isTrue();
        // AAPL has multiple dividend payouts and stock splits over 5 years
        assertThat(root.size()).isGreaterThan(0);

        JsonNode firstAction = root.get(0);
        assertThat(firstAction.has("Date")).isTrue();
        assertThat(firstAction.has("Dividends")).isTrue();
        assertThat(firstAction.has("Stock Splits")).isTrue();
    }

    @Test
    @DisplayName("Tool 3: get_financial_statement should return income statement for AAPL")
    void testGetFinancialStatementIncomeStmt() throws Exception {
        String json = service.getFinancialStatement("AAPL", "income_stmt");
        assertThat(json).isNotNull();
        assertThat(json).doesNotStartWith("Error:");

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.isArray()).isTrue();
        assertThat(root.size()).isGreaterThan(0);

        JsonNode firstStmt = root.get(0);
        assertThat(firstStmt.has("date")).isTrue();
        assertThat(firstStmt.has("totalRevenue")).isTrue();
        assertThat(firstStmt.get("totalRevenue").asDouble()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Tool 3: get_financial_statement should return error on invalid type")
    void testGetFinancialStatementInvalidType() {
        String json = service.getFinancialStatement("AAPL", "invalid_stmt_type");
        assertThat(json).startsWith("Error: invalid financial type invalid_stmt_type.");
    }

    @Test
    @DisplayName("Tool 4: get_recommendations should return trends for recommendations type")
    void testGetRecommendationsTrends() throws Exception {
        String json = service.getRecommendations("AAPL", "recommendations", 12);
        assertThat(json).isNotNull();
        assertThat(json).doesNotStartWith("Error:");

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.isArray()).isTrue();
        assertThat(root.size()).isGreaterThan(0);

        JsonNode firstTrend = root.get(0);
        assertThat(firstTrend.has("period")).isTrue();
        assertThat(firstTrend.has("strongBuy")).isTrue();
        assertThat(firstTrend.has("buy")).isTrue();
    }

    @Test
    @DisplayName("Tool 4: get_recommendations should return firm history for upgrades_downgrades")
    void testGetRecommendationsUpgradesDowngrades() throws Exception {
        String json = service.getRecommendations("AAPL", "upgrades_downgrades", 12);
        assertThat(json).isNotNull();
        assertThat(json).doesNotStartWith("Error:");

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.isArray()).isTrue();
        if (root.size() > 0) {
            JsonNode item = root.get(0);
            assertThat(item.has("GradeDate")).isTrue();
            assertThat(item.has("Firm")).isTrue();
            assertThat(item.has("ToGrade")).isTrue();
        }
    }

    @Test
    @DisplayName("Tool 4: get_recommendations should return error on invalid type")
    void testGetRecommendationsInvalidType() {
        String json = service.getRecommendations("AAPL", "invalid_rec_type", 12);
        assertThat(json).startsWith("Error: invalid recommendation type invalid_rec_type.");
    }
}
