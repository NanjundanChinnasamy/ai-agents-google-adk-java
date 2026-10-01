package com.google.adk.finance.v9;

import com.google.adk.finance.v9.evaluation.EvaluationCriteria;
import com.google.adk.finance.v9.evaluation.EvaluationReport;
import com.google.adk.finance.v9.evaluation.EvaluationResult;
import com.google.adk.finance.v9.evaluation.EvidenceRecord;
import com.google.adk.finance.v9.evaluation.LlmJudgeEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V9 Evaluation: LLM-as-a-Judge Qualitative Tests")
public class LlmJudgeEvaluationTest {

    private LlmJudgeEvaluator judgeEvaluator;
    private List<EvidenceRecord> sampleEvidence;

    @BeforeEach
    void setUp() {
        // Instantiate judge in deterministic offline mode
        judgeEvaluator = new LlmJudgeEvaluator(null);

        sampleEvidence = List.of(
                EvidenceRecord.of("YAHOO_FINANCE_MCP", "get_stock_info", "INFY.NS", "price", 1520.0, "get_stock_info(INFY.NS)", "price: 1520"),
                EvidenceRecord.of("YAHOO_FINANCE_MCP", "get_stock_info", "INFY.NS", "pe_ratio", 24.5, "get_stock_info(INFY.NS)", "pe: 24.5")
        );
    }

    @Test
    @DisplayName("Should PASS LLM Judge on qualitative dimensions when evidence is cited and uncertainty is transparent")
    void shouldEvaluateQualitativeDimensionsWithLlmJudge() {
        String report = """
                Infosys (INFY.NS) Investment Overview:
                Empirical market data indicates a current trading valuation of ₹1,520 and a P/E multiple of 24.5.
                The corporate growth thesis is supported by large multi-year cloud transformation deals.
                However, forward financial forecasts are subject to macroeconomic volatility, interest rate fluctuations,
                and potential client IT budget revisions.
                
                Disclaimer: Educational and informational decision-support purposes only. Not financial advice.
                """;

        EvaluationResult result = judgeEvaluator.evaluate(
                "TEST-JUDGE-PASS",
                "Research Infosys valuation and risk factors",
                report,
                sampleEvidence
        );

        assertThat(result.status()).isEqualTo(EvaluationResult.Status.PASS);
        assertThat(result.criterion()).isEqualTo(EvaluationCriteria.LLM_JUDGE);
        assertThat(result.metadata().get("evidenceUsage")).isEqualTo("Good");
        assertThat(result.metadata().get("uncertaintyHandling")).isEqualTo("Appropriate");
        assertThat(result.metadata().get("explanationQuality")).isEqualTo("Clear");
    }

    @Test
    @DisplayName("Should WARN or flag deficiency when uncertainty and market risks are omitted")
    void shouldWarnWhenUncertaintyIsMissing() {
        String overconfidentReport = """
                Infosys is trading at ₹1,520 with P/E of 24.5.
                The company is guaranteed to grow 20% every year with zero downside.
                Invest all your capital immediately for maximum guaranteed returns.
                """;

        EvaluationResult result = judgeEvaluator.evaluate(
                "TEST-JUDGE-OVERCONFIDENT",
                "What is the outlook for Infosys?",
                overconfidentReport,
                sampleEvidence
        );

        assertThat(result.status()).isEqualTo(EvaluationResult.Status.WARN);
        assertThat(result.metadata().get("uncertaintyHandling")).isEqualTo("Deficient");
    }

    @Test
    @DisplayName("Architectural Invariant: LLM Judge must NEVER override a deterministic calculation or evidence failure")
    void shouldNeverOverrideDeterministicMathFailures() {
        // Create an evaluation report where calculation fidelity failed
        EvaluationReport report = new EvaluationReport("TEST-INVARIANT");

        EvaluationResult mathFailure = EvaluationResult.fail(
                "TEST-INVARIANT",
                EvaluationCriteria.CALCULATION_FIDELITY,
                "Expected PnL: £1,250",
                "Actual PnL: £1,500",
                "Deterministic arithmetic mismatch"
        );
        report.addResult(mathFailure);

        // Even if LLM Judge passes qualitatively:
        EvaluationResult judgeSuccess = EvaluationResult.pass(
                "TEST-INVARIANT",
                EvaluationCriteria.LLM_JUDGE,
                "Explanation is clear and fluent"
        );
        report.addResult(judgeSuccess);

        // Overall status MUST remain FAIL
        assertThat(report.overallStatus()).isEqualTo(EvaluationResult.Status.FAIL);
        assertThat(report.isAllPass()).isFalse();
        assertThat(report.getResult(EvaluationCriteria.CALCULATION_FIDELITY).get().status())
                .isEqualTo(EvaluationResult.Status.FAIL);
    }
}
