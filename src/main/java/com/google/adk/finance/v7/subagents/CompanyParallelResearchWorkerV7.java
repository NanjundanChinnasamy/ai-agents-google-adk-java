package com.google.adk.finance.v7.subagents;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.adk.finance.tools.ProjectKnowledgeTool;
import com.google.adk.models.BaseLlm;
import com.google.adk.skills.LocalSkillSource;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.BaseToolset;
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
 * Week 4 — Version 7: Specialized Worker for Parallel Fan-Out Company Research.
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Runs concurrently as an independent parallel task for a specific company symbol.</li>
 *   <li>Combines:
 *     <ol>
 *       <li>Current company research & business profile</li>
 *       <li>Fundamental data retrieval via Yahoo Finance MCP (P/E, margins, debt, revenue)</li>
 *       <li>Systematic risk identification (Beta, volatility spread)</li>
 *     </ol>
 *   </li>
 *   <li>Publishes findings under a company-specific key {@code parallel_research_<symbol>}.</li>
 * </ul>
 */
public final class CompanyParallelResearchWorkerV7 {
    private static final Logger logger = LoggerFactory.getLogger(CompanyParallelResearchWorkerV7.class);

    public static final Path FINANCE_SKILLS_PATH = Path.of("skills", "finance");
    public static final Path KNOWLEDGE_PATH = Path.of("knowledge");

    public static String getAgentName(String symbol) {
        return "parallel_worker_" + normalizeKey(symbol);
    }

    public static String getOutputKey(String symbol) {
        return "parallel_research_" + normalizeKey(symbol);
    }

    public static String normalizeKey(String symbol) {
        return symbol.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    /**
     * Creates an independent parallel worker agent tailored for the specified company symbol.
     */
    public static LlmAgent create(String companySymbol, BaseToolset mcpToolset) {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);
        return create(companySymbol, model, mcpToolset);
    }

    /**
     * Backward-compatible overload for McpToolset.
     */
    public static LlmAgent create(String companySymbol, McpToolset mcpToolset) {
        return create(companySymbol, (BaseToolset) mcpToolset);
    }

    /**
     * Backward-compatible overload for McpToolset with explicit model.
     */
    public static LlmAgent create(String companySymbol, BaseLlm model, McpToolset mcpToolset) {
        return create(companySymbol, model, (BaseToolset) mcpToolset);
    }

    /**
     * Creates an independent parallel worker agent tailored for the specified company symbol with explicit model.
     */
    public static LlmAgent create(String companySymbol, BaseLlm model, BaseToolset mcpToolset) {
        String agentName = getAgentName(companySymbol);
        String outputKey = getOutputKey(companySymbol);

        String instruction = """
                You are %s (CompanyParallelResearchWorkerV7), an independent parallel research worker assigned exclusively to %s.
                Your task is to conduct an integrated company investigation combining market profile, fundamentals, and risk.

                Assigned Company: %s

                Investigation Directives:
                1. Market & Business Overview: Core industry, business model, and operational focus.
                2. Fundamental Profile: Call `get_stock_info` to retrieve Trailing P/E, Forward P/E, PEG, Operating Margin, Profit Margin, Revenue, and Debt/Equity.
                   - For company names or multi-word entities, resolve to the appropriate market ticker symbol when calling tools (e.g. "Infosys" -> "INFY" or "INFY.NS", "HDFC Bank" -> "HDB" or "HDFCBANK.NS", "Reliance" -> "RELIANCE.NS").
                3. Risk Profile: Retrieve Beta, 52-week high, and 52-week low.
                4. Failure Protocol: If data cannot be retrieved or the ticker is invalid, state the error explicitly. NEVER hallucinate missing numbers.

                Output Format:
                Format your output strictly using this structured template:
                [PARALLEL RESEARCH REPORT: %s]
                • Target Symbol / Company: %s
                • Business Profile: <Industry and primary activities>
                • Key Fundamentals:
                  - Valuation: Trailing P/E: <value>, Forward P/E: <value>, PEG: <value>
                  - Profitability: Operating Margin: <value>, Net Margin: <value>
                  - Balance Sheet: Cash & Debt profile
                • Risk Metrics:
                  - Beta: <value>
                  - 52-Week Range: <Low - High>
                • Data Status: SUCCESS
                """.formatted(agentName, companySymbol, companySymbol, companySymbol, companySymbol);

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
                logger.warn("Could not load skills for {}: {}", agentName, e.getMessage());
            }
        }

        if (Files.exists(KNOWLEDGE_PATH)) {
            tools.add(new ProjectKnowledgeTool());
        }

        Callbacks.BeforeAgentCallbackSync beforeCallback = context -> {
            logger.info("[PARALLEL] Starting {} research", companySymbol);
            return Optional.empty();
        };

        Callbacks.AfterAgentCallbackSync afterCallback = context -> {
            logger.info("[PARALLEL] {} research completed", companySymbol);
            return Optional.empty();
        };

        return LlmAgent.builder()
                .name(agentName)
                .description("Parallel research worker for " + companySymbol + ": gathers fundamentals, valuation, and risk metrics concurrently.")
                .model(model)
                .instruction(instruction)
                .outputKey(outputKey)
                .tools(tools)
                .beforeAgentCallbackSync(beforeCallback)
                .afterAgentCallbackSync(afterCallback)
                .build();
    }

    private CompanyParallelResearchWorkerV7() {}
}
