package com.google.adk.finance.v7.tools;

import com.google.adk.finance.v7.workflows.parallel.PortfolioParallelResearchWorkflowV7;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.adk.tools.mcp.McpToolset;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Week 4 — Version 7: Custom Tool executing the Parallel Fan-Out / Fan-In Research Workflow.
 */
public class RunParallelWorkflowTool extends BaseTool {
    private static final Logger logger = LoggerFactory.getLogger(RunParallelWorkflowTool.class);
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
            response.put("comparative_report", (result.comparativeReport() != null && !result.comparativeReport().isBlank())
                    ? result.comparativeReport()
                    : "Parallel comparative research report prepared.");
            return response;
        });
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
}
