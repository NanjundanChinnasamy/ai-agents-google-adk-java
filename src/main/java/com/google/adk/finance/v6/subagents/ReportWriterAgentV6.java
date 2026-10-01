package com.google.adk.finance.v6.subagents;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v4.ProjectKnowledgeTool;
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
 * Week 3 — Version 6: Specialized Institutional Report Writer Sub-Agent (report_writer).
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Synthesizes multi-agent research notes, fundamentals, and scenario models into an executive decision-support report.</li>
 *   <li>Structures findings into an institutional-grade report format with thesis, evidence matrix, scenario table, and risk flags.</li>
 *   <li>Adheres strictly to objective analytical standards and mandatory compliance disclaimers.</li>
 *   <li>Formats output following the standardized V6 Sub-Agent Output Contract.</li>
 * </ul>
 */
public final class ReportWriterAgentV6 {
    private static final Logger logger = LoggerFactory.getLogger(ReportWriterAgentV6.class);

    public static final String AGENT_NAME = "report_writer";
    public static final String OUTPUT_KEY = "executive_decision_report";
    public static final Path FINANCE_SKILLS_PATH = Path.of("skills", "finance");
    public static final Path KNOWLEDGE_PATH = Path.of("knowledge");

    public static final String INSTRUCTION = """
            You are report_writer (ReportWriterAgentV6), a specialized institutional financial report synthesis sub-agent.
            Your sole responsibility is to synthesize disparate specialist research notes, quantitative fundamentals, and scenario stress-tests into an institutional-grade decision-support report.

            Report Structure Framework:
            1. Executive Summary & Thesis:
               - Direct, high-level summary and balanced strategic thesis.
            2. Empirical Evidence Matrix:
               - Triangulated empirical facts: breaking news developments, quarterly earnings surprise, operating margins, and balance sheet solvency.
            3. Comparative Scenario Outcomes:
               - Summary table comparing Baseline, Bull, and Bear cases with key upside drivers and downside risks.
            4. Risk Taxonomy & Trade-Offs:
               - Systematic market beta, concentration flags, leverage vulnerabilities, and thesis risks.
            5. Rebalancing & Portfolio Considerations:
               - Strategic asset allocation trade-offs without issuing personalized buy/sell commands.
            6. Mandatory Regulatory Disclaimer:
               - Must conclude with the exact text:
                 "Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice."

            Standardized Sub-Agent Output Contract:
            You MUST format your final response using this structured layout:

            [SUB-AGENT REPORT: ReportWriterAgentV6 (report_writer)]
            • Specialist: ReportWriterAgentV6 (report_writer)
            • Task: <Brief summary of synthesis objective>
            • Key Findings:
              - Executive Thesis: <Core strategic thesis>
              - Evidence Matrix: <Key empirical facts synthesized>
              - Scenario Outcomes: <Summary of bull/bear trade-offs>
              - Core Risks: <Primary vulnerabilities identified>
            • Output Document:
              <Full institutional decision-support report following the 6-section structure above>
            • Limitations & Scope: Synthesizes provided specialist inputs; does not perform independent raw data retrieval.
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeAgentCallback = context -> {
        logger.info("[Lifecycle] ReportWriterAgentV6 (report_writer) started synthesizing decision report.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> {
        logger.info("[Lifecycle] ReportWriterAgentV6 (report_writer) completed decision report synthesis.");
        return Optional.empty();
    };

    /**
     * Creates an instance of {@link ReportWriterAgentV6} wired with the given model.
     */
    public static LlmAgent create(BaseLlm model) {
        List<Object> toolsAndToolsets = new ArrayList<>();

        // 1. Project Grounding Knowledge Tool
        toolsAndToolsets.add(new ProjectKnowledgeTool(KNOWLEDGE_PATH));

        // 2. Domain Skills Toolset
        if (Files.exists(FINANCE_SKILLS_PATH) && Files.isDirectory(FINANCE_SKILLS_PATH)) {
            try {
                SkillToolset skillToolset = new SkillToolset(new LocalSkillSource(FINANCE_SKILLS_PATH));
                toolsAndToolsets.add(skillToolset);
            } catch (Exception e) {
                logger.warn("Could not load skills for ReportWriterAgentV6: {}", e.getMessage());
            }
        }

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Specialist sub-agent synthesizing multi-agent research notes, fundamentals, and scenario stress-tests into an institutional decision-support report.")
                .model(model)
                .instruction(INSTRUCTION)
                .tools(toolsAndToolsets)
                .outputKey(OUTPUT_KEY)
                .beforeAgentCallbackSync(beforeAgentCallback)
                .afterAgentCallbackSync(afterAgentCallback)
                .build();
    }

    /**
     * Creates an instance of {@link ReportWriterAgentV6} using the configured model.
     */
    public static LlmAgent create() {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        return create(AppConfig.createModel(modelName));
    }

    private ReportWriterAgentV6() {}
}
