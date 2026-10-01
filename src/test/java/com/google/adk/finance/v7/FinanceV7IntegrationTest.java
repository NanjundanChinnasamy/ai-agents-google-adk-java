package com.google.adk.finance.v7;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.LoopAgent;
import com.google.adk.agents.ParallelAgent;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.finance.v2.CustomerPortfolioRepository;
import com.google.adk.finance.tools.PortfolioMathTool;
import com.google.adk.finance.v5.YahooFinanceMcpClientManager;
import com.google.adk.finance.v7.subagents.CompanyParallelResearchWorkerV7;
import com.google.adk.finance.v7.subagents.CompanyResearchAgentV7;
import com.google.adk.finance.v7.subagents.ComplianceEvidenceCriticAgentV7;
import com.google.adk.finance.v7.subagents.FinalReportPresenterAgentV7;
import com.google.adk.finance.v7.subagents.FundamentalAnalysisAgentV7;
import com.google.adk.finance.v7.subagents.ParallelPortfolioComparisonAgentV7;
import com.google.adk.finance.v7.subagents.ReportDraftingAgentV7;
import com.google.adk.finance.v7.subagents.RiskAnalysisAgentV7;
import com.google.adk.finance.v7.subagents.SequentialReportSynthesisAgentV7;
import com.google.adk.finance.v7.subagents.ValuationAnalysisAgentV7;
import com.google.adk.finance.v7.tools.RunCriticLoopWorkflowTool;
import com.google.adk.finance.v7.tools.RunParallelWorkflowTool;
import com.google.adk.finance.v7.tools.RunSequentialWorkflowTool;
import com.google.adk.finance.v7.workflows.loop.ResearchCriticLoopWorkflowV7;
import com.google.adk.finance.v7.workflows.parallel.PortfolioParallelResearchWorkflowV7;
import com.google.adk.finance.v7.workflows.sequential.InvestmentResearchSequentialWorkflowV7;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.BaseToolset;
import com.google.adk.tools.ExitLoopTool;
import com.google.adk.tools.GoogleSearchTool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FinanceV7IntegrationTest {

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        File dbFile = tempDir.resolve("integration_test_v7_portfolio.db").toFile();
        CustomerPortfolioRepository.setDbPath(dbFile.getAbsolutePath());
    }

    @AfterAll
    static void tearDown() {
        CustomerPortfolioRepository.setDbPath("finance_portfolio.db");
    }

    @Test
    @DisplayName("Test 1: Verify Sequential Workflow (InvestmentResearchSequentialWorkflowV7) Pipeline Structure & Dependency Chaining")
    void testSequentialWorkflowStructureAndContextChaining() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();

            SequentialAgent pipeline = InvestmentResearchSequentialWorkflowV7.createWorkflowAgent(mcpManager.getMcpToolset());

            assertThat(pipeline).isNotNull();
            assertThat(pipeline.name()).isEqualTo("investment_research_sequential_pipeline");
            assertThat(pipeline.description()).contains("sequential");

            List<? extends BaseAgent> subAgents = pipeline.subAgents();
            assertThat(subAgents).hasSize(5);

            // Stage 1: Company Research
            BaseAgent stage1 = subAgents.get(0);
            assertThat(stage1.name()).isEqualTo(CompanyResearchAgentV7.AGENT_NAME);
            assertThat(((LlmAgent) stage1).outputKey()).hasValue(CompanyResearchAgentV7.OUTPUT_KEY);
            List<BaseTool> s1Tools = ((LlmAgent) stage1).tools().blockingGet();
            assertThat(s1Tools).contains(GoogleSearchTool.INSTANCE);

            // Stage 2: Fundamental Analysis
            BaseAgent stage2 = subAgents.get(1);
            assertThat(stage2.name()).isEqualTo(FundamentalAnalysisAgentV7.AGENT_NAME);
            assertThat(((LlmAgent) stage2).outputKey()).hasValue(FundamentalAnalysisAgentV7.OUTPUT_KEY);
            assertThat(((LlmAgent) stage2).instruction().toString()).contains("{" + CompanyResearchAgentV7.OUTPUT_KEY + "?}");

            // Stage 3: Risk Analysis
            BaseAgent stage3 = subAgents.get(2);
            assertThat(stage3.name()).isEqualTo(RiskAnalysisAgentV7.AGENT_NAME);
            assertThat(((LlmAgent) stage3).outputKey()).hasValue(RiskAnalysisAgentV7.OUTPUT_KEY);
            assertThat(((LlmAgent) stage3).instruction().toString())
                    .contains("{" + CompanyResearchAgentV7.OUTPUT_KEY + "?}")
                    .contains("{" + FundamentalAnalysisAgentV7.OUTPUT_KEY + "?}");

            // Stage 4: Valuation Analysis
            BaseAgent stage4 = subAgents.get(3);
            assertThat(stage4.name()).isEqualTo(ValuationAnalysisAgentV7.AGENT_NAME);
            assertThat(((LlmAgent) stage4).outputKey()).hasValue(ValuationAnalysisAgentV7.OUTPUT_KEY);
            assertThat(((LlmAgent) stage4).instruction().toString())
                    .contains("{" + FundamentalAnalysisAgentV7.OUTPUT_KEY + "?}")
                    .contains("{" + RiskAnalysisAgentV7.OUTPUT_KEY + "?}");

            // Stage 5: Synthesis Report
            BaseAgent stage5 = subAgents.get(4);
            assertThat(stage5.name()).isEqualTo(SequentialReportSynthesisAgentV7.AGENT_NAME);
            assertThat(((LlmAgent) stage5).outputKey()).hasValue(SequentialReportSynthesisAgentV7.OUTPUT_KEY);
            assertThat(((LlmAgent) stage5).instruction().toString())
                    .contains("{" + CompanyResearchAgentV7.OUTPUT_KEY + "?}")
                    .contains("{" + FundamentalAnalysisAgentV7.OUTPUT_KEY + "?}")
                    .contains("{" + RiskAnalysisAgentV7.OUTPUT_KEY + "?}")
                    .contains("{" + ValuationAnalysisAgentV7.OUTPUT_KEY + "?}")
                    .contains("Disclaimer");
        }
    }

    @Test
    @DisplayName("Test 2: Verify Parallel Workflow (PortfolioParallelResearchWorkflowV7) Fan-Out Agent Composition")
    void testParallelWorkflowAgentComposition() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();

            List<String> tickers = List.of("INFY", "TCS", "RELIANCE");
            BaseAgent workflow = PortfolioParallelResearchWorkflowV7.createWorkflowAgent(tickers, mcpManager.getMcpToolset());

            assertThat(workflow).isInstanceOf(SequentialAgent.class);
            SequentialAgent composite = (SequentialAgent) workflow;
            assertThat(composite.subAgents()).hasSize(2);

            // First step is the ParallelAgent (Fan-Out)
            BaseAgent fanOut = composite.subAgents().get(0);
            assertThat(fanOut).isInstanceOf(ParallelAgent.class);
            ParallelAgent parallelAgent = (ParallelAgent) fanOut;
            assertThat(parallelAgent.subAgents()).hasSize(3);

            // Second step is the Fan-In Aggregator
            BaseAgent fanIn = composite.subAgents().get(1);
            assertThat(fanIn.name()).isEqualTo(ParallelPortfolioComparisonAgentV7.AGENT_NAME);
            assertThat(((LlmAgent) fanIn).outputKey()).hasValue(ParallelPortfolioComparisonAgentV7.OUTPUT_KEY);
        }
    }

    @Test
    @DisplayName("Test 3: Verify Parallel Workflow Execution & Partial Failure Resilience")
    void testParallelWorkflowExecutionAndPartialFailureHandling() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();

            List<String> tickers = List.of("INFY", "FAIL_TICKER");
            List<String> failureSet = List.of("FAIL_TICKER");

            PortfolioParallelResearchWorkflowV7.ParallelWorkflowResult result =
                    PortfolioParallelResearchWorkflowV7.executeWorkflow(tickers, mcpManager.getMcpToolset(), failureSet);

            assertThat(result).isNotNull();
            assertThat(result.hasFailures()).isTrue();
            assertThat(result.successfulSymbols()).contains("INFY");
            assertThat(result.failedSymbols()).contains("FAIL_TICKER");

            // Verify the comparative report was still produced despite the partial failure
            assertThat(result.comparativeReport()).isNotEmpty();
            assertThat(result.comparativeReport()).containsIgnoringCase("FAIL_TICKER");
            assertThat(result.comparativeReport()).containsIgnoringCase("Disclaimer");
        }
    }

    @Test
    @DisplayName("Test 4: Verify Iterative Loop Workflow (ResearchCriticLoopWorkflowV7) & ExitLoopTool Integration")
    void testLoopWorkflowConfigurationAndExitLoopTool() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();

            BaseAgent workflow = ResearchCriticLoopWorkflowV7.createWorkflowAgent(mcpManager.getMcpToolset());

            assertThat(workflow).isInstanceOf(SequentialAgent.class);
            SequentialAgent composite = (SequentialAgent) workflow;
            assertThat(composite.subAgents()).hasSize(2);

            // Step 1: LoopAgent (Drafter <-> Critic)
            BaseAgent loopStep = composite.subAgents().get(0);
            assertThat(loopStep).isInstanceOf(LoopAgent.class);
            LoopAgent loopAgent = (LoopAgent) loopStep;
            assertThat(loopAgent.maxIterations()).isEqualTo(3);
            assertThat(loopAgent.subAgents()).hasSize(2);

            BaseAgent drafter = loopAgent.subAgents().get(0);
            assertThat(drafter.name()).isEqualTo(ReportDraftingAgentV7.AGENT_NAME);
            assertThat(((LlmAgent) drafter).outputKey()).hasValue(ReportDraftingAgentV7.OUTPUT_KEY);

            // Drafter MUST decouple search via AgentTool, include math, and have all valid declarations
            List<BaseTool> drafterTools = ((LlmAgent) drafter).tools().blockingGet();
            assertThat(drafterTools).isNotEmpty();
            assertThat(drafterTools).noneMatch(t -> t == GoogleSearchTool.INSTANCE);
            assertThat(drafterTools).anyMatch(t -> t.name().equals(CompanyResearchAgentV7.AGENT_NAME));
            assertThat(drafterTools).anyMatch(t -> t instanceof PortfolioMathTool);
            for (BaseTool tool : drafterTools) {
                assertThat(tool.declaration()).isPresent();
            }

            BaseAgent critic = loopAgent.subAgents().get(1);
            assertThat(critic.name()).isEqualTo(ComplianceEvidenceCriticAgentV7.AGENT_NAME);
            assertThat(((LlmAgent) critic).outputKey()).hasValue(ComplianceEvidenceCriticAgentV7.OUTPUT_KEY);

            // Critic MUST have ExitLoopTool.INSTANCE registered
            List<BaseTool> criticTools = ((LlmAgent) critic).tools().blockingGet();
            assertThat(criticTools).contains(ExitLoopTool.INSTANCE);
            System.out.println("ExitLoopTool name is: " + ExitLoopTool.INSTANCE.name());

            // Step 2: Final Report Presenter
            BaseAgent presenter = composite.subAgents().get(1);
            assertThat(presenter.name()).isEqualTo(FinalReportPresenterAgentV7.AGENT_NAME);
            assertThat(((LlmAgent) presenter).outputKey()).hasValue(FinalReportPresenterAgentV7.OUTPUT_KEY);
        }
    }

    @Test
    @DisplayName("Test 5: Verify Root Orchestrator (FinanceAdvisorAgentV7) Wiring & 3 Workflow Tools")
    void testFinanceAdvisorAgentV7RootWiring() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();

            LlmAgent rootAgent = FinanceAdvisorAgentV7.createFinanceAdvisorAgentV7(mcpManager.getMcpToolset());

            assertThat(rootAgent).isNotNull();
            assertThat(rootAgent.name()).isEqualTo("finance_advisor_v7");
            assertThat(rootAgent.instruction().toString())
                    .contains("workflow_director")
                    .contains("run_sequential_research_workflow")
                    .contains("run_parallel_portfolio_research_workflow")
                    .contains("run_critic_loop_research_workflow")
                    .contains("Disclaimer");

            List<BaseTool> tools = rootAgent.tools().blockingGet();
            List<String> toolNames = tools.stream().map(BaseTool::name).toList();

            assertThat(toolNames).contains(
                    "run_sequential_research_workflow",
                    "run_parallel_portfolio_research_workflow",
                    "run_critic_loop_research_workflow",
                    "load_customer_portfolio",
                    "portfolio_math",
                    "read_project_knowledge"
            );
        }
    }

    @Test
    @DisplayName("Test 6: Verify Single Company Worker Output Naming Normalization")
    void testSingleCompanyWorkerOutputNaming() {
        String outputKey = CompanyParallelResearchWorkerV7.getOutputKey("HDFC-BANK.NS");
        assertThat(outputKey).isEqualTo("parallel_research_hdfc_bank_ns");

        String agentName = CompanyParallelResearchWorkerV7.getAgentName("HDFC-BANK.NS");
        assertThat(agentName).isEqualTo("parallel_worker_hdfc_bank_ns");
    }

    @Test
    @DisplayName("Test 7: Verify parseCompanyList Preserves Multi-Word Company Names & Handles Varied Delimiters")
    void testParseCompanyList() {
        // Multi-word company names with commas
        List<String> r1 = FinanceAdvisorAgentV7.parseCompanyList("Infosys, HDFC Bank, Reliance");
        assertThat(r1).containsExactly("Infosys", "HDFC Bank", "Reliance");

        // Multi-word company names with "and"
        List<String> r2 = FinanceAdvisorAgentV7.parseCompanyList("Infosys, HDFC Bank and Reliance");
        assertThat(r2).containsExactly("Infosys", "HDFC Bank", "Reliance");

        // Multi-word company names with "vs"
        List<String> r3 = FinanceAdvisorAgentV7.parseCompanyList("HDFC Bank vs ICICI Bank");
        assertThat(r3).containsExactly("HDFC Bank", "ICICI Bank");

        // Space-separated uppercase tickers
        List<String> r4 = FinanceAdvisorAgentV7.parseCompanyList("INFY TCS RELIANCE");
        assertThat(r4).containsExactly("INFY", "TCS", "RELIANCE");

        // Single multi-word company
        List<String> r5 = FinanceAdvisorAgentV7.parseCompanyList("HDFC Bank");
        assertThat(r5).containsExactly("HDFC Bank");
    }

    @Test
    @DisplayName("Test 8: Verify ThreadSafeMcpToolset Pre-warming & Concurrency Protection")
    void testThreadSafeMcpToolsetPrewarming() {
        try (YahooFinanceMcpClientManager mcpManager = new YahooFinanceMcpClientManager()) {
            mcpManager.start();

            com.google.adk.finance.v7.workflows.parallel.ThreadSafeMcpToolset safeToolset =
                    new com.google.adk.finance.v7.workflows.parallel.ThreadSafeMcpToolset(mcpManager.getMcpToolset());

            // Pre-warm tools sequentially
            safeToolset.warmUp();

            List<BaseTool> tools = safeToolset.getTools(null).toList().blockingGet();
            assertThat(tools).isNotEmpty();
            List<String> toolNames = tools.stream().map(BaseTool::name).toList();
            assertThat(toolNames).contains("get_stock_info", "get_financial_statement");

            // Calling getTools multiple times should return cached tools without re-querying MCP
            List<BaseTool> toolsSecondCall = safeToolset.getTools(null).toList().blockingGet();
            assertThat(toolsSecondCall).hasSize(tools.size());
        }
    }

    @Test
    @DisplayName("Test 9: Verify Workflow Tools (RunSequential, RunParallel, RunCriticLoop) Declaration & Schema Contract")
    void testWorkflowToolsDeclarationAndContract() {
        RunSequentialWorkflowTool seqTool = new RunSequentialWorkflowTool(null);
        assertThat(seqTool.name()).isEqualTo("run_sequential_research_workflow");
        assertThat(seqTool.declaration()).isPresent();
        assertThat(seqTool.declaration().get().name()).hasValue("run_sequential_research_workflow");
        assertThat(seqTool.declaration().get().parameters()).isPresent();
        assertThat(seqTool.declaration().get().parameters().get().properties().get()).containsKey("company");

        RunParallelWorkflowTool parTool = new RunParallelWorkflowTool(null);
        assertThat(parTool.name()).isEqualTo("run_parallel_portfolio_research_workflow");
        assertThat(parTool.declaration()).isPresent();
        assertThat(parTool.declaration().get().name()).hasValue("run_parallel_portfolio_research_workflow");
        assertThat(parTool.declaration().get().parameters()).isPresent();
        assertThat(parTool.declaration().get().parameters().get().properties().get()).containsKey("companies");

        RunCriticLoopWorkflowTool loopTool = new RunCriticLoopWorkflowTool((BaseToolset) null);
        assertThat(loopTool.name()).isEqualTo("run_critic_loop_research_workflow");
        assertThat(loopTool.declaration()).isPresent();
        assertThat(loopTool.declaration().get().name()).hasValue("run_critic_loop_research_workflow");
        assertThat(loopTool.declaration().get().parameters()).isPresent();
        assertThat(loopTool.declaration().get().parameters().get().properties().get()).containsKey("company");
        assertThat(loopTool.declaration().get().parameters().get().required().get()).contains("company");
    }
}

