package com.google.adk.finance.v7;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.finance.v5.YahooFinanceMcpClientManager;
import com.google.adk.finance.v7.workflows.loop.ResearchCriticLoopWorkflowV7;
import com.google.adk.finance.v7.workflows.parallel.PortfolioParallelResearchWorkflowV7;
import com.google.adk.finance.v7.workflows.sequential.InvestmentResearchSequentialWorkflowV7;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.tools.BaseTool;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

/**
 * Week 4 — Version 7: Dedicated Interactive CLI Runner for Finance Advisor v7 (Workflow Orchestration).
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Three distinct deterministic workflow paradigms: Sequential, Parallel, and Loop.</li>
 *   <li>Direct workflow triggers via CLI commands: {@code sequential <ticker>}, {@code parallel <tickers>}, {@code loop <ticker>}.</li>
 *   <li>Conversational natural language routing via {@link FinanceAdvisorAgentV7}.</li>
 *   <li>Live logging of fan-out, fan-in, loop iterations, and partial failure handling.</li>
 *   <li>Graceful MCP client lifecycle management on shutdown.</li>
 * </ul>
 */
public final class FinanceConsoleV7 {

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println("   Finance Advisor Agent v7 - Direct CLI Console                         ");
        System.out.println("   Milestone 7: Deterministic Workflow Orchestration                     ");
        System.out.println("   (1) Sequential Pipeline  (2) Parallel Fan-Out/In  (3) Critic Loop     ");
        System.out.println("=========================================================================");

        YahooFinanceMcpClientManager mcpClientManager = new YahooFinanceMcpClientManager();

        // JVM shutdown hook for clean MCP process termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Shutdown] Terminating MCP server and cleaning up resources...");
            mcpClientManager.close();
        }));

        try {
            System.out.println("\n[Lifecycle 1/4] Starting Yahoo Finance MCP Server...");
            mcpClientManager.start();

            System.out.println("[Lifecycle 2/4] MCP Server Ready! Discovered Tools:");
            List<BaseTool> mcpTools = mcpClientManager.getDiscoveredTools();
            for (BaseTool tool : mcpTools) {
                System.out.printf("   * %-26s : %s%n", tool.name(), tool.description());
            }

            System.out.println("\n[Lifecycle 3/4] Initializing Finance Advisor v7 Parent & Workflow Engines...");
            LlmAgent agent = FinanceAdvisorAgentV7.createFinanceAdvisorAgentV7(mcpClientManager.getMcpToolset());
            Runner runner = new InMemoryRunner(agent, FinanceAdvisorAgentV7.AGENT_NAME);
            System.out.println("[Lifecycle 4/4] Finance Advisor v7 Workflow Director is ready!\n");

            String userId = "finance-learner-7";
            String sessionId = "v7-session-" + UUID.randomUUID().toString().substring(0, 8);
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
                    System.out.print("\nAdvisor V7 > ");
                    if (!scanner.hasNextLine()) {
                        break;
                    }
                    String line = scanner.nextLine().trim();
                    if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                        System.out.println("Exiting Finance Advisor v7 console. Goodbye!");
                        break;
                    }
                    if (line.equalsIgnoreCase("help")) {
                        printHelp();
                        continue;
                    }
                    if (line.equalsIgnoreCase("workflows")) {
                        printWorkflows();
                        continue;
                    }
                    if (line.equalsIgnoreCase("mcp") || line.equalsIgnoreCase("tools")) {
                        printDiscoveredMcpTools(mcpClientManager);
                        continue;
                    }
                    if (line.equalsIgnoreCase("state")) {
                        printState(sessionState);
                        continue;
                    }

                    // Direct Workflow Triggers
                    if (line.toLowerCase().startsWith("sequential ")) {
                        String ticker = line.substring("sequential ".length()).trim();
                        System.out.printf("\n[Executing Sequential Pipeline directly for: %s]%n", ticker);
                        InvestmentResearchSequentialWorkflowV7.SequentialWorkflowResult res =
                                InvestmentResearchSequentialWorkflowV7.executeWorkflow(ticker, mcpClientManager.getMcpToolset());
                        System.out.println("\n" + res.finalReport());
                        continue;
                    }

                    if (line.toLowerCase().startsWith("parallel ")) {
                        String rawSymbols = line.substring("parallel ".length()).trim();
                        List<String> symbols = FinanceAdvisorAgentV7.parseCompanyList(rawSymbols);
                        System.out.printf("\n[Executing Parallel Fan-Out/In directly for: %s]%n", symbols);
                        PortfolioParallelResearchWorkflowV7.ParallelWorkflowResult res =
                                PortfolioParallelResearchWorkflowV7.executeWorkflow(symbols, mcpClientManager.getMcpToolset(), null);
                        System.out.println("\n" + res.comparativeReport());
                        continue;
                    }

                    if (line.toLowerCase().startsWith("parallel-fail ")) {
                        String rawSymbols = line.substring("parallel-fail ".length()).trim();
                        List<String> symbols = new java.util.ArrayList<>(FinanceAdvisorAgentV7.parseCompanyList(rawSymbols));
                        // Inject simulated failure
                        symbols.add("FAIL");
                        System.out.printf("\n[Executing Parallel Fan-Out/In with simulated failure for: %s]%n", symbols);
                        PortfolioParallelResearchWorkflowV7.ParallelWorkflowResult res =
                                PortfolioParallelResearchWorkflowV7.executeWorkflow(symbols, mcpClientManager.getMcpToolset(), List.of("FAIL"));
                        System.out.println("\n" + res.comparativeReport());
                        continue;
                    }

                    if (line.toLowerCase().startsWith("loop ")) {
                        String ticker = line.substring("loop ".length()).trim();
                        System.out.printf("\n[Executing Research-Critic Loop directly for: %s]%n", ticker);
                        ResearchCriticLoopWorkflowV7.LoopWorkflowResult res =
                                ResearchCriticLoopWorkflowV7.executeWorkflow(ticker, mcpClientManager.getMcpToolset());
                        System.out.printf("\n[Loop Status: Completed=%b, Approved=%b, Iterations=%d]%n",
                                res.completed(), res.criticApproved(), res.iterationsExecuted());
                        System.out.println("\n" + res.finalReport());
                        continue;
                    }

                    // Conversational Turn via Root Orchestrator
                    executeTurn(runner, userId, sessionId, line, sessionState);
                }
            }

        } catch (Exception e) {
            System.err.println("Fatal error in Finance Advisor v7: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executeTurn(Runner runner, String userId, String sessionId, String userPrompt, Map<String, Object> sessionState) {
        System.out.println("\n[Workflow Director Analyzing Inquiry & Selecting Pipeline...]\n");

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
                event.content().ifPresent(content -> {
                    content.parts().ifPresent(parts -> {
                        for (Part part : parts) {
                            part.functionCall().ifPresent(fnCall -> {
                                String name = fnCall.name().orElse("unknown");
                                Map<String, Object> args = fnCall.args().orElse(Map.of());
                                if (name.startsWith("run_") && name.endsWith("_workflow")) {
                                    System.out.printf("  [Workflow Trigger] -> Launching Workflow Engine: [%s]%n", name);
                                    System.out.printf("                        Parameters: %s%n", args);
                                } else {
                                    System.out.printf("  [Tool Execution]   -> Invoking: [%s] (args: %s)%n", name, args);
                                }
                            });

                            part.functionResponse().ifPresent(fnResp -> {
                                String name = fnResp.name().orElse("unknown");
                                if (name.startsWith("run_") && name.endsWith("_workflow")) {
                                    System.out.printf("  [Workflow Completed] -> Engine [%s] returned final report.%n", name);
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
        } catch (Exception ignored) {}
    }

    private static void printWorkflows() {
        System.out.println("""
            -------------------------------------------------------------------------
            Finance Advisor V7 — Deterministic Workflow Engines:
            -------------------------------------------------------------------------
            1. SEQUENTIAL PIPELINE (InvestmentResearchSequentialWorkflowV7):
               Topology:
               Company Research (Google Search)
                     ↓
               Fundamental Analysis (Yahoo Finance MCP)
                     ↓
               Risk Analysis (Beta & Volatility)
                     ↓
               Valuation Analysis (Grounded Multiples)
                     ↓
               Report Synthesis (Institutional 7-Section Document)

            2. PARALLEL FAN-OUT / FAN-IN (PortfolioParallelResearchWorkflowV7):
               Topology:
                                Portfolio
                                    │
                                 FAN-OUT
                              /     |     \\
                             v      v      v
                          Infosys  HDFC   Reliance
                           Worker Worker   Worker
                             \\      |      /
                              v     v     v
                                 FAN-IN
                                    │
                              Comparison Matrix
               Features: Concurrent execution, partial failure isolation, explicit data gap audit.

            3. ITERATIVE CRITIC LOOP (ResearchCriticLoopWorkflowV7):
               Topology:
                            Generate Draft
                                  ↓
                        Compliance / Evidence Critic
                                  ↓
                              Approved?
                              /       \\
                            YES        NO
                             │          │
                             v          v
                        Final Report  Revise Report (Critic Feedback)
                                        │
                                        └──> Critic Again (Max 3)
               Features: exit_loop termination tool, factual grounding validation.
            -------------------------------------------------------------------------
            """);
    }

    private static void printDiscoveredMcpTools(YahooFinanceMcpClientManager mcpClientManager) {
        System.out.println("\n--- Yahoo Finance MCP Tools Discovered ---");
        List<BaseTool> tools = mcpClientManager.getDiscoveredTools();
        for (BaseTool tool : tools) {
            System.out.printf("  * %-24s - %s%n", tool.name(), tool.description());
        }
        System.out.println("------------------------------------------\n");
    }

    private static void printState(Map<String, Object> sessionState) {
        System.out.println("\n--- Current Session State ---");
        if (sessionState.isEmpty()) {
            System.out.println("  (State is currently empty)");
        } else {
            sessionState.forEach((k, v) -> System.out.println("  " + k + ": " + v));
        }
        System.out.println("-----------------------------\n");
    }

    private static void printHelp() {
        System.out.println("""
            Commands:
              sequential <ticker>        - Run 5-stage sequential workflow for a company (e.g. sequential INFY)
              parallel <t1, t2, t3>      - Run parallel fan-out/in research across companies (e.g. parallel INFY, TCS, RELIANCE)
              parallel-fail <t1, t2>     - Run parallel research demonstrating partial failure resilience
              loop <ticker>              - Run iterative research-critic loop with exit_loop termination (e.g. loop INFY)
              workflows                  - Show workflow topologies and execution models
              mcp / tools                - List discovered Yahoo Finance MCP tools
              state                      - Show current session state
              help                       - Show this menu
              exit / quit                - Exit the CLI

            Try these natural language prompts:
              1. Sequential: "Prepare a structured investment research report on Infosys."
              2. Parallel:   "Analyse Infosys, HDFC Bank and Reliance and give me a comparable research summary."
              3. Loop:       "Create an investment research report on Infosys and ensure important factual claims are supported by evidence."
            """);
    }

    private FinanceConsoleV7() {}
}
