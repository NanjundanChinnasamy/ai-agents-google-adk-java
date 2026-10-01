package com.google.adk.finance.v8;

import com.google.adk.finance.v8.guardrails.GuardrailResult;
import com.google.adk.finance.v8.guardrails.input.PiiDetector;
import com.google.adk.finance.v8.guardrails.input.PiiSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PiiDetectorAndSanitizerTest {

    private PiiDetector detector;
    private PiiSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        detector = new PiiDetector();
        sanitizer = new PiiSanitizer(detector);
    }

    @Test
    @DisplayName("Clean text without PII should pass detector as ALLOW and remain untouched")
    void testCleanText() {
        String clean = "What are the latest revenue numbers and operating margins for Infosys?";
        GuardrailResult eval = detector.evaluate(clean);
        assertThat(eval.isAllowed()).isTrue();
        assertThat(eval.status()).isEqualTo(GuardrailResult.Status.ALLOW);

        String result = sanitizer.sanitize(clean);
        assertThat(result).isEqualTo(clean);
    }

    @Test
    @DisplayName("Canonical example: Name and bank account number redaction")
    void testCanonicalExample() {
        String input = "My name is John Smith and my account number is 1234567890. What is the latest Infosys news?";
        GuardrailResult eval = detector.evaluate(input);
        assertThat(eval.status()).isEqualTo(GuardrailResult.Status.WARN);

        String sanitized = sanitizer.sanitize(input);
        assertThat(sanitized).contains("[REDACTED_NAME]");
        assertThat(sanitized).contains("[REDACTED_ACCOUNT]");
        assertThat(sanitized).doesNotContain("John Smith");
        assertThat(sanitized).doesNotContain("1234567890");
        assertThat(sanitized).contains("What is the latest Infosys news?");
    }

    @Test
    @DisplayName("Detects and redacts credit card numbers")
    void testCreditCardRedaction() {
        String input = "Please charge my Visa card 4532-1234-5678-9012 for the premium investment report.";
        List<PiiDetector.PiiMatch> matches = detector.detectMatches(input);
        assertThat(matches).isNotEmpty();
        assertThat(matches.get(0).type()).isEqualTo("CREDIT_CARD");

        String sanitized = sanitizer.sanitize(input);
        assertThat(sanitized).contains("[REDACTED_CARD]");
        assertThat(sanitized).doesNotContain("4532-1234-5678-9012");
    }

    @Test
    @DisplayName("Detects and redacts email addresses and phone numbers")
    void testEmailAndPhoneRedaction() {
        String input = "Send the portfolio review to analyst@hedgefund.com or call me at +91-9876543210 immediately.";
        String sanitized = sanitizer.sanitize(input);
        assertThat(sanitized).contains("[REDACTED_EMAIL]");
        assertThat(sanitized).contains("[REDACTED_PHONE]");
        assertThat(sanitized).doesNotContain("analyst@hedgefund.com");
        assertThat(sanitized).doesNotContain("9876543210");
    }

    @Test
    @DisplayName("Detects and redacts tax and government identifiers (PAN and SSN)")
    void testGovernmentIdentifiers() {
        String panInput = "My Indian tax PAN is ABCDE1234F.";
        assertThat(sanitizer.sanitize(panInput)).contains("[REDACTED_PAN]").doesNotContain("ABCDE1234F");

        String ssnInput = "My SSN is 123-45-6789.";
        assertThat(sanitizer.sanitize(ssnInput)).contains("[REDACTED_SSN]").doesNotContain("123-45-6789");
    }

    @Test
    @DisplayName("evaluateAndSanitize returns structured SANITIZE result")
    void testEvaluateAndSanitizeResult() {
        String input = "Contact support@invest.com regarding account 987654321012.";
        GuardrailResult result = sanitizer.evaluateAndSanitize(input);
        assertThat(result.isSanitized()).isTrue();
        assertThat(result.sanitizedValue()).isPresent();
        assertThat(result.sanitizedValue().get()).contains("[REDACTED_EMAIL]");
        assertThat(result.metadata()).containsKey("redactedCount");
    }
}
