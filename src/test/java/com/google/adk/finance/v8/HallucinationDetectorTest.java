package com.google.adk.finance.v8;

import com.google.adk.finance.v8.evidence.EvidenceStore;
import com.google.adk.finance.v8.guardrails.GuardrailResult;
import com.google.adk.finance.v8.guardrails.output.HallucinationDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HallucinationDetectorTest {

    private EvidenceStore evidenceStore;
    private HallucinationDetector detector;

    @BeforeEach
    void setUp() {
        evidenceStore = new EvidenceStore();
        evidenceStore.recordStockFact("INFY.NS", 1500.0, "INR", Map.of("peRatio", 25.4), "get_stock_info");
        evidenceStore.recordStockFact("RELIANCE.NS", 2900.0, "INR", Map.of(), "get_stock_info");
        detector = new HallucinationDetector(evidenceStore, 0.01);
    }

    @Test
    @DisplayName("Factually consistent statement matching EvidenceStore price should PASS")
    void testGroundedPricePasses() {
        String claim = "Infosys is currently trading at ₹1,500 in the morning session.";
        GuardrailResult result = detector.evaluate(claim);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.status()).isEqualTo(GuardrailResult.Status.ALLOW);
        assertThat(result.reason()).contains("verified against empirical tool evidence");
    }

    @Test
    @DisplayName("Fabricated or hallucinated price diverging from EvidenceStore must be BLOCKED with audit warning")
    void testHallucinatedPriceBlocked() {
        String hallucination = "Infosys is currently trading at ₹1,850 in the morning session.";
        GuardrailResult result = detector.evaluate(hallucination);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.status()).isEqualTo(GuardrailResult.Status.BLOCK);
        assertThat(result.reason()).contains("Numerical hallucination or discrepancy detected");
        assertThat(result.metadata()).containsKey("alertWarning");
        assertThat((String) result.metadata().get("alertWarning")).contains("Factual Audit Alert");
        assertThat((String) result.metadata().get("alertWarning")).contains("INFY.NS");
    }

    @Test
    @DisplayName("Minor rounding discrepancy within 1.0% tolerance margin should PASS")
    void testPriceWithinTolerancePasses() {
        // 1505 is within 0.33% of 1500, less than 1.0%
        String nearClaim = "Infosys price is 1505.";
        GuardrailResult result = detector.evaluate(nearClaim);

        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("Statements without specific numerical price assertions should pass as ALLOW")
    void testGeneralUnrelatedTextAllowed() {
        String general = "Reliance Industries showed strong subscriber additions in its telecom vertical.";
        GuardrailResult result = detector.evaluate(general);

        assertThat(result.isAllowed()).isTrue();
    }
}
