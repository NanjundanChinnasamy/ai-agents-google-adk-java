package com.google.adk.finance.v6.subagents;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v3.PortfolioMathTool;
import com.google.adk.finance.v4.ProjectKnowledgeTool;
import com.google.adk.finance.v5.YahooFinanceMcpClientManager;
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
 * Week 3 — Version 6: Specialized Portfolio & Investment Risk Sub-Agent.
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Analyzes investment risk, volatility, concentration, and portfolio-level diversification.</li>
 *   <li>Consumes Yahoo Finance MCP tools for beta, 52-week price range spread, and market capitalization.</li>
 *   <li>Applies curated risk management frameworks and portfolio construction principles.</li>
 *   <li>Evaluates concentration risk using deterministic calculations via {@link PortfolioMathTool}.</li>
 *   <li>Provides risk analysis and considerations, NOT personalized buy/sell instructions.</li>
 *   <li>Formats output following the standardized V6 Sub-Agent Output Contract.</li>
 * </ul>
 */
public final class PortfolioRiskAgentV6 {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioRiskAgentV6.class);

    public static final String AGENT_NAME = "portfolio_risk_agent";
    public static final String OUTPUT_KEY = "portfolio_risk_report";
    public static final Path FINANCE_SKILLS_PATH = Path.of("skills", "finance");
    public static final Path KNOWLEDGE_PATH = Path.of("knowledge");

    public static final String INSTRUCTION = """
            You are portfolio_risk_agent (PortfolioRiskAgentV6), a specialized portfolio risk and investment hazard sub-agent.
            Your sole responsibility is to evaluate structural, statistical, and portfolio-level risks using Yahoo Finance MCP tools, deterministic math, and curated risk frameworks.

            Core Risk Evaluation Pillars:
            1. Systematic Market Sensitivity (Beta & Macro Exposure):
               - Beta (β): Measure sensitivity to market swings (β > 1: amplified volatility; β < 1: defensive).
               - Macroeconomic sensitivity: Interest rate risk, inflation vulnerability, currency headwinds.
            2. Volatility & Price Drawdown:
               - 52-week high vs low trading spread, standard deviation, and peak-to-trough drawdown potential.
            3. Balance Sheet & Solvency Hazards:
               - Excessive leverage, debt maturities, interest coverage ratio, liquidity buffers.
            4. Concentration & Diversification (Portfolio Principles):
               - Concentration Risk: Flag any position exceeding 25% of total portfolio value.
               - Correlation (ρ): Diversification benefit requires assets with low or negative correlation.
            5. Operational & Thesis Risks:
               - Regulatory scrutiny, customer concentration, competitive disruption, execution risk.

            Tool Usage Directives:
            - Call `get_stock_info` to retrieve beta, 52-week high/low, and market capitalization.
            - Call `read_project_knowledge` for risk taxonomies (topic='risk-framework') or portfolio construction (topic='portfolio-principles').
            - For portfolio allocation percentages or concentration flags, invoke `portfolio_math` with `operation='calculate_allocation'`.
            - *CRITICAL RULE*: Provide objective risk analysis and considerations, NOT personalized buy/sell commands.

            Standardized Sub-Agent Output Contract:
            You MUST format your final response using this structured layout:

            [SUB-AGENT REPORT: PortfolioRiskAgentV6]
            • Specialist: PortfolioRiskAgentV6
            • Task: <Brief summary of risk assessment>
            • Key Findings:
              - Volatility & Market Sensitivity: <Beta, 52-week price range spread, market sensitivity>
              - Financial & Solvency Risks: <Debt load, liquidity cushions, leverage vulnerabilities>
              - Concentration & Portfolio Considerations: <Allocation weights, correlation, diversification factors>
              - Operational & Sector Risks: <Industry headwinds, competitive threats, thesis risks>
            • Data Sources & Capabilities: Yahoo Finance MCP (Beta & Trading ranges) & Curated Risk Framework
            • Limitations & Scope: Evaluates structural and statistical risks; does NOT guarantee downside protection or predict exact market timing.
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeAgentCallback = context -> {
        logger.info("[Lifecycle] PortfolioRiskAgentV6 sub-agent started processing risk analysis.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> {
        logger.info("[Lifecycle] PortfolioRiskAgentV6 sub-agent completed risk analysis.");
        return Optional.empty();
    };

    private static final Callbacks.BeforeToolCallbackSync beforeToolCallback = (invocationContext, tool, args, toolContext) -> {
        logger.info("[Tool Usage] PortfolioRiskAgentV6 executing tool [{}] with args: {}", tool.name(), args);
        return Optional.empty();
    };

    /**
     * Creates an instance of {@link PortfolioRiskAgentV6} wired with the given model and McpToolset.
     */
    public static LlmAgent create(BaseLlm model, McpToolset mcpToolset) {
        List<Object> toolsAndToolsets = new ArrayList<>();

        // 1. Yahoo Finance MCP Toolset (for beta, trading range, valuation flags)
        if (mcpToolset != null) {
            toolsAndToolsets.add(mcpToolset);
            logger.info("Attached McpToolset to PortfolioRiskAgentV6.");
        } else {
            logger.warn("McpToolset is null. PortfolioRiskAgentV6 will run without MCP tools.");
        }

        // 2. Project Grounding Knowledge Tool (risk-framework, portfolio-principles)
        toolsAndToolsets.add(new ProjectKnowledgeTool(KNOWLEDGE_PATH));

        // 3. Deterministic Portfolio Math Tool (concentration & allocation math)
        toolsAndToolsets.add(new PortfolioMathTool());

        // 4. Domain Skills Toolset
        if (Files.exists(FINANCE_SKILLS_PATH) && Files.isDirectory(FINANCE_SKILLS_PATH)) {
            try {
                SkillToolset skillToolset = new SkillToolset(new LocalSkillSource(FINANCE_SKILLS_PATH));
                toolsAndToolsets.add(skillToolset);
            } catch (Exception e) {
                logger.warn("Could not load skills for PortfolioRiskAgentV6: {}", e.getMessage());
            }
        }

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Specialist sub-agent evaluating investment risks, market sensitivity (beta), volatility, capital structure leverage, concentration risk, and portfolio diversification considerations using Yahoo Finance MCP and curated risk frameworks.")
                .model(model)
                .instruction(INSTRUCTION)
                .tools(toolsAndToolsets)
                .outputKey(OUTPUT_KEY)
                .beforeAgentCallbackSync(beforeAgentCallback)
                .afterAgentCallbackSync(afterAgentCallback)
                .beforeToolCallbackSync(beforeToolCallback)
                .build();
    }

    /**
     * Creates an instance of {@link PortfolioRiskAgentV6} using the default configured model.
     */
    public static LlmAgent create(McpToolset mcpToolset) {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        return create(AppConfig.createModel(modelName), mcpToolset);
    }

    /**
     * Convenience factory method starting a temporary McpClientManager if needed.
     */
    public static LlmAgent create() {
        YahooFinanceMcpClientManager clientManager = new YahooFinanceMcpClientManager();
        clientManager.start();
        return create(clientManager.getMcpToolset());
    }

    private PortfolioRiskAgentV6() {}
}
