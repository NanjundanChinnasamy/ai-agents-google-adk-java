package com.google.adk.finance.v4;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
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
 * Week 2 — Version 4: Dedicated Interactive CLI Runner for Finance Advisor v4.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>On-demand Skill loading via {@code load_skill} (Finance Fundamentals, Valuation, Risk, Portfolio, etc.).</li>
 *   <li>Curated Project Grounding Knowledge retrieval via {@code read_project_knowledge}.</li>
 *   <li>Live Google Search grounding via {@code stockmarket_researcher} when current news is needed.</li>
 *   <li>Source attribution transparency distinguishing Project Knowledge, Current Search Data, and Analytical Reasoning.</li>
 * </ul>
 */
public final class FinanceConsoleV4 {

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("   Finance Advisor Agent v4 - Direct CLI Console         ");
        System.out.println("   Milestone 4: Skills + Project Grounding Knowledge     ");
        System.out.println("=========================================================");

        LlmAgent agent = FinanceAdvisorAgentV4Factory.createFinanceAdvisorAgentV4();
        Runner runner = new InMemoryRunner(agent, FinanceAdvisorAgentV4Factory.AGENT_NAME);

        String userId = "finance-learner-4";
        String sessionId = "v4-session-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> sessionState = new HashMap<>();

        // If one-shot query provided via command-line arguments
        if (args != null && args.length > 0) {
            String prompt = String.join(" ", args).trim();
            if (!prompt.isBlank()) {
                System.out.println("\n[One-shot query]: " + prompt);
                executeTurn(runner, userId, sessionId, prompt, sessionState);
                System.exit(0);
            }
        }

        printHelp();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nAdvisor V4 > ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String line = scanner.nextLine().trim();
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    System.out.println("Exiting Finance Advisor v4 console. Goodbye!");
                    break;
                }
                if (line.equalsIgnoreCase("help")) {
                    printHelp();
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
              skills    - List available domain skills in skills/finance/
              knowledge - List available project grounding knowledge documents in knowledge/
              state     - Print active session state variables (Customer ID, holdings, etc.)
              help      - Show this help menu
              exit/quit - Exit the CLI

            Try these learning prompts:
              1. General Concept (Project Knowledge):
                 Advisor V4 > What is P/E?
              2. Portfolio Ingestion & Risk (SQLite + Skills):
                 Advisor V4 > I am customer 1001. Load my portfolio and analyze concentration risk.
              3. Portfolio Structuring (Skills + Knowledge):
                 Advisor V4 > What is concentration risk and how should I think about it?
              4. Current Information (Google Search Grounding):
                 Advisor V4 > What is the latest news and earnings for Infosys?
              5. Combined Reasoning (Search + Fundamental Analysis):
                 Advisor V4 > What happened to HDFC Bank recently and what factors should I investigate?
              6. Model Knowledge Baseline:
                 Advisor V4 > What is a stock?
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
        System.out.println("\n[Agent Thinking & Consulting Knowledge...]\n");

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
                // Trace tool executions (skills, knowledge reads, search)
                if (event.content().isPresent()) {
                    Content content = event.content().get();
                    for (Part part : content.parts().orElse(List.of())) {
                        part.functionCall().ifPresent(fc ->
                                System.out.println("\n[Tool Call] -> " + fc.name() + "(" + fc.args() + ")")
                        );
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
            System.err.println("\n[Error executing Finance Advisor v4]: " + e.getMessage());
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

    private FinanceConsoleV4() {}
}
