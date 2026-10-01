package com.google.adk.finance.v7.subagents;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.adk.finance.tools.ProjectKnowledgeTool;
import com.google.adk.models.BaseLlm;
import com.google.adk.skills.LocalSkillSource;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.skills.SkillToolset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Week 4 — Version 7: Stage 3 Sub-Agent for Sequential Workflow (Risk Analysis).
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Stage 3 of {@code InvestmentResearchSequentialWorkflowV7}.</li>
 *   <li>Ingests previous stages via {@code {company_research_output?}} and {@code {fundamental_analysis_output?}}.</li>
 *   <li>Identifies business risks, financial leverage hazards, market beta sensitivity,
 *       historical drawdown volatility, and concentration risk.</li>
 *   <li>Publishes findings under {@link #OUTPUT_KEY} ({@code risk_analysis_output})
 *       for consumption by Stage 4 (Valuation Analysis).</li>
 * </ul>
 */
public final class RiskAnalysisAgentV7 {
    private static final Logger logger = LoggerFactory.getLogger(RiskAnalysisAgentV7.class);

    public static final String AGENT_NAME = "risk_analysis_step";
    public static final String OUTPUT_KEY = "risk_analysis_output";
    public static final Path FINANCE_SKILLS_PATH = Path.of("skills", "finance");
    public static final Path KNOWLEDGE_PATH = Path.of("knowledge");

    public static final String INSTRUCTION = """
            You are risk_analysis_step (RiskAnalysisAgentV7), Stage 3 of the deterministic Investment Research Sequential Workflow.
            Your task is to identify and structure investment risk factors, market sensitivity, and operational hazards.

            Context from Upstream Stage 1 (Company Research):
            {company_research_output?}

            Context from Upstream Stage 2 (Fundamental Analysis):
            {fundamental_analysis_output?}

            Core Risk Assessment Pillars:
            1. Systematic Market Risk: Beta (β) sensitivity relative to the market benchmark (>1.0 indicates higher volatility; <1.0 indicates defensive).
            2. Volatility & Drawdown Spread: 52-week high vs 52-week low spread, historical price swings.
            3. Financial & Leverage Risk: Debt burden, interest coverage risk, debt maturities, or liquidity stress identified from fundamentals.
            4. Operational & Business Risks: Customer/supplier concentration, regulatory shifts, technological obsolescence, competitive threats identified in Stage 1 research.
            5. Concentration & Portfolio Risk: Potential hazards if this asset represents an outsized share (>25%) of a portfolio.

            Tool Usage Directives:
            - Call `get_stock_info` to retrieve Beta, 52-week high/low, and volatility metrics.
            - Use `portfolio_math` for mathematical calculations if portfolio weights or spreads need computing.
            - Use `read_project_knowledge` for risk taxonomy (topic='risk-framework' or 'portfolio-principles').

            Output Format:
            Format your output strictly using this structured template:
            [STAGE 3: RISK ANALYSIS REPORT]
            • Company / Symbol: <Target Company>
            • Systematic Risk:
              - Beta (β): <Value and interpretation: aggressive vs defensive>
              - 52-Week Range & Volatility: <Low, High, Drawdown spread>
            • Financial & Capital Structure Risks:
              - Leverage & Solvency Hazards: <Debt/Equity analysis, liquidity concerns>
            • Business & Operational Vulnerabilities:
              - <Key risk factor 1 based on recent developments and industry dynamics>
              - <Key risk factor 2 based on recent developments and industry dynamics>
              - <Key risk factor 3 based on recent developments and industry dynamics>
            • Risk Classification Summary: <Low / Moderate / Elevated / High Risk profile with concise rationale>
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeAgentCallback = context -> {
        logger.info("[Sequential Stage 3] RiskAnalysisAgentV7 started risk assessment with Stages 1 & 2 context.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> {
        logger.info("[Sequential Stage 3] RiskAnalysisAgentV7 completed risk assessment. Output published to key: {}", OUTPUT_KEY);
        return Optional.empty();
    };

    /**
     * Creates an instance of {@link RiskAnalysisAgentV7} with default model and MCP toolset.
     */
    public static LlmAgent create(McpToolset mcpToolset) {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);
        return create(model, mcpToolset);
    }

    /**
     * Creates an instance of {@link RiskAnalysisAgentV7} with the given model and MCP toolset.
     */
    public static LlmAgent create(BaseLlm model, McpToolset mcpToolset) {
        List<Object> tools = new ArrayList<>();

        if (mcpToolset != null) {
            tools.add(mcpToolset);
        }

        tools.add(new PortfolioMathTool());

        if (Files.exists(FINANCE_SKILLS_PATH)) {
            try {
                SkillToolset skillToolset = new SkillToolset(new LocalSkillSource(FINANCE_SKILLS_PATH));
                tools.add(skillToolset);
            } catch (Exception e) {
                logger.warn("Could not load skills for RiskAnalysisAgentV7: {}", e.getMessage());
            }
        }

        if (Files.exists(KNOWLEDGE_PATH)) {
            tools.add(new ProjectKnowledgeTool());
        }

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Stage 3 of Sequential Workflow: Evaluates market beta, price volatility, leverage hazards, and operational risks.")
                .model(model)
                .instruction(INSTRUCTION)
                .outputKey(OUTPUT_KEY)
                .tools(tools)
                .beforeAgentCallbackSync(beforeAgentCallback)
                .afterAgentCallbackSync(afterAgentCallback)
                .build();
    }

    private RiskAnalysisAgentV7() {}
}
