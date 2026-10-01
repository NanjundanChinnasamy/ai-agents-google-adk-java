package com.google.adk.finance.v7.subagents;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v4.ProjectKnowledgeTool;
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
 * Week 4 — Version 7: Stage 2 Sub-Agent for Sequential Workflow (Fundamental Analysis).
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Stage 2 of {@code InvestmentResearchSequentialWorkflowV7}.</li>
 *   <li>Ingests Stage 1 context via {@code {company_research_output?}}.</li>
 *   <li>Uses Yahoo Finance MCP tools to analyze revenue, net income, operating margins,
 *       historical statements, debt/equity solvency, and analyst consensus.</li>
 *   <li>Publishes findings under {@link #OUTPUT_KEY} ({@code fundamental_analysis_output})
 *       for consumption by Stage 3 (Risk Analysis).</li>
 * </ul>
 */
public final class FundamentalAnalysisAgentV7 {
    private static final Logger logger = LoggerFactory.getLogger(FundamentalAnalysisAgentV7.class);

    public static final String AGENT_NAME = "fundamental_analysis_step";
    public static final String OUTPUT_KEY = "fundamental_analysis_output";
    public static final Path FINANCE_SKILLS_PATH = Path.of("skills", "finance");
    public static final Path KNOWLEDGE_PATH = Path.of("knowledge");

    public static final String INSTRUCTION = """
            You are fundamental_analysis_step (FundamentalAnalysisAgentV7), Stage 2 of the deterministic Investment Research Sequential Workflow.
            Your task is to analyze company fundamentals using Yahoo Finance MCP tools and financial knowledge frameworks.

            Context from Upstream Stage 1 (Company Research):
            {company_research_output?}

            Core Analytical Focus:
            1. Revenue & Growth: Trailing twelve-month revenue, revenue growth rate, and historical income statement trends.
            2. Profitability Margins: Gross margin, operating profit margin, and net profit margin.
            3. Capital Efficiency: Return on Equity (ROE) and Return on Capital Employed (ROCE).
            4. Balance Sheet Health & Solvency: Cash vs total debt, debt-to-equity ratio, and current ratio.
            5. Capital Allocation & Analyst Consensus: Dividend yield, payout history, and Wall Street target price consensus.

            Tool Usage Directives:
            - ALWAYS call `get_stock_info` for key statistics, margins, and financial highlights.
            - Call `get_financial_statement` for balance sheet, income statement, or cash flow history.
            - Call `get_recommendations` for analyst consensus price targets.
            - Use `read_project_knowledge` for fundamental frameworks (topic='fundamental-analysis' or 'valuation-principles').

            Output Format:
            Format your output strictly using this structured template:
            [STAGE 2: FUNDAMENTAL ANALYSIS REPORT]
            • Company / Symbol: <Target Company>
            • Operating Performance:
              - Revenue & Earnings: <Revenue, EBITDA, Net Income>
              - Profitability Margins: <Gross Margin, Operating Margin, Net Margin>
              - Capital Return: <ROE %, ROCE %>
            • Balance Sheet & Liquidity:
              - Total Cash vs Total Debt: <Cash, Debt, Net Debt/Cash>
              - Solvency Ratios: <Debt/Equity ratio, Current ratio>
            • Consensus & Actions:
              - Dividend Profile: <Yield, Payout Ratio>
              - Analyst Target Price: <Mean target price vs current price>
            • Grounded Data Sources: <MCP tools used, e.g. get_stock_info, get_financial_statement>
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeAgentCallback = context -> {
        logger.info("[Sequential Stage 2] FundamentalAnalysisAgentV7 started fundamental analysis with upstream Stage 1 context.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> {
        logger.info("[Sequential Stage 2] FundamentalAnalysisAgentV7 completed analysis. Output published to key: {}", OUTPUT_KEY);
        return Optional.empty();
    };

    /**
     * Creates an instance of {@link FundamentalAnalysisAgentV7} with default model and MCP toolset.
     */
    public static LlmAgent create(McpToolset mcpToolset) {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);
        return create(model, mcpToolset);
    }

    /**
     * Creates an instance of {@link FundamentalAnalysisAgentV7} with the given model and MCP toolset.
     */
    public static LlmAgent create(BaseLlm model, McpToolset mcpToolset) {
        List<Object> tools = new ArrayList<>();

        if (mcpToolset != null) {
            tools.add(mcpToolset);
        }

        if (Files.exists(FINANCE_SKILLS_PATH)) {
            try {
                SkillToolset skillToolset = new SkillToolset(new LocalSkillSource(FINANCE_SKILLS_PATH));
                tools.add(skillToolset);
            } catch (Exception e) {
                logger.warn("Could not load skills for FundamentalAnalysisAgentV7: {}", e.getMessage());
            }
        }

        if (Files.exists(KNOWLEDGE_PATH)) {
            tools.add(new ProjectKnowledgeTool());
        }

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Stage 2 of Sequential Workflow: Evaluates revenue, margins, balance sheet solvency, and financial metrics using Yahoo Finance MCP.")
                .model(model)
                .instruction(INSTRUCTION)
                .outputKey(OUTPUT_KEY)
                .tools(tools)
                .beforeAgentCallbackSync(beforeAgentCallback)
                .afterAgentCallbackSync(afterAgentCallback)
                .build();
    }

    private FundamentalAnalysisAgentV7() {}
}
