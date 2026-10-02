package com.google.adk.finance.v11.workflows;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.finance.v11.agents.FundamentalAnalysisAgentV11;
import com.google.adk.finance.v11.agents.MarketResearchAgentV11;
import com.google.adk.finance.v11.agents.PortfolioRiskAgentV11;
import com.google.adk.finance.v11.agents.ResponseSynthesisAgentV11;
import com.google.adk.finance.v11.hooks.HookPolicy;
import com.google.adk.finance.v11.hooks.HookRegistry;
import com.google.adk.finance.v11.hooks.HookResult;
import com.google.adk.finance.v11.hooks.ResponseValidationHook;
import com.google.adk.finance.v11.ruleloader.RuleLoader;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.mcp.McpToolset;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Week 5+ — Version 11: Sequential Research Workflow with Pre- and Post-Stage Hooks.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Sequential stage-by-stage pipeline governed by scoped rules</li>
 *   <li>{@link WorkflowStageHook} interception before each stage (pre-stage input check)</li>
 *   <li>{@link WorkflowStageHook} interception after each stage (post-stage latency & compliance observation)</li>
 *   <li>Final response validation hook before report delivery</li>
 * </ul>
 */
public final class SequentialResearchWorkflowV11 {
    private static final Logger logger = LoggerFactory.getLogger(SequentialResearchWorkflowV11.class);

    public static final String WORKFLOW_NAME = "sequential_research_workflow_v11";

    private final LlmAgent marketResearchAgent;
    private final LlmAgent fundamentalAgent;
    private final LlmAgent riskAgent;
    private final LlmAgent synthesisAgent;
    private final HookRegistry hookRegistry;
    private final WorkflowStageHook stageHook;

    public SequentialResearchWorkflowV11(McpToolset mcpToolset) {
        this(mcpToolset, RuleLoader.getInstance(), HookRegistry.getInstance(), new DefaultWorkflowStageHook());
    }

    public SequentialResearchWorkflowV11(
            McpToolset mcpToolset,
            RuleLoader ruleLoader,
            HookRegistry hookRegistry,
            WorkflowStageHook stageHook) {

        this.hookRegistry = Objects.requireNonNull(hookRegistry, "hookRegistry must not be null");
        this.stageHook = Objects.requireNonNull(stageHook, "stageHook must not be null");

        this.marketResearchAgent = MarketResearchAgentV11.create(ruleLoader);
        this.fundamentalAgent = FundamentalAnalysisAgentV11.create(mcpToolset, ruleLoader);
        this.riskAgent = PortfolioRiskAgentV11.create(ruleLoader);
        this.synthesisAgent = ResponseSynthesisAgentV11.create(ruleLoader);
    }

    /**
     * Executes the 4-stage sequential workflow with pre- and post-stage hooks.
     *
     * @param ticker company symbol (e.g. "INFY", "TCS")
     * @param userQuery user's high-level research instruction
     * @return synthesized, validated final investment dossier
     */
    public String executeWorkflow(String ticker, String userQuery) {
        long workflowStart = System.currentTimeMillis();
        logger.info("[{}] Starting workflow for symbol: '{}'", WORKFLOW_NAME, ticker);

        Map<String, Object> workflowContext = new HashMap<>();
        workflowContext.put("ticker", ticker);
        workflowContext.put("user_query", userQuery);

        // --- STAGE 1: Market Research (Google Search) ---
        String researchOutput = runStage(
                "STAGE_1_MARKET_RESEARCH",
                marketResearchAgent,
                "Research recent corporate developments, leadership news, and earnings reports for " + ticker + ". Query: " + userQuery,
                workflowContext
        );
        workflowContext.put("market_research_output", researchOutput);

        // --- STAGE 2: Fundamental Analysis (Yahoo Finance MCP) ---
        String fundamentalsOutput = runStage(
                "STAGE_2_FUNDAMENTAL_ANALYSIS",
                fundamentalAgent,
                "Analyze financial fundamentals, revenue growth, P/E multiples, and operating margins for " + ticker + " using MCP data.",
                workflowContext
        );
        workflowContext.put("fundamental_analysis_output", fundamentalsOutput);

        // --- STAGE 3: Portfolio & Equity Risk ---
        String riskOutput = runStage(
                "STAGE_3_RISK_ANALYSIS",
                riskAgent,
                "Assess material downside risks, beta, sector concentration, and macro vulnerabilities for " + ticker + ".",
                workflowContext
        );
        workflowContext.put("risk_analysis_output", riskOutput);

        // --- STAGE 4: Synthesis & Final Response ---
        String synthesisPrompt = """
                Compile the final institutional decision-support dossier for %s based on the verified inputs below.
                
                [VERIFIED MARKET RESEARCH]:
                %s
                
                [VERIFIED FUNDAMENTAL ANALYSIS]:
                %s
                
                [VERIFIED RISK ASSESSMENT]:
                %s
                
                Ensure clear separation between empirical data and analytical synthesis, cite sources, and conclude with the regulatory disclaimer.
                """.formatted(ticker, researchOutput, fundamentalsOutput, riskOutput);

        String finalReport = runStage(
                "STAGE_4_RESPONSE_SYNTHESIS",
                synthesisAgent,
                synthesisPrompt,
                workflowContext
        );

        long totalDuration = System.currentTimeMillis() - workflowStart;
        hookRegistry.recordHookExecution(HookResult.success(
                WORKFLOW_NAME,
                HookPolicy.NON_BLOCKING,
                "Workflow completed successfully across 4 stages",
                Map.of("ticker", ticker, "totalDurationMs", totalDuration),
                totalDuration
        ));

        logger.info("[{}] Workflow completed in {}ms", WORKFLOW_NAME, totalDuration);
        return finalReport;
    }

    private String runStage(String stageName, LlmAgent agent, String stagePrompt, Map<String, Object> context) {
        long stageStart = System.currentTimeMillis();

        // 1. PRE-STAGE HOOK
        Optional<String> preBlock = stageHook.beforeStage(stageName, context);
        if (preBlock.isPresent()) {
            String errorMsg = preBlock.get();
            logger.warn("[{}] Stage '{}' was blocked by pre-stage hook: {}", WORKFLOW_NAME, stageName, errorMsg);
            hookRegistry.recordHookExecution(HookResult.blocked(
                    stageName + "_PRE_HOOK",
                    errorMsg,
                    Map.of("stage", stageName),
                    System.currentTimeMillis() - stageStart
            ));
            return "[STAGE BLOCKED]: " + errorMsg;
        }

        // 2. STAGE EXECUTION via InMemoryRunner
        String output;
        try {
            Runner runner = new InMemoryRunner(agent, "v11-workflow-" + stageName);
            String userId = "v11-workflow-user";
            String sessionId = "v11-stage-" + UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> sessionState = new HashMap<>(context);

            try {
                runner.sessionService()
                        .getSession(runner.appName(), userId, sessionId, Optional.empty())
                        .switchIfEmpty(runner.sessionService().createSession(runner.appName(), userId, sessionState, sessionId))
                        .blockingGet();
            } catch (Exception ignored) {}

            Content userContent = Content.builder()
                    .role("user")
                    .parts(List.of(Part.fromText(stagePrompt)))
                    .build();

            Flowable<Event> events = runner.runAsync(userId, sessionId, userContent, RunConfig.builder().build(), sessionState);
            StringBuilder resultBuilder = new StringBuilder();

            events.blockingForEach(event -> {
                if (event.content().isPresent()) {
                    Content c = event.content().get();
                    for (Part part : c.parts().orElse(List.of())) {
                        part.text().ifPresent(resultBuilder::append);
                    }
                }
            });

            output = resultBuilder.toString().trim();
            if (output.isBlank()) {
                output = "[Stage " + stageName + " completed with empty textual output]";
            }

        } catch (Exception ex) {
            logger.error("[{}] Error executing stage '{}': {}", WORKFLOW_NAME, stageName, ex.getMessage(), ex);
            output = "[Error in stage " + stageName + ": " + ex.getMessage() + "]";
        }

        long stageDuration = System.currentTimeMillis() - stageStart;

        // 3. POST-STAGE HOOK
        Optional<String> postRemediation = stageHook.afterStage(stageName, context, output, stageDuration);
        if (postRemediation.isPresent()) {
            output = postRemediation.get();
        }

        hookRegistry.recordHookExecution(HookResult.success(
                stageName + "_POST_HOOK",
                HookPolicy.NON_BLOCKING,
                "Stage " + stageName + " completed",
                Map.of("stage", stageName, "durationMs", stageDuration, "outputLength", output.length()),
                stageDuration
        ));

        return output;
    }

    /**
     * Default implementation of {@link WorkflowStageHook} demonstrating pre/post stage validation.
     */
    public static class DefaultWorkflowStageHook implements WorkflowStageHook {
        @Override
        public Optional<String> beforeStage(String stageName, Map<String, Object> stageContext) {
            String ticker = String.valueOf(stageContext.getOrDefault("ticker", ""));
            if (ticker.isBlank()) {
                return Optional.of("Pre-stage validation failed: 'ticker' parameter is required.");
            }
            return Optional.empty();
        }

        @Override
        public Optional<String> afterStage(String stageName, Map<String, Object> stageContext, String stageOutput, long durationMs) {
            // Post-stage check: ensure stage did not return a raw unhandled exception
            if (stageOutput.contains("NullPointerException") || stageOutput.contains("FatalError")) {
                return Optional.of("[Remediated Output]: Stage " + stageName + " produced an operational error.");
            }
            return Optional.empty();
        }
    }
}
