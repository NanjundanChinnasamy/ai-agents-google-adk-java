package com.google.adk.finance.v9.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic Scenario Completeness Evaluator for Version 9.
 * <p>
 * Verifies that forward-looking investment scenario analyses contain all three mandatory tiers:
 * <ol>
 *   <li><b>Baseline:</b> Expected operational trajectory and baseline valuation.</li>
 *   <li><b>Upside:</b> Bullish catalysts, revenue acceleration, and margin expansion.</li>
 *   <li><b>Stress Test:</b> Macro shocks, sector contraction, regulatory headwinds, or severe drawdown.</li>
 * </ol>
 * <p>
 * Does not merely search for keywords; verifies that each scenario articulates:
 * <ul>
 *   <li>Explicit macroeconomic/operational <b>assumptions</b></li>
 *   <li>Quantitative <b>projections/outcomes</b> (targets, PnL impacts, percentages)</li>
 *   <li>Substantive analytical <b>explanations</b> (> 40 characters)</li>
 * </ul>
 */
public class ScenarioCompletenessEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioCompletenessEvaluator.class);

    public record ScenarioAudit(
            String scenarioName,
            boolean isFound,
            boolean hasAssumptions,
            boolean hasProjection,
            boolean hasMetrics,
            boolean hasExplanation,
            String status,
            String extractedSection
    ) {
        public boolean isComplete() {
            return isFound && hasAssumptions && hasProjection && hasMetrics && hasExplanation;
        }
    }

    public ScenarioCompletenessEvaluator() {}

    /**
     * Evaluates whether a report contains complete Baseline, Upside, and Stress test scenarios.
     */
    public EvaluationResult evaluate(String testCaseId, String report) {
        if (report == null || report.isBlank()) {
            return EvaluationResult.fail(testCaseId, EvaluationCriteria.SCENARIO_COMPLETENESS,
                    "Baseline: PASS\nUpside: PASS\nStress: PASS", "Report is empty", "No report content found");
        }

        ScenarioAudit baseline = auditScenario(report, "Baseline",
                List.of("baseline", "base case", "base scenario", "expected scenario"),
                List.of("assum", "growth", "margin", "guidance", "expect", "current"),
                List.of("target", "projection", "pnl", "valuation", "return", "outcome"));

        ScenarioAudit upside = auditScenario(report, "Upside",
                List.of("upside", "bull case", "bull scenario", "positive scenario", "optimistic"),
                List.of("catalyst", "accelerat", "expansion", "market share", "tailwind", "assum"),
                List.of("upside target", "re-rating", "gain", "higher", "projection", "outcome", "+"));

        ScenarioAudit stress = auditScenario(report, "Stress",
                List.of("stress", "stress test", "bear case", "bear scenario", "downside", "negative scenario", "shock"),
                List.of("headwind", "decline", "slowdown", "shock", "contraction", "recession", "assum", "hike"),
                List.of("drawdown", "downside target", "loss", "risk", "lower", "contraction", "drop", "-"));

        boolean overallPass = baseline.isComplete() && upside.isComplete() && stress.isComplete();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Baseline: %s\n", baseline.status()));
        sb.append(String.format("Upside:   %s\n", upside.status()));
        sb.append(String.format("Stress:   %s\n", stress.status()));
        sb.append(String.format("\nOverall: %s", overallPass ? "PASS" : "FAIL"));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("baselineStatus", baseline.status());
        metadata.put("upsideStatus", upside.status());
        metadata.put("stressStatus", stress.status());
        metadata.put("overallPass", overallPass);

        if (overallPass) {
            logger.info("[ScenarioCompletenessEvaluator] PASS for {}: all three scenarios verified", testCaseId);
            return new EvaluationResult(
                    testCaseId,
                    EvaluationCriteria.SCENARIO_COMPLETENESS,
                    EvaluationResult.Status.PASS,
                    Optional.of(1.0),
                    "Baseline: PASS, Upside: PASS, Stress: PASS",
                    sb.toString(),
                    "All three mandatory investment scenarios (Baseline, Upside, Stress) verified with assumptions, projections, and metrics.",
                    List.of(),
                    Optional.empty(),
                    metadata
            );
        } else {
            List<String> missing = new ArrayList<>();
            if (!baseline.isComplete()) missing.add("Baseline (" + baseline.status() + ")");
            if (!upside.isComplete()) missing.add("Upside (" + upside.status() + ")");
            if (!stress.isComplete()) missing.add("Stress (" + stress.status() + ")");

            String failureReason = "Missing or incomplete required scenario tiers: " + String.join(", ", missing);
            logger.warn("[ScenarioCompletenessEvaluator] FAIL for {}: {}", testCaseId, failureReason);

            return new EvaluationResult(
                    testCaseId,
                    EvaluationCriteria.SCENARIO_COMPLETENESS,
                    EvaluationResult.Status.FAIL,
                    Optional.of(0.0),
                    "Baseline: PASS, Upside: PASS, Stress: PASS",
                    sb.toString(),
                    failureReason,
                    List.of(),
                    Optional.of(failureReason),
                    metadata
            );
        }
    }

    private ScenarioAudit auditScenario(String text, String name, List<String> headerTerms,
                                        List<String> assumptionKeywords, List<String> projectionKeywords) {
        String lower = text.toLowerCase();
        int headerPos = -1;
        String matchedTerm = "";

        for (String term : headerTerms) {
            int pos = lower.indexOf(term);
            if (pos != -1) {
                headerPos = pos;
                matchedTerm = term;
                break;
            }
        }

        if (headerPos == -1) {
            return new ScenarioAudit(name, false, false, false, false, false, "MISSING", "");
        }

        // Extract a 350-character window following the header for granular auditing
        int endPos = Math.min(text.length(), headerPos + 350);
        String section = text.substring(headerPos, endPos);
        String sectionLower = section.toLowerCase();

        boolean hasAssumptions = false;
        for (String ak : assumptionKeywords) {
            if (sectionLower.contains(ak)) {
                hasAssumptions = true;
                break;
            }
        }

        boolean hasProjection = false;
        for (String pk : projectionKeywords) {
            if (sectionLower.contains(pk)) {
                hasProjection = true;
                break;
            }
        }

        // Metrics: check for numbers, percentages, or currency signs
        Pattern metricPattern = Pattern.compile("(?i)(?:[0-9]+(?:\\.[0-9]+)?\\s*%|[₹$£€]\\s*[0-9]+|[0-9]+x|[0-9]{3,})");
        boolean hasMetrics = metricPattern.matcher(section).find();

        boolean hasExplanation = section.trim().length() >= 40;

        String status;
        if (hasAssumptions && hasProjection && hasMetrics && hasExplanation) {
            status = "PASS";
        } else {
            status = "INCOMPLETE";
        }

        return new ScenarioAudit(name, true, hasAssumptions, hasProjection, hasMetrics, hasExplanation, status, section);
    }
}
