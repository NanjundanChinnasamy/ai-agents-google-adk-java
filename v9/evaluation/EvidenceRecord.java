package com.google.adk.finance.v9.evaluation;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Institutional representation of an empirical evidence record retrieved from tools.
 * <p>
 * Supports tracing factual assertions back to their original deterministic tool outputs:
 * <ul>
 *   <li><b>GOOGLE_SEARCH:</b> Live news snippets, SEC filings, earnings releases.</li>
 *   <li><b>YAHOO_FINANCE_MCP:</b> Real-time market quotes, P/E ratios, market cap, EPS.</li>
 *   <li><b>PORTFOLIO_MATH:</b> Deterministic PnL, cost basis, allocation weights.</li>
 *   <li><b>CUSTOMER_PORTFOLIO_DB:</b> Verified portfolio holdings, purchase dates, buy prices.</li>
 * </ul>
 */
public record EvidenceRecord(
        String source,
        String tool,
        String ticker,
        String field,
        String value,
        Optional<Double> numericValue,
        Instant timestamp,
        String sourceReference,
        String originalRetrievedContent
) {
    public EvidenceRecord {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(tool, "tool must not be null");
        ticker = ticker == null ? "" : ticker.trim().toUpperCase();
        field = field == null ? "" : field.trim().toLowerCase();
        value = value == null ? "" : value.trim();
        numericValue = numericValue == null ? Optional.empty() : numericValue;
        timestamp = timestamp == null ? Instant.now() : timestamp;
        sourceReference = sourceReference == null ? tool : sourceReference;
        originalRetrievedContent = originalRetrievedContent == null ? value : originalRetrievedContent;
    }

    public static EvidenceRecord of(String source, String tool, String ticker, String field, double numericValue, String reference, String rawContent) {
        return new EvidenceRecord(
                source,
                tool,
                ticker,
                field,
                String.valueOf(numericValue),
                Optional.of(numericValue),
                Instant.now(),
                reference,
                rawContent
        );
    }

    public static EvidenceRecord ofText(String source, String tool, String ticker, String field, String textValue, String reference, String rawContent) {
        Optional<Double> parsed = tryParseDouble(textValue);
        return new EvidenceRecord(
                source,
                tool,
                ticker,
                field,
                textValue,
                parsed,
                Instant.now(),
                reference,
                rawContent
        );
    }

    private static Optional<Double> tryParseDouble(String val) {
        if (val == null || val.isBlank()) return Optional.empty();
        try {
            String cleaned = val.replaceAll("[^0-9.-]", "");
            if (cleaned.isBlank() || cleaned.equals("-") || cleaned.equals(".")) return Optional.empty();
            return Optional.of(Double.parseDouble(cleaned));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
