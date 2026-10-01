package com.google.adk.finance.v7.workflows.sequential;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.events.Event;
import com.google.adk.finance.v7.subagents.CompanyResearchAgentV7;
import com.google.adk.finance.v7.subagents.FundamentalAnalysisAgentV7;
import com.google.adk.finance.v7.subagents.RiskAnalysisAgentV7;
import com.google.adk.finance.v7.subagents.SequentialReportSynthesisAgentV7;
import com.google.adk.finance.v7.subagents.ValuationAnalysisAgentV7;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.tools.mcp.McpToolset;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Week 4 — Version 7: Deterministic Sequential Investment Research Workflow.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li><b>Explicit Dependency Chaining</b>: Output of Stage $N$ automatically feeds as prompt context
 *       into Stage $N+1$ via Google ADK {@code outputKey} and {@code {outputKey}} prompt templating.</li>
 *   <li><b>Rigid 5-Stage Pipeline</b>:
 *     <ol>
 *       <li>Stage 1: {@link CompanyResearchAgentV7} (Live Google Search Grounding) -> {@code company_research_output}</li>
 *       <li>Stage 2: {@link FundamentalAnalysisAgentV7} (Yahoo Finance MCP Fundamentals) -> {@code fundamental_analysis_output}</li>
 *       <li>Stage 3: {@link RiskAnalysisAgentV7} (Volatility, Beta & Leverage) -> {@code risk_analysis_output}</li>
 *       <li>Stage 4: {@link ValuationAnalysisAgentV7} (Grounded Multiples Evaluation) -> {@code valuation_analysis_output}</li>
 *       <li>Stage 5: {@link SequentialReportSynthesisAgentV7} (Institutional 7-Section Synthesis) -> {@code investment_research_report}</li>
 *     </ol>
 *   </li>
 *   <li><b>Contrasted with V6 Dynamic Delegation</b>: In V6, the parent LLM decided dynamically which sub-agent
 *       to call. In V7, this {@link SequentialAgent} executes all 5 stages in deterministic linear order without LLM guessing.</li>
 * </ul>
 */
public final class InvestmentResearchSequentialWorkflowV7 {
    private static final Logger logger = LoggerFactory.getLogger(InvestmentResearchSequentialWorkflowV7.class);

    public static final String WORKFLOW_NAME = "investment_research_sequential_pipeline";

    private static final Callbacks.BeforeAgentCallback beforePipelineCallback = context -> {
        logger.info("[Sequential Workflow] Starting 5-stage sequential investment research pipeline.");
        return io.reactivex.rxjava3.core.Maybe.empty();
    };

    private static final Callbacks.AfterAgentCallback afterPipelineCallback = context -> {
        logger.info("[Sequential Workflow] 5-stage sequential investment research pipeline successfully completed.");
        return io.reactivex.rxjava3.core.Maybe.empty();
    };

    /**
     * Builds and configures the deterministic {@link SequentialAgent} with all 5 stages.
     *
     * @param mcpToolset the active McpToolset for Yahoo Finance tools
     * @return the configured {@link SequentialAgent}
     */
    public static SequentialAgent createWorkflowAgent(McpToolset mcpToolset) {
        LlmAgent stage1 = CompanyResearchAgentV7.create();
        LlmAgent stage2 = FundamentalAnalysisAgentV7.create(mcpToolset);
        LlmAgent stage3 = RiskAnalysisAgentV7.create(mcpToolset);
        LlmAgent stage4 = ValuationAnalysisAgentV7.create(mcpToolset);
        LlmAgent stage5 = SequentialReportSynthesisAgentV7.create();

        return SequentialAgent.builder()
                .name(WORKFLOW_NAME)
                .description("Deterministic 5-stage sequential investment research pipeline: Company Research -> Fundamentals -> Risk -> Valuation -> Synthesis.")
                .subAgents(stage1, stage2, stage3, stage4, stage5)
                .beforeAgentCallback(beforePipelineCallback)
                .afterAgentCallback(afterPipelineCallback)
                .build();
    }

    /**
     * Convenience method to execute the sequential workflow for a target company and return the accumulated output.
     *
     * @param companyTickerOrName company ticker or name (e.g., "INFY" or "Infosys")
     * @param mcpToolset active MCP toolset
     * @return the synthesized final report and intermediate stage outputs
     */
    public static SequentialWorkflowResult executeWorkflow(String companyTickerOrName, McpToolset mcpToolset) {
        SequentialAgent workflowAgent = createWorkflowAgent(mcpToolset);
        Runner runner = new InMemoryRunner(workflowAgent, WORKFLOW_NAME);

        String userId = "finance-learner-v7";
        String sessionId = "seq-session-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> sessionState = new HashMap<>();

        Content userPrompt = Content.builder()
                .role("user")
                .parts(List.of(Part.fromText("Prepare a structured investment research report on " + companyTickerOrName)))
                .build();

        RunConfig runConfig = RunConfig.builder().build();
        StringBuilder streamOutput = new StringBuilder();

        try {
            runner.sessionService()
                    .createSession(runner.appName(), userId, sessionState, sessionId)
                    .blockingGet();

            Flowable<Event> eventStream = runner.runAsync(userId, sessionId, userPrompt, runConfig, sessionState);

            eventStream.blockingForEach(event -> {
                event.content().ifPresent(content -> {
                    content.parts().ifPresent(parts -> {
                        for (Part part : parts) {
                            part.text().ifPresent(streamOutput::append);
                        }
                    });
                });
                if (event.actions() != null && event.actions().stateDelta() != null) {
                    sessionState.putAll(event.actions().stateDelta());
                }
            });

            String finalReport = (String) sessionState.getOrDefault(
                    SequentialReportSynthesisAgentV7.OUTPUT_KEY,
                    streamOutput.toString()
            );

            return new SequentialWorkflowResult(
                    companyTickerOrName,
                    true,
                    finalReport,
                    sessionState,
                    null
            );
        } catch (Exception e) {
            logger.error("[Sequential Workflow Error] Execution failed for {}: {}", companyTickerOrName, e.getMessage(), e);
            return new SequentialWorkflowResult(
                    companyTickerOrName,
                    false,
                    streamOutput.toString(),
                    sessionState,
                    e.getMessage()
            );
        }
    }

    /**
     * Immutable result container for sequential workflow execution.
     */
    public record SequentialWorkflowResult(
            String targetCompany,
            boolean successful,
            String finalReport,
            Map<String, Object> stageOutputs,
            String errorMessage
    ) {
        public String getStage1Output() {
            return (String) stageOutputs.get(CompanyResearchAgentV7.OUTPUT_KEY);
        }

        public String getStage2Output() {
            return (String) stageOutputs.get(FundamentalAnalysisAgentV7.OUTPUT_KEY);
        }

        public String getStage3Output() {
            return (String) stageOutputs.get(RiskAnalysisAgentV7.OUTPUT_KEY);
        }

        public String getStage4Output() {
            return (String) stageOutputs.get(ValuationAnalysisAgentV7.OUTPUT_KEY);
        }

        public String getStage5Synthesis() {
            return (String) stageOutputs.getOrDefault(SequentialReportSynthesisAgentV7.OUTPUT_KEY, finalReport);
        }
    }

    private InvestmentResearchSequentialWorkflowV7() {}
}
