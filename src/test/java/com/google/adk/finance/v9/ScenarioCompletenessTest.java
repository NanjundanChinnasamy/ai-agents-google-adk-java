package com.google.adk.finance.v9;

import com.google.adk.finance.v9.evaluation.EvaluationCriteria;
import com.google.adk.finance.v9.evaluation.EvaluationResult;
import com.google.adk.finance.v9.evaluation.ScenarioCompletenessEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V9 Evaluation Criterion 3: Scenario Completeness Tests")
public class ScenarioCompletenessTest {

    private ScenarioCompletenessEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ScenarioCompletenessEvaluator();
    }

    @Test
    @DisplayName("Should PASS scenario completeness when report contains all three substantive scenario tiers")
    void shouldPassScenarioCompletenessWhenAllThreeScenariosExist() {
        String report = """
                Investment Scenario Analysis:
                
                1. Baseline Scenario:
                Assumptions: Consistent annual revenue expansion of 8.0% with stable operating margins of 21.0%.
                Projection: Target price of ₹1,650 based on fair value DCF multiple of 22x.
                
                2. Upside Scenario:
                Assumptions: Acceleration in enterprise generative AI contracts and cloud migration catalysts.
                Projection: Upside target of ₹1,950 with 15% revenue expansion and multiple re-rating to 26x.
                
                3. Stress Test Scenario:
                Assumptions: Global macroeconomic recession shock leading to US tech discretionary budget freeze.
                Projection: Severe downside drawdown to ₹1,200 with margin compression of 300 bps.
                
                Conclusion: Risk-reward profile is skewed favorably towards baseline.
                """;

        EvaluationResult result = evaluator.evaluate("TEST-SCENARIO-PASS", report);

        assertThat(result.status()).isEqualTo(EvaluationResult.Status.PASS);
        assertThat(result.isPass()).isTrue();
        assertThat(result.criterion()).isEqualTo(EvaluationCriteria.SCENARIO_COMPLETENESS);
        assertThat(result.actual()).contains("Baseline: PASS");
        assertThat(result.actual()).contains("Upside:   PASS");
        assertThat(result.actual()).contains("Stress:   PASS");
        assertThat(result.actual()).contains("Overall: PASS");
    }

    @Test
    @DisplayName("Should FAIL scenario completeness when the negative Stress Test scenario is omitted")
    void shouldFailScenarioCompletenessWhenStressScenarioIsMissing() {
        // Report contains Baseline and Upside, but NO Stress scenario
        String report = """
                Forward Scenario Projections:
                
                Baseline Case:
                Assumptions: 7.5% annual growth with stable margin guidance. Target valuation ₹1,620.
                
                Upside Case:
                Assumptions: Favorable currency tailwind and market share gains driving 12% revenue growth. Target ₹1,880.
                
                Summary: Outlook remains positive across all operational units.
                """;

        EvaluationResult result = evaluator.evaluate("TEST-SCENARIO-NO-STRESS", report);

        assertThat(result.status()).isEqualTo(EvaluationResult.Status.FAIL);
        assertThat(result.isFail()).isTrue();
        assertThat(result.actual()).contains("Baseline: PASS");
        assertThat(result.actual()).contains("Upside:   PASS");
        assertThat(result.actual()).contains("Stress:   MISSING");
        assertThat(result.actual()).contains("Overall: FAIL");
        assertThat(result.failureReason()).isPresent();
        assertThat(result.failureReason().get()).contains("Stress (MISSING)");
    }

    @Test
    @DisplayName("Should FAIL scenario completeness when a scenario is superficial without metrics or assumptions")
    void shouldFailScenarioCompletenessWhenScenarioIsSuperficial() {
        // Report has header 'Stress test' but only trivial one-sentence without metrics or assumptions
        String report = """
                Scenarios:
                Baseline: Assumptions: 8% growth, target ₹1,650.
                Upside: Assumptions: 12% growth, target ₹1,850.
                Stress test: Things might be bad.
                """;

        EvaluationResult result = evaluator.evaluate("TEST-SCENARIO-INCOMPLETE", report);

        assertThat(result.status()).isEqualTo(EvaluationResult.Status.FAIL);
        assertThat(result.actual()).contains("Stress:   INCOMPLETE");
    }
}
