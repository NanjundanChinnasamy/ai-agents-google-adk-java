package com.google.adk.finance.v8.subagents;

import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.reactivex.rxjava3.core.Single;

import java.util.Map;
import java.util.Optional;

/**
 * Educational testbed agent providing a mock financial trade execution tool ({@code execute_trade}).
 * <p>
 * This class serves as the explicit verification vehicle demonstrating that {@link com.google.adk.finance.v8.guardrails.tool.ToolOperationGuard}
 * and {@link com.google.adk.finance.v8.callbacks.BeforeToolGuardrail} successfully intercept and block unauthorized
 * transactional capabilities before any external action or trade execution can take place.
 */
public final class MockTradingAgentV8 {

    public static class ExecuteTradeTool extends BaseTool {
        public static final String TOOL_NAME = "execute_trade";

        public ExecuteTradeTool() {
            super(TOOL_NAME, "Places an order to buy or sell equities or securities in the market.");
        }

        @Override
        public Optional<FunctionDeclaration> declaration() {
            return Optional.of(FunctionDeclaration.builder()
                    .name(TOOL_NAME)
                    .description("Places an order to buy or sell equities or securities in the market.")
                    .parameters(Schema.builder()
                            .type(Type.Known.OBJECT)
                            .properties(Map.of(
                                    "symbol", Schema.builder().type(Type.Known.STRING).description("Ticker symbol").build(),
                                    "action", Schema.builder().type(Type.Known.STRING).description("BUY or SELL").build(),
                                    "quantity", Schema.builder().type(Type.Known.INTEGER).description("Number of shares").build()
                            ))
                            .required(java.util.List.of("symbol", "action", "quantity"))
                            .build())
                    .build());
        }

        @Override
        public Single<Map<String, Object>> runAsync(Map<String, Object> arguments, ToolContext toolContext) {
            // If this code runs, it means the guardrail failed to intercept!
            return Single.just(Map.of(
                    "status", "EXECUTED_UNGUARDED",
                    "symbol", arguments.getOrDefault("symbol", "UNKNOWN"),
                    "action", arguments.getOrDefault("action", "UNKNOWN"),
                    "quantity", arguments.getOrDefault("quantity", 0),
                    "warning", "CRITICAL: Trade executed without guardrail interception!"
            ));
        }
    }

    private MockTradingAgentV8() {}
}
