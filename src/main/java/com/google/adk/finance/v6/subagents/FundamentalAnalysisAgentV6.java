package com.google.adk.finance.v6.subagents;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.tools.ProjectKnowledgeTool;
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
 * Week 3 — Version 6: Specialized Fundamental Analysis Sub-Agent.
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Analyzes company fundamentals, financial statements, valuation multiples, and operating metrics.</li>
 *   <li>Consumes Yahoo Finance MCP tools ({@code get_stock_info}, {@code get_financial_statement}, {@code get_stock_actions}, {@code get_recommendations}).</li>
 *   <li>Applies curated valuation principles and fundamental analysis domain skills.</li>
 *   <li>Formats output following the standardized V6 Sub-Agent Output Contract.</li>
 * </ul>
 */
public final class FundamentalAnalysisAgentV6 {
    private static final Logger logger = LoggerFactory.getLogger(FundamentalAnalysisAgentV6.class);

    public static final String AGENT_NAME = "fundamental_analysis_agent";
    public static final String OUTPUT_KEY = "fundamental_analysis_report";
    public static final Path FINANCE_SKILLS_PATH = Path.of("skills", "finance");
    public static final Path KNOWLEDGE_PATH = Path.of("knowledge");

    public static final String INSTRUCTION = """
            You are fundamental_analysis_agent (FundamentalAnalysisAgentV6), a specialized equity fundamental analysis sub-agent.
            Your sole responsibility is to analyze structured company fundamentals using Yahoo Finance MCP tools and curated valuation frameworks.

            Core Analytical Focus:
            1. Valuation Multiples: P/E (trailing & forward), PEG ratio, Price-to-Book (P/B), EV/EBITDA, and enterprise value.
               *Grounding Principle*: A high P/E does not automatically mean overvalued; evaluate valuation relative to earnings growth (PEG) and industry norms.
            2. Profitability & Margins: Gross margin, operating margin, net profit margin, Return on Equity (ROE), and Return on Capital Employed (ROCE).
            3. Financial Health & Solvency: Balance sheet liquidity, cash vs debt position, debt-to-equity ratio, and free cash flow generation.
            4. Historical Statements: Balance sheet, income statement, and cash flow trends via `get_financial_statement`.
            5. Corporate Actions & Capital Allocation: Historical dividends, dividend yield, and stock splits via `get_stock_actions`.
            6. Wall Street Consensus: Mean analyst recommendations and target price via `get_recommendations`.

            Tool Usage Directives:
            - ALWAYS call `get_stock_info` for valuation multiples, margins, and key statistics.
            - Call `get_financial_statement` when the user requests balance sheet, income statement, or cash flow data.
            - Call `get_stock_actions` for dividend payout history or stock splits.
            - Call `get_recommendations` for analyst targets and consensus.
            - Use `read_project_knowledge` for valuation principles (e.g. topic='valuation-principles' or 'fundamental-analysis').

            Standardized Sub-Agent Output Contract:
            You MUST format your final response using this structured layout:

            [SUB-AGENT REPORT: FundamentalAnalysisAgentV6]
            • Specialist: FundamentalAnalysisAgentV6
            • Task: <Brief summary of fundamentals investigated>
            • Key Findings:
              - Valuation Multiples: <P/E, PEG, P/B, EV/EBITDA metrics>
              - Operating & Profitability: <Margins, ROE, ROCE, revenue trend>
              - Balance Sheet & Solvency: <Cash, debt, liquidity, debt/equity>
              - Capital Allocation & Consensus: <Dividends, analyst target price>
            • Data Sources & Capabilities: Yahoo Finance MCP Server (get_stock_info, get_financial_statement, get_stock_actions, get_recommendations) & Curated Valuation Frameworks
            • Limitations & Scope: Based on reported financial filings and structured market quotes; does NOT track breaking live news or intraday sentiment.
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeAgentCallback = context -> {
        logger.info("[Lifecycle] FundamentalAnalysisAgentV6 sub-agent started processing task.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> {
        logger.info("[Lifecycle] FundamentalAnalysisAgentV6 sub-agent completed task.");
        return Optional.empty();
    };

    private static final Callbacks.BeforeToolCallbackSync beforeToolCallback = (invocationContext, tool, args, toolContext) -> {
        logger.info("[Tool Usage] FundamentalAnalysisAgentV6 executing tool [{}] with args: {}", tool.name(), args);
        return Optional.empty();
    };

    /**
     * Creates an instance of {@link FundamentalAnalysisAgentV6} wired with the given model and McpToolset.
     */
    public static LlmAgent create(BaseLlm model, McpToolset mcpToolset) {
        List<Object> toolsAndToolsets = new ArrayList<>();

        // 1. Yahoo Finance MCP Toolset
        if (mcpToolset != null) {
            toolsAndToolsets.add(mcpToolset);
            logger.info("Attached McpToolset to FundamentalAnalysisAgentV6.");
        } else {
            logger.warn("McpToolset is null. FundamentalAnalysisAgentV6 will run without MCP tools.");
        }

        // 2. Project Grounding Knowledge Tool
        toolsAndToolsets.add(new ProjectKnowledgeTool(KNOWLEDGE_PATH));

        // 3. Domain Skills Toolset
        if (Files.exists(FINANCE_SKILLS_PATH) && Files.isDirectory(FINANCE_SKILLS_PATH)) {
            try {
                SkillToolset skillToolset = new SkillToolset(new LocalSkillSource(FINANCE_SKILLS_PATH));
                toolsAndToolsets.add(skillToolset);
            } catch (Exception e) {
                logger.warn("Could not load skills for FundamentalAnalysisAgentV6: {}", e.getMessage());
            }
        }

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Specialist sub-agent analyzing company fundamentals, financial statements, valuation multiples, profitability margins, debt health, and Wall Street analyst targets using Yahoo Finance MCP and curated valuation knowledge.")
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
     * Creates an instance of {@link FundamentalAnalysisAgentV6} using the default configured model.
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

    private FundamentalAnalysisAgentV6() {}
}
