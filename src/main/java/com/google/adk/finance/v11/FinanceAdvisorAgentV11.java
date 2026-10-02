package com.google.adk.finance.v11;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.tools.LoadCustomerPortfolioTool;
import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.adk.finance.tools.ProjectKnowledgeTool;
import com.google.adk.finance.v5.YahooFinanceMcpClientManager;
import com.google.adk.finance.v11.agents.MarketResearchAgentV11;
import com.google.adk.finance.v11.agents.PortfolioRiskAgentV11;
import com.google.adk.finance.v11.hooks.HookRegistry;
import com.google.adk.finance.v11.hooks.PostToolObservationHook;
import com.google.adk.finance.v11.hooks.PreAgentRuleEnforcementHook;
import com.google.adk.finance.v11.hooks.PreToolSourceValidationHook;
import com.google.adk.finance.v11.hooks.ResponseValidationHook;
import com.google.adk.finance.v11.ruleloader.RuleLoader;
import com.google.adk.finance.v11.ruleloader.ScopedRules;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.AgentTool;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.adk.tools.mcp.McpToolset;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Week 5+ — Version 11: Finance Advisor V11 (Rules-Driven and Hook-Aware Agent).
 * <p>
 * Core Educational Pillars:
 * <ul>
 *   <li><b>RULES:</b> Persistent, deterministic instructions loaded from {@code v11/rules/} defining
 *       how the agent must behave (truthfulness, source mapping, risk assumptions, response standards).</li>
 *   <li><b>HOOKS:</b> Programmatic lifecycle interception points executing deterministic code before
 *       and after agent, tool, and model events to validate, observe, and enforce boundaries.</li>
 * </ul>
 * <p>
 * Lifecycle Hook Wiring:
 * <pre>
 * [PreAgentRuleEnforcementHook] (BeforeAgent, Non-Blocking: verifies rules loaded)
 *               │
 *               ▼
 *     FinanceAdvisorAgentV11
 *               │
 *        ┌──────┴──────┐
 *        ▼             ▼
 *    [Tool Call]   [Model Call]
 *        │             │
 *        ▼             ▼
 * [PreToolSourceValidationHook] (BeforeTool, Blocking: enforces source-rules.md)
 *        │
 *        ▼
 *  [Tool Execution: MCP / Search / Math / DB]
 *        │
 *        ▼
 * [PostToolObservationHook] (AfterTool, Non-Blocking: telemetry & latency capture)
 *        │
 *        ▼
 * [ResponseValidationHook] (AfterModel, Blocking/Remediating: checks citations, non-empty, disclaimer)
 * </pre>
 */
public final class FinanceAdvisorAgentV11 {
    private static final Logger logger = LoggerFactory.getLogger(FinanceAdvisorAgentV11.class);

    public static final String AGENT_NAME = "finance_advisor_v11";
    public static final String ROLE_NAME = "rules_hook_finance_advisor";

    public static final String BASE_INSTRUCTION = """
            You are rules_hook_finance_advisor (Finance Advisor v11), an institutional-grade investment research advisor.
            Your role is EVIDENCE-GROUNDED PORTFOLIO RESEARCH, FUNDAMENTAL ANALYSIS, QUANTITATIVE RISK, and SCENARIO MODELING.

            OPERATIONAL DIRECTIVES:
            1. You operate under strict V11 RULES and LIFECYCLE HOOKS.
            2. For structured market fundamentals and quotes, use official Yahoo Finance MCP tools (`get_stock_info`, etc.).
            3. For live breaking news, earnings releases, and analyst updates, delegate to `market_researcher_v11`.
            4. For deterministic math (PnL, allocation weights, concentration risk), call `portfolio_math`. Never invent numbers.
            5. For risk assessments, delegate to `portfolio_risk_agent_v11` or compute concentration metrics via `portfolio_math`.
            6. If customer ID is provided, query holdings via `load_customer_portfolio`.
            7. All assertions must cite primary sources (Yahoo Finance MCP, Google Search, SEC filings).
            8. Conclude every response with the mandatory regulatory disclaimer.
            """;

    private final LlmAgent agent;
    private final RuleLoader ruleLoader;
    private final HookRegistry hookRegistry;

    private FinanceAdvisorAgentV11(LlmAgent agent, RuleLoader ruleLoader, HookRegistry hookRegistry) {
        this.agent = agent;
        this.ruleLoader = ruleLoader;
        this.hookRegistry = hookRegistry;
    }

    public LlmAgent getAgent() {
        return agent;
    }

    public RuleLoader getRuleLoader() {
        return ruleLoader;
    }

    public HookRegistry getHookRegistry() {
        return hookRegistry;
    }

    /**
     * Core factory building Finance Advisor V11 with full lifecycle hooks and scoped rules.
     */
    public static FinanceAdvisorAgentV11 create(McpToolset mcpToolset, RuleLoader ruleLoader, HookRegistry hookRegistry) {
        Objects.requireNonNull(ruleLoader, "ruleLoader must not be null");
        Objects.requireNonNull(hookRegistry, "hookRegistry must not be null");

        String rootModelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm rootModel = AppConfig.createModel(rootModelName);
        logger.info("Initializing {} with model: {}", AGENT_NAME, rootModel.model());

        // 1. Load and inject scoped rules into instruction
        ScopedRules rootRules = ruleLoader.forRootAdvisorAgent();
        String fullInstruction = rootRules.applyToInstruction(BASE_INSTRUCTION);

        // 2. Register tools
        List<Object> tools = new ArrayList<>();
        if (mcpToolset != null) {
            tools.add(mcpToolset);
        }
        tools.add(new LoadCustomerPortfolioTool());
        tools.add(new PortfolioMathTool());
        tools.add(new ProjectKnowledgeTool());
        tools.add(AgentTool.create(MarketResearchAgentV11.create(ruleLoader)));
        tools.add(AgentTool.create(PortfolioRiskAgentV11.create(ruleLoader)));

        // Testbed tool to demonstrate deterministic PreTool blocking hook
        tools.add(new ProhibitedTradingTool());

        // 3. Instantiate V11 Lifecycle Hooks
        PreAgentRuleEnforcementHook beforeAgentHook = new PreAgentRuleEnforcementHook(hookRegistry, ruleLoader);
        PreToolSourceValidationHook beforeToolHook = new PreToolSourceValidationHook(hookRegistry);
        PostToolObservationHook afterToolHook = new PostToolObservationHook(hookRegistry);
        ResponseValidationHook afterModelHook = new ResponseValidationHook(hookRegistry);

        Callbacks.AfterAgentCallbackSync afterAgentHook = context -> {
            logger.info("[{}] Completed agent turn. Total hooks executed: {}, Blocked: {}",
                    AGENT_NAME, hookRegistry.getExecutionHistory().size(), hookRegistry.getBlockedCount());
            return Optional.empty();
        };

        // 4. Build LlmAgent with Google ADK lifecycle callbacks
        LlmAgent llmAgent = LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Finance Advisor v11 - Rules-driven and hook-aware institutional investment advisor.")
                .model(rootModel)
                .instruction(fullInstruction)
                .tools(tools)
                .beforeAgentCallbackSync(beforeAgentHook)
                .beforeToolCallbackSync(beforeToolHook)
                .afterToolCallbackSync(afterToolHook)
                .afterModelCallbackSync(afterModelHook)
                .afterAgentCallbackSync(afterAgentHook)
                .build();

        return new FinanceAdvisorAgentV11(llmAgent, ruleLoader, hookRegistry);
    }

    public static FinanceAdvisorAgentV11 create(McpToolset mcpToolset) {
        return create(mcpToolset, RuleLoader.getInstance(), HookRegistry.getInstance());
    }

    public static FinanceAdvisorAgentV11 createWithMcp() {
        YahooFinanceMcpClientManager clientManager = new YahooFinanceMcpClientManager();
        clientManager.start();
        return create(clientManager.getMcpToolset(), RuleLoader.getInstance(), HookRegistry.getInstance());
    }

    public static FinanceAdvisorAgentV11 createWithoutMcp() {
        return create(null, RuleLoader.getInstance(), HookRegistry.getInstance());
    }

    /**
     * Testbed tool representing an unauthorized trading operation.
     * When invoked, {@link PreToolSourceValidationHook} intercepts and blocks this tool call before execution.
     */
    public static final class ProhibitedTradingTool extends BaseTool {
        public ProhibitedTradingTool() {
            super("execute_trade", "Prohibited test tool for order execution");
        }

        @Override
        public Optional<FunctionDeclaration> declaration() {
            return Optional.of(FunctionDeclaration.builder()
                    .name(name())
                    .description(description())
                    .parameters(Schema.builder()
                            .type(Type.Known.OBJECT)
                            .properties(Map.of(
                                    "symbol", Schema.builder().type(Type.Known.STRING).description("Stock symbol").build(),
                                    "action", Schema.builder().type(Type.Known.STRING).description("BUY or SELL").build(),
                                    "quantity", Schema.builder().type(Type.Known.INTEGER).description("Number of shares").build()
                            ))
                            .build())
                    .build());
        }

        @Override
        public Single<Map<String, Object>> runAsync(Map<String, Object> input, ToolContext toolContext) {
            // Should never be reached if PreToolSourceValidationHook is working properly
            return Single.just(Map.of("status", "EXECUTED", "warning", "UNAUTHORIZED TRADE EXECUTED"));
        }
    }
}
