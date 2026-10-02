package com.google.adk.finance.v11.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central registry and telemetry aggregator for V11 lifecycle hooks.
 */
public final class HookRegistry {
    private static final Logger logger = LoggerFactory.getLogger(HookRegistry.class);

    private final List<HookResult> executionHistory = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> toolInvocationCounts = new ConcurrentHashMap<>();
    private final List<String> invokedTools = new CopyOnWriteArrayList<>();

    private static final HookRegistry INSTANCE = new HookRegistry();

    public static HookRegistry getInstance() {
        return INSTANCE;
    }

    public void recordHookExecution(HookResult result) {
        executionHistory.add(result);
        if (result.blocked()) {
            logger.warn("[HOOK REGISTRY] BLOCKING HOOK TRIGGERED: {} -> {}", result.hookName(), result.message());
        } else {
            logger.debug("[HOOK REGISTRY] Hook recorded: {} (Policy: {}, Success: {}, Duration: {}ms)",
                    result.hookName(), result.policy(), result.success(), result.executionDurationMs());
        }
    }

    public void recordToolInvocation(String toolName) {
        invokedTools.add(toolName);
        toolInvocationCounts.merge(toolName, 1, Integer::sum);
    }

    public List<HookResult> getExecutionHistory() {
        return Collections.unmodifiableList(new ArrayList<>(executionHistory));
    }

    public List<HookResult> getHookResults(String hookName) {
        return executionHistory.stream()
                .filter(h -> h.hookName().equalsIgnoreCase(hookName))
                .toList();
    }

    public List<String> getInvokedTools() {
        return Collections.unmodifiableList(new ArrayList<>(invokedTools));
    }

    public boolean wasToolInvoked(String toolNamePrefix) {
        return invokedTools.stream().anyMatch(t -> t.toLowerCase().contains(toolNamePrefix.toLowerCase()));
    }

    public boolean wasMcpToolInvoked() {
        return invokedTools.stream().anyMatch(t ->
                t.startsWith("get_stock_") ||
                t.startsWith("get_financial_") ||
                t.startsWith("get_recommendations") ||
                t.contains("yahoo_finance") ||
                t.contains("mcp"));
    }

    public boolean wasSearchToolInvoked() {
        return invokedTools.stream().anyMatch(t ->
                t.contains("search") ||
                t.contains("stockmarket_researcher") ||
                t.contains("google_search"));
    }

    public long getBlockedCount() {
        return executionHistory.stream().filter(HookResult::blocked).count();
    }

    public long getSuccessCount() {
        return executionHistory.stream().filter(HookResult::success).count();
    }

    public void clearHistory() {
        executionHistory.clear();
        toolInvocationCounts.clear();
        invokedTools.clear();
    }
}
