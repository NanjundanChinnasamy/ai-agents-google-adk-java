package com.google.adk.finance.v7.subagents;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v3.PortfolioMathTool;
import com.google.adk.finance.v4.ProjectKnowledgeTool;
import com.google.adk.models.BaseLlm;
import com.google.adk.skills.LocalSkillSource;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.AgentTool;
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
 * Week 4 — Version 7: Report Drafter / Reviser Sub-Agent in Loop Workflow.
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Step 1 of {@code ResearchCriticLoopWorkflowV7}.</li>
 *   <li>Drafts initial investment research report or incorporates feedback from {@code {critic_feedback?}}.</li>
 *   <li>Grounded in Yahoo Finance MCP fundamentals, Google Search developments via {@link AgentTool}, and curated knowledge.</li>
 *   <li>Publishes draft to {@link #OUTPUT_KEY} ({@code current_draft_report}).</li>
 * </ul>
 */
public final class ReportDraftingAgentV7 {
    private static final Logger logger = LoggerFactory.getLogger(ReportDraftingAgentV7.class);

    public static final String AGENT_NAME = "report_drafter";
    public static final String OUTPUT_KEY = "current_draft_report";
    public static final Path FINANCE_SKILLS_PATH = Path.of("skills", "finance");
    public static final Path KNOWLEDGE_PATH = Path.of("knowledge");

    public static final String INSTRUCTION = """
            You are report_drafter (ReportDraftingAgentV7), the Report Generator in the Iterative Research-Critic Loop.
            Your task is to draft an investment research report, or to revise an existing draft in response to critic feedback.

            Previous Critic Feedback (if revising a prior draft):
            {critic_feedback?}

            Drafting & Revision Instructions:
            1. If {critic_feedback?} is EMPTY:
               - Formulate a comprehensive initial investment report on the target company.
               - Retrieve current fundamentals using `get_stock_info`.
               - Note recent company catalysts using `company_research_step` (Google Search).
               - Identify key risk factors and valuation considerations.
               - Conclude with the mandatory regulatory disclaimer:
                 "Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice."

            2. If {critic_feedback?} is PRESENT:
               - The critic has flagged deficiencies in the previous draft!
               - Carefully inspect the critic's revision directives.
               - Update the draft specifically addressing each cited issue:
                 * If the critic asked for missing empirical citations: add exact sources/dates.
                 * If the critic requested balanced risk considerations: expand the risk section.
                 * If the critic noted a missing disclaimer: ensure the mandatory disclaimer is included verbatim.
               - Output the complete revised report.

            Output Template:
            # Investment Research Report
            - Target: <Company Name & Symbol>
            - Core Thesis: <Summary of strategic outlook>
            - Key Metrics & Valuation: <P/E, PEG, Margin, Beta with citations>
            - Recent Catalysts: <Notable business news with dates>
            - Risk & Vulnerabilities: <Balanced risk taxonomy>
            - Evidence & Sources: <Explicit citations of MCP / Search data>
            ---
            Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice.
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeCallback = context -> {
        logger.info("[Loop: Drafter] Generating or revising draft report.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterCallback = context -> {
        logger.info("[Loop: Drafter] Draft generated. Published to key: {}", OUTPUT_KEY);
        return Optional.empty();
    };

    /**
     * Creates an instance of {@link ReportDraftingAgentV7} with default model and BaseToolset.
     */
    public static LlmAgent create(BaseToolset mcpToolset) {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);
        return create(model, mcpToolset);
    }

    /**
     * Backward-compatible overload for McpToolset.
     */
    public static LlmAgent create(McpToolset mcpToolset) {
        return create((BaseToolset) mcpToolset);
    }

    /**
     * Backward-compatible overload for McpToolset with explicit model.
     */
    public static LlmAgent create(BaseLlm model, McpToolset mcpToolset) {
        return create(model, (BaseToolset) mcpToolset);
    }

    /**
     * Creates an instance of {@link ReportDraftingAgentV7} with explicit model and BaseToolset.
     */
    public static LlmAgent create(BaseLlm model, BaseToolset mcpToolset) {
        List<Object> tools = new ArrayList<>();

        if (mcpToolset != null) {
            tools.add(mcpToolset);
        }
        // Decouple Google Search via AgentTool wrapping CompanyResearchAgentV7 to comply with
        // Gemini tool exclusivity (no native search combined with client function declarations in same step)
        // and Ollama/OpenAI schema requirements.
        tools.add(AgentTool.create(CompanyResearchAgentV7.create()));
        tools.add(new PortfolioMathTool());

        if (Files.exists(FINANCE_SKILLS_PATH)) {
            try {
                SkillToolset skillToolset = new SkillToolset(new LocalSkillSource(FINANCE_SKILLS_PATH));
                tools.add(skillToolset);
            } catch (Exception e) {
                logger.warn("Could not load skills for ReportDraftingAgentV7: {}", e.getMessage());
            }
        }

        if (Files.exists(KNOWLEDGE_PATH)) {
            tools.add(new ProjectKnowledgeTool());
        }

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Report generator and reviser in the iterative critic loop.")
                .model(model)
                .instruction(INSTRUCTION)
                .outputKey(OUTPUT_KEY)
                .tools(tools)
                .beforeAgentCallbackSync(beforeCallback)
                .afterAgentCallbackSync(afterCallback)
                .build();
    }

    private ReportDraftingAgentV7() {}
}
