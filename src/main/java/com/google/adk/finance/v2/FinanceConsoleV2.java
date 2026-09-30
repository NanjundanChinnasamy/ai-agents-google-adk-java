package com.google.adk.finance.v2;

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
 * Week 1 — Version 2: Interactive CLI Runner for Finance Agent v2.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Multi-turn conversation with dynamic state preservation.</li>
 *   <li>Automatic invocation of {@link LoadCustomerPortfolioTool}.</li>
 *   <li>Live inspection of session state variables across turns.</li>
 * </ul>
 */
public final class FinanceConsoleV2 {

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("   Finance Portfolio Agent v2 - Direct CLI Console       ");
        System.out.println("   Milestone 2: Context & Holdings State Management      ");
        System.out.println("=========================================================");

        CustomerPortfolioRepository.initDb();

        LlmAgent agent = FinanceAgentV2Factory.createFinanceAgentV2();
        Runner runner = new InMemoryRunner(agent, FinanceAgentV2Factory.AGENT_NAME);

        String userId = "finance-user-2";
        String sessionId = "finance-v2-" + UUID.randomUUID().toString().substring(0, 8);
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
        System.out.println("Seed Test Data Available in SQLite:");
        System.out.println("  * Customer 1001: Portfolio 100001 (Reliance: 2 @ 1000 INR, TCS: 2 @ 1000 INR)");
        System.out.println("  * Customer 1002: Portfolio 100002 (Infosys: 10 @ 1500 INR)");
        System.out.println("\nCommands:");
        System.out.println("  'state'          : Inspect active session state keys and values");
        System.out.println("  'help'           : Show recommended multi-turn dialogue flow");
        System.out.println("  'exit' or 'quit' : Terminate the console");
        System.out.println("---------------------------------------------------------\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nFinance Analyst v2 > ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    System.out.println("Exiting Finance Agent v2. Goodbye!");
                    break;
                }

                if (input.equalsIgnoreCase("state")) {
                    printSessionState(sessionState);
                    continue;
                }

                if (input.equalsIgnoreCase("help")) {
                    printSampleDialogue();
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
                // If a tool call occurs, display it clearly in console
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

                // If state changes occurred during this event, capture them
                if (event.actions() != null && event.actions().stateDelta() != null && !event.actions().stateDelta().isEmpty()) {
                    state.putAll(event.actions().stateDelta());
                }
            });

            System.out.println(); // Trailing newline

            // Refresh state from session if available
            try {
                runner.sessionService()
                        .getSession(runner.appName(), userId, sessionId, Optional.empty())
                        .blockingGet()
                        .state()
                        .forEach(state::putIfAbsent);
            } catch (Exception ignored) {}

        } catch (Exception e) {
            System.err.println("\n[Error executing Finance Agent v2]: " + e.getMessage());
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
            System.out.println("  (State is currently empty. Provide your Customer ID to load portfolio.)");
        } else {
            state.forEach((k, v) -> {
                if (v instanceof String s && s.contains("\n")) {
                    System.out.println("  " + k + ":\n" + s.indent(4));
                } else {
                    System.out.println("  " + k + ": " + v);
                }
            });
        }
        System.out.println("-----------------------------");
    }

    private static void printSampleDialogue() {
        System.out.println("\nRecommended 3-Turn Dialogue to Test State Management:");
        System.out.println("  Turn 1: 'Can you analyze my portfolio?'");
        System.out.println("          -> Agent notices missing state and asks for your Customer ID.");
        System.out.println("  Turn 2: 'My customer ID is 1001'");
        System.out.println("          -> Agent calls load_customer_portfolio, seeds state, and displays Reliance + TCS.");
        System.out.println("  Turn 3: 'What is my total invested capital and purchase dates?'");
        System.out.println("          -> Agent answers immediately from session state without re-querying!");
    }

    private FinanceConsoleV2() {}
}
