package com.google.adk.finance.v4;

import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Custom ADK {@link BaseTool} for accessing curated project grounding knowledge files.
 * <p>
 * Core ADK Concept:
 * Exposing curated, local markdown domain knowledge to the LLM via deterministic function calling.
 * Enables the agent to retrieve exact project definitions and frameworks rather than relying on
 * massive static system prompts or general model assumptions.
 */
public class ProjectKnowledgeTool extends BaseTool {
    private static final Logger logger = LoggerFactory.getLogger(ProjectKnowledgeTool.class);

    public static final String TOOL_NAME = "read_project_knowledge";

    private static final Map<String, String> DOCUMENT_MAP = Map.ofEntries(
            Map.entry("glossary", "glossary.md"),
            Map.entry("valuation-principles", "valuation-principles.md"),
            Map.entry("valuation", "valuation-principles.md"),
            Map.entry("fundamental-analysis", "fundamental-analysis.md"),
            Map.entry("fundamentals", "fundamental-analysis.md"),
            Map.entry("risk-framework", "risk-framework.md"),
            Map.entry("risk", "risk-framework.md"),
            Map.entry("portfolio-principles", "portfolio-principles.md"),
            Map.entry("portfolio", "portfolio-principles.md"),
            Map.entry("market-research-framework", "market-research-framework.md"),
            Map.entry("market-research", "market-research-framework.md")
    );

    private final Path knowledgeBaseDir;

    public ProjectKnowledgeTool() {
        this(Path.of("knowledge"));
    }

    public ProjectKnowledgeTool(Path knowledgeBaseDir) {
        super(TOOL_NAME, "Retrieves curated project grounding knowledge documents on finance topics: glossary, valuation-principles, fundamental-analysis, risk-framework, portfolio-principles, and market-research-framework.");
        this.knowledgeBaseDir = knowledgeBaseDir;
    }

    @Override
    public Optional<FunctionDeclaration> declaration() {
        Schema topicSchema = Schema.builder()
                .type(Type.Known.STRING)
                .description("The knowledge topic or document to read: 'glossary', 'valuation-principles', 'fundamental-analysis', 'risk-framework', 'portfolio-principles', or 'market-research-framework'.")
                .build();

        Schema parameters = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of("topic", topicSchema))
                .required(List.of("topic"))
                .build();

        return Optional.of(FunctionDeclaration.builder()
                .name(TOOL_NAME)
                .description(description())
                .parameters(parameters)
                .build());
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
        return Single.fromCallable(() -> {
            String topicRaw = (String) args.getOrDefault("topic", "");
            String normalizedTopic = topicRaw.trim().toLowerCase();

            logger.info("Executing {} for topic='{}'", TOOL_NAME, normalizedTopic);

            String fileName = DOCUMENT_MAP.get(normalizedTopic);
            if (fileName == null) {
                // Check if the user passed the filename directly (e.g. "glossary.md")
                if (normalizedTopic.endsWith(".md")) {
                    String base = normalizedTopic.substring(0, normalizedTopic.length() - 3);
                    fileName = DOCUMENT_MAP.get(base);
                }
            }

            if (fileName == null) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("status", "error");
                errorResult.put("message", "Unknown knowledge topic: '" + topicRaw + "'.");
                errorResult.put("available_topics", List.of(
                        "glossary",
                        "valuation-principles",
                        "fundamental-analysis",
                        "risk-framework",
                        "portfolio-principles",
                        "market-research-framework"
                ));
                return errorResult;
            }

            Path filePath = knowledgeBaseDir.resolve(fileName);
            if (!Files.exists(filePath)) {
                Map<String, Object> missingResult = new HashMap<>();
                missingResult.put("status", "error");
                missingResult.put("message", "Grounding knowledge file not found on disk at: " + filePath.toAbsolutePath());
                return missingResult;
            }

            try {
                String content = Files.readString(filePath);
                Map<String, Object> successResult = new HashMap<>();
                successResult.put("status", "success");
                successResult.put("topic", normalizedTopic);
                successResult.put("file", fileName);
                successResult.put("content", content);
                return successResult;
            } catch (IOException e) {
                logger.error("Failed to read knowledge file {}: {}", filePath, e.getMessage(), e);
                Map<String, Object> ioError = new HashMap<>();
                ioError.put("status", "error");
                ioError.put("message", "Error reading file: " + e.getMessage());
                return ioError;
            }
        });
    }

    public List<String> getAvailableTopics() {
        return List.of(
                "glossary",
                "valuation-principles",
                "fundamental-analysis",
                "risk-framework",
                "portfolio-principles",
                "market-research-framework"
        );
    }
}
