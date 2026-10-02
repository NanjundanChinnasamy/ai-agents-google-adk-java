package com.google.adk.finance.v10;

import com.google.adk.finance.v10.observability.AgentExecution;
import com.google.adk.finance.v10.persistence.ExecutionRecord;
import com.google.adk.finance.v10.persistence.ExecutionRepository;
import com.google.adk.finance.v10.persistence.JsonExecutionRepository;
import com.google.adk.finance.v9.evaluation.EvaluationCase;
import com.google.adk.finance.v9.evaluation.EvaluationDatasetLoader;
import com.google.adk.finance.v9.evaluation.EvaluationReport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Interactive Terminal CLI Runner for Finance Advisor Version 10 (Observability & Persistence).
 * <p>
 * Supported commands:
 * <ul>
 *   <li>{@code ask <query>} - Executes agent and outputs synthesized answer plus execution trace.</li>
 *   <li>{@code trace <exec_id>} - Displays full ASCII timeline and metrics for a persisted run.</li>
 *   <li>{@code history [limit]} - Lists recent persisted execution runs.</li>
 *   <li>{@code failed} - Queries and diagnoses failed executions.</li>
 *   <li>{@code guardrails} - Queries executions that triggered safety guardrails.</li>
 *   <li>{@code eval-failures} - Queries executions that failed evaluation benchmark checks.</li>
 *   <li>{@code eval <case_id>} - Runs an evaluation case and records the execution.</li>
 *   <li>{@code inspect <exec_id>} - Dumps complete JSON payload for an execution.</li>
 *   <li>{@code help} - Displays interactive menu.</li>
 *   <li>{@code exit} - Quits console.</li>
 * </ul>
 */
public class FinanceConsoleV10 {
    private static final Gson prettyGson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   GOOGLE ADK JAVA — FINANCE ADVISOR V10: OBSERVABILITY + PERSISTENCE          ");
        System.out.println("   \"Can I see what my agent did, understand why, measure it, and retrieve it?\" ");
        System.out.println("================================================================================");

        ExecutionRepository repository = new JsonExecutionRepository();
        FinanceAdvisorAgentV10 agentV10 = FinanceAdvisorAgentV10.createOffline(repository);
        List<EvaluationCase> goldenCases = EvaluationDatasetLoader.loadFinanceEvaluationCases();

        String userId = "finance-learner-10";

        if (args.length > 0) {
            String initialCmd = String.join(" ", args).trim();
            handleCommand(initialCmd, agentV10, repository, goldenCases, userId);
            return;
        }

        printHelp();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nfinance-v10> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                System.out.println("Exiting Finance Advisor V10 Console. Goodbye!");
                break;
            }
            if (!line.isBlank()) {
                handleCommand(line, agentV10, repository, goldenCases, userId);
            }
        }
    }

    private static void handleCommand(
            String cmd,
            FinanceAdvisorAgentV10 agentV10,
            ExecutionRepository repository,
            List<EvaluationCase> goldenCases,
            String userId) {

        String[] parts = cmd.split("\\s+", 2);
        String rawAction = parts[0].toLowerCase();
        String action = rawAction.startsWith("/") ? rawAction.substring(1) : rawAction;
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (action) {
            case "help" -> printHelp();

            case "ask", "query" -> {
                if (arg.isBlank()) {
                    System.out.println("Usage: ask <financial question or portfolio query>");
                    return;
                }
                System.out.println("\n[1/2] Executing Finance Advisor V10 with Observability Tracing...");
                AgentExecution execution = agentV10.execute(userId, arg);

                System.out.println("\n--------------------------------------------------");
                System.out.println("AGENT SYNTHESIS");
                System.out.println("--------------------------------------------------");
                System.out.println(execution.response());

                System.out.println("\n[2/2] EXECUTION TRACE (persisted as ID: " + execution.executionId() + ")");
                System.out.println(execution.renderTrace());
            }

            case "trace" -> {
                if (arg.isBlank()) {
                    System.out.println("Usage: trace <execution_id>");
                    return;
                }
                Optional<ExecutionRecord> recordOpt = repository.findByExecutionId(arg);
                if (recordOpt.isEmpty()) {
                    System.out.println("Execution record not found for ID: " + arg);
                    return;
                }
                ExecutionRecord r = recordOpt.get();
                System.out.println("\nPersisted execution found for: " + r.executionId());
                System.out.println("Started: " + r.startedAt() + " | Status: " + r.status());
                System.out.println("Total Events: " + r.events().size());
                System.out.println("\nEvent Timeline:");
                r.events().forEach(e -> System.out.println("  " + e.toFormattedLine()));
            }

            case "history", "recent" -> {
                int limit = 10;
                if (!arg.isBlank()) {
                    try { limit = Integer.parseInt(arg); } catch (NumberFormatException ignored) {}
                }
                List<ExecutionRecord> recent = repository.findRecentExecutions(limit);
                System.out.printf("\n--- RECENT EXECUTIONS (Found %d, Total %d) ---\n", recent.size(), repository.count());
                if (recent.isEmpty()) {
                    System.out.println("(No executions persisted yet)");
                } else {
                    System.out.printf("%-24s %-22s %-12s %-10s %s\n", "EXECUTION ID", "STARTED AT", "STATUS", "DURATION", "PROMPT PREVIEW");
                    for (ExecutionRecord r : recent) {
                        long duration = r.metrics() != null ? r.metrics().totalExecutionDurationMs() : 0;
                        String promptPreview = r.sanitizedPrompt().length() > 30 ? r.sanitizedPrompt().substring(0, 27) + "..." : r.sanitizedPrompt();
                        System.out.printf("%-24s %-22s %-12s %-10d %s\n", r.executionId(), r.startedAt(), r.status(), duration, promptPreview);
                    }
                }
            }

            case "failed" -> {
                List<ExecutionRecord> failed = repository.findByStatus("FAILED");
                System.out.printf("\n--- FAILED EXECUTIONS (%d total) ---\n", failed.size());
                if (failed.isEmpty()) {
                    System.out.println("No failed executions recorded.");
                } else {
                    for (ExecutionRecord r : failed) {
                        System.out.printf("- %s | %s | Error: %s\n", r.executionId(), r.startedAt(), r.error().orElse("Unknown error"));
                    }
                }
            }

            case "guardrails" -> {
                List<ExecutionRecord> guardrailRuns = repository.findExecutionsWithGuardrailEvents();
                System.out.printf("\n--- GUARDRAIL TRIGGERED EXECUTIONS (%d total) ---\n", guardrailRuns.size());
                for (ExecutionRecord r : guardrailRuns) {
                    System.out.printf("- %s | Status: %s | Events: %d\n", r.executionId(), r.status(), r.events().size());
                }
            }

            case "eval-failures" -> {
                List<ExecutionRecord> evalFailures = repository.findExecutionsWithEvaluationFailures();
                System.out.printf("\n--- EVALUATION FAILED EXECUTIONS (%d total) ---\n", evalFailures.size());
                for (ExecutionRecord r : evalFailures) {
                    System.out.printf("- %s | Started: %s\n", r.executionId(), r.startedAt());
                }
            }

            case "eval" -> {
                if (arg.isBlank()) {
                    System.out.println("Usage: eval <case_id> (e.g., eval CASE-01-INFY-EARNINGS)");
                    return;
                }
                Optional<EvaluationCase> caseOpt = goldenCases.stream()
                        .filter(c -> c.id().equalsIgnoreCase(arg))
                        .findFirst();
                if (caseOpt.isEmpty()) {
                    System.out.println("Unknown evaluation case ID: " + arg);
                    return;
                }
                EvaluationCase evalCase = caseOpt.get();
                System.out.println("\nExecuting evaluation case: " + evalCase.id() + " with observability tracing...");
                AgentExecution exec = agentV10.executeWithEvaluation(userId, evalCase.userRequest(), evalCase);
                System.out.println(exec.renderTrace());
            }

            case "eval-all" -> {
                System.out.println("\n--- RUNNING ALL EVALUATION CASES WITH OBSERVABILITY PERSISTENCE ---");
                for (EvaluationCase c : goldenCases) {
                    System.out.println("\n>> Evaluating case: " + c.id());
                    AgentExecution exec = agentV10.executeWithEvaluation(userId, c.userRequest(), c);
                    System.out.printf("   Execution ID: %s | Status: %s | Duration: %dms\n",
                            exec.executionId(), exec.status(), exec.metrics().totalExecutionDurationMs());
                }
                System.out.println("\nAll benchmark cases executed and persisted.");
            }

            case "inspect" -> {
                if (arg.isBlank()) {
                    System.out.println("Usage: inspect <execution_id>");
                    return;
                }
                Optional<ExecutionRecord> recordOpt = repository.findByExecutionId(arg);
                if (recordOpt.isEmpty()) {
                    System.out.println("Execution not found: " + arg);
                    return;
                }
                System.out.println(prettyGson.toJson(recordOpt.get()));
            }

            default -> {
                System.out.println("Unknown command: '" + cmd + "'. Type 'help' for command reference.");
            }
        }
    }

    private static void printHelp() {
        System.out.println("\nAVAILABLE COMMANDS:");
        System.out.println("  ask <prompt>          - Executes query with observability tracing & persistence");
        System.out.println("  trace <exec_id>       - Displays ASCII timeline and metrics for a specific run");
        System.out.println("  history [limit]       - Lists recent persisted executions");
        System.out.println("  failed                - Lists all failed executions");
        System.out.println("  guardrails            - Lists executions that triggered guardrails");
        System.out.println("  eval-failures         - Lists executions that failed evaluation criteria");
        System.out.println("  eval <case_id>        - Runs an evaluation case and persists trace");
        System.out.println("  eval-all              - Runs all benchmark evaluation cases");
        System.out.println("  inspect <exec_id>     - Prints raw JSON payload of an execution");
        System.out.println("  help                  - Displays this menu");
        System.out.println("  exit                  - Exits the application");
    }
}
