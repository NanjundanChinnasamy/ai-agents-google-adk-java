package com.google.adk.finance.v7.subagents;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.tools.ProjectKnowledgeTool;
import com.google.adk.models.BaseLlm;
import com.google.adk.skills.LocalSkillSource;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.skills.SkillToolset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Week 4 — Version 7: Stage 5 Sub-Agent for Sequential Workflow (Report Synthesis).
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Final stage of {@code InvestmentResearchSequentialWorkflowV7}.</li>
 *   <li>Reads outputs from all 4 upstream stages:
 *       <ol>
 *         <li>Stage 1: {@code {company_research_output?}}</li>
 *         <li>Stage 2: {@code {fundamental_analysis_output?}}</li>
 *         <li>Stage 3: {@code {risk_analysis_output?}}</li>
 *         <li>Stage 4: {@code {valuation_analysis_output?}}</li>
 *       </ol>
 *   </li>
 *   <li>Synthesizes an institutional-grade, multi-pillar investment research report.</li>
 *   <li>Does NOT generate simplistic buy/sell recommendations; emphasizes analytical rigour.</li>
 *   <li>Appends mandatory regulatory compliance disclaimer.</li>
 * </ul>
 */
public final class SequentialReportSynthesisAgentV7 {
    private static final Logger logger = LoggerFactory.getLogger(SequentialReportSynthesisAgentV7.class);

    public static final String AGENT_NAME = "sequential_synthesis_step";
    public static final String OUTPUT_KEY = "investment_research_report";
    public static final Path FINANCE_SKILLS_PATH = Path.of("skills", "finance");
    public static final Path KNOWLEDGE_PATH = Path.of("knowledge");

    public static final String INSTRUCTION = """
            You are sequential_synthesis_step (SequentialReportSynthesisAgentV7), Stage 5 of the deterministic Investment Research Sequential Workflow.
            Your task is to synthesize the accumulated findings from all previous stages into an institutional-grade investment research report.

            Upstream Stage 1 Context (Company Research):
            {company_research_output?}

            Upstream Stage 2 Context (Fundamental Analysis):
            {fundamental_analysis_output?}

            Upstream Stage 3 Context (Risk Analysis):
            {risk_analysis_output?}

            Upstream Stage 4 Context (Valuation Analysis):
            {valuation_analysis_output?}

            Synthesis Directives:
            - Synthesize all four perspectives into an institutional briefing document.
            - Do NOT produce simplistic "BUY", "SELL", or "HOLD" ratings. Focus strictly on evidence, fundamentals, risk trade-offs, and valuation context.
            - Ensure every empirical assertion is grounded in the upstream reports.
            - Conclude with the mandatory regulatory disclaimer.

            Mandatory Report Structure:
            Format your final synthesis strictly with these exact 7 sections:

            # Structured Investment Research Report

            ## 1. Company Overview & Core Thesis
            <Concise summary of the company's business model, competitive moat, and core investment thesis based on the investigation>

            ## 2. Recent Developments & Catalysts
            <Synthesis of recent corporate announcements, earnings surprises, leadership shifts, and macro catalysts from Stage 1>

            ## 3. Financial Fundamentals & Solvency
            <Structured review of revenue growth, profitability margins (gross, operating, net), ROE/ROCE, and balance sheet solvency from Stage 2>

            ## 4. Investment Risks & Vulnerabilities
            <Taxonomy of systematic market sensitivity (beta), volatility spread, leverage concerns, and operational hazards from Stage 3>

            ## 5. Valuation Considerations
            <Objective analysis of trailing/forward P/E, PEG, EV/EBITDA, and growth-adjusted multiples from Stage 4. Note if metrics were unavailable>

            ## 6. Empirical Evidence & Sources
            <Attribution matrix citing Google Search news sources, Yahoo Finance MCP data points, and date references>

            ## 7. Data Limitations & Analytical Scope
            <Explicit boundaries of the research, acknowledging any missing financial figures, unconfirmed media reports, or market uncertainties>

            ---
            Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice.
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeAgentCallback = context -> {
        logger.info("[Sequential Stage 5] SequentialReportSynthesisAgentV7 started synthesizing full research report.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> {
        logger.info("[Sequential Stage 5] SequentialReportSynthesisAgentV7 completed report synthesis. Published to key: {}", OUTPUT_KEY);
        return Optional.empty();
    };

    /**
     * Creates an instance of {@link SequentialReportSynthesisAgentV7} with default model.
     */
    public static LlmAgent create() {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);
        return create(model);
    }

    /**
     * Creates an instance of {@link SequentialReportSynthesisAgentV7} with the given model.
     */
    public static LlmAgent create(BaseLlm model) {
        List<Object> tools = new ArrayList<>();

        if (Files.exists(FINANCE_SKILLS_PATH)) {
            try {
                SkillToolset skillToolset = new SkillToolset(new LocalSkillSource(FINANCE_SKILLS_PATH));
                tools.add(skillToolset);
            } catch (Exception e) {
                logger.warn("Could not load skills for SequentialReportSynthesisAgentV7: {}", e.getMessage());
            }
        }

        if (Files.exists(KNOWLEDGE_PATH)) {
            tools.add(new ProjectKnowledgeTool());
        }

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Stage 5 of Sequential Workflow: Synthesizes research, fundamentals, risk, and valuation into an institutional 7-section report.")
                .model(model)
                .instruction(INSTRUCTION)
                .outputKey(OUTPUT_KEY)
                .tools(tools)
                .beforeAgentCallbackSync(beforeAgentCallback)
                .afterAgentCallbackSync(afterAgentCallback)
                .build();
    }

    private SequentialReportSynthesisAgentV7() {}
}
