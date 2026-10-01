package com.google.adk.finance.v8;

import com.google.adk.finance.v8.guardrails.GuardrailResult;
import com.google.adk.finance.v8.guardrails.output.OffensiveLanguageDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OffensiveLanguageDetectorTest {

    private OffensiveLanguageDetector detector;

    @BeforeEach
    void setUp() {
        detector = new OffensiveLanguageDetector();
    }

    @Test
    @DisplayName("Professional financial research responses should be ALLOWED")
    void testCleanResponseAllowed() {
        String clean = "Infosys announced Q3 revenue growth of 4.2% YoY, supported by steady deal momentum in cloud services.";
        GuardrailResult result = detector.evaluate(clean);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.status()).isEqualTo(GuardrailResult.Status.ALLOW);
    }

    @Test
    @DisplayName("Profane or abusive responses must be BLOCKED with safe fallback")
    void testProfanityBlocked() {
        String toxic = "This investment strategy is fucking stupid and you are a complete idiot.";
        GuardrailResult result = detector.evaluate(toxic);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.status()).isEqualTo(GuardrailResult.Status.BLOCK);
        assertThat(result.metadata()).containsKey("safeFallback");
        assertThat((String) result.metadata().get("safeFallback")).contains("violates our professional communication policy");
    }

    @Test
    @DisplayName("Empty or null text should be handled gracefully")
    void testEmptyResponse() {
        assertThat(detector.evaluate("").isAllowed()).isTrue();
        assertThat(detector.evaluate(null).isAllowed()).isTrue();
    }
}
