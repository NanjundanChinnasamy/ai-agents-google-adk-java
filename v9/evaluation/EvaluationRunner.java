package com.google.adk.finance.v9.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * End-to-end evaluation orchestrator for Version 9.
 * <p>
 * Implements the V9 evaluation pipeline:
 * <pre>
 * Agent Response
 *       │
 *       ├──────────────► Deterministic Evaluators
 *       │                 ├── Faithfulness (retrieved fact consistency)
 *       │                 ├── Calculation Fidelity (PortfolioMathTool verification)
 *       │                 └── Scenario Completeness (Baseline / Upside / Stress tiers)
 *       │
 *       └──────────────► LLM Judge
 *                         ├── Evidence usage
 *                         ├── Explanation quality
 *                         ├── Uncertainty handling
 *                         └── Completeness
 *       │
 *       ▼
 * Evaluation Report
 * </pre>
 */
public class EvaluationRunner {
    private static final Logger logger = LoggerFactory.getLogger(EvaluationRunner.class);

    private final FaithfulnessEvaluator faithfulnessEvaluator;
    private final CalculationFidelityEvaluator calculationEvaluator;
    private final ScenarioCompletenessEvaluator scenarioEvaluator;
    private final LlmJudgeEvaluator llmJudgeEvaluator;

    public EvaluationRunner() {
        this(
                new FaithfulnessEvaluator(),
                new CalculationFidelityEvaluator(),
                new ScenarioCompletenessEvaluator(),
                new LlmJudgeEvaluator()
        );
    }

    public EvaluationRunner(
            FaithfulnessEvaluator faithfulnessEvaluator,
            CalculationFidelityEvaluator calculationEvaluator,
            ScenarioCompletenessEvaluator scenarioEvaluator,
            LlmJudgeEvaluator llmJudgeEvaluator) {
        this.faithfulnessEvaluator = faithfulnessEvaluator;
        this.calculationEvaluator = calculationEvaluator;
        this.scenarioEvaluator = scenarioEvaluator;
        this.llmJudgeEvaluator = llmJudgeEvaluator;
    }

    /**
     * Executes complete evaluation for a single {@link EvaluationCase}.
     */
    public EvaluationReport evaluate(EvaluationCase testCase) {
        logger.info("[EvaluationRunner] Executing evaluation for test case: {}", testCase.id());
        EvaluationReport report = new EvaluationReport(testCase.id());

        boolean hasExplicitTargets = !testCase.expectedResults().isEmpty();

        // 1. Deterministic Faithfulness Audit
        boolean shouldRunFaith = hasExplicitTargets
                ? testCase.expectedResults().containsKey(EvaluationCriteria.FAITHFULNESS)
                : !testCase.evidence().isEmpty();
        if (shouldRunFaith) {
            EvaluationResult faithRes = faithfulnessEvaluator.evaluate(
                    testCase.id(), testCase.sampleReport(), testCase.evidence());
            report.addResult(faithRes);
        }

        // 2. Deterministic Calculation Fidelity Audit (if positions are defined)
        boolean shouldRunCalc = hasExplicitTargets
                ? testCase.expectedResults().containsKey(EvaluationCriteria.CALCULATION_FIDELITY)
                : !testCase.expectedPositions().isEmpty();
        if (shouldRunCalc && !testCase.expectedPositions().isEmpty()) {
            EvaluationResult calcRes = calculationEvaluator.evaluate(
                    testCase.id(), testCase.sampleReport(), testCase.expectedPositions());
            report.addResult(calcRes);
        }

        // 3. Deterministic Scenario Completeness Audit
        boolean shouldRunScenario = hasExplicitTargets
                ? testCase.expectedResults().containsKey(EvaluationCriteria.SCENARIO_COMPLETENESS)
                : ("SCENARIO_ANALYSIS".equalsIgnoreCase(testCase.category()) || testCase.sampleReport().toLowerCase().contains("scenario"));
        if (shouldRunScenario) {
            EvaluationResult scenRes = scenarioEvaluator.evaluate(
                    testCase.id(), testCase.sampleReport());
            report.addResult(scenRes);
        }

        // 4. Qualitative LLM Judge Audit
        boolean shouldRunJudge = hasExplicitTargets
                ? testCase.expectedResults().containsKey(EvaluationCriteria.LLM_JUDGE)
                : "NORMAL_RESEARCH".equalsIgnoreCase(testCase.category());
        if (shouldRunJudge) {
            EvaluationResult judgeRes = llmJudgeEvaluator.evaluate(
                    testCase.id(), testCase.userRequest(), testCase.sampleReport(), testCase.evidence());
            report.addResult(judgeRes);
        }

        logger.info("[EvaluationRunner] Evaluation completed for {}: Overall Status = {}", testCase.id(), report.overallStatus());
        return report;
    }

    /**
     * Evaluates a raw generated report against an evidence store and user query.
     */
    public EvaluationReport evaluate(String testCaseId, String userQuery, String reportText, EvidenceStoreV9 evidenceStore) {
        EvaluationReport report = new EvaluationReport(testCaseId);

        // 1. Faithfulness
        report.addResult(faithfulnessEvaluator.evaluate(testCaseId, reportText, evidenceStore));

        // 2. Scenario Completeness (if scenario headers are detected)
        if (reportText.toLowerCase().contains("scenario") || reportText.toLowerCase().contains("baseline")) {
            report.addResult(scenarioEvaluator.evaluate(testCaseId, reportText));
        }

        // 3. LLM Judge
        report.addResult(llmJudgeEvaluator.evaluate(testCaseId, userQuery, reportText, evidenceStore.getAllRecords()));

        return report;
    }

    public FaithfulnessEvaluator getFaithfulnessEvaluator() {
        return faithfulnessEvaluator;
    }

    public CalculationFidelityEvaluator getCalculationEvaluator() {
        return calculationEvaluator;
    }

    public ScenarioCompletenessEvaluator getScenarioEvaluator() {
        return scenarioEvaluator;
    }

    public LlmJudgeEvaluator getLlmJudgeEvaluator() {
        return llmJudgeEvaluator;
    }
}
