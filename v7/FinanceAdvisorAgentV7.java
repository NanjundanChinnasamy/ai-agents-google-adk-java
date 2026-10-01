package com.google.adk.finance.v7;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v2.LoadCustomerPortfolioTool;
import com.google.adk.finance.v3.PortfolioMathTool;
import com.google.adk.finance.v4.ProjectKnowledgeTool;
import com.google.adk.finance.v5.YahooFinanceMcpClientManager;
import com.google.adk.finance.v7.workflows.loop.ResearchCriticLoopWorkflowV7;
import com.google.adk.finance.v7.workflows.parallel.PortfolioParallelResearchWorkflowV7;
import com.google.adk.finance.v7.workflows.sequential.InvestmentResearchSequentialWorkflowV7;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.adk.tools.mcp.McpToolset;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 *         <li><b>Sequential Workflow</b> ({@link InvestmentResearchSequentialWorkflowV7}):
 *             Fixed 5-stage dependency chain: Company Research -> Fundamentals -> Risk -> Valuation -> Synthesis.</li>
 *         <li><b>Parallel Workflow</b> ({@link PortfolioParallelResearchWorkflowV7}):
 *             Concurrent fan-out execution across multiple companies with fan-in comparison synthesis and partial failure resilience.</li>
 *         <li><b>Loop / Iterative Workflow</b> ({@link ResearchCriticLoopWorkflowV7}):
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
     * Custom Tool: Executes the 5-Stage Sequential Research Pipeline.
     */
    public static class RunSequentialWorkflowTool extends BaseTool {
        public static final String TOOL_NAME = "run_sequential_research_workflow";
        private final McpToolset mcpToolset;

        public RunSequentialWorkflowTool(McpToolset mcpToolset) {
            super(TOOL_NAME, "Executes the deterministic 5-stage sequential investment research workflow: Company Research -> Fundamentals -> Risk -> Valuation -> Synthesis.");
            this.mcpToolset = mcpToolset;
        }

        @Override
        public Optional<FunctionDeclaration> declaration() {
            Map<String, Schema> properties = new HashMap<>();
            properties.put("company", Schema.builder()
                    .type(Type.Known.STRING)
                    .description("The target company name or stock ticker to analyze sequentially (e.g. 'INFY' or 'Infosys').")
                    .build());

            Schema parameters = Schema.builder()
                    .type(Type.Known.OBJECT)
                    .properties(properties)
                    .required(List.of("company"))
                    .build();

            return Optional.of(FunctionDeclaration.builder()
                    .name(TOOL_NAME)
                    .description("Executes the deterministic 5-stage sequential investment research workflow.")
                    .parameters(parameters)
                    .build());
        }

        @Override
        public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
            return Single.fromCallable(() -> {
                String company = String.valueOf(args.getOrDefault("company", "INFY"));
                logger.info("[Workflow Execution] Starting Sequential Workflow for: {}", company);
                InvestmentResearchSequentialWorkflowV7.SequentialWorkflowResult result =
                        InvestmentResearchSequentialWorkflowV7.executeWorkflow(company, mcpToolset);

                Map<String, Object> response = new HashMap<>();
                response.put("status", result.successful() ? "success" : "failure");
                response.put("target_company", company);
                response.put("report", result.finalReport());
                if (!result.successful()) {
                    response.put("error", result.errorMessage());
                }
                return response;
            });
        }
    }

    /**
     * Custom Tool: Executes the Parallel Fan-Out / Fan-In Research Workflow.
     */
    public static class RunParallelWorkflowTool extends BaseTool {
        public static final String TOOL_NAME = "run_parallel_portfolio_research_workflow";
        private final McpToolset mcpToolset;

        public RunParallelWorkflowTool(McpToolset mcpToolset) {
            super(TOOL_NAME, "Executes concurrent fan-out company research across multiple stocks and fan-in comparative synthesis with partial failure resilience.");
            this.mcpToolset = mcpToolset;
        }

        @Override
        public Optional<FunctionDeclaration> declaration() {
            Map<String, Schema> properties = new HashMap<>();
            properties.put("companies", Schema.builder()
                    .type(Type.Known.STRING)
                    .description("Comma-separated list of company symbols/names to analyze in parallel (e.g. 'INFY, HDFCBANK, RELIANCE').")
                    .build());

            Schema parameters = Schema.builder()
                    .type(Type.Known.OBJECT)
                    .properties(properties)
                    .required(List.of("companies"))
                    .build();

            return Optional.of(FunctionDeclaration.builder()
                    .name(TOOL_NAME)
                    .description("Executes concurrent fan-out company research and fan-in comparative synthesis.")
                    .parameters(parameters)
                    .build());
        }

        @Override
        public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
            return Single.fromCallable(() -> {
                String rawCompanies = String.valueOf(args.getOrDefault("companies", "INFY, TCS, RELIANCE"));
                List<String> symbols = parseCompanyList(rawCompanies);

                logger.info("[Workflow Execution] Starting Parallel Fan-Out Workflow for: {}", symbols);
                PortfolioParallelResearchWorkflowV7.ParallelWorkflowResult result =
                        PortfolioParallelResearchWorkflowV7.executeWorkflow(symbols, mcpToolset, null);

                Map<String, Object> response = new HashMap<>();
                response.put("status", result.hasFailures() ? "partial_success" : "success");
                response.put("successful_symbols", result.successfulSymbols());
                response.put("failed_symbols", result.failedSymbols());
                response.put("comparative_report", result.comparativeReport());
                return response;
            });
        }
    }

    /**
     * Parses a company list string into individual entity names or tickers, safely preserving
     * multi-word company names (e.g. "HDFC Bank", "State Bank of India") when delimited by commas,
     * semicolons, or words like "and" / "vs".
     */
    public static List<String> parseCompanyList(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        String cleaned = input.trim();
        // 1. Delimited by commas, semicolons, or words like "and", "vs", "versus"
        if (cleaned.contains(",") || cleaned.contains(";") || cleaned.toLowerCase().matches(".*\\b(and|vs|versus)\\b.*")) {
            return Arrays.stream(cleaned.split("\\s*(?:,|;|\\b(?:and|vs|versus)\\b)\\s*"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        // 2. Space-separated single-word tickers (e.g. "INFY TCS RELIANCE")
        String[] parts = cleaned.split("\\s+");
        if (parts.length > 1) {
            boolean allTickers = Arrays.stream(parts).allMatch(p -> p.matches("^[A-Za-z0-9^.-]{1,10}$") && (p.equals(p.toUpperCase()) || p.contains(".")));
            if (allTickers) {
                return Arrays.stream(parts).map(String::trim).filter(s -> !s.isEmpty()).toList();
            }
        }
        // 3. Single company (e.g. "HDFC Bank")
        return List.of(cleaned);
    }

    /**
     * Custom Tool: Executes the Iterative Research-Critic Loop Workflow.
     */
    public static class RunCriticLoopWorkflowTool extends BaseTool {
        public static final String TOOL_NAME = "run_critic_loop_research_workflow";
        private final McpToolset mcpToolset;

        public RunCriticLoopWorkflowTool(McpToolset mcpToolset) {
            super(TOOL_NAME, "Executes an iterative authoring and compliance critic loop with exit_loop termination condition.");
            this.mcpToolset = mcpToolset;
        }

        @Override
        public Optional<FunctionDeclaration> declaration() {
            Map<String, Schema> properties = new HashMap<>();
            properties.put("company", Schema.builder()
                    .type(Type.Known.STRING)
                    .description("The target company symbol or name for evidence-grounded research (e.g. 'INFY').")
                    .build());

            Schema parameters = Schema.builder()
                    .type(Type.Known.OBJECT)
                    .properties(properties)
                    .required(List.of("company"))
                    .build();

            return Optional.of(FunctionDeclaration.builder()
                    .name(TOOL_NAME)
                    .description("Executes an iterative authoring and compliance critic loop.")
                    .parameters(parameters)
                    .build());
        }

        @Override
        public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
            return Single.fromCallable(() -> {
                String company = String.valueOf(args.getOrDefault("company", "INFY"));
                logger.info("[Workflow Execution] Starting Iterative Critic Loop Workflow for: {}", company);
                ResearchCriticLoopWorkflowV7.LoopWorkflowResult result =
                        ResearchCriticLoopWorkflowV7.executeWorkflow(company, mcpToolset);

                Map<String, Object> response = new HashMap<>();
                response.put("status", result.completed() ? "success" : "failure");
                response.put("critic_approved", result.criticApproved());
                response.put("iterations", result.iterationsExecuted());
                response.put("report", result.finalReport());
                response.put("critic_feedback", result.lastCriticFeedback());
                return response;
            });
        }
    }

    private FinanceAdvisorAgentV7() {}
}
