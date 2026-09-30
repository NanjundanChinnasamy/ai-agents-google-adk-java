package com.google.adk.finance.v1;

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
 * Week 1 — Version 1: Dedicated Interactive CLI Runner for Finance Agent v1.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>{@link InMemoryRunner}: Managing execution lifecycle for {@link LlmAgent}.</li>
 *   <li>Session Creation: Initializing persistent user sessions via {@code runner.sessionService()}.</li>
 *   <li>Reactive Streaming: Handling real-time token events via RxJava {@link Flowable}.</li>
 * </ul>
 */
public final class FinanceConsoleV1 {

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("   Finance Portfolio Agent v1 - Direct CLI Console       ");
        System.out.println("   Milestone 1: Agent, Model, Instruction & Session      ");
        System.out.println("=========================================================");

        LlmAgent agent = FinanceAgentV1Factory.createFinanceAgentV1();
        Runner runner = new InMemoryRunner(agent, FinanceAgentV1Factory.AGENT_NAME);

        String userId = "finance-user-1";
        String sessionId = "finance-v1-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> sessionState = new HashMap<>();

        // Ensure session is created in ADK session service
        ensureSession(runner, userId, sessionId, sessionState);

        // One-shot execution if argument provided
        if (args.length > 0 && !args[0].isBlank()) {
            String prompt = String.join(" ", args);
            System.out.println("\n[One-shot query]: " + prompt);
            executeTurn(runner, userId, sessionId, prompt, sessionState);
            return;
        }

        System.out.println("\nSession established: " + sessionId);
        System.out.println("Ask any financial question (e.g., asset classes, valuation, inflation, portfolio drift).");
        System.out.println("Commands:");
        System.out.println("  'exit' or 'quit' : Terminate the console");
        System.out.println("  'help'           : Show sample financial questions");
        System.out.println("---------------------------------------------------------\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nFinance Analyst v1 > ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    System.out.println("Exiting Finance Agent v1. Goodbye!");
                    break;
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
        System.out.println("\n[Agent Thinking...]\n");

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
                // Stream text chunks
                if (event.content().isPresent()) {
                    Content content = event.content().get();
                    for (Part part : content.parts().orElse(List.of())) {
                        part.text().ifPresent(System.out::print);
                    }
                }
            });

            System.out.println(); // Trailing newline
        } catch (Exception e) {
            System.err.println("\n[Error executing Finance Agent v1]: " + e.getMessage());
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
            // Already created or handled
        }
    }

    private static void printSampleQuestions() {
        System.out.println("\nSample Financial Decision-Support Inquiries:");
        System.out.println("  1. 'Explain the difference between growth and value investing strategies.'");
        System.out.println("  2. 'How does a Federal Reserve interest rate hike impact tech stocks vs dividend payers?'");
        System.out.println("  3. 'What is portfolio drift and how does periodic rebalancing mitigate concentration risk?'");
        System.out.println("  4. 'How do I interpret a 10-year Treasury yield curve inversion?'");
        System.out.println("  5. 'What are the key differences between Sharpe ratio and Beta when evaluating portfolio risk?'");
    }

    private FinanceConsoleV1() {}
}
