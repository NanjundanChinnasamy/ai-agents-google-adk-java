package com.google.adk.finance.v9.evaluation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Institutional ASCII evaluation report generator for Version 9.
 * Formats structured, human-readable audits for developers, reviewers, and quality gates.
 */
public class EvaluationReport {
    private final String testCaseId;
    private final Map<EvaluationCriteria, EvaluationResult> results = new LinkedHashMap<>();
    private EvaluationResult.Status overallStatus = EvaluationResult.Status.PASS;

    public EvaluationReport(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    public void addResult(EvaluationResult result) {
        if (result != null) {
            results.put(result.criterion(), result);
            if (result.status() == EvaluationResult.Status.FAIL) {
                overallStatus = EvaluationResult.Status.FAIL;
            } else if (result.status() == EvaluationResult.Status.ERROR && overallStatus != EvaluationResult.Status.FAIL) {
                overallStatus = EvaluationResult.Status.ERROR;
            } else if (result.status() == EvaluationResult.Status.WARN && overallStatus == EvaluationResult.Status.PASS) {
                overallStatus = EvaluationResult.Status.WARN;
            }
        }
    }

    public String testCaseId() {
        return testCaseId;
    }

    public Map<EvaluationCriteria, EvaluationResult> results() {
        return Collections.unmodifiableMap(results);
    }

    public EvaluationResult.Status overallStatus() {
        return overallStatus;
    }

    public boolean isAllPass() {
        return overallStatus == EvaluationResult.Status.PASS;
    }

    public Optional<EvaluationResult> getResult(EvaluationCriteria criterion) {
        return Optional.ofNullable(results.get(criterion));
    }

    /**
     * Generates standard comprehensive ASCII evaluation report.
     */
    public String toFormattedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append("FINANCE ADVISOR V9 EVALUATION\n");
        sb.append("==================================================\n");
        sb.append("Test Case: ").append(testCaseId).append("\n\n");

        for (Map.Entry<EvaluationCriteria, EvaluationResult> entry : results.entrySet()) {
            EvaluationCriteria crit = entry.getKey();
            EvaluationResult res = entry.getValue();

            sb.append(crit.displayName()).append("\n");
            sb.append("-".repeat(crit.displayName().length())).append("\n");
            sb.append(res.status()).append("\n");

            if (!res.actual().isBlank()) {
                sb.append(res.actual()).append("\n");
            }
            if (!res.explanation().isBlank() && !res.explanation().equals(res.actual())) {
                sb.append(res.explanation()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Overall\n");
        sb.append("-------\n");
        sb.append(overallStatus).append("\n");
        sb.append("==================================================");

        return sb.toString();
    }

    /**
     * Generates a focused failure diagnostic report for a specific failed criterion.
     */
    public String toFailureReport(EvaluationCriteria criterion) {
        EvaluationResult res = results.get(criterion);
        if (res == null || res.status() != EvaluationResult.Status.FAIL) {
            return toFormattedReport();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append("FAILURE DETECTED\n\n");
        sb.append("Criterion:\n").append(criterion.displayName()).append("\n\n");
        sb.append("Expected:\n").append(res.expected()).append("\n\n");
        sb.append("Actual:\n").append(res.actual()).append("\n\n");
        if (res.failureReason().isPresent()) {
            sb.append("Failure Reason:\n").append(res.failureReason().get()).append("\n\n");
        }
        sb.append("Result:\nFAIL\n");
        sb.append("==================================================");

        return sb.toString();
    }

    @Override
    public String toString() {
        return toFormattedReport();
    }
}
