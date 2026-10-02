package com.google.adk.finance.v11.agents;

import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v11.hooks.PostToolObservationHook;
import com.google.adk.finance.v11.hooks.PreToolSourceValidationHook;
import com.google.adk.finance.v11.ruleloader.RuleLoader;
import com.google.adk.finance.v11.ruleloader.ScopedRules;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.mcp.McpToolset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Specialized Fundamental Analysis Sub-Agent for V11.
 * <p>
 * Scoped with:
 * <ul>
 *   <li>{@code source-rules.md}: Structured financial data via Yahoo Finance MCP</li>
 *   <li>{@code finance-rules.md}: Non-fabrication & empirical truthfulness</li>
 * </ul>
 * Protected by {@link PreToolSourceValidationHook} and {@link PostToolObservationHook}.
 */
public final class FundamentalAnalysisAgentV11 {
    private static final Logger logger = LoggerFactory.getLogger(FundamentalAnalysisAgentV11.class);

    public static final String AGENT_NAME = "fundamental_analyst_v11";

    public static final String BASE_INSTRUCTION = """
            You are fundamental_analyst_v11, an institutional equity analyst.
            Your role is to analyze core financial statements, revenue growth, operating margins, capital efficiency (ROE/ROCE),
            cash generation, debt coverage, and valuation multiples.

            OPERATIONAL MANDATES:
            1. Rely on official Yahoo Finance MCP tools (`get_stock_info`, `get_financial_statement`, `get_recommendations`).
            2. Never invent P/E ratios, revenue growth, operating margins, or debt levels.
            3. If MCP data is unavailable, state the limitation rather than guessing.
            4. Cite Yahoo Finance MCP as the primary data source.
            """;

    public static LlmAgent create(McpToolset mcpToolset) {
        return create(mcpToolset, RuleLoader.getInstance());
    }

    public static LlmAgent create(McpToolset mcpToolset, RuleLoader ruleLoader) {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);

        ScopedRules scopedRules = ruleLoader.forFundamentalAnalysisAgent();
        String fullInstruction = scopedRules.applyToInstruction(BASE_INSTRUCTION);

        List<Object> tools = new ArrayList<>();
        if (mcpToolset != null) {
            tools.add(mcpToolset);
        }

        logger.info("Initializing {} with model: {} and scoped rules: {}",
                AGENT_NAME, model.model(), scopedRules.ruleFileNames());

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("V11 Fundamental Analyst - Evaluates balance sheets, margins, and valuation via Yahoo Finance MCP.")
                .model(model)
                .instruction(fullInstruction)
                .tools(tools)
                .beforeToolCallbackSync(new PreToolSourceValidationHook())
                .afterToolCallbackSync(new PostToolObservationHook())
                .build();
    }

    private FundamentalAnalysisAgentV11() {}
}
