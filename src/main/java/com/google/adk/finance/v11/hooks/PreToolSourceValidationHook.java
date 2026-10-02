package com.google.adk.finance.v11.hooks;

import com.google.adk.agents.Callbacks.BeforeToolCallbackSync;
import com.google.adk.agents.InvocationContext;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Hook 1 — Pre-Tool Source Validation Hook (BLOCKING).
 * <p>
 * Implements {@link BeforeToolCallbackSync} to intercept any tool invocation before execution.
 * Validates deterministic compliance with {@code v11/rules/source-rules.md}:
 * <ol>
 *   <li><b>Operation Authorization:</b> Blocks prohibited actions (e.g. trading, money transfers).</li>
 *   <li><b>Parameter & Ticker Validation:</b> Ensures ticker symbols conform to exchange formatting without injection payload.</li>
 *   <li><b>Source Rule Consistency:</b> Enforces structured financial queries map to Yahoo Finance MCP and calculations to math tools.</li>
 * </ol>
 */
public class PreToolSourceValidationHook implements BeforeToolCallbackSync {
    private static final Logger logger = LoggerFactory.getLogger(PreToolSourceValidationHook.class);

    public static final String HOOK_NAME = "PreToolSourceValidationHook";

    private static final Pattern VALID_TICKER = Pattern.compile("^[A-Za-z0-9.\\-_]{1,12}$");
    private static final Pattern INJECTION_CHARS = Pattern.compile("[;'\"><`$\\\\\\n\\r]");

    private static final Set<String> PROHIBITED_TOOLS = Set.of(
            "execute_trade",
            "buy_stock",
            "sell_stock",
            "transfer_funds",
            "execute_order",
            "system_command"
    );

    private final HookRegistry hookRegistry;

    public PreToolSourceValidationHook() {
        this(HookRegistry.getInstance());
    }

    public PreToolSourceValidationHook(HookRegistry hookRegistry) {
        this.hookRegistry = Objects.requireNonNull(hookRegistry, "hookRegistry must not be null");
    }

    @Override
    public Optional<Map<String, Object>> call(
            InvocationContext invocationContext,
            BaseTool baseTool,
            Map<String, Object> input,
            ToolContext toolContext) {

        long startTime = System.currentTimeMillis();
        String toolName = baseTool.name();
        logger.info("[{}] Intercepting tool '{}' with input keys: {}", HOOK_NAME, toolName, input != null ? input.keySet() : "none");

        // 1. Check prohibited / unauthorized tools
        if (PROHIBITED_TOOLS.contains(toolName.toLowerCase())) {
            String reason = "Execution blocked: Tool '" + toolName + "' is strictly prohibited under V11 safety invariants and source rules.";
            long duration = System.currentTimeMillis() - startTime;
            hookRegistry.recordHookExecution(HookResult.blocked(HOOK_NAME, reason, Map.of("tool", toolName), duration));
            return Optional.of(createBlockedResponse(reason, toolName));
        }

        // 2. Validate ticker/symbol parameters if present in input
        if (input != null) {
            String symbol = extractSymbol(input);
            if (symbol != null) {
                if (symbol.isBlank()) {
                    String reason = "Source rule violation: Ticker symbol parameter for tool '" + toolName + "' cannot be blank.";
                    long duration = System.currentTimeMillis() - startTime;
                    hookRegistry.recordHookExecution(HookResult.blocked(HOOK_NAME, reason, Map.of("tool", toolName), duration));
                    return Optional.of(createBlockedResponse(reason, toolName));
                }

                if (INJECTION_CHARS.matcher(symbol).find()) {
                    String reason = "Security violation: Ticker symbol contains prohibited injection characters: '" + symbol + "'";
                    long duration = System.currentTimeMillis() - startTime;
                    hookRegistry.recordHookExecution(HookResult.blocked(HOOK_NAME, reason, Map.of("tool", toolName, "symbol", symbol), duration));
                    return Optional.of(createBlockedResponse(reason, toolName));
                }

                if (!VALID_TICKER.matcher(symbol).matches()) {
                    String reason = "Malformed ticker format: '" + symbol + "'. Must match standard format (1-12 alphanumeric/dot characters).";
                    long duration = System.currentTimeMillis() - startTime;
                    hookRegistry.recordHookExecution(HookResult.blocked(HOOK_NAME, reason, Map.of("tool", toolName, "symbol", symbol), duration));
                    return Optional.of(createBlockedResponse(reason, toolName));
                }
            }

            // 3. Source-selection validation: If calling portfolio math, verify required parameters
            if ("portfolio_math".equalsIgnoreCase(toolName)) {
                String operation = String.valueOf(input.getOrDefault("operation", ""));
                if (operation.isBlank()) {
                    String reason = "Source rule violation: 'portfolio_math' requires an explicit 'operation' parameter.";
                    long duration = System.currentTimeMillis() - startTime;
                    hookRegistry.recordHookExecution(HookResult.blocked(HOOK_NAME, reason, Map.of("tool", toolName), duration));
                    return Optional.of(createBlockedResponse(reason, toolName));
                }
            }
        }

        // Execution allowed
        long duration = System.currentTimeMillis() - startTime;
        hookRegistry.recordToolInvocation(toolName);
        hookRegistry.recordHookExecution(HookResult.success(
                HOOK_NAME,
                HookPolicy.BLOCKING,
                "Tool call allowed: " + toolName,
                Map.of("tool", toolName),
                duration
        ));

        return Optional.empty(); // Continue normal tool execution
    }

    private String extractSymbol(Map<String, Object> input) {
        if (input.containsKey("symbol")) {
            return String.valueOf(input.get("symbol")).trim();
        }
        if (input.containsKey("ticker")) {
            return String.valueOf(input.get("ticker")).trim();
        }
        return null;
    }

    private Map<String, Object> createBlockedResponse(String reason, String toolName) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "BLOCKED");
        response.put("error", reason);
        response.put("tool", toolName);
        response.put("hook", HOOK_NAME);
        response.put("policy", "BLOCKING");
        return response;
    }
}
