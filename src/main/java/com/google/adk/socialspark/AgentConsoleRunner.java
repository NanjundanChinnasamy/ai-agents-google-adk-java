package com.google.adk.socialspark;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.finance.v1.FinanceAgentV1Factory;
import com.google.adk.finance.v2.FinanceAgentV2Factory;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.socialspark.agents.DraftAgentFactory;
import com.google.adk.socialspark.agents.SocialPosterAgentFactory;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

/**
 * Direct CLI Runner for testing Google ADK agents without needing the web server or frontend UI.
 */
public final class AgentConsoleRunner {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   SocialSpark Java ADK - Direct Agent Console    ");
        System.out.println("==================================================");

        // Check if an argument was passed for a one-shot execution
        if (args.length > 0 && !args[0].isBlank()) {
            String prompt = String.join(" ", args);
            System.out.println("[Mode] One-shot execution: \"" + prompt + "\"\n");
            runTurn(prompt, new HashMap<>(), true);
            return;
        }

        // Interactive Loop
        Map<String, Object> state = new HashMap<>();
        String threadId = "cli-" + UUID.randomUUID().toString().substring(0, 8);
        Runner runner = new InMemoryRunner(SocialPosterAgentFactory.createRootAgent(), "social_poster");

        System.out.println("Type your prompt below to chat directly with the agent.");
        System.out.println("Commands:");
        System.out.println("  'exit' or 'quit'   : Stop the console");
        System.out.println("  'state'            : View current agent state variables");
        System.out.println("  'draft_only <p>'   : Test draft_agent in isolation");
        System.out.println("  'finance_v1 <p>'   : Test finance_agent_v1 directly");
        System.out.println("  'finance_v2 <p>'   : Test finance_agent_v2 directly");
        System.out.println("--------------------------------------------------\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
            System.out.print("\nUser > ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println("Exiting agent console. Goodbye!");
                break;
            }

            if (input.equalsIgnoreCase("state")) {
                System.out.println("\n--- Current Agent State ---");
                state.forEach((k, v) -> System.out.println("  " + k + ": " + v));
                System.out.println("---------------------------");
                continue;
            }

            if (input.toLowerCase().startsWith("draft_only ")) {
                String draftPrompt = input.substring("draft_only ".length()).trim();
                System.out.println("\n[Testing draft_agent directly...]");
                testDraftAgentDirectly(draftPrompt);
                continue;
            }

            if (input.toLowerCase().startsWith("finance_v1 ")) {
                String financePrompt = input.substring("finance_v1 ".length()).trim();
                System.out.println("\n[Testing finance_agent_v1 directly...]");
                testFinanceV1Directly(financePrompt);
                continue;
            }

            if (input.toLowerCase().startsWith("finance_v2 ")) {
                String financePrompt = input.substring("finance_v2 ".length()).trim();
                System.out.println("\n[Testing finance_agent_v2 directly...]");
                testFinanceV2Directly(financePrompt);
                continue;
            }

            if (input.isEmpty()) {
                continue;
            }

            // Execute turn via root agent
            executeAgentTurn(runner, threadId, input, state);
        }
    }
}

    private static void executeAgentTurn(Runner runner, String threadId, String userPrompt, Map<String, Object> state) {
        Content userContent = Content.builder()
                .role("user")
                .parts(List.of(Part.fromText(userPrompt)))
                .build();

        RunConfig runConfig = RunConfig.builder().build();
        System.out.println("\nAgent thinking & executing tools...");

        ensureSession(runner, "cli-tester", threadId, state);

        try {
            Flowable<Event> eventFlow = runner.runAsync(
                    "cli-tester",
                    threadId,
                    userContent,
                    runConfig,
                    state
            );

            StringBuilder responseText = new StringBuilder();

            eventFlow.blockingForEach(event -> {
                // Track state deltas
                if (event.actions() != null && event.actions().stateDelta() != null) {
                    Map<String, Object> delta = event.actions().stateDelta();
                    state.putAll(delta);
                    if (delta.containsKey("pipeline_stage")) {
                        System.out.println("\n  [Stage changed -> " + delta.get("pipeline_stage") + "]");
                    }
                }

                // Log function/sub-agent calls
                for (FunctionCall call : event.functionCalls()) {
                    String toolName = call.name().orElse("unknown_tool");
                    System.out.println("\n  -> [Tool Called: " + toolName + " args=" + call.args().orElse(Map.of()) + "]");
                }

                // Stream model content
                if (event.content().isPresent()) {
                    Content content = event.content().get();
                    for (Part part : content.parts().orElse(List.of())) {
                        part.text().ifPresent(chunk -> {
                            System.out.print(chunk);
                            responseText.append(chunk);
                        });
                    }
                }
            });

            System.out.println(); // newline after stream ends

        } catch (Exception e) {
            System.err.println("\n[Error executing agent]: " + e.getMessage());
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

    public static void testDraftAgentDirectly(String prompt) {
        LlmAgent draftAgent = DraftAgentFactory.createDraftAgent();
        Runner runner = new InMemoryRunner(draftAgent, "draft_agent");
        Content userContent = Content.builder()
                .role("user")
                .parts(List.of(Part.fromText(prompt)))
                .build();

        ensureSession(runner, "cli-tester", "draft-test", Map.of());

        try {
            Flowable<Event> flow = runner.runAsync(
                    "cli-tester",
                    "draft-test",
                    userContent,
                    RunConfig.builder().build(),
                    new HashMap<>()
            );

            System.out.print("\nDraft Agent > ");
            flow.blockingForEach(event -> {
                if (event.content().isPresent()) {
                    for (Part p : event.content().get().parts().orElse(List.of())) {
                        p.text().ifPresent(System.out::print);
                    }
                }
            });
            System.out.println();
        } catch (Exception e) {
            System.err.println("\n[Error running draft_agent]: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void testFinanceV1Directly(String prompt) {
        LlmAgent financeAgent = FinanceAgentV1Factory.createFinanceAgentV1();
        Runner runner = new InMemoryRunner(financeAgent, FinanceAgentV1Factory.AGENT_NAME);
        Content userContent = Content.builder()
                .role("user")
                .parts(List.of(Part.fromText(prompt)))
                .build();

        ensureSession(runner, "cli-tester", "finance-v1-cli", Map.of());

        try {
            Flowable<Event> flow = runner.runAsync(
                    "cli-tester",
                    "finance-v1-cli",
                    userContent,
                    RunConfig.builder().build(),
                    new HashMap<>()
            );

            System.out.print("\nFinance Agent v1 > ");
            flow.blockingForEach(event -> {
                if (event.content().isPresent()) {
                    for (Part p : event.content().get().parts().orElse(List.of())) {
                        p.text().ifPresent(System.out::print);
                    }
                }
            });
            System.out.println();
        } catch (Exception e) {
            System.err.println("\n[Error running finance_agent_v1]: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void testFinanceV2Directly(String prompt) {
        LlmAgent financeAgent = FinanceAgentV2Factory.createFinanceAgentV2();
        Runner runner = new InMemoryRunner(financeAgent, FinanceAgentV2Factory.AGENT_NAME);
        Content userContent = Content.builder()
                .role("user")
                .parts(List.of(Part.fromText(prompt)))
                .build();

        ensureSession(runner, "cli-tester", "finance-v2-cli", Map.of());

        try {
            Flowable<Event> flow = runner.runAsync(
                    "cli-tester",
                    "finance-v2-cli",
                    userContent,
                    RunConfig.builder().build(),
                    new HashMap<>()
            );

            System.out.print("\nFinance Agent v2 > ");
            flow.blockingForEach(event -> {
                if (event.content().isPresent()) {
                    for (Part p : event.content().get().parts().orElse(List.of())) {
                        p.functionCall().ifPresent(fc ->
                                System.out.println("\n[Tool Call] -> " + fc.name() + "(" + fc.args() + ")")
                        );
                        p.functionResponse().ifPresent(fr ->
                                System.out.println("[Tool Response] <- " + fr.name() + " executed.")
                        );
                        p.text().ifPresent(System.out::print);
                    }
                }
            });
            System.out.println();
        } catch (Exception e) {
            System.err.println("\n[Error running finance_agent_v2]: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runTurn(String prompt, Map<String, Object> state, boolean printOutput) {
        if (prompt.toLowerCase().startsWith("draft_only ")) {
            testDraftAgentDirectly(prompt.substring("draft_only ".length()).trim());
            return;
        }
        if (prompt.toLowerCase().startsWith("finance_v1 ")) {
            testFinanceV1Directly(prompt.substring("finance_v1 ".length()).trim());
            return;
        }
        if (prompt.toLowerCase().startsWith("finance_v2 ")) {
            testFinanceV2Directly(prompt.substring("finance_v2 ".length()).trim());
            return;
        }
        Runner runner = new InMemoryRunner(SocialPosterAgentFactory.createRootAgent(), "social_poster");
        executeAgentTurn(runner, "one-shot-" + UUID.randomUUID(), prompt, state);
    }

    private AgentConsoleRunner() {}
}
