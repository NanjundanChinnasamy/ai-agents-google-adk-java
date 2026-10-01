package com.google.adk.finance.v8.guardrails.tool;

import com.google.adk.finance.v8.guardrails.GuardrailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Deterministic authorization guardrail enforcing tool execution boundaries for Finance Advisor V8.
 * <p>
 * Enforces the strict institutional rule that Finance Advisor is an analytical and research system.
 * Financial transaction execution (e.g. trading, placing orders, transferring funds) and arbitrary
 * system modifications are categorically prohibited.
 */
public class ToolOperationGuard {
    private static final Logger logger = LoggerFactory.getLogger(ToolOperationGuard.class);

    // Permitted read-only research, mathematical calculation, knowledge, and workflow tools
    public static final Set<String> ALLOWED_TOOLS = Set.of(
            // Yahoo Finance MCP Tools
            "get_stock_info",
            "get_stock_actions",
            "get_financial_statement",
            "get_recommendations",

            // Domain Tools
            "load_customer_portfolio",
            "portfolio_math",
            "read_project_knowledge",

            // Search Grounding Sub-Agent Tool
            "stockmarket_researcher",

            // Deterministic Workflow Tools
            "run_sequential_research_workflow",
            "run_parallel_portfolio_research_workflow",
            "run_critic_loop_research_workflow",

            // ADK Loop Utility
            "exit_loop"
    );

    // Explicitly blocked operations (unauthorized for an advisory agent)
    public static final Set<String> FORBIDDEN_OPERATIONS = Set.of(
            "execute_trade",
            "place_order",
            "buy_stock",
            "sell_stock",
            "create_order",
            "cancel_order",
            "transfer_funds",
            "withdraw_cash",
            "wire_transfer",
            "send_payment",
            "modify_account",
            "update_balance",
            "change_password",
            "delete_account",
            "get_credentials",
            "dump_env",
            "read_keys",
            "list_secrets",
            "execute_command",
            "shell_exec",
            "delete_file",
            "write_file"
    );

    public ToolOperationGuard() {}

    /**
     * Evaluates whether a requested tool operation is authorized for execution.
     *
     * @param toolName the name of the tool to be invoked
     * @return {@link GuardrailResult#allow} if authorized, or {@link GuardrailResult#block} if prohibited
     */
    public GuardrailResult evaluate(String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return GuardrailResult.block("ToolOperationGuard", "Tool name must not be empty.");
        }

        String normalized = toolName.trim().toLowerCase();

        // 1. Check explicit forbidden operations
        if (FORBIDDEN_OPERATIONS.contains(normalized)) {
            logger.error("[Tool Guardrail] Categorically blocked forbidden financial or system operation: '{}'", toolName);
            return GuardrailResult.block(
                    "ToolOperationGuard",
                    "Unauthorized Operation: '" + toolName + "' is strictly prohibited. " +
                    "Finance Advisor V8 is a research and decision-support assistant and does not possess authority to execute " +
                    "financial transactions, account modifications, or system commands.",
                    Map.of("toolName", toolName, "violationType", "FORBIDDEN_OPERATION")
            );
        }

        // 2. Check if tool is within the permitted analytical toolset
        if (!ALLOWED_TOOLS.contains(normalized)) {
            logger.warn("[Tool Guardrail] Blocked unknown or unauthorized tool: '{}'", toolName);
            return GuardrailResult.block(
                    "ToolOperationGuard",
                    "Access Denied: Tool '" + toolName + "' is not registered in the permitted research toolset for Finance Advisor V8.",
                    Map.of("toolName", toolName, "violationType", "UNREGISTERED_TOOL")
            );
        }

        return GuardrailResult.allow(
                "ToolOperationGuard",
                "Tool '" + toolName + "' is authorized for analytical research.",
                Map.of("toolName", toolName)
        );
    }
}
