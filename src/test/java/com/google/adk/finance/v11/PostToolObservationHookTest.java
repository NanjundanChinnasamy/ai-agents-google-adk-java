package com.google.adk.finance.v11;

import com.google.adk.finance.v11.hooks.HookPolicy;
import com.google.adk.finance.v11.hooks.HookRegistry;
import com.google.adk.finance.v11.hooks.PostToolObservationHook;
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

class PostToolObservationHookTest {

    private PostToolObservationHook hook;
    private HookRegistry hookRegistry;

    @BeforeEach
    void setUp() {
        hookRegistry = new HookRegistry();
        hook = new PostToolObservationHook(hookRegistry);
    }

    @Test
    @DisplayName("Should observe successful tool execution and record metadata without blocking")
    void shouldObserveSuccessfulToolExecution() {
        BaseTool mcpTool = new DummyTool("get_stock_info");
        Map<String, Object> input = Map.of("symbol", "INFY");
        String toolResponse = "{\"symbol\":\"INFY\",\"price\":1850.50,\"currency\":\"INR\"}";

        Optional<Map<String, Object>> result = hook.call(null, mcpTool, input, null, toolResponse);

        assertThat(result).isEmpty(); // Non-blocking: output untouched
        assertThat(hookRegistry.getExecutionHistory()).hasSize(1);

        var record = hookRegistry.getExecutionHistory().get(0);
        assertThat(record.hookName()).isEqualTo(PostToolObservationHook.HOOK_NAME);
        assertThat(record.policy()).isEqualTo(HookPolicy.NON_BLOCKING);
        assertThat(record.success()).isTrue();
        assertThat(record.blocked()).isFalse();
        assertThat(record.metadata().get("capability")).isEqualTo("MCP_FINANCIAL_DATA");
        assertThat(record.metadata().get("hasResponse")).isEqualTo(true);
        assertThat(record.metadata().get("isError")).isEqualTo(false);
    }

    @Test
    @DisplayName("Should categorize search capability correctly")
    void shouldCategorizeSearchCapability() {
        BaseTool searchTool = new DummyTool("stockmarket_researcher");
        Map<String, Object> input = Map.of("query", "Infosys Q3 results");
        String toolResponse = "Recent earnings summary";

        hook.call(null, searchTool, input, null, toolResponse);

        var record = hookRegistry.getExecutionHistory().get(0);
        assertThat(record.metadata().get("capability")).isEqualTo("PUBLIC_WEB_SEARCH");
    }

    @Test
    @DisplayName("Should detect blocked tool error response gracefully without throwing exception")
    void shouldDetectBlockedErrorResponse() {
        BaseTool tradeTool = new DummyTool("execute_trade");
        Map<String, Object> blockedResponse = Map.of("status", "BLOCKED", "error", "Prohibited tool");

        Optional<Map<String, Object>> result = hook.call(null, tradeTool, Map.of(), null, blockedResponse);

        assertThat(result).isEmpty();
        var record = hookRegistry.getExecutionHistory().get(0);
        assertThat(record.metadata().get("isError")).isEqualTo(true);
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
