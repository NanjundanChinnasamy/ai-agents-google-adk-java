package com.google.adk.finance.v9;

import com.google.adk.finance.v9.evaluation.EvaluationCriteria;
import com.google.adk.finance.v9.evaluation.EvaluationResult;
import com.google.adk.finance.v9.evaluation.EvidenceRecord;
import com.google.adk.finance.v9.evaluation.EvidenceStoreV9;
import com.google.adk.finance.v9.evaluation.FaithfulnessEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V9 Evaluation Criterion 1: Faithfulness / Groundedness Tests")
public class FaithfulnessEvaluationTest {

    private FaithfulnessEvaluator evaluator;
    private EvidenceStoreV9 evidenceStore;

    @BeforeEach
    void setUp() {
        evaluator = new FaithfulnessEvaluator();
        evidenceStore = new EvidenceStoreV9();

        // Seed empirical evidence from Yahoo Finance MCP and Search
        evidenceStore.recordFact(
                "YAHOO_FINANCE_MCP",
                "get_stock_info",
                "INFY.NS",
                "price",
                1520.0,
                "get_stock_info(INFY.NS)",
                "{\"symbol\":\"INFY.NS\",\"currentPrice\":1520.0}"
        );
        evidenceStore.recordFact(
                "YAHOO_FINANCE_MCP",
                "get_stock_info",
                "INFY.NS",
                "pe_ratio",
                24.5,
                "get_stock_info(INFY.NS)",
                "{\"pe_ratio\":24.5}"
        );
    }

    @Test
    @DisplayName("Should PASS faithfulness when claimed price matches verified evidence exactly")
    void shouldPassFaithfulnessWhenClaimsMatchEvidence() {
        String report = """
                Infosys Investment Summary:
                Infosys (INFY.NS) is currently trading at ₹1,520 with a trailing P/E of 24.5.
                Operations remain solid with stable margin performance.
                """;

        EvaluationResult result = evaluator.evaluate("TEST-INFY-PASS", report, evidenceStore);

        assertThat(result.status()).isEqualTo(EvaluationResult.Status.PASS);
        assertThat(result.isPass()).isTrue();
        assertThat(result.criterion()).isEqualTo(EvaluationCriteria.FAITHFULNESS);
        assertThat(result.explanation()).contains("Supported claims: 2");
        assertThat(result.explanation()).contains("Unsupported claims: 0");
    }

    @Test
    @DisplayName("Should FAIL faithfulness when report asserts an unsupported or fabricated price")
    void shouldFailFaithfulnessWhenClaimIsUnsupported() {
        // Evidence says ₹1520, but report claims ₹1850
        String report = """
                Infosys Investment Summary:
                Infosys (INFY.NS) is currently trading at ₹1,850 on the exchange.
                P/E is 24.5.
                """;

        EvaluationResult result = evaluator.evaluate("TEST-INFY-FAIL", report, evidenceStore);

        assertThat(result.status()).isEqualTo(EvaluationResult.Status.FAIL);
        assertThat(result.isFail()).isTrue();
        assertThat(result.failureReason()).isPresent();
        assertThat(result.failureReason().get())
                .contains("Claimed price 1850.00 but available evidence")
                .contains("shows 1520.00");
    }

    @Test
    @DisplayName("Should PASS faithfulness with reasonable tolerance for currency formatting")
    void shouldPassFaithfulnessWithFormattingTolerance() {
        String report = "Infosys is trading around 1,520 INR with strong balance sheet.";

        EvaluationResult result = evaluator.evaluate("TEST-INFY-FMT", report, evidenceStore);

        assertThat(result.status()).isEqualTo(EvaluationResult.Status.PASS);
        assertThat(result.isPass()).isTrue();
    }

    @Test
    @DisplayName("Should FAIL faithfulness when report is completely empty or blank")
    void shouldFailFaithfulnessWhenReportIsEmpty() {
        EvaluationResult result = evaluator.evaluate("TEST-EMPTY", "   ", evidenceStore);

        assertThat(result.status()).isEqualTo(EvaluationResult.Status.FAIL);
        assertThat(result.failureReason()).contains("Report was empty");
    }

    @Test
    @DisplayName("Should distinguish derived calculations from raw retrieved facts")
    void shouldDistinguishDerivedCalculationsFromRawFacts() {
        EvidenceRecord calcRecord = EvidenceRecord.of(
                "PORTFOLIO_MATH", "portfolio_math", "INFY.NS", "pnl", 500.0, "PortfolioMathTool.calculate_pnl", "pnl: 500.0"
        );

        List<FaithfulnessEvaluator.ClaimAudit> audits = evaluator.auditReport(
                "Infosys PnL is ₹500.",
                List.of(calcRecord)
        );

        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).type()).isEqualTo(FaithfulnessEvaluator.ClaimType.DERIVED_CALCULATION);
        assertThat(audits.get(0).isSupported()).isTrue();
    }
}
