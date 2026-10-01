package com.google.adk.finance.v7.tools;

import com.google.adk.finance.v7.workflows.sequential.InvestmentResearchSequentialWorkflowV7;
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
 * Week 4 — Version 7: Custom Tool executing the 5-Stage Sequential Research Pipeline.
 */
public class RunSequentialWorkflowTool extends BaseTool {
    private static final Logger logger = LoggerFactory.getLogger(RunSequentialWorkflowTool.class);
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
            response.put("report", (result.finalReport() != null && !result.finalReport().isBlank())
                    ? result.finalReport()
                    : "Sequential investment research report prepared for " + company + ".");
            if (!result.successful()) {
                response.put("error", result.errorMessage() != null ? result.errorMessage() : "Execution failed");
            }
            return response;
        });
    }
}
