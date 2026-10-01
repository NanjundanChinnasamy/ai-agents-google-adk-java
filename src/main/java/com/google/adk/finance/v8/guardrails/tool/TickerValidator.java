package com.google.adk.finance.v8.guardrails.tool;

import com.google.adk.finance.v8.guardrails.GuardrailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deterministic validator for financial ticker symbols before external MCP / market data tool invocation.
 * <p>
 * Ensures symbol requests adhere to expected exchange notations (e.g. NSE: {@code .NS}, BSE: {@code .BO}, US: {@code AAPL},
 * Indices: {@code ^NSEI}) and immediately rejects malicious, malformed, or out-of-scope strings before network execution.
 */
public class TickerValidator {
    private static final Logger logger = LoggerFactory.getLogger(TickerValidator.class);

    // Standard ticker pattern: 1-12 alphanumeric characters, optional exchange suffix (e.g. .NS, .BO), or index prefix (^)
    private static final Pattern VALID_TICKER_PATTERN = Pattern.compile(
            "^[\\^]?[A-Za-z0-9]{1,10}(?:[\\.\\-][A-Za-z0-9]{1,5})?$"
    );

    // Malicious injection patterns: SQL quotes, comments, semicolons, shell piping, path traversal
    private static final Pattern MALICIOUS_PAYLOAD_PATTERN = Pattern.compile(
            "['\";|&<>`\\\\/\\*\\$]|--|\\b(?:DROP|SELECT|INSERT|DELETE|UNION|EXEC)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Known universe of frequently analyzed Indian & global tickers for additional advisory validation
    public static final Set<String> COMMON_INDIAN_TICKERS = Set.of(
            "INFY.NS", "RELIANCE.NS", "TCS.NS", "HDFCBANK.NS", "ICICIBANK.NS",
            "SBIN.NS", "BHARTIARTL.NS", "ITC.NS", "LT.NS", "KOTAKBANK.NS",
            "INFY.BO", "RELIANCE.BO", "TCS.BO", "HDFCBANK.BO"
    );

    public TickerValidator() {}

    /**
     * Validates whether a ticker symbol is syntactically sound and safe to pass to external MCP tools.
     *
     * @param ticker the ticker string
     * @return {@link GuardrailResult#allow} if valid, or {@link GuardrailResult#block} if invalid or dangerous
     */
    public GuardrailResult evaluate(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            return GuardrailResult.block(
                    "TickerValidator",
                    "Ticker symbol must not be empty or blank."
            );
        }

        String trimmed = ticker.trim();

        // 1. Malicious payload detection (SQL injection, shell injection, script tags)
        if (MALICIOUS_PAYLOAD_PATTERN.matcher(trimmed).find()) {
            logger.warn("[Tool Guardrail] Rejected suspicious ticker payload: '{}'", trimmed);
            return GuardrailResult.block(
                    "TickerValidator",
                    "Security Violation: Ticker symbol contains prohibited characters or injection syntax: '" + trimmed + "'."
            );
        }

        // 2. Length check: tickers rarely exceed 15 chars (e.g. "HDFCBANK.NS" is 11)
        if (trimmed.length() > 15) {
            return GuardrailResult.block(
                    "TickerValidator",
                    "Ticker symbol length (" + trimmed.length() + ") exceeds maximum permitted limit of 15 characters."
            );
        }

        // 3. Format syntax check
        if (!VALID_TICKER_PATTERN.matcher(trimmed).matches()) {
            return GuardrailResult.block(
                    "TickerValidator",
                    "Malformed ticker symbol: '" + trimmed + "'. Expected format: standard exchange symbol (e.g. INFY.NS, RELIANCE.NS, AAPL)."
            );
        }

        // Normalized uppercase ticker
        String normalized = trimmed.toUpperCase();
        return GuardrailResult.allow(
                "TickerValidator",
                "Ticker '" + normalized + "' is valid and authorized for market lookup.",
                Map.of("normalizedTicker", normalized, "isKnownUniverse", COMMON_INDIAN_TICKERS.contains(normalized))
        );
    }

    /**
     * Normalizes a ticker string to uppercase standard format if valid.
     */
    public String normalize(String ticker) {
        GuardrailResult result = evaluate(ticker);
        if (result.isBlocked()) {
            throw new IllegalArgumentException(result.reason());
        }
        return ticker.trim().toUpperCase();
    }
}
