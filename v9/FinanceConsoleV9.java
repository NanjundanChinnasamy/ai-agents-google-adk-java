package com.google.adk.finance.v9;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.finance.v9.evaluation.EvaluationCase;
import com.google.adk.finance.v9.evaluation.EvaluationDatasetLoader;
import com.google.adk.finance.v9.evaluation.EvaluationReport;
import com.google.adk.finance.v9.evaluation.EvaluationRunner;
import com.google.adk.finance.v9.evaluation.EvidenceRecord;
import com.google.adk.finance.v9.testing.FailureScenario;
import com.google.adk.finance.v9.testing.FailureScenarioRunner;
import com.google.adk.finance.v9.testing.FailureTestResult;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

/**
 * Interactive Terminal CLI Runner for Finance Advisor Version 9 (Evaluation & Failure Testing).
 * <p>
 * Supported commands:
 * <ul>
 *   <li>{@code eval <case_id>} - Evaluates a specific golden test case fixture.</li>
 *   <li>{@code eval-all} - Runs the full deterministic evaluation benchmark suite.</li>
 *   <li>{@code test-failures} - Runs deliberate failure, injection, and anomaly scenarios.</li>
 *   <li>{@code evidence} - Displays all empirical records captured in the EvidenceStore.</li>
 *   <li>{@code research <ticker>} - Invokes live FinanceAdvisorAgentV9 for empirical equity analysis.</li>
 *   <li>{@code help} - Displays interactive menu and command reference.</li>
 *   <li>{@code exit} - Quits the console session.</li>
 * </ul>
 */
public class FinanceConsoleV9 {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   GOOGLE ADK JAVA — FINANCE ADVISOR V9: EVALUATION & FAILURE TESTING          ");
        System.out.println("   \"How do I know my Finance Advisor is actually producing trustworthy results?\" ");
        System.out.println("================================================================================");

        FinanceAdvisorAgentV9 agentV9 = FinanceAdvisorAgentV9.createOffline();
        Runner runner = new InMemoryRunner(agentV9.getAgent(), FinanceAdvisorAgentV9.AGENT_NAME);
        EvaluationRunner evalRunner = agentV9.getEvaluationRunner();
        FailureScenarioRunner failureRunner = new FailureScenarioRunner();
        List<EvaluationCase> goldenCases = EvaluationDatasetLoader.loadFinanceEvaluationCases();
        List<FailureScenario> failureCases = EvaluationDatasetLoader.loadFailureTestCases();

        String userId = "finance-learner-9";
        String sessionId = "v9-session-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> sessionState = new HashMap<>();

        if (args.length > 0) {
            String initialCmd = String.join(" ", args).trim();
            handleCommand(initialCmd, runner, evalRunner, failureRunner, goldenCases, failureCases, agentV9, userId, sessionId, sessionState);
            return;
        }

        printHelp();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nfinance-v9> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                System.out.println("Exiting Finance Advisor V9 Console. Goodbye!");
                break;
            }
            if (!line.isBlank()) {
                handleCommand(line, runner, evalRunner, failureRunner, goldenCases, failureCases, agentV9, userId, sessionId, sessionState);
            }
        }
    }

    private static void handleCommand(
            String cmd,
            Runner runner,
            EvaluationRunner evalRunner,
            FailureScenarioRunner failureRunner,
            List<EvaluationCase> goldenCases,
            List<FailureScenario> failureCases,
            FinanceAdvisorAgentV9 agentV9,
            String userId,
            String sessionId,
            Map<String, Object> sessionState) {

        String[] parts = cmd.split("\\s+", 2);
        String rawAction = parts[0].toLowerCase();
        String action = rawAction.startsWith("/") ? rawAction.substring(1) : rawAction;
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (action) {
            case "help" -> printHelp();

            case "cases" -> {
                System.out.println("\n--- GOLDEN EVALUATION BENCHMARK CASES ---");
                for (EvaluationCase c : goldenCases) {
                    System.out.printf("- %-26s [%-20s] %s\n", c.id(), c.category(), c.description());
                }
            }

            case "eval-all" -> {
                System.out.println("\n--- RUNNING COMPLETE GOLDEN EVALUATION BENCHMARK SUITE ---");
                int passed = 0;
                int failed = 0;
                for (EvaluationCase c : goldenCases) {
                    System.out.println("\n--------------------------------------------------");
                    EvaluationReport report = evalRunner.evaluate(c);
                    System.out.println(report.toFormattedReport());
                    if (report.isAllPass()) passed++;
                    else failed++;
                }
                System.out.println("\n==================================================");
                System.out.printf("BENCHMARK SUMMARY: %d Total, %d PASS, %d DETECTED FAILURES\n",
                        goldenCases.size(), passed, failed);
                System.out.println("Note: Detected failures in negative test cases confirm evaluation rigor.");
                System.out.println("==================================================");
            }

            case "eval" -> {
                if (arg.isBlank()) {
                    System.out.println("Please provide a case ID (e.g. 'eval INFY-RESEARCH-001' or 'eval GROUNDEDNESS-FAIL-001').");
                    return;
                }
                EvaluationCase matched = goldenCases.stream()
                        .filter(c -> c.id().equalsIgnoreCase(arg))
                        .findFirst()
                        .orElse(null);

                if (matched != null) {
                    EvaluationReport report = evalRunner.evaluate(matched);
                    System.out.println("\n" + report.toFormattedReport());
                } else {
                    System.out.println("Unknown case ID: " + arg + ". Available cases:");
                    goldenCases.forEach(c -> System.out.println("- " + c.id() + " (" + c.category() + ")"));
                }
            }

            case "fail", "failures", "test-failures" -> {
                System.out.println("\n--- RUNNING DELIBERATE ADVERSARIAL & FAILURE INJECTION SUITE ---");
                int passCount = 0;
                for (FailureScenario fs : failureCases) {
                    System.out.println("\n--------------------------------------------------");
                    FailureTestResult res = failureRunner.runScenario(fs);
                    System.out.printf("Scenario:    %s [%s]\n", res.scenarioId(), res.category());
                    System.out.printf("Expected:    %s\n", res.expectedBehavior());
                    System.out.printf("Actual:      %s\n", res.actualBehavior());
                    System.out.printf("Intercepted: %s\n", res.interceptedBy().orElse("None"));
                    System.out.printf("Result:      %s\n", res.status());
                    System.out.printf("Explanation: %s\n", res.explanation());
                    if (res.isPass()) passCount++;
                }
                System.out.println("\n==================================================");
                System.out.printf("FAILURE SUITE RESULT: %d / %d VULNERABILITIES SAFELY INTERCEPTED\n",
                        passCount, failureCases.size());
                System.out.println("==================================================");
            }

            case "evidence" -> {
                List<EvidenceRecord> records = agentV9.getEvidenceStore().getAllRecords();
                System.out.println("\n--- EVIDENCE STORE CONTENTS (" + records.size() + " verified records) ---");
                if (records.isEmpty()) {
                    System.out.println("(Evidence store is currently empty. Run research or load test cases to populate.)");
                } else {
                    for (EvidenceRecord r : records) {
                        System.out.printf("- [%s] %s (%s): %s = %s [Ref: %s]\n",
                                r.timestamp(), r.source(), r.ticker(), r.field(), r.value(), r.sourceReference());
                    }
                }
            }

            case "research" -> {
                if (arg.isBlank()) {
                    System.out.println("Please specify a ticker (e.g. 'research INFY.NS' or 'research AAPL').");
                    return;
                }
                System.out.println("\nInvoking FinanceAdvisorAgentV9 for " + arg + "...");
                try {
                    ensureSession(runner, userId, sessionId, sessionState);
                    Content userContent = Content.builder().role("user")
                            .parts(Part.fromText("Research " + arg + " valuation, variance, and scenario analysis."))
                            .build();

                    Flowable<Event> events = runner.runAsync(userId, sessionId, userContent, RunConfig.builder().build(), sessionState);
                    StringBuilder responseText = new StringBuilder();

                    events.blockingForEach(event -> {
                        event.content().ifPresent(content -> {
                            content.parts().ifPresent(contentParts -> {
                                for (Part p : contentParts) {
                                    p.text().ifPresent(t -> {
                                        System.out.print(t);
                                        responseText.append(t);
                                    });
                                }
                            });
                        });
                        if (event.actions() != null && event.actions().stateDelta() != null) {
                            sessionState.putAll(event.actions().stateDelta());
                        }
                    });
                    System.out.println();

                    // Automatically evaluate the generated live response
                    System.out.println("\n--- AUTOMATIC LIVE EVALUATION REPORT ---");
                    EvaluationReport report = evalRunner.evaluate("LIVE-" + arg, "Research " + arg, responseText.toString(), agentV9.getEvidenceStore());
                    System.out.println(report.toFormattedReport());

                } catch (Exception e) {
                    System.err.println("Execution failed: " + e.getMessage());
                }
            }

            default -> {
                System.out.println("Unknown command: '" + action + "'. Type 'help' for available commands.");
            }
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

    private static void printHelp() {
        System.out.println("""
                Available Commands:
                  eval-all         : Run complete golden evaluation benchmark suite
                  eval <case_id>   : Evaluate a specific golden test case fixture
                  test-failures    : Run deliberate adversarial and failure injection scenarios
                  evidence         : Inspect verified evidence records currently in store
                  research <sym>   : Run live research on ticker and auto-evaluate output
                  help             : Display this command reference
                  exit             : Quit the console
                """);
    }
}
