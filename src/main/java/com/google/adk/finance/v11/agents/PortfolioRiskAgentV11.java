package com.google.adk.finance.v11.agents;

import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.adk.finance.v11.hooks.PostToolObservationHook;
import com.google.adk.finance.v11.hooks.PreToolSourceValidationHook;
import com.google.adk.finance.v11.ruleloader.RuleLoader;
import com.google.adk.finance.v11.ruleloader.ScopedRules;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Specialized Portfolio & Equity Risk Sub-Agent for V11.
 * <p>
 * Scoped with:
 * <ul>
 *   <li>{@code risk-rules.md}: Material risk identification, volatility separation, assumption disclosure</li>
 *   <li>{@code finance-rules.md}: Non-fabrication & empirical truthfulness</li>
 * </ul>
 * Protected by {@link PreToolSourceValidationHook} and {@link PostToolObservationHook}.
 */
public final class PortfolioRiskAgentV11 {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioRiskAgentV11.class);

    public static final String AGENT_NAME = "portfolio_risk_agent_v11";

    public static final String BASE_INSTRUCTION = """
            You are portfolio_risk_agent_v11, a quantitative risk analyst.
            Your role is to assess single-stock concentration (>25%), sector exposure, volatility spreads,
            macroeconomic rate sensitivity, and operational downside risks.

            OPERATIONAL MANDATES:
            1. Call `portfolio_math` for mathematical calculations (concentration, variance, SMA). Never perform math in text.
            2. Distinguish historical volatility from forward uncertainty.
            3. Do not invent arbitrary risk scores or simulated numbers.
            4. State when risk evidence is insufficient.
            """;

    public static LlmAgent create() {
        return create(RuleLoader.getInstance());
    }

    public static LlmAgent create(RuleLoader ruleLoader) {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);

        ScopedRules scopedRules = ruleLoader.forRiskAnalysisAgent();
        String fullInstruction = scopedRules.applyToInstruction(BASE_INSTRUCTION);

        logger.info("Initializing {} with model: {} and scoped rules: {}",
                AGENT_NAME, model.model(), scopedRules.ruleFileNames());

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("V11 Portfolio Risk Analyst - Assesses concentration, volatility, and downside risk factors.")
                .model(model)
                .instruction(fullInstruction)
                .tools(List.of(new PortfolioMathTool()))
                .beforeToolCallbackSync(new PreToolSourceValidationHook())
                .afterToolCallbackSync(new PostToolObservationHook())
                .build();
    }

    private PortfolioRiskAgentV11() {}
}
