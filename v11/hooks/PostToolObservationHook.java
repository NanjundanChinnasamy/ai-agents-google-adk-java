package com.google.adk.finance.v11.hooks;

import com.google.adk.agents.Callbacks.AfterToolCallbackSync;
import com.google.adk.agents.InvocationContext;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Hook 2 — Post-Tool Observation Hook (NON-BLOCKING).
 */
public class PostToolObservationHook implements AfterToolCallbackSync {
    private static final Logger logger = LoggerFactory.getLogger(PostToolObservationHook.class);

    public static final String HOOK_NAME = "PostToolObservationHook";

    private final HookRegistry hookRegistry;

    public PostToolObservationHook() {
        this(HookRegistry.getInstance());
    }

    public PostToolObservationHook(HookRegistry hookRegistry) {
        this.hookRegistry = Objects.requireNonNull(hookRegistry, "hookRegistry must not be null");
    }

    @Override
    public Optional<Map<String, Object>> call(
            InvocationContext invocationContext,
            BaseTool baseTool,
            Map<String, Object> input,
            ToolContext toolContext,
            Object response) {

        long startTime = System.currentTimeMillis();
        String toolName = baseTool.name();

        try {
            String capability = categorizeCapability(toolName);
            boolean hasResponse = response != null;
            int responseLength = 0;
            boolean isError = false;

            if (response != null) {
                String responseStr = response.toString();
                responseLength = responseStr.length();
                if (response instanceof Map<?, ?> respMap) {
                    if ("BLOCKED".equalsIgnoreCase(String.valueOf(respMap.get("status"))) ||
                            respMap.containsKey("error")) {
                        isError = true;
                    }
                }
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("capability", capability);
            metadata.put("tool", toolName);
            metadata.put("hasResponse", hasResponse);
            metadata.put("responseSizeChars", responseLength);
            metadata.put("isError", isError);

            long duration = System.currentTimeMillis() - startTime;
            HookResult result = HookResult.success(
                    HOOK_NAME,
                    HookPolicy.NON_BLOCKING,
                    "Observed completion of " + capability + " tool '" + toolName + "'",
                    metadata,
                    duration
            );

            hookRegistry.recordHookExecution(result);
            logger.info("[{}] Tool '{}' [{}] observed successfully (size: {} chars, error: {})",
                    HOOK_NAME, toolName, capability, responseLength, isError);

        } catch (Exception ex) {
            logger.warn("[{}] Non-blocking observation error on tool '{}': {}", HOOK_NAME, toolName, ex.getMessage());
            long duration = System.currentTimeMillis() - startTime;
            hookRegistry.recordHookExecution(HookResult.failure(
                    HOOK_NAME,
                    HookPolicy.NON_BLOCKING,
                    "Observation failed: " + ex.getMessage(),
                    Map.of("tool", toolName),
                    duration
            ));
        }

        return Optional.empty();
    }

    private String categorizeCapability(String toolName) {
        String lower = toolName.toLowerCase();
        if (lower.startsWith("get_stock_") || lower.startsWith("get_financial_") ||
                lower.startsWith("get_recommendations") || lower.contains("yahoo")) {
            return "MCP_FINANCIAL_DATA";
        }
        if (lower.contains("search") || lower.contains("google")) {
            return "PUBLIC_WEB_SEARCH";
        }
        if (lower.contains("portfolio_math") || lower.contains("math")) {
            return "DETERMINISTIC_MATH";
        }
        if (lower.contains("customer") || lower.contains("portfolio") || lower.contains("load")) {
            return "RELATIONAL_PERSISTENCE";
        }
        if (lower.contains("knowledge") || lower.contains("skill")) {
            return "PROJECT_KNOWLEDGE";
        }
        return "GENERAL_TOOL";
    }
}
