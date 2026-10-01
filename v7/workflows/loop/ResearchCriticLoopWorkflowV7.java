package com.google.adk.finance.v7.workflows.loop;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.LoopAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.events.Event;
import com.google.adk.finance.v7.subagents.ComplianceEvidenceCriticAgentV7;
import com.google.adk.finance.v7.subagents.FinalReportPresenterAgentV7;
import com.google.adk.finance.v7.subagents.ReportDraftingAgentV7;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.tools.ExitLoopTool;
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
 * Week 4 — Version 7: Deterministic Iterative Loop Workflow (Research-Critic Loop).
 * <p>
 * Demonstrates:
 * <ul>
 *   <li><b>Iterative Refinement</b>: An authoring agent ({@link ReportDraftingAgentV7}) generates a draft report,
 *       and an independent critic agent ({@link ComplianceEvidenceCriticAgentV7}) audits the draft for factual grounding,
 *       sources, and regulatory compliance.</li>
 *   <li><b>Loop Termination with {@link ExitLoopTool}</b>:
 *     <ul>
 *       <li>If the draft passes the critic's audit standards, the critic invokes {@code exit_loop()}, which sets
 *           an escalate action on the event, causing {@link LoopAgent} to cleanly terminate.</li>
 *       <li>If rejected, the critic does not invoke {@code exit_loop()} and emits revision directives.
 *           The loop iterates back to the drafter with {@code {critic_feedback}} populated.</li>
 *     </ul>
 *   </li>
 *   <li><b>Iteration Safety Bounds</b>: Configured with {@code maxIterations(3)} to guarantee termination
 *       even if the LLM critic is unresolved.</li>
 *   <li><b>ADK Native Composition</b>: Follows the official Google ADK pattern of placing {@link LoopAgent}
 *       and a downstream presenter ({@link FinalReportPresenterAgentV7}) inside a root {@link SequentialAgent}.</li>
 * </ul>
 */
public final class ResearchCriticLoopWorkflowV7 {
    private static final Logger logger = LoggerFactory.getLogger(ResearchCriticLoopWorkflowV7.class);

    public static final String WORKFLOW_NAME = "research_critic_loop_workflow";
    public static final int MAX_ITERATIONS = 3;

    private static final Callbacks.BeforeAgentCallback beforeLoopCallback = context -> {
        logger.info("[Critic Loop] Starting iterative research-critic loop (max iterations: {}).", MAX_ITERATIONS);
        return io.reactivex.rxjava3.core.Maybe.empty();
    };

    private static final Callbacks.AfterAgentCallback afterLoopCallback = context -> {
        logger.info("[Critic Loop] Research-critic loop finished.");
        return io.reactivex.rxjava3.core.Maybe.empty();
    };

    /**
     * Builds and configures the deterministic composite workflow agent:
     * {@code SequentialAgent( LoopAgent(Drafter, Critic), Presenter )}.
     *
     * @param mcpToolset active McpToolset for Yahoo Finance tools
     * @return the configured composite {@link BaseAgent}
     */
    public static BaseAgent createWorkflowAgent(McpToolset mcpToolset) {
        LlmAgent drafter = ReportDraftingAgentV7.create(mcpToolset);
        LlmAgent critic = ComplianceEvidenceCriticAgentV7.create();
        LlmAgent presenter = FinalReportPresenterAgentV7.create();

        LoopAgent loopAgent = LoopAgent.builder()
                .name("research_critic_loop_engine")
                .description("Iterative loop refining report drafts until approved by compliance critic or max iterations reached.")
                .subAgents(drafter, critic)
                .maxIterations(MAX_ITERATIONS)
                .beforeAgentCallback(beforeLoopCallback)
                .afterAgentCallback(afterLoopCallback)
                .build();

        return SequentialAgent.builder()
                .name(WORKFLOW_NAME)
                .description("End-to-end Research-Critic Workflow: Iterative Loop (Drafter <-> Critic) -> Final Presenter.")
                .subAgents(loopAgent, presenter)
                .build();
    }

    /**
     * Executes the research-critic loop workflow for a target company and collects audit metrics.
     *
     * @param companyTickerOrName target company name or ticker (e.g. "INFY" or "Infosys")
     * @param mcpToolset active MCP toolset
     * @return result object containing final report, audit status, iteration count, and critic feedback
     */
    public static LoopWorkflowResult executeWorkflow(String companyTickerOrName, McpToolset mcpToolset) {
        BaseAgent workflow = createWorkflowAgent(mcpToolset);
        Runner runner = new InMemoryRunner(workflow, WORKFLOW_NAME);

        String userId = "loop-user";
        String sessionId = "loop-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> sessionState = new HashMap<>();

        Content userPrompt = Content.builder()
                .role("user")
                .parts(List.of(Part.fromText("Create an investment research report on " + companyTickerOrName + " and ensure important factual claims are supported by evidence.")))
                .build();

        RunConfig runConfig = RunConfig.builder().build();
        StringBuilder streamOutput = new StringBuilder();
        final int[] iterationCounter = {0};
        final boolean[] exitLoopCalled = {false};

        try {
            runner.sessionService()
                    .createSession(runner.appName(), userId, sessionState, sessionId)
                    .blockingGet();

            Flowable<Event> stream = runner.runAsync(userId, sessionId, userPrompt, runConfig, sessionState);

            stream.blockingForEach(event -> {
                event.content().ifPresent(content -> {
                    content.parts().ifPresent(parts -> {
                        for (Part part : parts) {
                            part.functionCall().ifPresent(fnCall -> {
                                String name = fnCall.name().orElse("");
                                if (name.equals("exit_loop")) {
                                    exitLoopCalled[0] = true;
                                    logger.info("[LOOP] Critic invoked exit_loop! Terminating loop with approval.");
                                }
                            });
                            part.text().ifPresent(streamOutput::append);
                        }
                    });
                });

                if (event.actions() != null && event.actions().stateDelta() != null) {
                    sessionState.putAll(event.actions().stateDelta());
                    if (event.actions().stateDelta().containsKey(ReportDraftingAgentV7.OUTPUT_KEY)) {
                        iterationCounter[0]++;
                        logger.info("[LOOP] Drafter produced draft version #{}", iterationCounter[0]);
                    }
                }
            });

            String finalReport = (String) sessionState.getOrDefault(
                    FinalReportPresenterAgentV7.OUTPUT_KEY,
                    sessionState.getOrDefault(ReportDraftingAgentV7.OUTPUT_KEY, streamOutput.toString().trim())
            );
            String criticFeedback = (String) sessionState.get(ComplianceEvidenceCriticAgentV7.OUTPUT_KEY);

            return new LoopWorkflowResult(
                    companyTickerOrName,
                    true,
                    exitLoopCalled[0],
                    Math.max(1, iterationCounter[0]),
                    finalReport,
                    criticFeedback,
                    null
            );
        } catch (Exception e) {
            logger.error("[Loop Workflow Error] Execution failed for {}: {}", companyTickerOrName, e.getMessage(), e);
            return new LoopWorkflowResult(
                    companyTickerOrName,
                    false,
                    exitLoopCalled[0],
                    iterationCounter[0],
                    streamOutput.toString(),
                    (String) sessionState.get(ComplianceEvidenceCriticAgentV7.OUTPUT_KEY),
                    e.getMessage()
            );
        }
    }

    /**
     * Result of executing the iterative loop workflow.
     */
    public record LoopWorkflowResult(
            String targetCompany,
            boolean completed,
            boolean criticApproved,
            int iterationsExecuted,
            String finalReport,
            String lastCriticFeedback,
            String errorMessage
    ) {}

    private ResearchCriticLoopWorkflowV7() {}
}
