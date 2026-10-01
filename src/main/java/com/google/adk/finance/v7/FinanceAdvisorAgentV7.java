package com.google.adk.finance.v7;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.tools.LoadCustomerPortfolioTool;
import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.adk.finance.tools.ProjectKnowledgeTool;
import com.google.adk.finance.v5.YahooFinanceMcpClientManager;
import com.google.adk.finance.v7.tools.RunCriticLoopWorkflowTool;
import com.google.adk.finance.v7.tools.RunParallelWorkflowTool;
import com.google.adk.finance.v7.tools.RunSequentialWorkflowTool;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.mcp.McpToolset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Week 4 — Version 7: Root Orchestrator Agent (Workflow Director / Finance Advisor V7).
 * <p>
 * Core Architectural Concept:
 * <ul>
 *   <li><b>From Dynamic Sub-Agent Delegation (V6) to Deterministic Workflow Orchestration (V7)</b>:
 *       In V6, the parent LLM dynamically decided which specialist to call turn-by-turn.
 *       In V7, complex multi-step processes are governed by rigid deterministic workflows:
 *       <ol>
 *         <li><b>Sequential Workflow</b> ({@link RunSequentialWorkflowTool}):
 *             Fixed 5-stage dependency chain: Company Research -> Fundamentals -> Risk -> Valuation -> Synthesis.</li>
 *         <li><b>Parallel Workflow</b> ({@link RunParallelWorkflowTool}):
 *             Concurrent fan-out execution across multiple companies with fan-in comparison synthesis and partial failure resilience.</li>
 *         <li><b>Loop / Iterative Workflow</b> ({@link RunCriticLoopWorkflowTool}):
 *             Iterative drafter <-> critic loop with stopping condition enforced via {@link com.google.adk.tools.ExitLoopTool}.</li>
 *       </ol>
 *   </li>
 * </ul>
 */
public final class FinanceAdvisorAgentV7 {
    private static final Logger logger = LoggerFactory.getLogger(FinanceAdvisorAgentV7.class);

    public static final String AGENT_NAME = "finance_advisor_v7";
    public static final String ROLE_NAME = "workflow_director";

    public static final String INSTRUCTION = """
            You are workflow_director (Finance Advisor v7), an institutional-grade investment research orchestrator.
            Your role is WORKFLOW SELECTION, DETERMINISTIC PIPELINE ORCHESTRATION, and USER SYNTHESIS.

            Unlike prior versions that guessed ad-hoc specialist calls, you route complex financial tasks to three deterministic workflow engines:

            Workflow Engine 1: SEQUENTIAL RESEARCH PIPELINE (`run_sequential_research_workflow`)
            - Architecture: Linear 5-Stage Dependency Chain:
              Stage 1 (Company Research) -> Stage 2 (Fundamentals) -> Stage 3 (Risk) -> Stage 4 (Valuation) -> Stage 5 (Synthesis)
            - When to invoke: Single-company comprehensive research inquiries (e.g. "Prepare a structured investment research report on Infosys").
            - Characteristics: Rigid dependencies; output of each stage feeds the next stage.

            Workflow Engine 2: PARALLEL PORTFOLIO FAN-OUT / FAN-IN (`run_parallel_portfolio_research_workflow`)
            - Architecture: Concurrent Fan-Out across N companies -> Fan-In Comparison Matrix Aggregator.
            - When to invoke: Multi-company portfolio comparative inquiries (e.g. "Analyse Infosys, HDFC Bank and Reliance and give me a comparable research summary").
            - Characteristics: Independent concurrent execution; partial failure resilience (does NOT crash if one ticker fails); never fabricates missing data.

            Workflow Engine 3: ITERATIVE CRITIC LOOP (`run_critic_loop_research_workflow`)
            - Architecture: Generator (Report Drafter) <-> Critic (Compliance / Evidence Auditor) loop bounded by max 3 iterations.
            - When to invoke: Inquiries requesting verified, evidence-grounded reports with strict compliance (e.g. "Create an investment research report on Infosys and ensure important factual claims are supported by evidence").
            - Characteristics: Critic halts the loop via `exit_loop` only when empirical evidence and regulatory standards pass audit.

            General Directives:
            - If customer ID is provided, call `load_customer_portfolio` to ingest holdings.
            - For exact arithmetic, call `portfolio_math`.
            - Conclude all syntheses with the mandatory regulatory disclaimer:
              "Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice."
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeAgentCallback = context -> {
        logger.info("[Parent Agent Lifecycle] Workflow Director (FinanceAdvisorAgentV7) started processing request.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> {
        logger.info("[Parent Agent Lifecycle] Workflow Director (FinanceAdvisorAgentV7) completed response synthesis.");
        return Optional.empty();
    };

    /**
     * Creates an instance of the parent {@link FinanceAdvisorAgentV7} wired with deterministic workflow tools.
     *
     * @param mcpToolset the active McpToolset for Yahoo Finance tools
     * @return the configured {@link LlmAgent}
     */
    public static LlmAgent createFinanceAdvisorAgentV7(McpToolset mcpToolset) {
        String rootModelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm rootModel = AppConfig.createModel(rootModelName);
        logger.info("Initializing {} with model: {}", AGENT_NAME, rootModel.model());

        List<Object> tools = new ArrayList<>();

        // 1. Workflow Tool 1: Sequential Pipeline
        tools.add(new RunSequentialWorkflowTool(mcpToolset));

        // 2. Workflow Tool 2: Parallel Portfolio Research
        tools.add(new RunParallelWorkflowTool(mcpToolset));

        // 3. Workflow Tool 3: Iterative Critic Loop
        tools.add(new RunCriticLoopWorkflowTool(mcpToolset));

        // 4. Persistence & Math Tools
        tools.add(new LoadCustomerPortfolioTool());
        tools.add(new PortfolioMathTool());
        tools.add(new ProjectKnowledgeTool());

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Workflow Director - Deterministic workflow orchestrator controlling Sequential, Parallel, and Loop research workflows.")
                .model(rootModel)
                .instruction(INSTRUCTION)
                .tools(tools)
                .beforeAgentCallbackSync(beforeAgentCallback)
                .afterAgentCallbackSync(afterAgentCallback)
                .build();
    }

    /**
     * Convenience factory method that starts YahooFinanceMcpClientManager and creates the agent.
     */
    public static LlmAgent createFinanceAdvisorAgentV7() {
        YahooFinanceMcpClientManager clientManager = new YahooFinanceMcpClientManager();
        clientManager.start();
        return createFinanceAdvisorAgentV7(clientManager.getMcpToolset());
    }

    /**
     * Parses a company list string into individual entity names or tickers, delegating to
     * {@link RunParallelWorkflowTool#parseCompanyList(String)}.
     */
    public static List<String> parseCompanyList(String input) {
        return RunParallelWorkflowTool.parseCompanyList(input);
    }

    private FinanceAdvisorAgentV7() {}
}
