package com.google.adk.finance.v10.observability.events;

import com.google.adk.finance.v10.observability.EventType;
import com.google.adk.finance.v10.observability.ExecutionEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Event helper for tool invocation initiation.
 * Identifies tool source (MCP Yahoo, Search, Java Tool) and captures sanitized arguments.
 */
public final class ToolCallEvent {

    public static ExecutionEvent started(
            String executionId,
            String agentName,
            String toolName,
            String toolSource,
            Map<String, Object> safeArgs) {

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("toolName", toolName != null ? toolName : "unknown_tool");
        meta.put("toolSource", toolSource != null ? toolSource : resolveSource(toolName));
        if (safeArgs != null) {
            meta.putAll(sanitizeArgs(safeArgs));
        }

        String targetInfo = extractTargetInfo(safeArgs);
        String summary = String.format("Invoking tool '%s' [%s]%s",
                toolName, meta.get("toolSource"), targetInfo.isEmpty() ? "" : " on " + targetInfo);

        return ExecutionEvent.builder(executionId, EventType.TOOL_CALL_STARTED)
                .agentName(agentName)
                .component("TOOL")
                .status("STARTED")
                .safeSummary(summary)
                .metadata(meta)
                .build();
    }

    public static String resolveSource(String toolName) {
        if (toolName == null) return "JAVA_LOCAL";
        String lower = toolName.toLowerCase();
        if (lower.contains("yahoo") || lower.contains("stock_info") || lower.contains("financial")) {
            return "YAHOO_FINANCE_MCP";
        } else if (lower.contains("search") || lower.contains("research")) {
            return "GOOGLE_SEARCH";
        } else if (lower.contains("math") || lower.contains("pnl") || lower.contains("allocation")) {
            return "PORTFOLIO_MATH";
        } else if (lower.contains("portfolio") || lower.contains("holding")) {
            return "SQLITE_PORTFOLIO_DB";
        } else if (lower.contains("knowledge")) {
            return "LOCAL_KNOWLEDGE";
        }
        return "JAVA_LOCAL";
    }

    private static Map<String, Object> sanitizeArgs(Map<String, Object> rawArgs) {
        Map<String, Object> clean = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawArgs.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (val == null) continue;

            String keyLower = key.toLowerCase();
            if (keyLower.contains("pass") || keyLower.contains("secret") || keyLower.contains("token")
                    || keyLower.contains("key") || keyLower.contains("auth")) {
                clean.put(key, "[REDACTED_SECRET]");
            } else if (keyLower.contains("account") || keyLower.contains("card") || keyLower.contains("ssn")) {
                clean.put(key, "[REDACTED_FINANCIAL_ID]");
            } else if (keyLower.contains("customer")) {
                clean.put(key, "[CUSTOMER_IDENTIFIER]");
            } else {
                clean.put(key, val);
            }
        }
        return clean;
    }

    private static String extractTargetInfo(Map<String, Object> args) {
        if (args == null) return "";
        if (args.containsKey("ticker")) return "ticker=" + args.get("ticker");
        if (args.containsKey("symbol")) return "symbol=" + args.get("symbol");
        if (args.containsKey("query")) return "query=\"" + args.get("query") + "\"";
        if (args.containsKey("operation")) return "op=" + args.get("operation");
        return "";
    }
}
