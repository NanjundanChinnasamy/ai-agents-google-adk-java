package com.google.adk.finance.v9;

import com.google.adk.finance.v9.evaluation.EvaluationCase;
import com.google.adk.finance.v9.evaluation.EvaluationCriteria;
import com.google.adk.finance.v9.evaluation.EvaluationDatasetLoader;
import com.google.adk.finance.v9.evaluation.EvaluationReport;
import com.google.adk.finance.v9.evaluation.EvaluationResult;
import com.google.adk.finance.v9.evaluation.EvaluationRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V9 End-to-End Evaluation Runner & Golden Dataset Benchmark Tests")
public class FinanceAdvisorV9EvaluationTest {

    private EvaluationRunner evaluationRunner;
    private List<EvaluationCase> goldenCases;

    @BeforeEach
    void setUp() {
        evaluationRunner = new EvaluationRunner();
        goldenCases = EvaluationDatasetLoader.loadFinanceEvaluationCases();
    }

    @Test
    @DisplayName("Should evaluate INFY-RESEARCH-001 with PASS across all relevant criteria")
    void shouldEvaluateNormalResearchCaseSuccessfully() {
        EvaluationCase infyCase = goldenCases.stream()
                .filter(c -> c.id().equals("INFY-RESEARCH-001"))
                .findFirst()
                .orElseThrow();

        EvaluationReport report = evaluationRunner.evaluate(infyCase);

        assertThat(report.isAllPass()).isTrue();
        assertThat(report.overallStatus()).isEqualTo(EvaluationResult.Status.PASS);

        EvaluationResult faith = report.getResult(EvaluationCriteria.FAITHFULNESS).orElseThrow();
        assertThat(faith.isPass()).isTrue();
        assertThat(faith.actual()).contains("All 2 verified claims are grounded in tool evidence");

        EvaluationResult judge = report.getResult(EvaluationCriteria.LLM_JUDGE).orElseThrow();
        assertThat(judge.isPass()).isTrue();

        String formatted = report.toFormattedReport();
        assertThat(formatted).contains("FINANCE ADVISOR V9 EVALUATION");
        assertThat(formatted).contains("Test Case: INFY-RESEARCH-001");
        assertThat(formatted).contains("Overall\n-------\nPASS");
    }

    @Test
    @DisplayName("Should evaluate PORTFOLIO-MATH-001 with deterministic calculation fidelity PASS")
    void shouldEvaluatePortfolioCalculationCaseSuccessfully() {
        EvaluationCase mathCase = goldenCases.stream()
                .filter(c -> c.id().equals("PORTFOLIO-MATH-001"))
                .findFirst()
                .orElseThrow();

        EvaluationReport report = evaluationRunner.evaluate(mathCase);

        assertThat(report.overallStatus()).isEqualTo(EvaluationResult.Status.PASS);

        EvaluationResult calcResult = report.getResult(EvaluationCriteria.CALCULATION_FIDELITY).orElseThrow();
        assertThat(calcResult.isPass()).isTrue();
        assertThat(calcResult.actual()).contains("All calculated metrics matched within tolerance");
    }

    @Test
    @DisplayName("Should evaluate SCENARIO-STRESS-001 with complete 3-tier scenario analysis PASS")
    void shouldEvaluateScenarioAnalysisCaseSuccessfully() {
        EvaluationCase scenCase = goldenCases.stream()
                .filter(c -> c.id().equals("SCENARIO-STRESS-001"))
                .findFirst()
                .orElseThrow();

        EvaluationReport report = evaluationRunner.evaluate(scenCase);

        assertThat(report.overallStatus()).isEqualTo(EvaluationResult.Status.PASS);

        EvaluationResult scenResult = report.getResult(EvaluationCriteria.SCENARIO_COMPLETENESS).orElseThrow();
        assertThat(scenResult.isPass()).isTrue();
        assertThat(scenResult.actual()).contains("Baseline: PASS");
        assertThat(scenResult.actual()).contains("Upside:   PASS");
        assertThat(scenResult.actual()).contains("Stress:   PASS");
    }

    @Test
    @DisplayName("Educational Rigor: Should intentionally detect Groundedness failure on GROUNDEDNESS-FAIL-001")
    void shouldIntentionallyDetectGroundednessFailure() {
        EvaluationCase failCase = goldenCases.stream()
                .filter(c -> c.id().equals("GROUNDEDNESS-FAIL-001"))
                .findFirst()
                .orElseThrow();

        EvaluationReport report = evaluationRunner.evaluate(failCase);

        assertThat(report.isAllPass()).isFalse();
        assertThat(report.overallStatus()).isEqualTo(EvaluationResult.Status.FAIL);

        EvaluationResult faith = report.getResult(EvaluationCriteria.FAITHFULNESS).orElseThrow();
        assertThat(faith.isFail()).isTrue();
        assertThat(faith.failureReason()).isPresent();
        assertThat(faith.failureReason().get())
                .contains("Claimed price 1850.00 but available evidence")
                .contains("shows 1520.00");

        String failureReport = report.toFailureReport(EvaluationCriteria.FAITHFULNESS);
        assertThat(failureReport).contains("FAILURE DETECTED");
        assertThat(failureReport).contains("Criterion:\nFaithfulness / Groundedness");
        assertThat(failureReport).contains("Result:\nFAIL");
    }

    @Test
    @DisplayName("Educational Rigor: Should intentionally detect Calculation mismatch on CALC-MISMATCH-001")
    void shouldIntentionallyDetectCalculationMismatch() {
        EvaluationCase failCase = goldenCases.stream()
                .filter(c -> c.id().equals("CALC-MISMATCH-001"))
                .findFirst()
                .orElseThrow();

        EvaluationReport report = evaluationRunner.evaluate(failCase);

        assertThat(report.isAllPass()).isFalse();
        assertThat(report.overallStatus()).isEqualTo(EvaluationResult.Status.FAIL);

        EvaluationResult calc = report.getResult(EvaluationCriteria.CALCULATION_FIDELITY).orElseThrow();
        assertThat(calc.isFail()).isTrue();
    }

    @Test
    @DisplayName("Educational Rigor: Should intentionally detect Missing Stress scenario on SCENARIO-MISSING-STRESS-001")
    void shouldIntentionallyDetectMissingStressScenario() {
        EvaluationCase failCase = goldenCases.stream()
                .filter(c -> c.id().equals("SCENARIO-MISSING-STRESS-001"))
                .findFirst()
                .orElseThrow();

        EvaluationReport report = evaluationRunner.evaluate(failCase);

        assertThat(report.isAllPass()).isFalse();
        assertThat(report.overallStatus()).isEqualTo(EvaluationResult.Status.FAIL);

        EvaluationResult scen = report.getResult(EvaluationCriteria.SCENARIO_COMPLETENESS).orElseThrow();
        assertThat(scen.isFail()).isTrue();
        assertThat(scen.actual()).contains("Stress:   MISSING");
    }
}
