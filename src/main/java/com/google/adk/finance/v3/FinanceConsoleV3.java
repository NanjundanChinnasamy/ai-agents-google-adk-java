package com.google.adk.finance.v3;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.finance.v2.CustomerPortfolioRepository;
import com.google.adk.finance.v3.agents.FinanceAgentV3Factory;
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
 * Week 2 — Version 3: Dedicated Interactive CLI Runner for Finance Agent v3.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Live Google Search grounding for earnings, company news, and analyst commentary.</li>
 *   <li>Deterministic calculations via {@link PortfolioMathTool} (PnL, allocation weights, technical indicators).</li>
 *   <li>Real-time visualization of tool invocations and structured outputs.</li>
 * </ul>
 */
public final class FinanceConsoleV3 {

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("   Finance Portfolio Agent v3 - Direct CLI Console       ");
        System.out.println("   Milestone 3: Java Tools & Grounded Google Search      ");
        System.out.println("=========================================================");

        CustomerPortfolioRepository.initDb();

        LlmAgent agent = FinanceAgentV3Factory.createFinanceAgentV3();
        Runner runner = new InMemoryRunner(agent, FinanceAgentV3Factory.AGENT_NAME);

        String userId = "finance-user-3";
        String sessionId = "finance-v3-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> sessionState = new HashMap<>();

        ensureSession(runner, userId, sessionId, sessionState);

        // One-shot execution if argument provided
        if (args.length > 0 && !args[0].isBlank()) {
            String prompt = String.join(" ", args);
            System.out.println("\n[One-shot query]: " + prompt);
            executeTurn(runner, userId, sessionId, prompt, sessionState);
            return;
        }

        System.out.println("\nSession established: " + sessionId);
        System.out.println("Capabilities Active:");
        System.out.println("  * Google Search Grounding: Recent news, latest earnings, analyst commentary, official filings");
        System.out.println("  * Portfolio Math Tool: Deterministic PnL, allocation weights, concentration risk, technical SMA");
        System.out.println("  * SQLite Integration: Load holdings for customer 1001 or 1002");
        System.out.println("\nCommands:");
        System.out.println("  'state'          : Inspect active session state keys and values");
        System.out.println("  'help'           : Show recommended financial inquiries");
        System.out.println("  'exit' or 'quit' : Terminate the console");
        System.out.println("---------------------------------------------------------\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nFinance Analyst v3 > ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    System.out.println("Exiting Finance Agent v3. Goodbye!");
                    break;
                }

                if (input.equalsIgnoreCase("state")) {
                    printSessionState(sessionState);
                    continue;
                }

                if (input.equalsIgnoreCase("help")) {
                    printSampleQuestions();
                    continue;
                }

                if (input.isEmpty()) {
                    continue;
                }

                executeTurn(runner, userId, sessionId, input, sessionState);
            }
        }
    }

    public static void executeTurn(Runner runner, String userId, String sessionId, String userPrompt, Map<String, Object> state) {
        Content userContent = Content.builder()
                .role("user")
                .parts(List.of(Part.fromText(userPrompt)))
                .build();

        RunConfig runConfig = RunConfig.builder().build();
        System.out.println("\n[Agent Thinking & Executing Tools...]\n");

        try {
            ensureSession(runner, userId, sessionId, state);

            Flowable<Event> eventFlow = runner.runAsync(
                    userId,
                    sessionId,
                    userContent,
                    runConfig,
                    state
            );

            eventFlow.blockingForEach(event -> {
                if (event.content().isPresent()) {
                    Content content = event.content().get();
                    for (Part part : content.parts().orElse(List.of())) {
                        part.functionCall().ifPresent(fc ->
                                System.out.println("[Tool Call] -> " + fc.name() + "(" + fc.args() + ")")
                        );
                        part.functionResponse().ifPresent(fr ->
                                System.out.println("[Tool Response] <- " + fr.name() + " executed.")
                        );
                        part.text().ifPresent(System.out::print);
                    }
                }

                if (event.actions() != null && event.actions().stateDelta() != null && !event.actions().stateDelta().isEmpty()) {
                    state.putAll(event.actions().stateDelta());
                }
            });

            System.out.println(); // Trailing newline

        } catch (Exception e) {
            System.err.println("\n[Error executing Finance Agent v3]: " + e.getMessage());
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
            // Already created
        }
    }

    private static void printSessionState(Map<String, Object> state) {
        System.out.println("\n--- Current Session State ---");
        if (state.isEmpty()) {
            System.out.println("  (State is empty)");
        } else {
            state.forEach((k, v) -> System.out.println("  " + k + ": " + v));
        }
        System.out.println("-----------------------------");
    }

    private static void printSampleQuestions() {
        System.out.println("\nSample Inquiries for Finance Agent v3:");
        System.out.println("  1. Google Search Grounding (Earnings & Commentary):");
        System.out.println("     'What are the latest quarterly results and recent analyst commentary for Nvidia (NVDA)?'");
        System.out.println("  2. Deterministic Math Tool (Unrealized PnL):");
        System.out.println("     'I bought 25 shares of Microsoft at $380 and the current price is $420. Calculate my exact PnL and return %.'");
        System.out.println("  3. Portfolio Loading + Math + Live Search:");
        System.out.println("     'Load portfolio for customer 1001, calculate their allocation weights, and search for recent news on Reliance.'");
        System.out.println("  4. Technical Indicator Calculation:");
        System.out.println("     'Calculate the 5-day SMA and price change for a stock with prices: 100, 102, 105, 103, 110.'");
    }

    private FinanceConsoleV3() {}
}
