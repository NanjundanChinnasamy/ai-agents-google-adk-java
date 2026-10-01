package com.google.adk.finance.v7.tools;

import com.google.adk.finance.v7.workflows.loop.ResearchCriticLoopWorkflowV7;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.BaseToolset;
import com.google.adk.tools.ToolContext;
import com.google.adk.tools.mcp.McpToolset;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Week 4 — Version 7: Custom Tool executing the Iterative Research-Critic Loop Workflow.
 */
public class RunCriticLoopWorkflowTool extends BaseTool {
    private static final Logger logger = LoggerFactory.getLogger(RunCriticLoopWorkflowTool.class);
    public static final String TOOL_NAME = "run_critic_loop_research_workflow";
    private final BaseToolset mcpToolset;

    public RunCriticLoopWorkflowTool(BaseToolset mcpToolset) {
        super(TOOL_NAME, "Executes an iterative authoring and compliance critic loop with exit_loop termination condition.");
        this.mcpToolset = mcpToolset;
    }

    public RunCriticLoopWorkflowTool(McpToolset mcpToolset) {
        this((BaseToolset) mcpToolset);
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
            response.put("target_company", company);
            response.put("critic_approved", result.criticApproved());
            response.put("iterations", result.iterationsExecuted());
            response.put("report", (result.finalReport() != null && !result.finalReport().isBlank())
                    ? result.finalReport()
                    : "Investment research report prepared for " + company + ".");
            response.put("critic_feedback", (result.lastCriticFeedback() != null && !result.lastCriticFeedback().isBlank())
                    ? result.lastCriticFeedback()
                    : (result.criticApproved() ? "Critic approved draft via exit_loop." : "No critic feedback recorded."));
            if (!result.completed()) {
                response.put("error", result.errorMessage() != null ? result.errorMessage() : "Execution failed");
            }
            return response;
        });
    }
}
