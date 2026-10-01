package com.google.adk.finance.v9;

import com.google.adk.finance.v9.evaluation.CalculationFidelityEvaluator;
import com.google.adk.finance.v9.evaluation.EvaluationCriteria;
import com.google.adk.finance.v9.evaluation.EvaluationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V9 Evaluation Criterion 2: Calculation Fidelity Tests")
public class CalculationFidelityTest {

    private CalculationFidelityEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new CalculationFidelityEvaluator();
    }

    @Test
    @DisplayName("Should PASS calculation fidelity when report P&L and weight match PortfolioMathTool output")
    void shouldPassCalculationFidelityWhenPnLMatchesPortfolioMathTool() {
        // Position A: 50 shares bought at £100, current price £125 -> PnL = (125-100)*50 = £1,250
        List<CalculationFidelityEvaluator.ExpectedPosition> positions = List.of(
                new CalculationFidelityEvaluator.ExpectedPosition("Position A", 100.0, 125.0, 50)
        );

        String report = """
                Portfolio Position A Analysis:
                Cost basis: £5,000. Current Valuation: £6,250.
                Total P&L is £1,250 with a portfolio weight of 100.0%.
                """;

        EvaluationResult result = evaluator.evaluate("TEST-CALC-PASS", report, positions);

        assertThat(result.status()).isEqualTo(EvaluationResult.Status.PASS);
        assertThat(result.isPass()).isTrue();
        assertThat(result.criterion()).isEqualTo(EvaluationCriteria.CALCULATION_FIDELITY);
        assertThat(result.explanation()).contains("Position A PnL: Expected=1250.00, Actual=1250.00 -> PASS");
    }

    @Test
    @DisplayName("Should FAIL calculation fidelity when agent asserts wrong P&L (e.g. £1,500 vs £1,250)")
    void shouldFailCalculationFidelityWhenPnLDoesNotMatch() {
        // Expected £1,250, Report claims £1,500
        EvaluationResult result = evaluator.evaluateSingle(
                "TEST-CALC-FAIL",
                "Portfolio summary: Position A P&L is £1,500 with weight of 35.5%.",
                "Position A",
                1250.0,
                35.5
        );

        assertThat(result.status()).isEqualTo(EvaluationResult.Status.FAIL);
        assertThat(result.isFail()).isTrue();
        assertThat(result.explanation()).contains("Expected: 1250.00");
        assertThat(result.explanation()).contains("Actual:   1500.00");
        assertThat(result.explanation()).contains("Result: FAIL");
        assertThat(result.explanation()).contains("Weight:\nExpected: 35.50%\nActual:   35.50%\nResult: PASS");
    }

    @Test
    @DisplayName("Should PASS calculation fidelity for multi-asset portfolio weights and PnL")
    void shouldPassCalculationFidelityWhenPortfolioWeightsMatch() {
        // Asset 1: RELIANCE: 2 shares, buy 1000, current 1500 -> value = 3000, PnL = 1000
        // Asset 2: TCS: 2 shares, buy 1000, current 1500 -> value = 3000, PnL = 1000
        // Total value = 6000. Weight each = 50.0%
        List<CalculationFidelityEvaluator.ExpectedPosition> positions = List.of(
                new CalculationFidelityEvaluator.ExpectedPosition("RELIANCE", 1000.0, 1500.0, 2),
                new CalculationFidelityEvaluator.ExpectedPosition("TCS", 1000.0, 1500.0, 2)
        );

        String report = """
                Multi-Asset Allocation Breakdown:
                - RELIANCE: PnL is ₹1,000, representing 50.0% allocation.
                - TCS: PnL is ₹1,000, representing 50.0% allocation.
                """;

        EvaluationResult result = evaluator.evaluate("TEST-MULTI-PASS", report, positions);

        assertThat(result.status()).isEqualTo(EvaluationResult.Status.PASS);
        assertThat(result.isPass()).isTrue();
    }

    @Test
    @DisplayName("Should FAIL calculation fidelity when weight percentage is inaccurate")
    void shouldFailCalculationFidelityWhenWeightIsIncorrect() {
        EvaluationResult result = evaluator.evaluateSingle(
                "TEST-WEIGHT-FAIL",
                "Position A PnL is £1,250 with weight of 20.0%.",
                "Position A",
                1250.0,
                35.5
        );

        assertThat(result.status()).isEqualTo(EvaluationResult.Status.FAIL);
        assertThat(result.explanation()).contains("Weight:\nExpected: 35.50%\nActual:   20.00%\nResult: FAIL");
    }
}
