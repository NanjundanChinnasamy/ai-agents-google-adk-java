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
 * Deterministic Faithfulness / Groundedness Evaluator for Version 9.
 * <p>
 * Evaluates whether factual and numerical statements in the Finance Advisor's generated report
 * are strictly traceable to empirical tool evidence records (Google Search, Yahoo Finance MCP, PortfolioMathTool).
 * <p>
 * Distinguishes four categories of assertions:
 * <ol>
 *   <li><b>RETRIEVED_FACT:</b> Direct empirical observation from Search or Yahoo Finance MCP (e.g. quote, market cap).</li>
 *   <li><b>DERIVED_CALCULATION:</b> Output from deterministic calculation tools like {@code PortfolioMathTool} (e.g. PnL, allocation weights).</li>
 *   <li><b>MODEL_INTERPRETATION:</b> Qualitative reasoning or investment thesis that does not contradict empirical data.</li>
 *   <li><b>UNSUPPORTED_CLAIM:</b> Asserted numerical figure or market quote that contradicts or lacks corresponding evidence.</li>
 * </ol>
 * <p>
 * <i>Limitation:</i> This is an educational evaluation harness designed to audit factual consistency against
 * structured tool outputs. It does not replace universal NLP semantic entailment models.
 */
public class FaithfulnessEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(FaithfulnessEvaluator.class);

    public static final double DEFAULT_TOLERANCE_PCT = 0.01; // 1% tolerance for quote timing / rounding

    public enum ClaimType {
        RETRIEVED_FACT,
        DERIVED_CALCULATION,
        MODEL_INTERPRETATION,
        UNSUPPORTED_CLAIM
    }

    public record ClaimAudit(
            String sentence,
            String ticker,
            String field,
            double claimedValue,
            ClaimType type,
            boolean isSupported,
            Optional<EvidenceRecord> supportingEvidence,
            String explanation
    ) {}

    private final double tolerancePct;

    public FaithfulnessEvaluator() {
        this(DEFAULT_TOLERANCE_PCT);
    }

    public FaithfulnessEvaluator(double tolerancePct) {
        this.tolerancePct = tolerancePct;
    }

    /**
     * Evaluates the faithfulness of a report against the captured evidence store.
     */
    public EvaluationResult evaluate(String testCaseId, String report, EvidenceStoreV9 evidenceStore) {
        return evaluate(testCaseId, report, evidenceStore.getAllRecords());
    }

    /**
     * Evaluates the faithfulness of a report against a list of evidence records.
     */
    public EvaluationResult evaluate(String testCaseId, String report, List<EvidenceRecord> evidence) {
        if (report == null || report.isBlank()) {
            return EvaluationResult.fail(testCaseId, EvaluationCriteria.FAITHFULNESS,
                    "Report must contain grounded analysis", "Empty or null report", "Report was empty");
        }

        List<ClaimAudit> audits = auditReport(report, evidence);

        int supportedCount = 0;
        int unsupportedCount = 0;
        List<String> failureReasons = new ArrayList<>();
        List<String> evidenceRefs = new ArrayList<>();

        for (ClaimAudit audit : audits) {
            if (audit.isSupported()) {
                supportedCount++;
                audit.supportingEvidence().ifPresent(e -> evidenceRefs.add(e.sourceReference()));
            } else if (audit.type() == ClaimType.UNSUPPORTED_CLAIM) {
                unsupportedCount++;
                failureReasons.add(audit.explanation());
            }
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("supportedClaims", supportedCount);
        metadata.put("unsupportedClaims", unsupportedCount);
        metadata.put("totalAuditedClaims", audits.size());

        if (unsupportedCount > 0) {
            String primaryFailure = failureReasons.get(0);
            logger.warn("[Faithfulness Evaluator] Groundedness FAIL for {}: {}", testCaseId, primaryFailure);

            return new EvaluationResult(
                    testCaseId,
                    EvaluationCriteria.FAITHFULNESS,
                    EvaluationResult.Status.FAIL,
                    Optional.of(Math.max(0.0, (double) supportedCount / (supportedCount + unsupportedCount))),
                    "All factual and numerical claims traceable to retrieved evidence",
                    String.format("Found %d unsupported claim(s). Primary: %s", unsupportedCount, primaryFailure),
                    String.format("Supported claims: %d, Unsupported claims: %d", supportedCount, unsupportedCount),
                    evidenceRefs,
                    Optional.of(primaryFailure),
                    metadata
            );
        }

        logger.info("[Faithfulness Evaluator] Groundedness PASS for {}: {} claims verified", testCaseId, supportedCount);
        return new EvaluationResult(
                testCaseId,
                EvaluationCriteria.FAITHFULNESS,
                EvaluationResult.Status.PASS,
                Optional.of(1.0),
                "All factual claims traceable to retrieved evidence",
                String.format("All %d verified claims are grounded in tool evidence", supportedCount),
                String.format("Supported claims: %d, Unsupported claims: 0", supportedCount),
                evidenceRefs,
                Optional.empty(),
                metadata
        );
    }

    /**
     * Extracts and audits claims from the report against the evidence records.
     */
    public List<ClaimAudit> auditReport(String report, List<EvidenceRecord> evidence) {
        List<ClaimAudit> audits = new ArrayList<>();
        String[] sentences = report.split("(?<!\\d)\\.(?!\\d)|\\r?\\n+");

        // Patterns for detecting financial numbers with currencies or percentages
        Pattern currencyPricePattern = Pattern.compile("(?:[₹$£€]|INR\\s*|USD\\s*|GBP\\s*|EUR\\s*|Rs\\.?\\s*)([0-9,]+(?:\\.[0-9]+)?)");
        Pattern postCurrencyPattern = Pattern.compile("([0-9,]+(?:\\.[0-9]+)?)\\s*(?:INR|USD|GBP|EUR|rupees|pounds|dollars)", Pattern.CASE_INSENSITIVE);
        Pattern pePattern = Pattern.compile("P/E(?:\\s*ratio)?\\s*(?:of|:)?\\s*([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
        Pattern pnlPattern = Pattern.compile("(?:P&L|PnL|profit|gain|loss)\\s*(?:is|of|:)?\\s*(?:[₹$£€]|INR\\s*|GBP\\s*|USD\\s*)?([0-9,]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() < 10) continue;

            String ticker = extractTicker(trimmed, evidence);

            // Audit claims: PnL has precedence over generic currency symbol to avoid duplicate price claims
            Matcher pnlMatcher = pnlPattern.matcher(trimmed);
            Matcher peMatcher = pePattern.matcher(trimmed);
            Matcher m1 = currencyPricePattern.matcher(trimmed);
            Matcher m2 = postCurrencyPattern.matcher(trimmed);

            if (pnlMatcher.find()) {
                double val = parseNumeric(pnlMatcher.group(1));
                audits.add(verifyClaim(trimmed, ticker, "pnl", val, evidence));
            } else if (m1.find()) {
                double val = parseNumeric(m1.group(1));
                audits.add(verifyClaim(trimmed, ticker, "price", val, evidence));
            } else if (m2.find()) {
                double val = parseNumeric(m2.group(1));
                audits.add(verifyClaim(trimmed, ticker, "price", val, evidence));
            }

            if (peMatcher.find()) {
                double val = parseNumeric(peMatcher.group(1));
                audits.add(verifyClaim(trimmed, ticker, "pe_ratio", val, evidence));
            }
        }

        // If no explicit numerical regex matched but we have evidence records, check text-based alignment
        if (audits.isEmpty() && !evidence.isEmpty()) {
            for (EvidenceRecord record : evidence) {
                if (record.numericValue().isPresent()) {
                    double val = record.numericValue().get();
                    String formattedVal = String.format("%.0f", val);
                    if (report.contains(formattedVal) || report.contains(String.valueOf(val))) {
                        audits.add(new ClaimAudit(
                                "Evidence match found in report",
                                record.ticker(),
                                record.field(),
                                val,
                                ClaimType.RETRIEVED_FACT,
                                true,
                                Optional.of(record),
                                "Directly supported by " + record.sourceReference()
                        ));
                    }
                }
            }
        }

        return audits;
    }

    private boolean isCompatibleField(String claimField, String evidenceField) {
        if (claimField.equalsIgnoreCase(evidenceField)) {
            return true;
        }
        String c = claimField.toLowerCase();
        String e = evidenceField.toLowerCase();
        if ((c.contains("price") || c.equals("current_price") || c.equals("buy_price"))
                && (e.contains("price") || e.equals("current_price") || e.equals("buy_price") || e.equals("regularmarketprice"))) {
            return true;
        }
        if ((c.contains("pnl") || c.contains("profit") || c.contains("gain"))
                && (e.contains("pnl") || e.contains("profit") || e.contains("gain") || e.equals("unrealized_pnl"))) {
            return true;
        }
        if ((c.contains("pe") || c.contains("p/e") || c.contains("trailingpe"))
                && (e.contains("pe") || e.contains("p/e") || e.contains("trailingpe"))) {
            return true;
        }
        if ((c.contains("weight") || c.contains("allocation"))
                && (e.contains("weight") || e.contains("allocation"))) {
            return true;
        }
        return false;
    }

    private ClaimAudit verifyClaim(String sentence, String ticker, String field, double claimedVal, List<EvidenceRecord> evidence) {
        // 1. Search evidence for matching ticker and compatible field that satisfies tolerance
        EvidenceRecord contradictoryRecord = null;

        for (EvidenceRecord rec : evidence) {
            boolean tickerMatch = ticker.isBlank() || rec.ticker().equalsIgnoreCase(ticker)
                    || rec.ticker().startsWith(ticker) || ticker.startsWith(rec.ticker());

            if (tickerMatch && isCompatibleField(field, rec.field()) && rec.numericValue().isPresent()) {
                double evVal = rec.numericValue().get();
                double diffPct = Math.abs(claimedVal - evVal) / (evVal == 0 ? 1.0 : Math.abs(evVal));

                if (diffPct <= tolerancePct) {
                    ClaimType type = "PORTFOLIO_MATH".equalsIgnoreCase(rec.source())
                            ? ClaimType.DERIVED_CALCULATION
                            : ClaimType.RETRIEVED_FACT;

                    return new ClaimAudit(
                            sentence,
                            rec.ticker(),
                            field,
                            claimedVal,
                            type,
                            true,
                            Optional.of(rec),
                            String.format("Claimed %.2f supported by %s (observed: %.2f)", claimedVal, rec.sourceReference(), evVal)
                    );
                } else {
                    contradictoryRecord = rec;
                }
            }
        }

        // If a contradictory record was found for this compatible field
        if (contradictoryRecord != null) {
            double evVal = contradictoryRecord.numericValue().get();
            return new ClaimAudit(
                    sentence,
                    contradictoryRecord.ticker(),
                    field,
                    claimedVal,
                    ClaimType.UNSUPPORTED_CLAIM,
                    false,
                    Optional.of(contradictoryRecord),
                    String.format("Claimed %s %.2f but available evidence (%s) shows %.2f",
                            field, claimedVal, contradictoryRecord.sourceReference(), evVal)
            );
        }

        // Check if sentence is a forward-looking scenario projection / model interpretation
        String sLower = sentence.toLowerCase();
        if (sLower.contains("target") || sLower.contains("projection") || sLower.contains("forecast")
                || sLower.contains("assumptions") || sLower.contains("scenario") || sLower.contains("drawdown")
                || sLower.contains("fair value")) {
            return new ClaimAudit(
                    sentence,
                    ticker,
                    field,
                    claimedVal,
                    ClaimType.MODEL_INTERPRETATION,
                    true,
                    Optional.empty(),
                    "Forward-looking scenario projection / model interpretation"
            );
        }

        // If no evidence found for this ticker/field
        return new ClaimAudit(
                sentence,
                ticker,
                field,
                claimedVal,
                ClaimType.UNSUPPORTED_CLAIM,
                false,
                Optional.empty(),
                String.format("Claimed %s %.2f for '%s' without supporting evidence", field, claimedVal, ticker.isBlank() ? "asset" : ticker)
        );
    }

    private String extractTicker(String text, List<EvidenceRecord> evidence) {
        String upper = text.toUpperCase();
        for (EvidenceRecord rec : evidence) {
            if (!rec.ticker().isBlank()) {
                String t = rec.ticker();
                String raw = t.contains(".") ? t.substring(0, t.indexOf('.')) : t;
                if (upper.contains(t) || upper.contains(raw)) {
                    return t;
                }
            }
        }
        // Common known tickers
        for (String common : List.of("INFY", "RELIANCE", "TCS", "HDFC", "AAPL", "MSFT", "GOOGL")) {
            if (upper.contains(common)) return common;
        }
        return "";
    }

    private double parseNumeric(String str) {
        if (str == null) return 0.0;
        try {
            return Double.parseDouble(str.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
