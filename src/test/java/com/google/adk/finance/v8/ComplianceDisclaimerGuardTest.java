package com.google.adk.finance.v8;

import com.google.adk.finance.v8.guardrails.GuardrailResult;
import com.google.adk.finance.v8.guardrails.output.ComplianceDisclaimerGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceDisclaimerGuardTest {

    private ComplianceDisclaimerGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ComplianceDisclaimerGuard();
    }

    @Test
    @DisplayName("Response already carrying regulatory disclaimer should be ALLOWED without duplicate injection")
    void testDisclaimerAlreadyPresent() {
        String compliant = "Infosys is trading at ₹1,500.\n\n---\n**Regulatory Disclaimer**: This report is for educational purposes and not financial advice.";
        GuardrailResult result = guard.enforce(compliant);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.status()).isEqualTo(GuardrailResult.Status.ALLOW);
    }

    @Test
    @DisplayName("Response lacking disclaimer must have mandatory disclaimer appended (SANITIZE)")
    void testDisclaimerAppendedWhenMissing() {
        String nonCompliant = "Infosys has strong growth momentum and we recommend overweight exposure.";
        GuardrailResult result = guard.enforce(nonCompliant);

        assertThat(result.isSanitized()).isTrue();
        assertThat(result.sanitizedValue()).isPresent();
        String output = result.sanitizedValue().get();
        assertThat(output).contains(nonCompliant);
        assertThat(output).contains("Regulatory Disclaimer");
        assertThat(output).contains("not constitute certified financial");
    }

    @Test
    @DisplayName("Empty text should still receive mandatory disclaimer")
    void testEmptyTextReceivesDisclaimer() {
        GuardrailResult result = guard.enforce("");
        assertThat(result.sanitizedValue()).isPresent();
        assertThat(result.sanitizedValue().get()).contains("Regulatory Disclaimer");
    }
}
