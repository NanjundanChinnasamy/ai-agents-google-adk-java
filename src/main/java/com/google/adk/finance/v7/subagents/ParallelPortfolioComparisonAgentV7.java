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
 * Week 4 — Version 7: Fan-In Aggregator Sub-Agent for Parallel Portfolio Research.
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Stage 2 (Fan-In Aggregator) of {@code PortfolioParallelResearchWorkflowV7}.</li>
 *   <li>Gathers outputs from all concurrently executed {@link CompanyParallelResearchWorkerV7} tasks.</li>
 *   <li><b>Partial Failure Resilience</b>: If one or more company tasks fail (e.g. invalid ticker, network error),
 *       the comparison agent still produces a comparative report, explicitly listing:
 *       <ol>
 *         <li>Successful analyses</li>
 *         <li>Failed analyses</li>
 *         <li>Missing information & data gaps</li>
 *         <li>Scope limitations</li>
 *       </ol>
 *   </li>
 *   <li><b>Strict Rule</b>: Never fabricates or estimates missing company data.</li>
 *   <li>Concludes with the mandatory regulatory disclaimer.</li>
 * </ul>
 */
public final class ParallelPortfolioComparisonAgentV7 {
    private static final Logger logger = LoggerFactory.getLogger(ParallelPortfolioComparisonAgentV7.class);

    public static final String AGENT_NAME = "parallel_portfolio_comparator";
    public static final String OUTPUT_KEY = "portfolio_comparison_report";
    public static final Path FINANCE_SKILLS_PATH = Path.of("skills", "finance");
    public static final Path KNOWLEDGE_PATH = Path.of("knowledge");

    public static final String INSTRUCTION = """
            You are parallel_portfolio_comparator (ParallelPortfolioComparisonAgentV7), the Fan-In Aggregation Specialist of the Parallel Research Workflow.
            Your task is to synthesize the concurrently retrieved company findings into a structured comparative decision-support brief.

            Parallel Inputs Received:
            {parallel_findings?}

            Core Synthesis Requirements:
            1. Status & Failure Attribution:
               - Identify which company analyses SUCCEEDED.
               - Explicitly identify which company analyses FAILED or encountered errors.
               - Detail exactly what information is MISSING due to partial failures.
               - NEVER fabricate or invent data for missing or failed companies.
            2. Cross-Company Comparative Matrix:
               - Compare the successful companies on Valuation (Trailing P/E, Forward P/E, PEG).
               - Compare Operating and Net Profit Margins.
               - Compare Systematic Risk (Beta β) and Volatility (52-week spread).
            3. Portfolio Allocation & Diversification Insights:
               - Highlight complementary characteristics (e.g., defensive low-beta vs growth high-beta).
            4. Scope Limitations & Data Warnings.
            5. Conclude with the mandatory regulatory disclaimer.

            Required Document Structure:
            # Comparative Portfolio Research Summary (Parallel Fan-In)

            ## 1. Execution & Data Integrity Audit
            - **Successful Analyses**: <List of successfully analyzed symbols>
            - **Failed Analyses**: <List of failed or unavailable symbols with reason, or 'None'>
            - **Missing Information**: <Explicit statement of omitted metrics>

            ## 2. Multi-Company Comparison Matrix
            | Company / Ticker | Trailing P/E | Forward P/E | Operating Margin | Beta (β) | Risk Classification |
            |---|---|---|---|---|---|
            | <Symbol 1> | ... | ... | ... | ... | ... |
            | <Symbol 2> | ... | ... | ... | ... | ... |

            ## 3. Relative Valuation & Fundamental Insights
            <Comparative narrative evaluating relative valuation, earnings quality, and operational efficiency across the evaluated companies>

            ## 4. Risk & Volatility Comparison
            <Comparative discussion of market sensitivity, drawdown exposure, and balance sheet leverage>

            ## 5. Limitations & Analytic Constraints
            <Explicit constraints including partial failure impacts and point-in-time quote dependencies>

            ---
            Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice.
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeCallback = context -> {
        logger.info("[Fan-In Aggregator] Starting comparative synthesis of parallel research findings.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterCallback = context -> {
        logger.info("[Fan-In Aggregator] Comparative synthesis complete. Published to key: {}", OUTPUT_KEY);
        return Optional.empty();
    };

    /**
     * Creates an instance of {@link ParallelPortfolioComparisonAgentV7} with default model.
     */
    public static LlmAgent create() {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);
        return create(model);
    }

    /**
     * Creates an instance of {@link ParallelPortfolioComparisonAgentV7} with the given model.
     */
    public static LlmAgent create(BaseLlm model) {
        List<Object> tools = new ArrayList<>();

        if (Files.exists(FINANCE_SKILLS_PATH)) {
            try {
                SkillToolset skillToolset = new SkillToolset(new LocalSkillSource(FINANCE_SKILLS_PATH));
                tools.add(skillToolset);
            } catch (Exception e) {
                logger.warn("Could not load skills for ParallelPortfolioComparisonAgentV7: {}", e.getMessage());
            }
        }

        if (Files.exists(KNOWLEDGE_PATH)) {
            tools.add(new ProjectKnowledgeTool());
        }

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Fan-In Aggregator: Compiles parallel company findings into a structured cross-company comparison matrix with explicit failure handling.")
                .model(model)
                .instruction(INSTRUCTION)
                .outputKey(OUTPUT_KEY)
                .tools(tools)
                .beforeAgentCallbackSync(beforeCallback)
                .afterAgentCallbackSync(afterCallback)
                .build();
    }

    private ParallelPortfolioComparisonAgentV7() {}
}
