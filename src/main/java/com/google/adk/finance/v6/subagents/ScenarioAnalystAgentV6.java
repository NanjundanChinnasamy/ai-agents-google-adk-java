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
 * Week 3 — Version 6: Specialized Scenario & Stress-Testing Sub-Agent (scenario_analyst).
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Models forward-looking macro and equity scenarios: Baseline, Bull, and Bear cases.</li>
 *   <li>Evaluates portfolio sensitivity based on asset weights, beta ($\beta$), and sector concentrations.</li>
 *   <li>Performs hypothetical stress-testing (e.g., interest rate shocks, tech corrections, stagflation).</li>
 *   <li>Formats output following the standardized V6 Sub-Agent Output Contract.</li>
 * </ul>
 */
public final class ScenarioAnalystAgentV6 {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioAnalystAgentV6.class);

    public static final String AGENT_NAME = "scenario_analyst";
    public static final String OUTPUT_KEY = "scenario_analysis_report";
    public static final Path FINANCE_SKILLS_PATH = Path.of("skills", "finance");
    public static final Path KNOWLEDGE_PATH = Path.of("knowledge");

    public static final String INSTRUCTION = """
            You are scenario_analyst (ScenarioAnalystAgentV6), a specialized quantitative scenario modeling and stress-testing sub-agent.
            Your sole responsibility is to model forward-looking macro scenarios (Bull, Bear, Baseline) and evaluate asset and portfolio sensitivity.

            Core Scenario Modeling Framework:
            1. Baseline Case:
               - Probability-weighted consensus path given current earnings growth, operating margins, and market multiple.
            2. Bull Case (Upside Catalyst):
               - Multiple expansion, accelerating top-line growth, margin improvement, rate cuts, or favorable regulatory tailwinds.
            3. Bear Case (Downside Shock):
               - Macro contraction, sticky inflation, higher interest rates, multiple compression, or sector-specific headwinds.
            4. Portfolio Sensitivity & Stress-Testing:
               - Weight-adjusted impact based on holding weights and systematic beta ($\beta$).
               - Flagging concentration vulnerability (>25% position) during adverse market corrections.

            Tool Usage Directives:
            - Call `get_stock_info` to retrieve current valuation multiples, beta, and 52-week trading ranges.
            - Call `portfolio_math` with `operation='calculate_allocation'` or `operation='calculate_pnl'` for weighting and price change simulations.
            - Call `read_project_knowledge` for portfolio principles (`portfolio-principles`) or risk taxonomy (`risk-framework`).
            - *CRITICAL RULE*: Provide objective comparative scenario modeling, NOT personalized buy/sell instructions.

            Standardized Sub-Agent Output Contract:
            You MUST format your final response using this structured layout:

            [SUB-AGENT REPORT: ScenarioAnalystAgentV6 (scenario_analyst)]
            • Specialist: ScenarioAnalystAgentV6 (scenario_analyst)
            • Task: <Brief summary of scenario modeling task>
            • Key Findings:
              - Baseline Case: <Consensus expectations, baseline return range>
              - Bull Case Scenario: <Upside catalysts, target multiple expansion, simulated upside %>
              - Bear Case Scenario: <Downside shock, multiple compression, simulated drawdown %>
              - Portfolio Sensitivity: <Beta exposure, concentration vulnerability>
            • Data Sources & Capabilities: Yahoo Finance MCP (Beta & Multiples) + PortfolioMathTool + Risk Principles
            • Limitations & Scope: Forward-looking hypothetical stress-testing; does NOT guarantee future market outcomes.
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeAgentCallback = context -> {
        logger.info("[Lifecycle] ScenarioAnalystAgentV6 (scenario_analyst) started processing scenario task.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> {
        logger.info("[Lifecycle] ScenarioAnalystAgentV6 (scenario_analyst) completed scenario task.");
        return Optional.empty();
    };

    private static final Callbacks.BeforeToolCallbackSync beforeToolCallback = (invocationContext, tool, args, toolContext) -> {
        logger.info("[Tool Usage] ScenarioAnalystAgentV6 executing tool [{}] with args: {}", tool.name(), args);
        return Optional.empty();
    };

    /**
     * Creates an instance of {@link ScenarioAnalystAgentV6} wired with the given model and McpToolset.
     */
    public static LlmAgent create(BaseLlm model, McpToolset mcpToolset) {
        List<Object> toolsAndToolsets = new ArrayList<>();

        // 1. Yahoo Finance MCP Toolset (for beta, valuation multiples, 52-week range)
        if (mcpToolset != null) {
            toolsAndToolsets.add(mcpToolset);
            logger.info("Attached McpToolset to ScenarioAnalystAgentV6.");
        } else {
            logger.warn("McpToolset is null. ScenarioAnalystAgentV6 will run without MCP tools.");
        }

        // 2. Project Grounding Knowledge Tool (risk-framework, portfolio-principles)
        toolsAndToolsets.add(new ProjectKnowledgeTool(KNOWLEDGE_PATH));

        // 3. Deterministic Portfolio Math Tool (allocation and simulation math)
        toolsAndToolsets.add(new PortfolioMathTool());

        // 4. Domain Skills Toolset
        if (Files.exists(FINANCE_SKILLS_PATH) && Files.isDirectory(FINANCE_SKILLS_PATH)) {
            try {
                SkillToolset skillToolset = new SkillToolset(new LocalSkillSource(FINANCE_SKILLS_PATH));
                toolsAndToolsets.add(skillToolset);
            } catch (Exception e) {
                logger.warn("Could not load skills for ScenarioAnalystAgentV6: {}", e.getMessage());
            }
        }

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Specialist sub-agent modeling forward-looking macro scenarios (Bull, Bear, Baseline) and stress-testing portfolio asset allocations using Yahoo Finance MCP, deterministic math, and risk frameworks.")
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
     * Creates an instance of {@link ScenarioAnalystAgentV6} using the configured model.
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

    private ScenarioAnalystAgentV6() {}
}
