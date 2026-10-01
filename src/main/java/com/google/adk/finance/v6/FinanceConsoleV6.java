package com.google.adk.finance.v6;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.finance.v5.YahooFinanceMcpClientManager;
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
 * Week 3 — Version 6: Dedicated Interactive CLI Runner for Finance Advisor v6 (Sub-Agents).
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Parent agent orchestrating 3 specialized sub-agents via {@code AgentTool}.</li>
 *   <li>Real-time event streaming highlighting delegation decisions, sub-agent lifecycles, and synthesis.</li>
 *   <li>Multi-specialist inquiries delegating to multiple sub-agents in a single turn.</li>
 *   <li>Inspection of discovered MCP tools, domain skills, knowledge documents, and session state.</li>
 *   <li>Graceful shutdown of MCP child processes on exit.</li>
 * </ul>
 */
public final class FinanceConsoleV6 {

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println("   Finance Advisor Agent v6 - Direct CLI Console                         ");
        System.out.println("   Milestone 6: Specialized Sub-Agents & Parent Orchestration            ");
        System.out.println("=========================================================================");

        YahooFinanceMcpClientManager mcpClientManager = new YahooFinanceMcpClientManager();

        // Register JVM shutdown hook for clean resource termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Shutdown] Shutting down Finance Advisor v6 and terminating MCP server...");
            mcpClientManager.close();
        }));

        try {
            System.out.println("\n[Lifecycle 1/4] Starting Yahoo Finance MCP Server & Verifying Toolset...");
            mcpClientManager.start();

            System.out.println("[Lifecycle 2/4] MCP Server Ready! Discovered Tools:");
            List<BaseTool> mcpTools = mcpClientManager.getDiscoveredTools();
            for (BaseTool tool : mcpTools) {
                System.out.printf("   * %-26s : %s%n", tool.name(), tool.description());
            }

            System.out.println("\n[Lifecycle 3/4] Initializing Finance Advisor v6 Parent & Sub-Agents...");
            LlmAgent agent = FinanceAdvisorAgentV6.createFinanceAdvisorAgentV6(mcpClientManager.getMcpToolset());
            Runner runner = new InMemoryRunner(agent, FinanceAdvisorAgentV6.AGENT_NAME);
            System.out.println("[Lifecycle 4/4] Finance Advisor v6 Parent Orchestrator is ready!\n");

            String userId = "finance-learner-6";
            String sessionId = "v6-session-" + UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> sessionState = new HashMap<>();

            // One-shot execution mode
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
                    System.out.print("\nAdvisor V6 > ");
                    if (!scanner.hasNextLine()) {
                        break;
                    }
                    String line = scanner.nextLine().trim();
                    if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                        System.out.println("Exiting Finance Advisor v6 console. Goodbye!");
                        break;
                    }
                    if (line.equalsIgnoreCase("help")) {
                        printHelp();
                        continue;
                    }
                    if (line.equalsIgnoreCase("agents") || line.equalsIgnoreCase("subagents")) {
                        printSubAgents();
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

    private static void printSubAgents() {
        System.out.println("""
            --- V6 Specialized Sub-Agents Architecture ---
            Root Orchestrator:
               • portfolio_director (FinanceAdvisorAgentV6)
               • Role: Workflow orchestration, multi-agent delegation, and user alignment.

            Specialist Sub-Agents:
            1. stockmarket_researcher (MarketResearchAgentV6)
               • Responsibility: Real-time public-web developments, corporate announcements, breaking news, and price change attribution.
               • Tools: GoogleSearchTool (Live Web Retrieval).
               • Framework: 8-stage market research methodology.
            
            2. scenario_analyst (ScenarioAnalystAgentV6)
               • Responsibility: Forward-looking macro scenarios (Baseline, Bull, Bear), stress-testing portfolio sensitivity, rate hike shocks, and multiple compression.
               • Tools: Yahoo Finance MCP (Beta & Multiples) + PortfolioMathTool + Risk Principles.
               • Output: Standardized Scenario & Stress-Testing Report.
            
            3. report_writer (ReportWriterAgentV6)
               • Responsibility: Synthesizes multi-agent research notes, fundamentals, and scenario findings into an institutional decision-support report.
               • Tools: Project Knowledge + Domain Skills.
               • Output: Institutional Decision-Support Document (Thesis, Evidence Matrix, Scenario Table, Risk Flags, Disclaimers).
            
            4. fundamental_analysis_agent (FundamentalAnalysisAgentV6)
               • Responsibility: Quantitative company fundamentals, valuation multiples, and historical statements.
               • Tools: Yahoo Finance MCP (get_stock_info, get_financial_statement, get_stock_actions, get_recommendations).
               • Knowledge: Valuation principles & fundamental analysis frameworks.
            
            5. portfolio_risk_agent (PortfolioRiskAgentV6)
               • Responsibility: Investment risk, market sensitivity (beta), volatility, concentration (>25%), and diversification.
               • Tools: Yahoo Finance MCP (Beta/52-week range) & PortfolioMathTool (concentration math).
               • Knowledge: Risk framework & portfolio construction principles.
            ----------------------------------------------
            """);
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
              agents/subagents - Show specialized sub-agents and their division of labor
              mcp/tools        - List discovered Yahoo Finance MCP tools
              skills           - List available domain skills in skills/finance/
              knowledge        - List available project grounding knowledge documents in knowledge/
              state            - Print active session state variables (Customer ID, holdings, etc.)
              help             - Show this help menu
              exit/quit        - Exit the CLI
            
            Try these learning prompts:
              1. Delegation to Market Research Sub-Agent:
                 Advisor V6 > What happened with Infosys this week? Any recent news or announcements?
              2. Delegation to Fundamental Analysis Sub-Agent:
                 Advisor V6 > What are Infosys' latest fundamentals, valuation multiples, and profit margins?
              3. Delegation to Portfolio Risk Sub-Agent:
                 Advisor V6 > What are the major risks of investing in Infosys? How volatile is it?
              4. Delegation to Scenario Analyst Sub-Agent:
                 Advisor V6 > Model bull and bear scenarios for Infosys and stress-test against a rate hike.
              5. Delegation to Report Writer Sub-Agent:
                 Advisor V6 > Synthesize an institutional decision-support report for my portfolio.
              6. Multi-Specialist Synthesis (All Specialists):
                 Advisor V6 > Give me a full view of Infosys: recent developments, fundamentals, key risks, and bull/bear scenarios.
              7. Customer Portfolio Ingestion & Analysis:
                 Advisor V6 > I am customer 1001. Load my portfolio and evaluate the fundamental health and risk of my holdings.
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
        System.out.println("\n[Parent Agent Analyzing Inquiry & Selecting Specialists...]\n");

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

            StringBuilder finalResponse = new StringBuilder();

            eventStream.blockingForEach(event -> {
                // Inspect event parts for sub-agent delegation and tool invocation
                event.content().ifPresent(content -> {
                    content.parts().ifPresent(parts -> {
                        for (Part part : parts) {
                            part.functionCall().ifPresent(fnCall -> {
                                String name = fnCall.name().orElse("unknown");
                                Map<String, Object> args = fnCall.args().orElse(Map.of());
                                if (name.contains("agent") || name.contains("research") || name.contains("fundamental") || name.contains("risk") || name.contains("scenario") || name.contains("writer") || name.contains("director")) {
                                    System.out.printf("  [Delegation Decision] -> Delegating to specialist: [%s]%n", name);
                                    System.out.printf("                         Task: %s%n", args.getOrDefault("request", args));
                                } else {
                                    System.out.printf("  [Tool Invocation] -> Executing: [%s] (args: %s)%n", name, args);
                                }
                            });

                            part.functionResponse().ifPresent(fnResp -> {
                                String name = fnResp.name().orElse("unknown");
                                if (name.contains("agent") || name.contains("research") || name.contains("fundamental") || name.contains("risk") || name.contains("scenario") || name.contains("writer") || name.contains("director")) {
                                    System.out.printf("  [Sub-Agent Returned] -> Specialist [%s] returned report to parent.%n", name);
                                }
                            });

                            part.text().ifPresent(finalResponse::append);
                        }
                    });
                });
                if (event.actions() != null && event.actions().stateDelta() != null && !event.actions().stateDelta().isEmpty()) {
                    sessionState.putAll(event.actions().stateDelta());
                }
            });

            System.out.println("\n-------------------------------------------------------------------------");
            System.out.println(finalResponse.toString().trim());
            System.out.println("-------------------------------------------------------------------------\n");

        } catch (Exception e) {
            System.err.println("\n[Execution Error] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void ensureSession(Runner runner, String userId, String sessionId, Map<String, Object> sessionState) {
        try {
            runner.sessionService()
                    .getSession(runner.appName(), userId, sessionId, Optional.empty())
                    .switchIfEmpty(runner.sessionService().createSession(runner.appName(), userId, sessionState != null ? sessionState : Map.of(), sessionId))
                    .blockingGet();
        } catch (Exception e) {
            // Ignore if session already exists
        }
    }

    private FinanceConsoleV6() {}
}
