package com.google.adk.finance.v8.guardrails.input;

import com.google.adk.finance.v8.guardrails.GuardrailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic pattern-based PII (Personally Identifiable Information) detector.
 * <p>
 * Scans user inputs and external payloads for sensitive identifiers relevant to financial advisory:
 * <ul>
 *   <li>Bank account numbers (e.g. 9-18 digit accounts, IBANs)</li>
 *   <li>Payment card numbers (13-19 digits, Visa, Mastercard, Amex patterns)</li>
 *   <li>Phone numbers (international and local formats)</li>
 *   <li>Email addresses</li>
 *   <li>Government / Tax identifiers (SSN: {@code XXX-XX-XXXX}, Indian PAN: {@code [A-Z]{5}[0-9]{4}[A-Z]}, Aadhaar)</li>
 *   <li>Customer / Portfolio account tokens (e.g. "Customer ID: 1001", "Account #123456")</li>
 *   <li>Personal name introductions (e.g. "My name is John Smith")</li>
 * </ul>
 * <p>
 * <b>Architectural Limitations of Regex/Pattern-Based PII Detection:</b>
 * <ol>
 *   <li><b>Context Blindness:</b> RegEx cannot reliably distinguish between a 10-digit phone number, an Indian mobile number,
 *       and a 10-digit transaction volume or company market cap figure without surrounding contextual heuristics.</li>
 *   <li><b>Name Detection Ambiguity:</b> Natural language names (e.g. "Tata", "Ford", "Infosys") can collide with company names,
 *       requiring NER (Named Entity Recognition) models in production rather than simple keyword templates.</li>
 *   <li><b>Evasion / Obfuscation:</b> Adversaries can space out numbers ("1 2 3 4 - 5 6 7 8") or use homoglyphs to bypass regex filters.</li>
 * </ol>
 * <i>Security Invariant: Raw PII is NEVER logged to stdout, logs, or debugging facilities.</i>
 */
public class PiiDetector {
    private static final Logger logger = LoggerFactory.getLogger(PiiDetector.class);

    public record PiiMatch(String type, int start, int end, String maskedPreview, String redactionToken) {}

    // 1. Credit / Debit card: 13-19 digits, separated by hyphens, spaces, or contiguous
    private static final Pattern CARD_PATTERN = Pattern.compile(
            "\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12}|(?:[0-9]{4}[-\\s]){3}[0-9]{4})\\b"
    );

    // 2. Bank account: preceded by account keywords, 9-18 digits OR international IBAN
    private static final Pattern BANK_ACCOUNT_PATTERN = Pattern.compile(
            "(?i)\\b(?:account(?:\\s+number|\\s+no|\\s+#)?|a/c(?:\\s+no|\\s+#)?|acct)\\s*(?:is|was|:|=)?\\s*([0-9][0-9\\-\\s]{7,18}[0-9])\\b|" +
            "\\b([A-Z]{2}[0-9]{2}[A-Z0-9]{11,30})\\b"
    );

    // 3. Email address
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    );

    // 4. Phone number: international or standard 10-digit formats
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?i)\\b(?:phone|mobile|tel|contact)?\\s*[:=]?\\s*(?:\\+\\d{1,3}[-\\s.]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b|" +
            "\\b(?:\\+91[-\\s]?)?[6789]\\d{9}\\b"
    );

    // 5. SSN (Social Security Number)
    private static final Pattern SSN_PATTERN = Pattern.compile(
            "\\b(?!000|666|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0000)\\d{4}\\b"
    );

    // 6. Tax / Government ID: Indian PAN ([A-Z]{5}[0-9]{4}[A-Z]) & Aadhaar (\d{4}\s\d{4}\s\d{4})
    private static final Pattern PAN_PATTERN = Pattern.compile(
            "\\b[A-Z]{5}[0-9]{4}[A-Z]\\b"
    );
    private static final Pattern AADHAAR_PATTERN = Pattern.compile(
            "\\b[2-9]{1}[0-9]{3}\\s[0-9]{4}\\s[0-9]{4}\\b"
    );

    // 7. Customer / Account ID token
    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile(
            "(?i)\\b(?:customer|client|portfolio)\\s*(?:id|#|no\\.?)\\s*[:=]?\\s*([0-9]{4,10})\\b"
    );

    // 8. Personal Name Introductions (e.g. "My name is John Smith", "I am Alice Cooper")
    private static final Pattern NAME_INTRODUCTION_PATTERN = Pattern.compile(
            "(?i)\\b(?:my name is|i am)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+)\\b"
    );

    public PiiDetector() {}

    /**
     * Scans the input text for all known PII patterns.
     *
     * @param input the raw user text
     * @return list of detected {@link PiiMatch} entries
     */
    public List<PiiMatch> detectMatches(String input) {
        if (input == null || input.isBlank()) {
            return Collections.emptyList();
        }

        List<PiiMatch> matches = new ArrayList<>();

        findMatches(matches, CARD_PATTERN.matcher(input), "CREDIT_CARD", "[REDACTED_CARD]");
        findMatches(matches, BANK_ACCOUNT_PATTERN.matcher(input), "BANK_ACCOUNT", "[REDACTED_ACCOUNT]");
        findMatches(matches, EMAIL_PATTERN.matcher(input), "EMAIL", "[REDACTED_EMAIL]");
        findMatches(matches, PHONE_PATTERN.matcher(input), "PHONE", "[REDACTED_PHONE]");
        findMatches(matches, SSN_PATTERN.matcher(input), "SSN", "[REDACTED_SSN]");
        findMatches(matches, PAN_PATTERN.matcher(input), "TAX_PAN", "[REDACTED_PAN]");
        findMatches(matches, AADHAAR_PATTERN.matcher(input), "GOV_AADHAAR", "[REDACTED_AADHAAR]");
        findMatches(matches, CUSTOMER_ID_PATTERN.matcher(input), "CUSTOMER_ID", "[REDACTED_CUSTOMER_ID]");
        findMatches(matches, NAME_INTRODUCTION_PATTERN.matcher(input), "NAME", "[REDACTED_NAME]");

        return matches;
    }

    /**
     * Evaluates the input string against PII guardrails.
     *
     * @param input the raw text
     * @return {@link GuardrailResult#allow(String)} if clean, or {@link GuardrailResult#warn(String, String)} if PII found
     */
    public GuardrailResult evaluate(String input) {
        List<PiiMatch> matches = detectMatches(input);
        if (matches.isEmpty()) {
            return GuardrailResult.allow("PiiDetector", "No sensitive PII detected.");
        }

        logger.info("[PII Guardrail] Detected {} sensitive identifier(s). Safe sanitization recommended.", matches.size());
        return GuardrailResult.warn(
                "PiiDetector",
                "Input contains " + matches.size() + " sensitive PII token(s) requiring redaction."
        );
    }

    private void findMatches(List<PiiMatch> target, Matcher matcher, String type, String redactionToken) {
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String raw = matcher.group();
            // Create a safe non-reversible preview (e.g. length and asterisks)
            String preview = maskPreview(raw);
            target.add(new PiiMatch(type, start, end, preview, redactionToken));
        }
    }

    private String maskPreview(String raw) {
        if (raw.length() <= 4) {
            return "****";
        }
        return "****" + raw.substring(raw.length() - 4);
    }
}
