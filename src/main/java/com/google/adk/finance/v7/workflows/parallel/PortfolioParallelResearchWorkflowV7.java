package com.google.adk.finance.v7.workflows.parallel;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.ParallelAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.events.Event;
import com.google.adk.finance.v7.subagents.CompanyParallelResearchWorkerV7;
import com.google.adk.finance.v7.subagents.ParallelPortfolioComparisonAgentV7;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.tools.BaseToolset;
import com.google.adk.tools.mcp.McpToolset;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Week 4 — Version 7: Deterministic Parallel Fan-Out / Fan-In Research Workflow.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li><b>Concurrent Fan-Out</b>: Executes independent research tasks across multiple companies concurrently
 *       rather than waiting sequentially.</li>
 *   <li><b>Real Concurrency Logging</b>:
 *       <pre>{@code
 *       [PARALLEL] Starting Infosys research
 *       [PARALLEL] Starting HDFC Bank research
 *       [PARALLEL] Starting Reliance research
 *       ...
 *       [PARALLEL] Infosys research completed
 *       [PARALLEL] HDFC Bank research completed
 *       [PARALLEL] Reliance research completed
 *       }</pre>
 *   </li>
 *   <li><b>Partial Failure Isolation & Resilience</b>: If one or more company tasks fail (e.g. invalid ticker,
 *       network timeout, or simulated test failure), the fan-in aggregator still executes, explicitly documenting:
 *       <ol>
 *         <li>Successful analyses</li>
 *         <li>Failed analyses</li>
 *         <li>Missing information</li>
 *         <li>Scope limitations</li>
 *       </ol>
 *       Missing data is NEVER fabricated.
 *   </li>
 *   <li><b>ADK Native Integration</b>: Also provides {@link #createWorkflowAgent(List, McpToolset)} combining
 *       {@link ParallelAgent} and {@link SequentialAgent}.</li>
 * </ul>
 */
public final class PortfolioParallelResearchWorkflowV7 {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioParallelResearchWorkflowV7.class);

    public static final String WORKFLOW_NAME = "portfolio_parallel_research_workflow";

    private static final ExecutorService parallelExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Builds a native Google ADK {@link SequentialAgent} that first executes a {@link ParallelAgent}
     * across all company workers, followed by the {@link ParallelPortfolioComparisonAgentV7} fan-in aggregator.
     *
     * @param companySymbols list of company tickers/symbols
     * @param mcpToolset active McpToolset for Yahoo Finance
     * @return the configured composite agent
     */
    public static BaseAgent createWorkflowAgent(List<String> companySymbols, McpToolset mcpToolset) {
        List<BaseAgent> parallelWorkers = new ArrayList<>();
        for (String symbol : companySymbols) {
            parallelWorkers.add(CompanyParallelResearchWorkerV7.create(symbol, mcpToolset));
        }

        ParallelAgent fanOutAgent = ParallelAgent.builder()
                .name("parallel_research_fanout")
                .description("Fan-Out agent executing company research in parallel.")
                .subAgents(parallelWorkers)
                .build();

        LlmAgent aggregator = ParallelPortfolioComparisonAgentV7.create();

        return SequentialAgent.builder()
                .name(WORKFLOW_NAME)
                .description("End-to-end parallel research workflow: Parallel Fan-Out -> Fan-In Comparison Aggregator.")
                .subAgents(fanOutAgent, aggregator)
                .build();
    }

    /**
     * Executes the fan-out / fan-in parallel research workflow with genuine concurrent task execution,
     * thread-safe event logging, and resilient partial failure handling.
     *
     * @param companySymbols list of company tickers or symbols
     * @param mcpToolset active MCP toolset
     * @param failureSimulationTickers optional set of tickers to simulate failures for (for testing resilience)
     * @return parallel research result record containing successful reports, failed reports, and final comparison
     */
    public static ParallelWorkflowResult executeWorkflow(
            List<String> companySymbols,
            McpToolset mcpToolset,
            List<String> failureSimulationTickers
    ) {
        return executeWorkflow(companySymbols, (BaseToolset) mcpToolset, failureSimulationTickers);
    }

    /**
     * Executes the fan-out / fan-in parallel research workflow with genuine concurrent task execution,
     * thread-safe event logging, and resilient partial failure handling.
     *
     * @param companySymbols list of company tickers or symbols
     * @param mcpToolset active MCP toolset (or ThreadSafeMcpToolset)
     * @param failureSimulationTickers optional set of tickers to simulate failures for (for testing resilience)
     * @return parallel research result record containing successful reports, failed reports, and final comparison
     */
    public static ParallelWorkflowResult executeWorkflow(
            List<String> companySymbols,
            BaseToolset mcpToolset,
            List<String> failureSimulationTickers
    ) {
        if (companySymbols == null || companySymbols.isEmpty()) {
            throw new IllegalArgumentException("Company symbols list cannot be empty for parallel research");
        }

        // Clean and normalize company symbols
        List<String> cleanSymbols = companySymbols.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<String> failureSet = failureSimulationTickers != null ? failureSimulationTickers : List.of();
        Map<String, SingleCompanyResearchResult> companyResults = new ConcurrentHashMap<>();

        logger.info("[PARALLEL] Initiating concurrent fan-out for {} companies: {}", cleanSymbols.size(), cleanSymbols);

        // Pre-warm and wrap McpToolset to ensure thread safety across concurrent workers
        BaseToolset safeMcpToolset = mcpToolset != null ? ThreadSafeMcpToolset.wrap(mcpToolset) : null;
        if (safeMcpToolset instanceof ThreadSafeMcpToolset ts) {
            ts.warmUp();
        }

        // 1. FAN-OUT: Trigger concurrent execution for each company
        List<CompletableFuture<Void>> futures = cleanSymbols.stream()
                .map(symbol -> CompletableFuture.runAsync(() -> {
                    logger.info("[PARALLEL] Starting {} research", symbol);

                    // Check for simulated failure
                    if (failureSet.contains(symbol) || symbol.equalsIgnoreCase("FAIL") || symbol.equalsIgnoreCase("ERROR")) {
                        String errorMsg = "Simulated connection failure / data unavailable for " + symbol;
                        logger.warn("[PARALLEL] {} research FAILED: {}", symbol, errorMsg);
                        companyResults.put(symbol, new SingleCompanyResearchResult(
                                symbol, false, null, errorMsg
                        ));
                        return;
                    }

                    try {
                        LlmAgent workerAgent = CompanyParallelResearchWorkerV7.create(symbol, safeMcpToolset);
                        Runner runner = new InMemoryRunner(workerAgent, workerAgent.name());
                        String sessionId = "worker-" + symbol.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 6);
                        String userId = "parallel-user";
                        Map<String, Object> state = new HashMap<>();

                        runner.sessionService()
                                .createSession(runner.appName(), userId, state, sessionId)
                                .blockingGet();

                        Content userContent = Content.builder()
                                .role("user")
                                .parts(List.of(Part.fromText("Perform comprehensive company research, fundamentals, and risk analysis for " + symbol)))
                                .build();

                        RunConfig runConfig = RunConfig.builder().build();
                        StringBuilder workerOutput = new StringBuilder();

                        Flowable<Event> eventStream = runner.runAsync(userId, sessionId, userContent, runConfig, state);
                        eventStream.blockingForEach(event -> {
                            event.content().ifPresent(content -> {
                                content.parts().ifPresent(parts -> {
                                    for (Part part : parts) {
                                        part.text().ifPresent(workerOutput::append);
                                    }
                                });
                            });
                        });

                        String report = workerOutput.toString().trim();
                        logger.info("[PARALLEL] {} research completed", symbol);
                        companyResults.put(symbol, new SingleCompanyResearchResult(
                                symbol, true, report, null
                        ));

                    } catch (Exception e) {
                        logger.warn("[PARALLEL] {} research FAILED: {}", symbol, e.getMessage());
                        companyResults.put(symbol, new SingleCompanyResearchResult(
                                symbol, false, null, e.getMessage()
                        ));
                    }
                }, parallelExecutor))
                .toList();

        // Wait for all concurrent tasks to finish (fan-in join)
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 2. FAN-IN: Aggregate results and compile comparative brief
        logger.info("[PARALLEL] Fan-In: All {} tasks joined. Aggregating findings for synthesis...", cleanSymbols.size());

        List<String> successfulSymbols = new ArrayList<>();
        List<String> failedSymbols = new ArrayList<>();
        StringBuilder aggregatedFindings = new StringBuilder();

        for (String symbol : cleanSymbols) {
            SingleCompanyResearchResult result = companyResults.get(symbol);
            if (result != null && result.success()) {
                successfulSymbols.add(symbol);
                aggregatedFindings.append("\n=== ").append(symbol).append(" (SUCCESS) ===\n")
                        .append(result.report())
                        .append("\n");
            } else {
                failedSymbols.add(symbol);
                String error = result != null ? result.errorMessage() : "Unknown failure";
                aggregatedFindings.append("\n=== ").append(symbol).append(" (FAILED) ===\n")
                        .append("Error: ").append(error).append("\n")
                        .append("Data Limitation: No fundamental or valuation data could be verified for this company.\n");
            }
        }

        // Run Fan-In Aggregator agent
        LlmAgent aggregator = ParallelPortfolioComparisonAgentV7.create();
        Runner aggRunner = new InMemoryRunner(aggregator, aggregator.name());
        String aggSessionId = "aggregator-" + UUID.randomUUID().toString().substring(0, 6);
        String userId = "parallel-user";
        Map<String, Object> aggState = new HashMap<>();
        aggState.put("parallel_findings", aggregatedFindings.toString());

        StringBuilder finalComparisonBrief = new StringBuilder();

        try {
            aggRunner.sessionService()
                    .createSession(aggRunner.appName(), userId, aggState, aggSessionId)
                    .blockingGet();

            Content aggPrompt = Content.builder()
                    .role("user")
                    .parts(List.of(Part.fromText("Synthesize the parallel findings into a comparative research report.")))
                    .build();

            Flowable<Event> stream = aggRunner.runAsync(userId, aggSessionId, aggPrompt, RunConfig.builder().build(), aggState);
            stream.blockingForEach(event -> {
                event.content().ifPresent(content -> {
                    content.parts().ifPresent(parts -> {
                        for (Part part : parts) {
                            part.text().ifPresent(finalComparisonBrief::append);
                        }
                    });
                });
            });
        } catch (Exception e) {
            logger.error("[Fan-In Error] Failed to generate comparison report: {}", e.getMessage(), e);
            finalComparisonBrief.append("Error generating comparison report: ").append(e.getMessage());
        }

        return new ParallelWorkflowResult(
                cleanSymbols,
                successfulSymbols,
                failedSymbols,
                Collections.unmodifiableMap(companyResults),
                finalComparisonBrief.toString().trim()
        );
    }

    /**
     * Result of an individual company research task.
     */
    public record SingleCompanyResearchResult(
            String symbol,
            boolean success,
            String report,
            String errorMessage
    ) {}

    /**
     * Result of the complete fan-out / fan-in parallel workflow.
     */
    public record ParallelWorkflowResult(
            List<String> requestedSymbols,
            List<String> successfulSymbols,
            List<String> failedSymbols,
            Map<String, SingleCompanyResearchResult> companyResults,
            String comparativeReport
    ) {
        public boolean hasFailures() {
            return !failedSymbols.isEmpty();
        }

        public boolean isCompletelySuccessful() {
            return failedSymbols.isEmpty() && !successfulSymbols.isEmpty();
        }
    }

    private PortfolioParallelResearchWorkflowV7() {}
}
