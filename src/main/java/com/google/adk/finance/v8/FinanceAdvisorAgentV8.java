package com.google.adk.finance.v8;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.tools.LoadCustomerPortfolioTool;
import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.adk.finance.tools.ProjectKnowledgeTool;
import com.google.adk.finance.v5.YahooFinanceMcpClientManager;
import com.google.adk.finance.v8.callbacks.AfterModelGuardrail;
import com.google.adk.finance.v8.callbacks.AfterToolEvidenceCapture;
import com.google.adk.finance.v8.callbacks.BeforeAgentGuardrail;
import com.google.adk.finance.v8.callbacks.BeforeModelGuardrail;
import com.google.adk.finance.v8.callbacks.BeforeToolGuardrail;
import com.google.adk.finance.v8.evidence.EvidenceStore;
import com.google.adk.finance.v8.subagents.GuardedMarketResearchAgentV8;
import com.google.adk.finance.v8.subagents.MockTradingAgentV8;
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
 * Week 4 — Version 8: Finance Advisor V8 (Comprehensive Input, Tool, Model, and Output Guardrails).
 * <p>
 * Primary Learning Objective:
 * <blockquote>
 * Learn how deterministic guardrails and Google ADK lifecycle callbacks protect an autonomous agent
 * before input is processed, before tools are executed, after the model responds, and before the final
 * answer reaches the user.
 * </blockquote>
 * <p>
 * Architectural Lifecycle Pipeline:
 * <pre>
 *                  USER INPUT
 *                       │
 *                       ▼
 *              BeforeAgentCallback (BeforeAgentGuardrail)
 *              ├── PromptInjectionDetector (Blocks instruction overrides & extraction)
 *              └── PiiSanitizer (Redacts bank accounts, cards, phone, email, SSN, PAN)
 *                       │
 *                       ▼
 *              BeforeModelCallback (BeforeModelGuardrail)
 *              └── Wire-level sanitization before sending to Gemini
 *                       │
 *                       ▼
 *                 AGENT / MODEL
 *                       │
 *                       ▼
 *              BeforeToolCallback (BeforeToolGuardrail)
 *              ├── ToolOperationGuard (Authorizes research tools; blocks trades & transfers)
 *              └── TickerValidator (Validates exchange format, length, injection safety)
 *                       │
 *                       ▼
 *                  MCP / Search Tool
 *                       │
 *                       ▼
 *              AfterToolCallback (AfterToolEvidenceCapture)
 *              └── Records empirical figures (symbol, price, metrics) into EvidenceStore
 *                       │
 *                       ▼
 *                     MODEL
 *                       │
 *                       ▼
 *              AfterModelCallback (AfterModelGuardrail)
 *              ├── OffensiveLanguageDetector (Blocks abusive or toxic terms)
 *              ├── Output PII Scrubbing (Prevents data reflection)
 *              ├── HallucinationDetector (Audits claimed numbers vs EvidenceStore)
 *              └── ComplianceDisclaimerGuard (Enforces institutional regulatory disclaimer)
 *                       │
 *                       ▼
 *                 FINAL RESPONSE
 * </pre>
 */
public final class FinanceAdvisorAgentV8 {
    private static final Logger logger = LoggerFactory.getLogger(FinanceAdvisorAgentV8.class);

    public static final String AGENT_NAME = "finance_advisor_v8";
    public static final String ROLE_NAME = "guarded_finance_advisor";

    public static final String INSTRUCTION = """
            You are guarded_finance_advisor (Finance Advisor v8), an institutional-grade investment research and decision-support advisor.
            Your role is EVIDENCE-GROUNDED PORTFOLIO RESEARCH, VARIANCE ATTRIBUTION, and SCENARIO MODELING.

            SYSTEM BOUNDARIES & SAFETY INVARIANTS:
            1. You are strictly a research, analytical, and educational decision-support assistant.
               You MUST NOT attempt to execute trades, buy/sell securities, or transfer funds.
            2. For empirical market quotes and valuation metrics, rely on official Yahoo Finance MCP tools (`get_stock_info`, etc.).
            3. For real-time news, filings, and earnings announcements, invoke `stockmarket_researcher`.
            4. For deterministic mathematical calculations (PnL, weights, concentration), call `portfolio_math`. Never invent numbers.
            5. If a customer ID is provided, query holdings via `load_customer_portfolio`.
            6. All statements must be factually consistent with verified tool outputs.
            7. Conclude every synthesis with the mandatory regulatory disclaimer.
            """;

    private final EvidenceStore evidenceStore;
    private final LlmAgent agent;

    private FinanceAdvisorAgentV8(EvidenceStore evidenceStore, LlmAgent agent) {
        this.evidenceStore = evidenceStore;
        this.agent = agent;
    }

    public EvidenceStore getEvidenceStore() {
        return evidenceStore;
    }

    public LlmAgent getAgent() {
        return agent;
    }

    /**
     * Creates an instance of {@link FinanceAdvisorAgentV8} with an isolated {@link EvidenceStore}
     * and full lifecycle guardrails.
     *
     * @param mcpToolset the active Yahoo Finance MCP toolset (can be null if running offline/mock tests)
     * @param evidenceStore the shared in-memory evidence store
     * @return initialized {@link FinanceAdvisorAgentV8}
     */
    public static FinanceAdvisorAgentV8 create(McpToolset mcpToolset, EvidenceStore evidenceStore) {
        Objects.requireNonNull(evidenceStore, "evidenceStore must not be null");

        String rootModelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm rootModel = AppConfig.createModel(rootModelName);
        logger.info("Initializing {} with model: {}", AGENT_NAME, rootModel.model());

        List<Object> tools = new ArrayList<>();

        // 1. Yahoo Finance MCP Tools (if toolset available)
        if (mcpToolset != null) {
            tools.add(mcpToolset);
        }

        // 2. Domain Knowledge, Persistence & Math Tools
        tools.add(new LoadCustomerPortfolioTool());
        tools.add(new PortfolioMathTool());
        tools.add(new ProjectKnowledgeTool());

        // 3. Isolated Web Search Grounding Sub-Agent
        tools.add(AgentTool.create(GuardedMarketResearchAgentV8.createGuardedMarketResearchAgent()));

        // 4. Testbed Mock Trading Tool (to demonstrate deterministic BeforeTool interception)
        tools.add(new MockTradingAgentV8.ExecuteTradeTool());

        // Wire Google ADK Lifecycle Callbacks
        BeforeAgentGuardrail beforeAgentCallback = new BeforeAgentGuardrail();
        BeforeModelGuardrail beforeModelCallback = new BeforeModelGuardrail();
        BeforeToolGuardrail beforeToolCallback = new BeforeToolGuardrail();
        AfterToolEvidenceCapture afterToolCallback = new AfterToolEvidenceCapture(evidenceStore);
        AfterModelGuardrail afterModelCallback = new AfterModelGuardrail(evidenceStore);

        Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> {
            logger.info("[Parent Agent Lifecycle] FinanceAdvisorAgentV8 completed turn. Total verified facts in store: {}",
                    evidenceStore.getAllFacts().size());
            return Optional.empty();
        };

        LlmAgent llmAgent = LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Guarded Finance Advisor - Institutional investment research agent protected by BeforeAgent, BeforeModel, BeforeTool, AfterTool, and AfterModel guardrails.")
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

        return new FinanceAdvisorAgentV8(evidenceStore, llmAgent);
    }

    /**
     * Convenience factory creating an agent with an internal new {@link EvidenceStore}.
     */
    public static FinanceAdvisorAgentV8 create(McpToolset mcpToolset) {
        return create(mcpToolset, new EvidenceStore());
    }

    /**
     * Convenience factory connecting to Yahoo Finance MCP server via {@link YahooFinanceMcpClientManager}.
     */
    public static FinanceAdvisorAgentV8 createWithMcp() {
        YahooFinanceMcpClientManager clientManager = new YahooFinanceMcpClientManager();
        clientManager.start();
        return create(clientManager.getMcpToolset(), new EvidenceStore());
    }

    /**
     * Convenience factory creating an isolated agent without MCP server for offline unit and integration tests.
     */
    public static FinanceAdvisorAgentV8 createWithoutMcp(EvidenceStore evidenceStore) {
        return create(null, evidenceStore);
    }
}
