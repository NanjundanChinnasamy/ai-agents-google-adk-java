package com.google.adk.finance.v5;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.tools.BaseTool;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

/**
 * Week 3 — Version 5: Dedicated Interactive CLI Runner for Finance Advisor v5.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Automatic startup of standalone Yahoo Finance MCP Server.</li>
 *   <li>Readiness verification and dynamic tool discovery over STDIO transport.</li>
 *   <li>Real-time event streaming with explicit MCP tool-call attribution.</li>
 *   <li>Multi-source intelligence: Yahoo Finance MCP (structured market quotes/financials)
 *       combined with Google Search (news/events), Domain Skills, Grounding Knowledge, and SQLite Portfolio State.</li>
 *   <li>Graceful shutdown of MCP server child processes on exit.</li>
 * </ul>
 */
public final class FinanceConsoleV5 {

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println("   Finance Advisor Agent v5 - Direct CLI Console                         ");
        System.out.println("   Milestone 5: Yahoo Finance MCP Integration + Skills + Knowledge       ");
        System.out.println("=========================================================================");

        YahooFinanceMcpClientManager mcpClientManager = new YahooFinanceMcpClientManager();

        // Register shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Shutdown] Shutting down Finance Advisor v5 and closing MCP server...");
            mcpClientManager.close();
        }));

        try {
            System.out.println("\n[Lifecycle 1/4] Starting Yahoo Finance MCP Server & Connecting McpToolset...");
            mcpClientManager.start();

            System.out.println("[Lifecycle 2/4] MCP Server Ready! Discovered Tools:");
            List<BaseTool> tools = mcpClientManager.getDiscoveredTools();
            for (BaseTool tool : tools) {
                System.out.printf("   * %-26s : %s%n", tool.name(), tool.description());
            }

            System.out.println("\n[Lifecycle 3/4] Initializing Finance Advisor v5 Agent...");
            LlmAgent agent = FinanceAdvisorAgentV5Factory.createFinanceAdvisorAgentV5(mcpClientManager.getMcpToolset());
            Runner runner = new InMemoryRunner(agent, FinanceAdvisorAgentV5Factory.AGENT_NAME);
            System.out.println("[Lifecycle 4/4] Finance Advisor v5 is ready to accept requests.\n");

            String userId = "finance-learner-5";
            String sessionId = "v5-session-" + UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> sessionState = new HashMap<>();

            // If one-shot query provided via command-line arguments
            if (args != null && args.length > 0) {
                String prompt = String.join(" ", args).trim();
                if (!prompt.isBlank()) {
                    System.out.println("[One-shot query]: " + prompt);
                    executeTurn(runner, userId, sessionId, prompt, sessionState);
                    System.exit(0);
                }
            }

            printHelp();

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\nAdvisor V5 > ");
                    if (!scanner.hasNextLine()) {
                        break;
                    }
                    String line = scanner.nextLine().trim();
                    if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                        System.out.println("Exiting Finance Advisor v5 console. Goodbye!");
                        break;
                    }
                    if (line.equalsIgnoreCase("help")) {
                        printHelp();
                        continue;
                    }
                    if (line.equalsIgnoreCase("mcp") || line.equalsIgnoreCase("tools")) {
                        printDiscoveredMcpTools(mcpClientManager);
                        continue;
                    }
                    if (line.equalsIgnoreCase("skills")) {
                        printSkills();
                        continue;
                    }
                    if (line.equalsIgnoreCase("knowledge")) {
                        printKnowledge();
                        continue;
                    }
                    if (line.equalsIgnoreCase("state")) {
                        printState(sessionState);
                        continue;
                    }
                    if (line.isBlank()) {
                        continue;
                    }

                    executeTurn(runner, userId, sessionId, line, sessionState);
                }
            }
        } catch (Exception e) {
            System.err.println("\n[Fatal Startup Error] " + e.getMessage());
            e.printStackTrace();
        } finally {
            mcpClientManager.close();
        }
    }

    private static void printDiscoveredMcpTools(YahooFinanceMcpClientManager mcpClientManager) {
        System.out.println("\n--- Yahoo Finance MCP Tools Discovered ---");
        List<BaseTool> tools = mcpClientManager.getDiscoveredTools();
        if (tools.isEmpty()) {
            System.out.println("  (No tools discovered or MCP server not initialized)");
        } else {
            for (BaseTool tool : tools) {
                System.out.printf("  * %-24s - %s%n", tool.name(), tool.description());
            }
        }
        System.out.println("------------------------------------------\n");
    }

    private static void printState(Map<String, Object> sessionState) {
        System.out.println("\n--- Current Session State ---");
        if (sessionState.isEmpty()) {
            System.out.println("  (State is currently empty. Provide a Customer ID like 1001 or 1002 to load holdings)");
        } else {
            sessionState.forEach((k, v) -> System.out.println("  " + k + ": " + v));
        }
        System.out.println("-----------------------------\n");
    }

    private static void printHelp() {
        System.out.println("""
            Commands:
              mcp/tools - List discovered Yahoo Finance MCP tools
              skills    - List available domain skills in skills/finance/
              knowledge - List available project grounding knowledge documents in knowledge/
              state     - Print active session state variables (Customer ID, holdings, etc.)
              help      - Show this help menu
              exit/quit - Exit the CLI

            Try these learning prompts:
              1. Live Stock Quote (Yahoo Finance MCP -> get_stock_info):
                 Advisor V5 > What is the latest available price and valuation for Infosys (INFY)?
              2. Corporate Actions (Yahoo Finance MCP -> get_stock_actions):
                 Advisor V5 > Show recent dividend payouts and stock splits for Apple (AAPL).
              3. Financial Statements (Yahoo Finance MCP -> get_financial_statement):
                 Advisor V5 > Give me the latest quarterly income statement for Microsoft (MSFT).
              4. Wall Street Ratings (Yahoo Finance MCP -> get_recommendations):
                 Advisor V5 > What are current analyst recommendations and price targets for NVDA?
              5. Current News (Google Search Grounding -> stockmarket_researcher):
                  Advisor V5 > What is the latest news and earnings reports for Infosys?
              6. Combined Grounding (Yahoo Finance MCP + Google Search):
                  Advisor V5 > What is the latest Infosys price and what recent events may have affected the company?
              7. Customer Portfolio + MCP Quotes + Math (SQLite + MCP + Math):
                  Advisor V5 > I am customer 1001. Load my portfolio, fetch current market prices for my holdings, and evaluate my performance.
            """);
    }

    private static void printSkills() {
        System.out.println("""
            Available Domain Skills (skills/finance/):
              - finance-fundamentals : Stocks, market cap, EPS, revenue, profit, cash flow
              - fundamental-analysis : Margins, ROE, ROCE, debt health, competitive moat
              - valuation            : Trailing/Forward P/E, EV/EBITDA, P/B, DCF concept
              - risk-management      : Systematic vs company risk, concentration, drawdown
              - portfolio-analysis   : Allocation, diversification, correlation, horizons
              - market-research      : 8-stage framework for corporate & market investigations
            """);
    }

    private static void printKnowledge() {
        System.out.println("""
            Available Grounding Documents (knowledge/):
              - glossary.md                  : Core financial terminology
              - valuation-principles.md      : Multiple analysis & intrinsic valuation
              - fundamental-analysis.md      : Multi-pillar operating & balance sheet quality
              - risk-framework.md            : Taxonomy of financial risks & mitigations
              - portfolio-principles.md      : Portfolio construction & correlation rules
              - market-research-framework.md : Structured real-time equity investigations
            """);
    }

    private static void executeTurn(Runner runner, String userId, String sessionId, String userPrompt, Map<String, Object> sessionState) {
        System.out.println("\n[Agent Thinking & Consulting Tools...]\n");

        Content userContent = Content.builder()
                .role("user")
                .parts(List.of(Part.fromText(userPrompt)))
                .build();

        RunConfig runConfig = RunConfig.builder().build();

        try {
            ensureSession(runner, userId, sessionId, sessionState);

            Flowable<Event> eventStream = runner.runAsync(
                    userId,
                    sessionId,
                    userContent,
                    runConfig,
                    sessionState
            );

            eventStream.blockingForEach(event -> {
                // Trace tool executions (MCP calls, skills, knowledge reads, search)
                if (event.content().isPresent()) {
                    Content content = event.content().get();
                    for (Part part : content.parts().orElse(List.of())) {
                        part.functionCall().ifPresent(fc -> {
                            String name = fc.name().orElse("unknown");
                            if (name.startsWith("get_stock") || name.equals("get_financial_statement") || name.equals("get_recommendations")) {
                                System.out.println("\n[Yahoo Finance MCP Tool Call] -> " + name + "(" + fc.args() + ")");
                            } else if (name.equals("stockmarket_researcher")) {
                                System.out.println("\n[Google Search Sub-Agent Call] -> " + name + "(" + fc.args() + ")");
                            } else {
                                System.out.println("\n[Tool Call] -> " + name + "(" + fc.args() + ")");
                            }
                        });
                        part.functionResponse().ifPresent(fr ->
                                System.out.println("[Tool Response] <- " + fr.name() + " executed.")
                        );
                        part.text().ifPresent(System.out::print);
                    }
                }

                if (event.actions() != null && event.actions().stateDelta() != null && !event.actions().stateDelta().isEmpty()) {
                    sessionState.putAll(event.actions().stateDelta());
                }
            });
            System.out.println();
        } catch (Exception e) {
            System.err.println("\n[Error executing Finance Advisor v5]: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void ensureSession(Runner runner, String userId, String sessionId, Map<String, Object> state) {
        try {
            runner.sessionService()
                    .getSession(runner.appName(), userId, sessionId, Optional.empty())
                    .switchIfEmpty(runner.sessionService().createSession(runner.appName(), userId, state != null ? state : Map.of(), sessionId))
                    .blockingGet();
        } catch (Exception ignored) {
            // Session already active
        }
    }

    private FinanceConsoleV5() {}
}
