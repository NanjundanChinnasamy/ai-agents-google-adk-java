package com.google.adk.finance.v3;

import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.genai.types.FunctionDeclaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class PortfolioMathToolTest {

    private PortfolioMathTool tool;

    @BeforeEach
    void setUp() {
        tool = new PortfolioMathTool();
    }

    @Test
    void testToolDeclaration() {
        Optional<FunctionDeclaration> declOpt = tool.declaration();
        assertThat(declOpt).isPresent();

        FunctionDeclaration decl = declOpt.get();
        assertThat(decl.name()).hasValue("portfolio_math");
        assertThat(decl.description()).isPresent();
        assertThat(decl.parameters()).isPresent();
        assertThat(decl.parameters().get().properties()).isPresent();
        assertThat(decl.parameters().get().properties().get()).containsKey("operation");
        assertThat(decl.parameters().get().properties().get()).containsKey("symbol");
        assertThat(decl.parameters().get().properties().get()).containsKey("buy_price");
        assertThat(decl.parameters().get().properties().get()).containsKey("current_price");
    }

    @Test
    void testCalculatePnlProfitable() {
        Map<String, Object> args = Map.of(
                "operation", "calculate_pnl",
                "symbol", "RELIANCE",
                "buy_price", 1000.0,
                "current_price", 1250.0,
                "quantity", 2
        );

        Map<String, Object> result = tool.runAsync(args, null).blockingGet();

        assertThat(result).containsEntry("status", "success");
        assertThat(result).containsEntry("symbol", "RELIANCE");
        assertThat(result).containsEntry("total_cost", 2000.0);
        assertThat(result).containsEntry("current_value", 2500.0);
        assertThat(result).containsEntry("unrealized_pnl", 500.0);
        assertThat(result).containsEntry("return_pct", 25.0);
        assertThat(result).containsEntry("is_profitable", true);
    }

    @Test
    void testCalculatePnlUnprofitable() {
        Map<String, Object> args = Map.of(
                "operation", "calculate_pnl",
                "symbol", "INFY",
                "buy_price", 1500.0,
                "current_price", 1350.0,
                "quantity", 10
        );

        Map<String, Object> result = tool.runAsync(args, null).blockingGet();

        assertThat(result).containsEntry("status", "success");
        assertThat(result).containsEntry("total_cost", 15000.0);
        assertThat(result).containsEntry("current_value", 13500.0);
        assertThat(result).containsEntry("unrealized_pnl", -1500.0);
        assertThat(result).containsEntry("return_pct", -10.0);
        assertThat(result).containsEntry("is_profitable", false);
    }

    @Test
    void testCalculateAllocationConcentrated() {
        List<Map<String, Object>> positions = List.of(
                Map.of("symbol", "RELIANCE", "value", 3000.0),
                Map.of("symbol", "TCS", "value", 1000.0)
        );

        Map<String, Object> args = Map.of(
                "operation", "calculate_allocation",
                "positions", positions
        );

        Map<String, Object> result = tool.runAsync(args, null).blockingGet();

        assertThat(result).containsEntry("status", "success");
        assertThat(result).containsEntry("total_portfolio_value", 4000.0);
        assertThat(result).containsEntry("positions_count", 2);
        assertThat(result).containsEntry("max_concentration_symbol", "RELIANCE");
        assertThat(result).containsEntry("max_concentration_pct", 75.0);
        assertThat(result).containsEntry("concentration_risk_flag", true);
    }

    @Test
    void testCalculateAllocationDiversified() {
        List<Map<String, Object>> positions = List.of(
                Map.of("symbol", "A", "value", 200.0),
                Map.of("symbol", "B", "value", 200.0),
                Map.of("symbol", "C", "value", 200.0),
                Map.of("symbol", "D", "value", 200.0),
                Map.of("symbol", "E", "value", 200.0)
        );

        Map<String, Object> args = Map.of(
                "operation", "calculate_allocation",
                "positions", positions
        );

        Map<String, Object> result = tool.runAsync(args, null).blockingGet();

        assertThat(result).containsEntry("status", "success");
        assertThat(result).containsEntry("total_portfolio_value", 1000.0);
        assertThat(result).containsEntry("concentration_risk_flag", false);
    }

    @Test
    void testCalculateTechnicalIndicatorSma() {
        List<Double> prices = List.of(100.0, 102.0, 104.0, 106.0, 108.0);
        Map<String, Object> args = Map.of(
                "operation", "calculate_technical_indicator",
                "symbol", "AAPL",
                "indicator", "sma",
                "prices", prices
        );

        Map<String, Object> result = tool.runAsync(args, null).blockingGet();

        assertThat(result).containsEntry("status", "success");
        assertThat(result).containsEntry("symbol", "AAPL");
        assertThat(result).containsEntry("periods_analyzed", 5);
        assertThat(result).containsEntry("sma", 104.0);
        assertThat(result).containsEntry("first_price", 100.0);
        assertThat(result).containsEntry("last_price", 108.0);
        assertThat(result).containsEntry("price_change", 8.0);
        assertThat(result).containsEntry("price_change_pct", 8.0);
        assertThat(result).containsEntry("min_price", 100.0);
        assertThat(result).containsEntry("max_price", 108.0);
    }

    @Test
    void testUnknownOperation() {
        Map<String, Object> args = Map.of("operation", "non_existent_op");
        Map<String, Object> result = tool.runAsync(args, null).blockingGet();

        assertThat(result).containsEntry("status", "error");
        assertThat(result.get("message").toString()).contains("Supported operations");
    }
}
