package com.google.adk.finance.v11;

import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.adk.finance.v11.hooks.HookPolicy;
import com.google.adk.finance.v11.hooks.HookRegistry;
import com.google.adk.finance.v11.hooks.PreToolSourceValidationHook;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.FunctionDeclaration;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PreToolValidationHookTest {

    private PreToolSourceValidationHook hook;
    private HookRegistry hookRegistry;

    @BeforeEach
    void setUp() {
        hookRegistry = new HookRegistry();
        hook = new PreToolSourceValidationHook(hookRegistry);
    }

    @Test
    @DisplayName("Should allow valid tool calls with conformant parameters")
    void shouldAllowValidToolCalls() {
        BaseTool mockStockTool = new DummyTool("get_stock_info");
        Map<String, Object> input = Map.of("symbol", "INFY");

        Optional<Map<String, Object>> result = hook.call(null, mockStockTool, input, null);

        assertThat(result).isEmpty();
        assertThat(hookRegistry.getBlockedCount()).isEqualTo(0);
        assertThat(hookRegistry.getSuccessCount()).isEqualTo(1);
        assertThat(hookRegistry.wasToolInvoked("get_stock_info")).isTrue();
    }

    @Test
    @DisplayName("Should BLOCK prohibited trading tool (execute_trade) under BLOCKING policy")
    void shouldBlockProhibitedTradingTool() {
        BaseTool tradeTool = new DummyTool("execute_trade");
        Map<String, Object> input = Map.of("symbol", "INFY", "action", "BUY", "quantity", 50);

        Optional<Map<String, Object>> result = hook.call(null, tradeTool, input, null);

        assertThat(result).isPresent();
        Map<String, Object> payload = result.get();
        assertThat(payload.get("status")).isEqualTo("BLOCKED");
        assertThat(payload.get("policy")).isEqualTo("BLOCKING");
        assertThat(payload.get("error").toString()).contains("strictly prohibited");
        assertThat(hookRegistry.getBlockedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should BLOCK ticker containing command or SQL injection characters")
    void shouldBlockInjectionCharactersInTicker() {
        BaseTool stockTool = new DummyTool("get_stock_info");
        Map<String, Object> input = Map.of("symbol", "INFY; DROP TABLE");

        Optional<Map<String, Object>> result = hook.call(null, stockTool, input, null);

        assertThat(result).isPresent();
        assertThat(result.get().get("status")).isEqualTo("BLOCKED");
        assertThat(result.get().get("error").toString()).contains("injection characters");
        assertThat(hookRegistry.getBlockedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should BLOCK blank ticker symbol")
    void shouldBlockBlankTicker() {
        BaseTool stockTool = new DummyTool("get_stock_info");
        Map<String, Object> input = Map.of("symbol", "   ");

        Optional<Map<String, Object>> result = hook.call(null, stockTool, input, null);

        assertThat(result).isPresent();
        assertThat(result.get().get("status")).isEqualTo("BLOCKED");
        assertThat(result.get().get("error").toString()).contains("cannot be blank");
    }

    @Test
    @DisplayName("Should BLOCK excessively long malformed ticker")
    void shouldBlockExcessivelyLongTicker() {
        BaseTool stockTool = new DummyTool("get_stock_info");
        Map<String, Object> input = Map.of("symbol", "VERYLONGTICKERNAMEEXCEEDINGLIMITS");

        Optional<Map<String, Object>> result = hook.call(null, stockTool, input, null);

        assertThat(result).isPresent();
        assertThat(result.get().get("status")).isEqualTo("BLOCKED");
        assertThat(result.get().get("error").toString()).contains("Malformed ticker format");
    }

    @Test
    @DisplayName("Should BLOCK portfolio_math tool call missing operation parameter")
    void shouldBlockPortfolioMathMissingOperation() {
        PortfolioMathTool mathTool = new PortfolioMathTool();
        Map<String, Object> input = Map.of("symbol", "INFY"); // missing "operation"

        Optional<Map<String, Object>> result = hook.call(null, mathTool, input, null);

        assertThat(result).isPresent();
        assertThat(result.get().get("status")).isEqualTo("BLOCKED");
        assertThat(result.get().get("error").toString()).contains("requires an explicit 'operation' parameter");
    }

    private static class DummyTool extends BaseTool {
        public DummyTool(String name) {
            super(name, "Dummy test tool");
        }

        @Override
        public Optional<FunctionDeclaration> declaration() {
            return Optional.of(FunctionDeclaration.builder().name(name()).description(description()).build());
        }

        @Override
        public Single<Map<String, Object>> runAsync(Map<String, Object> input, ToolContext toolContext) {
            return Single.just(Map.of("status", "OK"));
        }
    }
}
