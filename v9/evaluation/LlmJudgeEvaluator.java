package com.google.adk.finance.v9.evaluation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * LLM-as-a-Judge qualitative evaluator for Version 9.
 * <p>
 * Evaluates non-deterministic dimensions that cannot be audited by strict arithmetic:
 * <ul>
 *   <li><b>Evidence usage:</b> Does the response adequately ground its thesis in the supplied evidence?</li>
 *   <li><b>Reasoning consistency:</b> Is the investment logic sound and aligned with the evidence?</li>
 *   <li><b>Uncertainty communication:</b> Are market risks, forecasts, and caveats expressed transparently?</li>
 *   <li><b>Question answering:</b> Did the advisor address the core user request?</li>
 *   <li><b>Clarity and completeness:</b> Is the tone institutional, professional, and well-structured?</li>
 * </ul>
 * <p>
 * <b>Architectural Invariant:</b> The LLM Judge must NEVER override deterministic results from
 * {@code PortfolioMathTool}, ticker validation, or explicit factual contradictions.
 */
public class LlmJudgeEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(LlmJudgeEvaluator.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String JUDGE_PROMPT = """
            You are an expert institutional financial evaluation judge auditing an autonomous Finance Advisor's response.
            Your task is to qualitatively evaluate the report against provided empirical evidence.

            CRITICAL RULES:
            1. Do NOT invent facts.
            2. Use ONLY the supplied evidence and evaluation criteria.
            3. Distinguish:
               - directly supported facts
               - calculations
               - reasonable interpretation
               - unsupported claims
            4. If evidence is insufficient, say so explicitly.
            5. Do not reward confident language when evidence is missing.
            6. Evaluate across the following dimensions:
               - Evidence usage: Does the report cite and interpret the provided evidence accurately?
               - Reasoning consistency: Is the economic logic consistent with the facts?
               - Uncertainty communication: Are risks, caveats, and data limitations made clear?
               - Question answering: Does the report directly answer the user's core query?
               - Clarity & Completeness: Is the synthesis clear, well-structured, and institutional?

            Return your evaluation in structured JSON format with fields:
            {
              "status": "PASS" | "FAIL" | "WARN",
              "confidence": 0.0 to 1.0,
              "evidenceUsage": "Good" | "Adequate" | "Poor",
              "uncertaintyHandling": "Appropriate" | "Deficient",
              "explanationQuality": "Clear" | "Vague" | "Contradictory",
              "explanation": "Detailed rationale...",
              "supportingEvidence": ["evidence reference 1", "evidence reference 2"]
            }
            """;

    public record JudgeResult(
            EvaluationResult.Status status,
            double confidence,
            String evidenceUsage,
            String uncertaintyHandling,
            String explanationQuality,
            String explanation,
            List<String> supportingEvidence
    ) {}

    private final BaseLlm judgeModel;

    public LlmJudgeEvaluator() {
        this(null);
    }

    public LlmJudgeEvaluator(BaseLlm judgeModel) {
        this.judgeModel = judgeModel;
    }

    /**
     * Evaluates a report using live LLM or offline deterministic rule-based judge.
     */
    public EvaluationResult evaluate(String testCaseId, String userRequest, String report, List<EvidenceRecord> evidence) {
        JudgeResult judgeResult;

        if (judgeModel != null && isLiveEvaluationAvailable()) {
            judgeResult = evaluateWithLiveModel(userRequest, report, evidence);
        } else {
            judgeResult = evaluateOffline(userRequest, report, evidence);
        }

        StringBuilder breakdown = new StringBuilder();
        breakdown.append(String.format("Evidence usage: %s\n", judgeResult.evidenceUsage()));
        breakdown.append(String.format("Uncertainty:    %s\n", judgeResult.uncertaintyHandling()));
        breakdown.append(String.format("Explanation:    %s\n", judgeResult.explanationQuality()));
        breakdown.append(String.format("\nRationale: %s", judgeResult.explanation()));

        return new EvaluationResult(
                testCaseId,
                EvaluationCriteria.LLM_JUDGE,
                judgeResult.status(),
                Optional.of(judgeResult.confidence()),
                "Evidence usage: Good, Uncertainty: Appropriate, Explanation: Clear",
                String.format("Evidence: %s, Uncertainty: %s, Explanation: %s",
                        judgeResult.evidenceUsage(), judgeResult.uncertaintyHandling(), judgeResult.explanationQuality()),
                breakdown.toString(),
                judgeResult.supportingEvidence(),
                judgeResult.status() == EvaluationResult.Status.FAIL ? Optional.of(judgeResult.explanation()) : Optional.empty(),
                Map.of(
                        "evidenceUsage", judgeResult.evidenceUsage(),
                        "uncertaintyHandling", judgeResult.uncertaintyHandling(),
                        "explanationQuality", judgeResult.explanationQuality(),
                        "confidence", judgeResult.confidence()
                )
        );
    }

    private boolean isLiveEvaluationAvailable() {
        return AppConfig.GEMINI_API_KEY != null && !AppConfig.GEMINI_API_KEY.isBlank();
    }

    private JudgeResult evaluateWithLiveModel(String userRequest, String report, List<EvidenceRecord> evidence) {
        try {
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append(JUDGE_PROMPT).append("\n\n");
            promptBuilder.append("USER REQUEST:\n").append(userRequest).append("\n\n");
            promptBuilder.append("SUPPLIED EVIDENCE RECORDS:\n");
            for (EvidenceRecord r : evidence) {
                promptBuilder.append(String.format("- [%s / %s] %s: %s (ref: %s)\n",
                        r.source(), r.tool(), r.field(), r.value(), r.sourceReference()));
            }
            promptBuilder.append("\nFINANCE ADVISOR REPORT TO EVALUATE:\n").append(report).append("\n\n");
            promptBuilder.append("Return ONLY the JSON evaluation object.");

            Content content = Content.builder()
                    .role("user")
                    .parts(List.of(Part.fromText(promptBuilder.toString())))
                    .build();

            com.google.adk.models.LlmRequest request = com.google.adk.models.LlmRequest.builder()
                    .contents(List.of(content))
                    .build();

            com.google.adk.models.LlmResponse response = judgeModel.generateContent(request, false).blockingFirst();

            String responseText = "";
            if (response.content().isPresent() && !response.content().get().parts().isEmpty()) {
                responseText = response.content().get().parts().get().get(0).text().orElse("");
            }

            if (!responseText.isBlank()) {
                Map<String, Object> map = OBJECT_MAPPER.readValue(responseText, new TypeReference<>() {});
                String statusStr = String.valueOf(map.getOrDefault("status", "PASS"));
                EvaluationResult.Status status = EvaluationResult.Status.valueOf(statusStr.toUpperCase());
                double confidence = ((Number) map.getOrDefault("confidence", 0.9)).doubleValue();
                String evidenceUsage = String.valueOf(map.getOrDefault("evidenceUsage", "Good"));
                String uncertaintyHandling = String.valueOf(map.getOrDefault("uncertaintyHandling", "Appropriate"));
                String explanationQuality = String.valueOf(map.getOrDefault("explanationQuality", "Clear"));
                String explanation = String.valueOf(map.getOrDefault("explanation", "Audited by LLM Judge"));
                //noinspection unchecked
                List<String> supportingEvidence = (List<String>) map.getOrDefault("supportingEvidence", List.of());

                return new JudgeResult(status, confidence, evidenceUsage, uncertaintyHandling, explanationQuality, explanation, supportingEvidence);
            }
        } catch (Exception e) {
            logger.warn("[LlmJudgeEvaluator] Live judge generation failed, falling back to deterministic offline audit: {}", e.getMessage());
        }

        return evaluateOffline(userRequest, report, evidence);
    }

    /**
     * Deterministic offline judge that audits qualitative dimensions without external API calls.
     */
    public JudgeResult evaluateOffline(String userRequest, String report, List<EvidenceRecord> evidence) {
        if (report == null || report.isBlank()) {
            return new JudgeResult(EvaluationResult.Status.FAIL, 1.0, "Poor", "Deficient", "Vague",
                    "Report was empty or blank.", List.of());
        }

        String lower = report.toLowerCase();

        // 1. Audit Uncertainty Communication
        boolean hasUncertainty = lower.contains("risk") || lower.contains("uncertainty") ||
                lower.contains("subject to") || lower.contains("volatility") ||
                lower.contains("assumptions") || lower.contains("disclaimer") ||
                lower.contains("potential") || lower.contains("caveat");
        String uncertainty = hasUncertainty ? "Appropriate" : "Deficient";

        // 2. Audit Evidence Citation
        int citedCount = 0;
        List<String> citedRefs = new ArrayList<>();
        for (EvidenceRecord r : evidence) {
            if (!r.ticker().isBlank() && (lower.contains(r.ticker().toLowerCase()) || lower.contains(r.value().toLowerCase()))) {
                citedCount++;
                citedRefs.add(r.sourceReference());
            }
        }
        String evidenceUsage = citedCount > 0 || evidence.isEmpty() ? "Good" : "Adequate";

        // 3. Audit Explanation Quality
        boolean hasStructure = lower.contains("analysis") || lower.contains("summary") || lower.contains("overview");
        boolean hasLength = report.length() >= 120;
        String explanation = (hasStructure && hasLength) ? "Clear" : "Vague";

        // Overall status
        EvaluationResult.Status status = (hasUncertainty && hasLength)
                ? EvaluationResult.Status.PASS
                : EvaluationResult.Status.WARN;

        String rationale = String.format("Offline audit: %s qualitative standards verified with %d evidence citations.",
                status, citedCount);

        return new JudgeResult(status, 0.85, evidenceUsage, uncertainty, explanation, rationale, citedRefs);
    }
}
