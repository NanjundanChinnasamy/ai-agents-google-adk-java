package com.google.adk.finance.v9;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.tools.LoadCustomerPortfolioTool;
import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.adk.finance.tools.ProjectKnowledgeTool;
import com.google.adk.finance.v5.YahooFinanceMcpClientManager;
import com.google.adk.finance.v8.callbacks.AfterModelGuardrail;
import com.google.adk.finance.v8.callbacks.BeforeAgentGuardrail;
import com.google.adk.finance.v8.callbacks.BeforeModelGuardrail;
import com.google.adk.finance.v8.callbacks.BeforeToolGuardrail;
import com.google.adk.finance.v8.evidence.EvidenceStore;
import com.google.adk.finance.v8.subagents.GuardedMarketResearchAgentV8;
import com.google.adk.finance.v8.subagents.MockTradingAgentV8;
import com.google.adk.finance.v9.evaluation.EvaluationCase;
import com.google.adk.finance.v9.evaluation.EvaluationReport;
import com.google.adk.finance.v9.evaluation.EvaluationRunner;
import com.google.adk.finance.v9.evaluation.EvidenceStoreV9;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.AgentTool;
import com.google.adk.tools.mcp.McpToolset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Week 5 — Version 9: Finance Advisor V9 (Systematic Evaluation + Adversarial Failure Testing).
 * <p>
 * Primary Learning Objective:
 * <blockquote>
 * <i>"How do I know my Finance Advisor is actually producing trustworthy results?"</i><br>
 * Learn how to systematically evaluate an AI agent across deterministic criteria (Faithfulness,
 * Calculation Fidelity, Scenario Completeness), benchmark with LLM-as-a-Judge, and deliberately
 * test failure/injection behaviors when things go wrong.
 * </blockquote>
 * <p>
 * Architectural Topology:
 * <pre>
 *                           USER REQUEST / TEST CASE
 *                                      │
 *                                      ▼
 *                         +──────────────────────────+
 *                         |  FinanceAdvisorAgentV9   |
 *                         |  (Google ADK LlmAgent)   |
 *                         +────────────┬─────────────+
 *                                      │
 *            ┌─────────────────────────┼─────────────────────────┐
 *            ▼                         ▼                         ▼
 *  [Yahoo Finance MCP]        [Google Search Agent]      [PortfolioMathTool]
 *  (Quotes, P/E, Cap)         (News, SEC Filings)        (PnL, Weights, SMA)
 *            │                         │                         │
 *            └─────────────────────────┼─────────────────────────┘
 *                                      │
 *                                      ▼
 *                         [AfterTool Evidence Capture]
 *                                      │
 *                                      ▼
 *                         [EvidenceStoreV9 Repository]
 *                                      │
 *                         +────────────┴─────────────+
 *                         |    FINAL AGENT REPORT    |
 *                         +────────────┬─────────────+
 *                                      │
 *            ┌─────────────────────────┴─────────────────────────┐
 *            ▼                                                   ▼
 *  [Deterministic Evaluators]                              [LLM Judge]
 *  ├── FaithfulnessEvaluator (Traceability to Evidence)   ├── Evidence Usage
 *  ├── CalculationFidelityEvaluator (PortfolioMathTool)   ├── Uncertainty Handling
 *  └── ScenarioCompletenessEvaluator (3 Tiers)            └── Explanation Quality
 *            │                                                   │
 *            └─────────────────────────┬─────────────────────────┘
 *                                      │
 *                                      ▼
 *                          [EvaluationReport: PASS/FAIL]
 * </pre>
 */
public final class FinanceAdvisorAgentV9 {
    private static final Logger logger = LoggerFactory.getLogger(FinanceAdvisorAgentV9.class);

    public static final String AGENT_NAME = "finance_advisor_v9";
    public static final String ROLE_NAME = "evaluated_finance_advisor";

    public static final String INSTRUCTION = """
            You are evaluated_finance_advisor (Finance Advisor v9), an institutional-grade investment research and decision-support advisor.
            Your role is EVIDENCE-GROUNDED PORTFOLIO RESEARCH, VARIANCE ATTRIBUTION, DETERMINISTIC MATH, and 3-TIER SCENARIO MODELING.

            SYSTEM BOUNDARIES & VERIFIABILITY INVARIANTS:
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
            9. If asked to inspect, verify, or show captured evidence, call 'get_captured_evidence' to display the verified records.
            """;

    private final EvidenceStoreV9 evidenceStore;
    private final EvaluationRunner evaluationRunner;
    private final LlmAgent agent;

    private FinanceAdvisorAgentV9(EvidenceStoreV9 evidenceStore, EvaluationRunner evaluationRunner, LlmAgent agent) {
        this.evidenceStore = evidenceStore;
        this.evaluationRunner = evaluationRunner;
        this.agent = agent;
    }

    public EvidenceStoreV9 getEvidenceStore() {
        return evidenceStore;
    }

    public EvaluationRunner getEvaluationRunner() {
        return evaluationRunner;
    }

    public LlmAgent getAgent() {
        return agent;
    }

    /**
     * Executes the V9 evaluation runner against a golden test case.
     */
    public EvaluationReport evaluate(EvaluationCase evaluationCase) {
        return evaluationRunner.evaluate(evaluationCase);
    }

    /**
     * Creates an instance of {@link FinanceAdvisorAgentV9} with an isolated {@link EvidenceStoreV9}
     * and full evaluation harness.
     *
     * @param mcpToolset the active Yahoo Finance MCP toolset (can be null if running offline/mock tests)
     * @param evidenceStore the shared in-memory V9 evidence store
     * @return initialized {@link FinanceAdvisorAgentV9}
     */
    public static FinanceAdvisorAgentV9 create(McpToolset mcpToolset, EvidenceStoreV9 evidenceStore) {
        Objects.requireNonNull(evidenceStore, "evidenceStore must not be null");

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

        // Wire V8 Lifecycle Perimeter (Input Sanitization, Injection Defense, Output Safety)
        BeforeAgentGuardrail beforeAgentCallback = new BeforeAgentGuardrail();
        BeforeModelGuardrail beforeModelCallback = new BeforeModelGuardrail();
        BeforeToolGuardrail beforeToolCallback = new BeforeToolGuardrail();

        // Dual-Evidence Store Bridge for V8 & V9 Compatibility
        EvidenceStore v8StoreBridge = new EvidenceStore();
        AfterModelGuardrail afterModelCallback = new AfterModelGuardrail(v8StoreBridge);
        com.google.adk.finance.v9.callbacks.AfterToolEvidenceCaptureV9 afterToolCallback =
                new com.google.adk.finance.v9.callbacks.AfterToolEvidenceCaptureV9(evidenceStore, v8StoreBridge);

        Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> {
            logger.info("[Parent Agent Lifecycle] FinanceAdvisorAgentV9 completed turn. Total verified evidence records: {}",
                    evidenceStore.size());
            return Optional.empty();
        };

        LlmAgent llmAgent = LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Evaluated Finance Advisor - Institutional investment research agent evaluated across Faithfulness, Math Fidelity, Scenario Completeness, and Failure Testing.")
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
        return new FinanceAdvisorAgentV9(evidenceStore, evaluationRunner, llmAgent);
    }

    /**
     * Convenience factory creating an agent with an internal new {@link EvidenceStoreV9}.
     */
    public static FinanceAdvisorAgentV9 create(McpToolset mcpToolset) {
        return create(mcpToolset, new EvidenceStoreV9());
    }

    /**
     * Convenience factory connecting to Yahoo Finance MCP server via {@link YahooFinanceMcpClientManager}.
     */
    public static FinanceAdvisorAgentV9 createWithMcp() {
        YahooFinanceMcpClientManager clientManager = new YahooFinanceMcpClientManager();
        clientManager.start();
        return create(clientManager.getMcpToolset(), new EvidenceStoreV9());
    }

    /**
     * Convenience factory creating an isolated agent without MCP server for offline unit and integration tests.
     */
    public static FinanceAdvisorAgentV9 createWithoutMcp(EvidenceStoreV9 evidenceStore) {
        return create(null, evidenceStore);
    }

    /**
     * Convenience factory creating a completely isolated offline agent.
     */
    public static FinanceAdvisorAgentV9 createOffline() {
        return create(null, new EvidenceStoreV9());
    }
}
