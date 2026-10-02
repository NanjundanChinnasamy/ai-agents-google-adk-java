package com.google.adk.finance.v11;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.finance.v5.YahooFinanceMcpClientManager;
import com.google.adk.finance.v11.hooks.HookRegistry;
import com.google.adk.finance.v11.hooks.HookResult;
import com.google.adk.finance.v11.ruleloader.RuleLoader;
import com.google.adk.finance.v11.ruleloader.ScopedRules;
import com.google.adk.finance.v11.workflows.SequentialResearchWorkflowV11;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.mcp.McpToolset;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

/**
 * Interactive Terminal Console for Finance Advisor V11.
 * <p>
 * Supports live rule inspection, hook audit trails, blocking/non-blocking demonstrations,
 * and sequential workflow execution.
 */
public final class FinanceConsoleV11 {
    private static final Logger logger = LoggerFactory.getLogger(FinanceConsoleV11.class);

    private static final String BANNER = """
            ================================================================================
            FINANCE ADVISOR V11 — RULES-DRIVEN AND HOOK-AWARE AGENT CONSOLE
            Google Agent Development Kit (ADK) Java 1.4.0
            ================================================================================
            Commands:
              /rules           : List all 5 V11 rules files and loaded statuses
              /rule <name>     : Print content of a specific rule file (e.g. /rule source-rules.md)
              /scoped          : Inspect rule scoping across specialists
              /hooks           : Display hook audit trail and telemetry from HookRegistry
              /test-blocking   : Run test verifying BLOCKING hook on prohibited tool call
              /test-nonblocking: Run test verifying NON-BLOCKING post-tool observation
              /workflow <sym>  : Execute 4-stage sequential workflow with stage hooks (e.g. /workflow INFY)
              /scenario        : Execute canonical Infosys investment research scenario
              /help            : Display this help message
              exit / quit      : Terminate console
            ================================================================================
            """;

    public static void main(String[] args) {
        System.out.println(BANNER);

        YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager();
        McpToolset mcpToolset = null;
        try {
            logger.info("Initializing Yahoo Finance MCP Client Manager...");
            mcpManager.start();
            mcpToolset = mcpManager.getMcpToolset();
            System.out.println("[MCP] Yahoo Finance MCP Server connected successfully.");
        } catch (Exception e) {
            System.out.println("[MCP WARNING] Could not start Yahoo Finance MCP server: " + e.getMessage());
            System.out.println("[MCP WARNING] Running in mock/offline mode.");
        }

        RuleLoader ruleLoader = RuleLoader.getInstance();
        HookRegistry hookRegistry = HookRegistry.getInstance();
        FinanceAdvisorAgentV11 advisor = FinanceAdvisorAgentV11.create(mcpToolset, ruleLoader, hookRegistry);
        LlmAgent agent = advisor.getAgent();

        Runner runner = new InMemoryRunner(agent, FinanceAdvisorAgentV11.AGENT_NAME);
        String userId = "finance-learner-11";
        String sessionId = "v11-session-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> sessionState = new java.util.HashMap<>();

        try {
            runner.sessionService()
                    .getSession(runner.appName(), userId, sessionId, Optional.empty())
                    .switchIfEmpty(runner.sessionService().createSession(runner.appName(), userId, sessionState, sessionId))
                    .blockingGet();
        } catch (Exception ignored) {}

        // One-shot argument execution
        if (args != null && args.length > 0 && !args[0].isBlank()) {
            String initialQuery = String.join(" ", args);
            System.out.println("\nExecuting one-shot query: " + initialQuery);
            handleUserQuery(runner, userId, sessionId, sessionState, initialQuery);
            shutdown(mcpManager);
            return;
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n[V11] > ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                break;
            }
            if (line.isBlank()) {
                continue;
            }

            if (line.equalsIgnoreCase("/help")) {
                System.out.println(BANNER);
            } else if (line.equalsIgnoreCase("/rules")) {
                printRules(ruleLoader);
            } else if (line.startsWith("/rule ")) {
                String ruleName = line.substring(6).trim();
                printSingleRule(ruleLoader, ruleName);
            } else if (line.equalsIgnoreCase("/scoped")) {
                printScopedRules(ruleLoader);
            } else if (line.equalsIgnoreCase("/hooks")) {
                printHookTelemetry(hookRegistry);
            } else if (line.equalsIgnoreCase("/test-blocking")) {
                testBlockingHook(advisor);
            } else if (line.equalsIgnoreCase("/test-nonblocking")) {
                testNonBlockingHook(runner, userId, sessionId, sessionState);
            } else if (line.startsWith("/workflow")) {
                String ticker = line.length() > 9 ? line.substring(9).trim() : "INFY";
                if (ticker.isBlank()) ticker = "INFY";
                runWorkflow(mcpToolset, ruleLoader, hookRegistry, ticker);
            } else if (line.equalsIgnoreCase("/scenario")) {
                runCanonicalScenario(runner, userId, sessionId, sessionState);
            } else {
                handleUserQuery(runner, userId, sessionId, sessionState, line);
            }
        }

        shutdown(mcpManager);
    }

    private static void handleUserQuery(Runner runner, String userId, String sessionId, Map<String, Object> sessionState, String query) {
        System.out.println("\n[Processing via FinanceAdvisorAgentV11...]");
        try {
            Content userContent = Content.builder()
                    .role("user")
                    .parts(List.of(Part.fromText(query)))
                    .build();

            Flowable<Event> events = runner.runAsync(userId, sessionId, userContent, RunConfig.builder().build(), sessionState);
            StringBuilder response = new StringBuilder();

            events.blockingForEach(event -> {
                if (event.content().isPresent()) {
                    Content c = event.content().get();
                    for (Part p : c.parts().orElse(List.of())) {
                        p.text().ifPresent(response::append);
                    }
                }
            });

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(response.toString().trim());
            System.out.println("--------------------------------------------------------------------------------");

        } catch (Exception ex) {
            System.err.println("[ERROR] Agent invocation failed: " + ex.getMessage());
            logger.error("Agent failure", ex);
        }
    }

    private static void printRules(RuleLoader ruleLoader) {
        System.out.println("\n=== V11 ACTIVE RULES FILES ===");
        Map<String, String> rules = ruleLoader.loadAllRules();
        for (Map.Entry<String, String> entry : rules.entrySet()) {
            System.out.printf(" - %-20s : %5d characters | %s\n",
                    entry.getKey(),
                    entry.getValue().length(),
                    entry.getValue().startsWith("# Error") ? "FAILED" : "LOADED");
        }
    }

    private static void printSingleRule(RuleLoader ruleLoader, String ruleName) {
        if (!ruleName.endsWith(".md")) {
            ruleName += ".md";
        }
        System.out.println("\n=== CONTENT OF " + ruleName + " ===");
        String content = ruleLoader.loadRule(ruleName);
        System.out.println(content);
    }

    private static void printScopedRules(RuleLoader ruleLoader) {
        System.out.println("\n=== V11 SCOPED RULE DISTRIBUTIONS ===");
        printScopeDetails(ruleLoader.forFinanceAgent());
        printScopeDetails(ruleLoader.forMarketResearchAgent());
        printScopeDetails(ruleLoader.forFundamentalAnalysisAgent());
        printScopeDetails(ruleLoader.forRiskAnalysisAgent());
        printScopeDetails(ruleLoader.forResponseSynthesisAgent());
        printScopeDetails(ruleLoader.forRootAdvisorAgent());
    }

    private static void printScopeDetails(ScopedRules scope) {
        System.out.printf("Scope: %-25s -> Rules: %s\n", scope.scopeName(), scope.ruleFileNames());
    }

    private static void printHookTelemetry(HookRegistry hookRegistry) {
        System.out.println("\n=== HOOK AUDIT TRAIL & TELEMETRY ===");
        List<HookResult> history = hookRegistry.getExecutionHistory();
        System.out.printf("Total Hook Invocations: %d | Blocked Operations: %d | Allowed: %d\n",
                history.size(), hookRegistry.getBlockedCount(), hookRegistry.getSuccessCount());
        System.out.println("Invoked Tools: " + hookRegistry.getInvokedTools());
        System.out.println("--------------------------------------------------------------------------------");
        for (int i = 0; i < history.size(); i++) {
            HookResult h = history.get(i);
            System.out.printf("[%02d] %-28s | Policy: %-12s | Status: %-7s | Time: %3dms | %s\n",
                    i + 1,
                    h.hookName(),
                    h.policy(),
                    h.blocked() ? "BLOCKED" : (h.success() ? "SUCCESS" : "FAILED"),
                    h.executionDurationMs(),
                    h.message());
        }
        System.out.println("--------------------------------------------------------------------------------");
    }

    private static void testBlockingHook(FinanceAdvisorAgentV11 advisor) {
        System.out.println("\n=== TESTING BLOCKING HOOK (PreToolSourceValidationHook) ===");
        System.out.println("Simulating unauthorized call to prohibited trading tool: 'execute_trade'...");

        FinanceAdvisorAgentV11.ProhibitedTradingTool mockTool = new FinanceAdvisorAgentV11.ProhibitedTradingTool();
        Map<String, Object> input = Map.of("symbol", "INFY", "action", "BUY", "quantity", 100);

        var preHook = new com.google.adk.finance.v11.hooks.PreToolSourceValidationHook();
        var blockedOpt = preHook.call(null, mockTool, input, null);

        if (blockedOpt.isPresent()) {
            System.out.println(">>> SUCCESS: BLOCKING HOOK INTERCEPTED TOOL EXECUTION!");
            System.out.println("Returned payload: " + blockedOpt.get());
        } else {
            System.out.println(">>> FAILURE: Tool was NOT blocked!");
        }
    }

    private static void testNonBlockingHook(Runner runner, String userId, String sessionId, Map<String, Object> sessionState) {
        System.out.println("\n=== TESTING NON-BLOCKING HOOK (PostToolObservationHook) ===");
        System.out.println("Executing portfolio math calculation to observe telemetry capture...");
        handleUserQuery(runner, userId, sessionId, sessionState, "Calculate the PnL for 10 shares bought at 1500 with current price 1800 using portfolio_math.");
        System.out.println("Check telemetry via /hooks to verify captured PostToolObservationHook metadata.");
    }

    private static void runWorkflow(McpToolset mcpToolset, RuleLoader ruleLoader, HookRegistry hookRegistry, String ticker) {
        System.out.println("\n=== RUNNING SEQUENTIAL RESEARCH WORKFLOW FOR: " + ticker + " ===");
        SequentialResearchWorkflowV11 workflow = new SequentialResearchWorkflowV11(mcpToolset, ruleLoader, hookRegistry, new SequentialResearchWorkflowV11.DefaultWorkflowStageHook());
        String report = workflow.executeWorkflow(ticker, "Provide full institutional coverage on " + ticker);
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(report);
        System.out.println("--------------------------------------------------------------------------------");
    }

    private static void runCanonicalScenario(Runner runner, String userId, String sessionId, Map<String, Object> sessionState) {
        String canonicalPrompt = "Research Infosys and provide a current analysis covering recent developments, fundamentals and risks.";
        System.out.println("\n=== RUNNING CANONICAL DEMONSTRATION SCENARIO ===");
        System.out.println("User Prompt: \"" + canonicalPrompt + "\"");
        handleUserQuery(runner, userId, sessionId, sessionState, canonicalPrompt);
    }

    private static void shutdown(YahooFinanceMcpClientManager mcpManager) {
        System.out.println("\nShutting down Finance Advisor V11 Console...");
        try {
            mcpManager.close();
        } catch (Exception ignored) {}
    }
}
