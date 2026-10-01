package com.google.adk.finance.v9.evaluation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single golden test case in the Version 9 evaluation harness.
 */
public record EvaluationCase(
        String id,
        String description,
        String category,
        String userRequest,
        List<EvidenceRecord> evidence,
        String sampleReport,
        Map<EvaluationCriteria, EvaluationResult.Status> expectedResults,
        Optional<String> failureReason,
        List<CalculationFidelityEvaluator.ExpectedPosition> expectedPositions
) {
    public EvaluationCase {
        Objects.requireNonNull(id, "id must not be null");
        description = description == null ? "" : description;
        category = category == null ? "GENERAL" : category;
        userRequest = userRequest == null ? "" : userRequest;
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        sampleReport = sampleReport == null ? "" : sampleReport;
        expectedResults = expectedResults == null ? Map.of() : Map.copyOf(expectedResults);
        failureReason = failureReason == null ? Optional.empty() : failureReason;
        expectedPositions = expectedPositions == null ? List.of() : List.copyOf(expectedPositions);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private String description = "";
        private String category = "GENERAL";
        private String userRequest = "";
        private List<EvidenceRecord> evidence = List.of();
        private String sampleReport = "";
        private Map<EvaluationCriteria, EvaluationResult.Status> expectedResults = Map.of();
        private String failureReason = null;
        private List<CalculationFidelityEvaluator.ExpectedPosition> expectedPositions = List.of();

        public Builder(String id) {
            this.id = id;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder userRequest(String userRequest) {
            this.userRequest = userRequest;
            return this;
        }

        public Builder evidence(List<EvidenceRecord> evidence) {
            this.evidence = evidence;
            return this;
        }

        public Builder sampleReport(String sampleReport) {
            this.sampleReport = sampleReport;
            return this;
        }

        public Builder expectedResults(Map<EvaluationCriteria, EvaluationResult.Status> expectedResults) {
            this.expectedResults = expectedResults;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder expectedPositions(List<CalculationFidelityEvaluator.ExpectedPosition> positions) {
            this.expectedPositions = positions;
            return this;
        }

        public EvaluationCase build() {
            return new EvaluationCase(
                    id, description, category, userRequest, evidence,
                    sampleReport, expectedResults, Optional.ofNullable(failureReason), expectedPositions
            );
        }
    }
}
