package com.google.adk.finance.v9.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.finance.tools.PortfolioMathTool;
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
 * Deterministic Calculation Fidelity Evaluator for Version 9.
 * <p>
 * Uses {@link PortfolioMathTool} directly as the single source of truth for financial math.
 * Cross-references claimed PnL, cost basis, return percentages, and allocation weights in the
 * agent's report against deterministic Java calculations.
 * <p>
 * Enforces floating-point tolerances and surfaces human-readable audit breakdowns.
 */
public class CalculationFidelityEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(CalculationFidelityEvaluator.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final double DEFAULT_TOLERANCE = 0.5; // £0.50 or 0.5% tolerance for rounding in formatted output

    public record ExpectedPosition(
            String symbol,
            double buyPrice,
            double currentPrice,
            int quantity
    ) {}

    public record CalculationAudit(
            String metricName,
            double expectedValue,
            Optional<Double> actualValue,
            boolean isMatch,
            String formattedExpected,
            String formattedActual,
            String resultStatus
    ) {}

    private final PortfolioMathTool mathTool;
    private final double tolerance;

    public CalculationFidelityEvaluator() {
        this(new PortfolioMathTool(), DEFAULT_TOLERANCE);
    }

    public CalculationFidelityEvaluator(PortfolioMathTool mathTool, double tolerance) {
        this.mathTool = mathTool;
        this.tolerance = tolerance;
    }

    /**
     * Evaluates calculation fidelity for a set of known holdings.
     */
    public EvaluationResult evaluate(String testCaseId, String report, List<ExpectedPosition> positions) {
        if (positions == null || positions.isEmpty()) {
            return EvaluationResult.warn(testCaseId, EvaluationCriteria.CALCULATION_FIDELITY,
                    "No portfolio positions provided for deterministic math verification.");
        }

        List<CalculationAudit> audits = new ArrayList<>();
        boolean allPass = true;
        List<Map<String, Object>> allocationInputs = new ArrayList<>();
        Map<String, Double> expectedPnls = new HashMap<>();

        // 1. Calculate expected position-level PnL for all positions
        for (ExpectedPosition pos : positions) {
            Map<String, Object> pnlArgs = Map.of(
                    "operation", "calculate_pnl",
                    "symbol", pos.symbol(),
                    "buy_price", pos.buyPrice(),
                    "current_price", pos.currentPrice(),
                    "quantity", pos.quantity()
            );

            Map<String, Object> pnlResult = mathTool.runAsync(pnlArgs, null).blockingGet();
            double expectedPnl = pnlResult.containsKey("unrealized_pnl")
                    ? ((Number) pnlResult.get("unrealized_pnl")).doubleValue()
                    : ((Number) pnlResult.getOrDefault("pnl", 0.0)).doubleValue();
            double expectedCurrentVal = pnlResult.containsKey("current_value")
                    ? ((Number) pnlResult.get("current_value")).doubleValue()
                    : ((Number) pnlResult.getOrDefault("value", 0.0)).doubleValue();

            expectedPnls.put(pos.symbol(), expectedPnl);
            allocationInputs.add(Map.of(
                    "symbol", pos.symbol(),
                    "value", expectedCurrentVal
            ));
        }

        // Determine which positions are actively mentioned in the report to audit
        List<ExpectedPosition> mentionedPositions = positions.stream()
                .filter(p -> report.toLowerCase().contains(p.symbol().toLowerCase())
                          || report.toLowerCase().contains(p.symbol().replace(" ", "").toLowerCase()))
                .toList();
        List<ExpectedPosition> targetPositions = mentionedPositions.isEmpty() ? positions : mentionedPositions;

        // Audit PnL for targeted positions
        for (ExpectedPosition pos : targetPositions) {
            double expectedPnl = expectedPnls.getOrDefault(pos.symbol(), 0.0);
            Optional<Double> actualPnl = extractMetricForSymbol(report, pos.symbol(), "pnl");
            boolean pnlMatch = actualPnl.isPresent() && Math.abs(actualPnl.get() - expectedPnl) <= tolerance;
            if (!pnlMatch) allPass = false;

            audits.add(new CalculationAudit(
                    pos.symbol() + " PnL",
                    expectedPnl,
                    actualPnl,
                    pnlMatch,
                    String.format("%.2f", expectedPnl),
                    actualPnl.map(v -> String.format("%.2f", v)).orElse("NOT_FOUND"),
                    pnlMatch ? "PASS" : "FAIL"
            ));
        }

        // 2. Calculate and audit portfolio allocation weights using PortfolioMathTool
        try {
            String positionsJson = OBJECT_MAPPER.writeValueAsString(allocationInputs);
            Map<String, Object> allocArgs = Map.of(
                    "operation", "calculate_allocation",
                    "positions_json", positionsJson
            );
            Map<String, Object> allocResult = mathTool.runAsync(allocArgs, null).blockingGet();

            if (allocResult.containsKey("allocations") && allocResult.get("allocations") instanceof List<?> allocList) {
                for (Object item : allocList) {
                    if (item instanceof Map<?, ?> am) {
                        String sym = String.valueOf(am.get("symbol"));
                        boolean isTarget = targetPositions.stream().anyMatch(p -> p.symbol().equalsIgnoreCase(sym));
                        if (!isTarget) continue;

                        double expectedWeight = ((Number) am.get("weight_pct")).doubleValue();

                        Optional<Double> actualWeight = extractWeightForSymbol(report, sym);
                        boolean weightMatch = actualWeight.isPresent() && Math.abs(actualWeight.get() - expectedWeight) <= tolerance;
                        if (!weightMatch) allPass = false;

                        audits.add(new CalculationAudit(
                                sym + " Weight",
                                expectedWeight,
                                actualWeight,
                                weightMatch,
                                String.format("%.2f%%", expectedWeight),
                                actualWeight.map(v -> String.format("%.2f%%", v)).orElse("NOT_FOUND"),
                                weightMatch ? "PASS" : "FAIL"
                        ));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("[CalculationFidelityEvaluator] Failed to compute allocations: {}", e.getMessage());
        }

        // Build summary report breakdown
        StringBuilder breakdown = new StringBuilder();
        breakdown.append("CALCULATION FIDELITY AUDIT:\n");
        for (CalculationAudit a : audits) {
            breakdown.append(String.format("- %s: Expected=%s, Actual=%s -> %s\n",
                    a.metricName(), a.formattedExpected(), a.formattedActual(), a.resultStatus()));
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalMetricsAudited", audits.size());
        metadata.put("passedAudit", allPass);

        if (allPass) {
            return new EvaluationResult(
                    testCaseId,
                    EvaluationCriteria.CALCULATION_FIDELITY,
                    EvaluationResult.Status.PASS,
                    Optional.of(1.0),
                    "Deterministic match with PortfolioMathTool",
                    "All calculated metrics matched within tolerance",
                    breakdown.toString(),
                    List.of("PortfolioMathTool.calculate_pnl", "PortfolioMathTool.calculate_allocation"),
                    Optional.empty(),
                    metadata
            );
        } else {
            return new EvaluationResult(
                    testCaseId,
                    EvaluationCriteria.CALCULATION_FIDELITY,
                    EvaluationResult.Status.FAIL,
                    Optional.of(0.0),
                    "Deterministic match with PortfolioMathTool",
                    "Discrepancy detected between report assertions and PortfolioMathTool calculations",
                    breakdown.toString(),
                    List.of("PortfolioMathTool.calculate_pnl", "PortfolioMathTool.calculate_allocation"),
                    Optional.of("Numerical calculation mismatch detected"),
                    metadata
            );
        }
    }

    /**
     * Direct evaluation for single PnL and weight verification against expected numbers.
     */
    public EvaluationResult evaluateSingle(String testCaseId, String report, String symbol, double expectedPnl, double expectedWeight) {
        Optional<Double> actualPnl = extractMetricForSymbol(report, symbol, "pnl");
        Optional<Double> actualWeight = extractWeightForSymbol(report, symbol);

        boolean pnlMatch = actualPnl.isPresent() && Math.abs(actualPnl.get() - expectedPnl) <= tolerance;
        boolean weightMatch = actualWeight.isPresent() && Math.abs(actualWeight.get() - expectedWeight) <= tolerance;

        StringBuilder sb = new StringBuilder();
        sb.append("PnL:\n");
        sb.append(String.format("Expected: %.2f\n", expectedPnl));
        sb.append(String.format("Actual:   %s\n", actualPnl.map(v -> String.format("%.2f", v)).orElse("NOT_FOUND")));
        sb.append(String.format("Result: %s\n\n", pnlMatch ? "PASS" : "FAIL"));

        sb.append("Weight:\n");
        sb.append(String.format("Expected: %.2f%%\n", expectedWeight));
        sb.append(String.format("Actual:   %s\n", actualWeight.map(v -> String.format("%.2f%%", v)).orElse("NOT_FOUND")));
        sb.append(String.format("Result: %s\n", weightMatch ? "PASS" : "FAIL"));

        boolean overallPass = pnlMatch && weightMatch;
        sb.append(String.format("\nOverall: %s", overallPass ? "PASS" : "FAIL"));

        return new EvaluationResult(
                testCaseId,
                EvaluationCriteria.CALCULATION_FIDELITY,
                overallPass ? EvaluationResult.Status.PASS : EvaluationResult.Status.FAIL,
                Optional.of(overallPass ? 1.0 : 0.0),
                String.format("PnL: %.2f, Weight: %.2f%%", expectedPnl, expectedWeight),
                String.format("PnL: %s, Weight: %s",
                        actualPnl.map(v -> String.format("%.2f", v)).orElse("NOT_FOUND"),
                        actualWeight.map(v -> String.format("%.2f%%", v)).orElse("NOT_FOUND")),
                sb.toString(),
                List.of("PortfolioMathTool"),
                overallPass ? Optional.empty() : Optional.of("Calculation fidelity failed on numerical comparison"),
                Map.of("pnlPass", pnlMatch, "weightPass", weightMatch)
        );
    }

    private Optional<Double> extractMetricForSymbol(String report, String symbol, String metric) {
        String cleanSym = symbol.contains(".") ? symbol.substring(0, symbol.indexOf('.')) : symbol;

        // Pattern 1: Look for PnL / profit near the symbol or generally in report
        Pattern pnlPattern = Pattern.compile("(?i)(?:pnl|p&l|profit|return|gain)\\s*(?:is|of|:|=)?\\s*(?:[₹$£€]|inr|gbp|usd)?\\s*([+-]?[0-9,]+(?:\\.[0-9]+)?)");
        Matcher m = pnlPattern.matcher(report);
        if (m.find()) {
            return parseVal(m.group(1));
        }

        // Pattern 2: Look for currency value directly if preceded by symbol
        Pattern symPattern = Pattern.compile("(?i)" + Pattern.quote(cleanSym) + "[^\\n]*?(?:[₹$£€]|inr|gbp|usd)\\s*([+-]?[0-9,]+(?:\\.[0-9]+)?)");
        Matcher sm = symPattern.matcher(report);
        if (sm.find()) {
            return parseVal(sm.group(1));
        }

        return Optional.empty();
    }

    private Optional<Double> extractWeightForSymbol(String report, String symbol) {
        String cleanSym = symbol.contains(".") ? symbol.substring(0, symbol.indexOf('.')) : symbol;

        // Pattern 1: Look for weight / allocation keywords with percentage e.g. "weight of 35.5%"
        Pattern weightPattern = Pattern.compile("(?i)(?:weight|allocation)[^0-9%]*?([0-9]+(?:\\.[0-9]+)?)\\s*%");
        Matcher m = weightPattern.matcher(report);
        if (m.find()) {
            return parseVal(m.group(1));
        }

        // Pattern 2: Fallback for symbol followed on same line by percentage
        Pattern symPattern = Pattern.compile("(?i)" + Pattern.quote(cleanSym) + "[^\\n]*?([0-9]+(?:\\.[0-9]+)?)\\s*%");
        Matcher sm = symPattern.matcher(report);
        if (sm.find()) {
            return parseVal(sm.group(1));
        }

        return Optional.empty();
    }

    private Optional<Double> parseVal(String str) {
        if (str == null) return Optional.empty();
        try {
            return Optional.of(Double.parseDouble(str.replace(",", "").trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
