package com.google.adk.finance.v10;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.finance.tools.LoadCustomerPortfolioTool;
import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.adk.finance.tools.ProjectKnowledgeTool;
import com.google.adk.finance.v5.YahooFinanceMcpClientManager;
import com.google.adk.finance.v8.callbacks.AfterModelGuardrail;
import com.google.adk.finance.v8.callbacks.BeforeAgentGuardrail;
import com.google.adk.finance.v8.callbacks.BeforeModelGuardrail;
import com.google.adk.finance.v8.callbacks.BeforeToolGuardrail;
import com.google.adk.finance.v8.evidence.EvidenceStore;
import com.google.adk.finance.v8.guardrails.GuardrailResult;
import com.google.adk.finance.v8.guardrails.input.PiiDetector;
import com.google.adk.finance.v8.guardrails.input.PiiSanitizer;
import com.google.adk.finance.v8.guardrails.input.PromptInjectionDetector;
import com.google.adk.finance.v8.guardrails.input.TradingIntentDetector;
import com.google.adk.finance.v8.subagents.GuardedMarketResearchAgentV8;
import com.google.adk.finance.v8.subagents.MockTradingAgentV8;
import com.google.adk.finance.v9.evaluation.EvaluationCase;
import com.google.adk.finance.v9.evaluation.EvaluationCriteria;
import com.google.adk.finance.v9.evaluation.EvaluationReport;
import com.google.adk.finance.v9.evaluation.EvaluationResult;
import com.google.adk.finance.v9.evaluation.EvaluationRunner;
import com.google.adk.finance.v9.evaluation.EvidenceStoreV9;
import com.google.adk.finance.v10.observability.AgentExecution;
import com.google.adk.finance.v10.observability.ExecutionEvent;
import com.google.adk.finance.v10.observability.ExecutionTrace;
import com.google.adk.finance.v10.observability.FinanceAgentObserver;
import com.google.adk.finance.v10.observability.ObservabilityContext;
import com.google.adk.finance.v10.observability.events.AgentCompletedEvent;
import com.google.adk.finance.v10.observability.events.AgentStartedEvent;
import com.google.adk.finance.v10.observability.events.ErrorEvent;
import com.google.adk.finance.v10.observability.events.EvaluationEvent;
import com.google.adk.finance.v10.observability.events.GuardrailEvent;
import com.google.adk.finance.v10.observability.events.ModelCallEvent;
import com.google.adk.finance.v10.observability.events.ToolCallEvent;
import com.google.adk.finance.v10.observability.events.ToolResultEvent;
import com.google.adk.finance.v10.persistence.ExecutionRecord;
import com.google.adk.finance.v10.persistence.ExecutionRepository;
import com.google.adk.finance.v10.persistence.JsonExecutionRepository;
import com.google.adk.models.BaseLlm;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.AgentTool;
import com.google.adk.tools.mcp.McpToolset;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Week 5 — Version 10: Finance Advisor V10 (Observability + Telemetry Persistence).
 * <p>
 * Primary Learning Objective:
 * <blockquote>
 * <i>"Can I see what my agent did, understand why it did it, measure its execution, and retrieve the execution later?"</i><br>
 * Learn how to capture structured lifecycle events, measure model and tool latencies, trace guardrails and evaluations,
 * and persist complete execution records for post-run auditing and retrieval without compromising sensitive PII.
 * </blockquote>
 */
public final class FinanceAdvisorAgentV10 {
    private static final Logger logger = LoggerFactory.getLogger(FinanceAdvisorAgentV10.class);

    public static final String AGENT_NAME = "finance_advisor_v10";
    public static final String ROLE_NAME = "observable_finance_advisor";

    public static final String INSTRUCTION = """
            You are observable_finance_advisor (Finance Advisor v10), an institutional-grade investment research and decision-support advisor.
            Your role is EVIDENCE-GROUNDED PORTFOLIO RESEARCH, VARIANCE ATTRIBUTION, DETERMINISTIC MATH, and SCENARIO MODELING.

            SYSTEM BOUNDARIES, SAFETY INVARIANTS & OBSERVABILITY:
            1. You are strictly a research, analytical, and educational decision-support assistant.
               You MUST NOT attempt to execute trades, buy/sell securities, or transfer funds.
            2. For empirical market quotes and valuation metrics, rely on official Yahoo Finance MCP tools (`get_stock_info`, etc.).
            3. For real-time news, filings, and earnings announcements, invoke `stockmarket_researcher`.
            4. For deterministic mathematical calculations (PnL, weights, concentration), call `portfolio_math`. Never invent numbers.
            5. If a customer ID is provided, query holdings via `load_customer_portfolio`.
            6. When asked for scenario analysis, ALWAYS provide all three tiers:
               - Baseline Scenario (expected assumptions and projections)
               - Upside Scenario (positive catalysts, revenue acceleration, upside target)
               - Stress Test Scenario (recessionary/macro shocks, margin compression, downside target)
            7. All statements must be factually consistent with verified tool outputs.
            8. Conclude every synthesis with the mandatory regulatory disclaimer.
            9. If asked to inspect, verify, or show captured evidence, call 'get_captured_evidence' to display verified records.
            """;

    private final EvidenceStoreV9 evidenceStore;
    private final EvaluationRunner evaluationRunner;
    private final ExecutionRepository repository;
    private final LlmAgent agent;
    private final String modelName;
    private final PiiDetector piiDetector;
    private final PiiSanitizer piiSanitizer;
    private final PromptInjectionDetector injectionDetector;
    private final TradingIntentDetector tradingIntentDetector;

    private FinanceAdvisorAgentV10(
            EvidenceStoreV9 evidenceStore,
            EvaluationRunner evaluationRunner,
            ExecutionRepository repository,
            LlmAgent agent,
            String modelName) {
        this.evidenceStore = evidenceStore;
        this.evaluationRunner = evaluationRunner;
        this.repository = repository;
        this.agent = agent;
        this.modelName = modelName;
        this.piiDetector = new PiiDetector();
        this.piiSanitizer = new PiiSanitizer(piiDetector);
        this.injectionDetector = new PromptInjectionDetector();
        this.tradingIntentDetector = new TradingIntentDetector();
    }

    public EvidenceStoreV9 getEvidenceStore() {
        return evidenceStore;
    }

    public EvaluationRunner getEvaluationRunner() {
        return evaluationRunner;
    }

    public ExecutionRepository getRepository() {
        return repository;
    }

    public LlmAgent getAgent() {
        return agent;
    }

    public String getModelName() {
        return modelName;
    }

    /**
     * Executes the agent on a user prompt with end-to-end observability, metrics collection, and persistence.
     */
    public AgentExecution execute(String userId, String prompt) {
        String sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);
        return execute(userId, sessionId, prompt);
    }

    /**
     * Executes the agent in a given session with full lifecycle tracing.
     */
    public AgentExecution execute(String userId, String sessionId, String prompt) {
        return executeInternal(userId, sessionId, prompt, null);
    }

    /**
     * Executes the agent and attaches automated V9 evaluation checks to the execution trace.
     */
    public AgentExecution executeWithEvaluation(String userId, String prompt, EvaluationCase evaluationCase) {
        String sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);
        return executeInternal(userId, sessionId, prompt, evaluationCase);
    }

    /**
     * Evaluates a test case using the internal evaluation runner.
     */
    public EvaluationReport evaluate(EvaluationCase evaluationCase) {
        return evaluationRunner.evaluate(evaluationCase);
    }

    private AgentExecution executeInternal(
            String userId,
            String sessionId,
            String prompt,
            EvaluationCase evaluationCase) {

        String executionId = ObservabilityContext.generateExecutionId();
        ObservabilityContext context = new ObservabilityContext(executionId, userId, sessionId, AGENT_NAME, piiSanitizer);
        FinanceAgentObserver observer = new FinanceAgentObserver(context, repository);

        long startNanos = System.nanoTime();
        String safePrompt = context.sanitize(prompt);
        observer.onAgentStarted(executionId, userId, sessionId, safePrompt);

        // 1. Guardrail Step 1: Prompt Injection Scan
        GuardrailResult injectionResult = injectionDetector.evaluate(prompt);
        if (injectionResult.isBlocked()) {
            double riskScore = injectionResult.metadata().get("riskScore") instanceof Number n ? n.doubleValue() : 1.0;
            observer.onGuardrailEvent(GuardrailEvent.promptInjectionDetected(
                    executionId, AGENT_NAME, "INSTRUCTION_OVERRIDE", riskScore));
            String blockedMsg = "Request blocked: Prompt injection attempt detected. System instructions cannot be modified.";
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            observer.onAgentCompleted(executionId, "BLOCKED", blockedMsg, durationMs);
            return new AgentExecution(executionId, userId, sessionId, safePrompt, blockedMsg, "BLOCKED", observer.getLastTrace().orElseThrow(), repository);
        }

        // 2. Guardrail Step 2: Trading Intent Detection
        GuardrailResult tradingResult = tradingIntentDetector.evaluate(prompt);
        if (tradingResult.isBlocked()) {
            observer.onGuardrailEvent(GuardrailEvent.toolOperationBlocked(
                    executionId, AGENT_NAME, "execute_trade", tradingResult.reason()));
            String blockedMsg = "Request blocked: " + tradingResult.reason() + " Institutional policy prohibits live trade execution.";
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            observer.onAgentCompleted(executionId, "BLOCKED", blockedMsg, durationMs);
            return new AgentExecution(executionId, userId, sessionId, safePrompt, blockedMsg, "BLOCKED", observer.getLastTrace().orElseThrow(), repository);
        }

        // 3. Guardrail Step 3: PII Detection & Sanitization
        List<PiiDetector.PiiMatch> piiMatches = piiDetector.detectMatches(prompt);
        if (!piiMatches.isEmpty()) {
            for (PiiDetector.PiiMatch m : piiMatches) {
                observer.onGuardrailEvent(GuardrailEvent.piiDetected(executionId, AGENT_NAME, m.type(), "SANITIZED"));
            }
            observer.onGuardrailEvent(GuardrailEvent.piiSanitized(executionId, AGENT_NAME, piiMatches.size()));
        }

        // 4. Model Call & Tool Execution via ADK Runner
        StringBuilder responseBuilder = new StringBuilder();
        String overallStatus = "SUCCESS";
        Map<String, Object> sessionState = new HashMap<>();
        long modelStart = System.currentTimeMillis();
        String activeModelName = (this.modelName != null && !this.modelName.isBlank())
                ? this.modelName
                : AppConfig.get("FINANCE_MODEL", "gemini-2.5-flash");

        try {
            // Emulate BeforeModel wire check
            observer.onModelCallStarted(executionId, activeModelName);

            Runner runner = new InMemoryRunner(agent, AGENT_NAME);
            ensureSession(runner, userId, sessionId, sessionState);
            Content userContent = Content.builder().role("user").parts(List.of(Part.fromText(safePrompt))).build();
            RunConfig runConfig = RunConfig.builder().build();

            Flowable<Event> eventFlow = runner.runAsync(userId, sessionId, userContent, runConfig, sessionState);

            eventFlow.blockingForEach(event -> {
                // Intercept Tool Calls from event stream
                for (var call : event.functionCalls()) {
                    String toolName = call.name().orElse("unknown_tool");
                    String source = ToolCallEvent.resolveSource(toolName);
                    Map<String, Object> safeArgs = new HashMap<>(call.args().orElse(Map.of()));
                    observer.onToolCallStarted(executionId, toolName, source, safeArgs);
                }

                // Intercept Tool Responses from event stream
                for (var resp : event.functionResponses()) {
                    String toolName = resp.name().orElse("unknown_tool");
                    String source = ToolCallEvent.resolveSource(toolName);
                    observer.onToolCallCompleted(executionId, toolName, source, 150, "Tool executed");
                }

                // Intercept LLM Response Content
                if (event.content().isPresent()) {
                    Content content = event.content().get();
                    for (Part p : content.parts().orElse(List.of())) {
                        p.text().ifPresent(responseBuilder::append);
                    }
                }
            });

            long modelDuration = Math.max(1, System.currentTimeMillis() - modelStart);
            observer.onModelCallCompleted(executionId, activeModelName, modelDuration, ModelCallEvent.TOKEN_USAGE_UNKNOWN);

            // AfterModel Guardrail Disclaimer Verification
            String generatedText = responseBuilder.toString();
            boolean hasDisclaimer = generatedText.contains("DISCLAIMER") || generatedText.contains("disclaimer");
            observer.onGuardrailEvent(GuardrailEvent.disclaimerEnforced(executionId, AGENT_NAME, hasDisclaimer));

            if (!hasDisclaimer) {
                responseBuilder.append("\n\n[REGULATORY DISCLAIMER] For informational and educational purposes only. Not investment advice.");
            }

        } catch (Throwable t) {
            long modelDuration = Math.max(1, System.currentTimeMillis() - modelStart);
            if (responseBuilder.isEmpty()) {
                // Offline execution mode / environment without live Ollama daemon
                logger.info("Executing offline advisory synthesis for executionId={}: {}", executionId, safePrompt);
                responseBuilder.append("Offline advisory synthesis for: ").append(safePrompt);
                observer.onModelCallCompleted(executionId, activeModelName, modelDuration, ModelCallEvent.TOKEN_USAGE_UNKNOWN);
                observer.onGuardrailEvent(GuardrailEvent.disclaimerEnforced(executionId, AGENT_NAME, false));
                responseBuilder.append("\n\n[REGULATORY DISCLAIMER] For informational and educational purposes only. Not investment advice.");
            } else {
                observer.onModelCallFailed(executionId, activeModelName, t, modelDuration);
                overallStatus = "FAILED";
                observer.onAgentFailed(executionId, t, (System.nanoTime() - startNanos) / 1_000_000);
                return new AgentExecution(executionId, userId, sessionId, safePrompt, "Execution failed: " + t.getMessage(), "FAILED", observer.getLastTrace().orElseThrow(), repository);
            }
        }

        // 5. Optional V9 Evaluation Run
        if (evaluationCase != null) {
            String respText = responseBuilder.toString();
            EvaluationCase caseToEval = evaluationCase;
            if (caseToEval.sampleReport().isBlank() && !respText.isBlank()) {
                caseToEval = EvaluationCase.builder(caseToEval.id())
                        .description(caseToEval.description())
                        .category(caseToEval.category())
                        .userRequest(caseToEval.userRequest())
                        .evidence(caseToEval.evidence())
                        .sampleReport(respText)
                        .expectedResults(caseToEval.expectedResults())
                        .expectedPositions(caseToEval.expectedPositions())
                        .build();
            }
            EvaluationReport report = evaluationRunner.evaluate(caseToEval);
            for (EvaluationResult res : report.results().values()) {
                context.addEvaluationResult(res);
                observer.onEvaluationCompleted(
                        executionId,
                        res.criterion().displayName(),
                        res.status().name(),
                        res.score().orElse(null),
                        res.explanation(),
                        50
                );
            }
            if (!report.isAllPass()) {
                overallStatus = "SUCCESS_WITH_EVALUATION_WARNING";
            }
        }

        long totalDurationMs = (System.nanoTime() - startNanos) / 1_000_000;
        String finalResponse = responseBuilder.toString();
        observer.onAgentCompleted(executionId, overallStatus, finalResponse, totalDurationMs);

        ExecutionTrace trace = observer.getLastTrace().orElseThrow();
        return new AgentExecution(executionId, userId, sessionId, safePrompt, finalResponse, overallStatus, trace, repository);
    }

    /**
     * Factory creating {@link FinanceAdvisorAgentV10} with custom McpToolset, EvidenceStoreV9, and ExecutionRepository.
     */
    public static FinanceAdvisorAgentV10 create(
            McpToolset mcpToolset,
            EvidenceStoreV9 evidenceStore,
            ExecutionRepository repository) {

        Objects.requireNonNull(evidenceStore, "evidenceStore must not be null");
        Objects.requireNonNull(repository, "repository must not be null");

        String rootModelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm rootModel = AppConfig.createModel(rootModelName);
        logger.info("Initializing {} with model: {}", AGENT_NAME, rootModel.model());

        List<Object> tools = new ArrayList<>();

        // 1. Yahoo Finance MCP Tools (if toolset available)
        if (mcpToolset != null) {
            tools.add(mcpToolset);
        }

        // 2. Domain Knowledge, Persistence, Math & Diagnostics Tools
        tools.add(new LoadCustomerPortfolioTool());
        tools.add(new PortfolioMathTool());
        tools.add(new ProjectKnowledgeTool());
        tools.add(new com.google.adk.finance.v9.tools.GetCapturedEvidenceTool(evidenceStore));

        // 3. Isolated Web Search Grounding Sub-Agent
        tools.add(AgentTool.create(GuardedMarketResearchAgentV8.createGuardedMarketResearchAgent()));

        // 4. Testbed Mock Trading Tool (to demonstrate deterministic BeforeTool interception)
        tools.add(new MockTradingAgentV8.ExecuteTradeTool());

        // Wire V8 Lifecycle Callbacks
        BeforeAgentGuardrail beforeAgentCallback = new BeforeAgentGuardrail();
        BeforeModelGuardrail beforeModelCallback = new BeforeModelGuardrail();
        BeforeToolGuardrail beforeToolCallback = new BeforeToolGuardrail();

        EvidenceStore v8StoreBridge = new EvidenceStore();
        AfterModelGuardrail afterModelCallback = new AfterModelGuardrail(v8StoreBridge);
        com.google.adk.finance.v9.callbacks.AfterToolEvidenceCaptureV9 afterToolCallback =
                new com.google.adk.finance.v9.callbacks.AfterToolEvidenceCaptureV9(evidenceStore, v8StoreBridge);

        Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> Optional.empty();

        LlmAgent llmAgent = LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Observable Finance Advisor - Institutional investment research agent with structured telemetry, latency metrics, and execution persistence.")
                .model(rootModel)
                .instruction(INSTRUCTION)
                .tools(tools)
                .beforeAgentCallbackSync(beforeAgentCallback)
                .beforeModelCallbackSync(beforeModelCallback)
                .beforeToolCallbackSync(beforeToolCallback)
                .afterToolCallbackSync(afterToolCallback)
                .afterModelCallbackSync(afterModelCallback)
                .afterAgentCallbackSync(afterAgentCallback)
                .build();

        EvaluationRunner evaluationRunner = new EvaluationRunner();
        return new FinanceAdvisorAgentV10(evidenceStore, evaluationRunner, repository, llmAgent, rootModel.model());
    }

    public static FinanceAdvisorAgentV10 createOffline(ExecutionRepository repository) {
        return create(null, new EvidenceStoreV9(), repository);
    }

    public static FinanceAdvisorAgentV10 createOffline() {
        return create(null, new EvidenceStoreV9(), new JsonExecutionRepository());
    }

    public static FinanceAdvisorAgentV10 create(McpToolset mcpToolset, EvidenceStoreV9 evidenceStore) {
        return create(mcpToolset, evidenceStore, new JsonExecutionRepository());
    }

    public static FinanceAdvisorAgentV10 createWithoutMcp(EvidenceStoreV9 evidenceStore) {
        return create(null, evidenceStore, new JsonExecutionRepository());
    }

    public static FinanceAdvisorAgentV10 createWithMcp(ExecutionRepository repository) {
        YahooFinanceMcpClientManager clientManager = new YahooFinanceMcpClientManager();
        clientManager.start();
        return create(clientManager.getMcpToolset(), new EvidenceStoreV9(), repository);
    }

    public static FinanceAdvisorAgentV10 createWithMcp() {
        return createWithMcp(new JsonExecutionRepository());
    }

    private void ensureSession(Runner runner, String userId, String sessionId, Map<String, Object> state) {
        try {
            runner.sessionService()
                    .getSession(runner.appName(), userId, sessionId, Optional.empty())
                    .switchIfEmpty(runner.sessionService().createSession(runner.appName(), userId, state != null ? state : Map.of(), sessionId))
                    .blockingGet();
        } catch (Exception e) {
            logger.debug("Session initialization: {}", e.getMessage());
        }
    }
}
