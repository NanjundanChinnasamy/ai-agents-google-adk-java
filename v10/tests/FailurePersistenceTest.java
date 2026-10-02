package com.google.adk.finance.v10;

import com.google.adk.finance.v10.observability.AgentExecution;
import com.google.adk.finance.v10.observability.EventType;
import com.google.adk.finance.v10.observability.FinanceAgentObserver;
import com.google.adk.finance.v10.observability.ObservabilityContext;
import com.google.adk.finance.v10.persistence.ExecutionRecord;
import com.google.adk.finance.v10.persistence.JsonExecutionRepository;
import com.google.adk.finance.v9.evaluation.EvaluationCase;
import com.google.adk.finance.v9.evaluation.EvaluationCriteria;
import com.google.adk.finance.v9.evaluation.EvaluationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V10 Observability: Failure Diagnosis & Failure Persistence")
public class FailurePersistenceTest {

    @TempDir
    Path tempDir;

    private JsonExecutionRepository repository;
    private FinanceAdvisorAgentV10 agent;

    @BeforeEach
    void setUp() {
        repository = new JsonExecutionRepository(tempDir);
        agent = FinanceAdvisorAgentV10.createOffline(repository);
    }

    @Test
    @DisplayName("J. Failure Persistence: Simulated MCP/Tool failure records TOOL_CALL_FAILED and FAILED status")
    void testToolFailureRecordingAndPersistence() {
        String execId = ObservabilityContext.generateExecutionId();
        ObservabilityContext context = new ObservabilityContext(execId, "user-err", "session-err", FinanceAdvisorAgentV10.AGENT_NAME);
        FinanceAgentObserver observer = new FinanceAgentObserver(context, repository);

        // Simulate tool execution pipeline with MCP timeout
        observer.onAgentStarted(execId, "user-err", "session-err", "Get live quotes for INVALID_TICKER");
        observer.onModelCallStarted(execId, "gemini-2.5-flash");
        observer.onModelCallCompleted(execId, "gemini-2.5-flash", 400, "UNKNOWN");
        observer.onToolCallStarted(execId, "YahooFinance", "YAHOO_FINANCE_MCP", Map.of("ticker", "INVALID_TICKER"));

        // Tool failure occurs
        observer.onToolCallFailed(execId, "YahooFinance", "YAHOO_FINANCE_MCP", new RuntimeException("MCP connection timeout after 5000ms"), 5000);
        observer.onAgentFailed(execId, new RuntimeException("Downstream tool failed: YahooFinance"), 5450);

        // Verify retrieval from repository
        Optional<ExecutionRecord> retrievedOpt = repository.findByExecutionId(execId);
        assertThat(retrievedOpt).isPresent();

        ExecutionRecord record = retrievedOpt.get();
        assertThat(record.status()).isEqualTo("FAILED");
        assertThat(record.events()).anyMatch(e -> e.eventType() == EventType.TOOL_CALL_FAILED);

        // Verify developer can query failed executions
        List<ExecutionRecord> failedList = repository.findByStatus("FAILED");
        assertThat(failedList).isNotEmpty();
        assertThat(failedList.get(0).executionId()).isEqualTo(execId);
    }

    @Test
    @DisplayName("K. Evaluation Persistence: V9 evaluation failures are retrievable from persisted execution")
    void testEvaluationFailurePersistence() {
        EvaluationCase mockCase = EvaluationCase.builder("CASE-TEST-FAIL")
                .category("SCENARIO_ANALYSIS")
                .description("Evaluate portfolio resilience under rate shocks")
                .userRequest("Analyze portfolio for rate shocks")
                .sampleReport("Rate shock scenario indicates -5% impact on portfolio.")
                .expectedResults(Map.of(EvaluationCriteria.SCENARIO_COMPLETENESS, EvaluationResult.Status.PASS))
                .build();

        AgentExecution execution = agent.executeWithEvaluation("eval-user", "Analyze portfolio for rate shocks", mockCase);

        Optional<ExecutionRecord> recordOpt = repository.findByExecutionId(execution.executionId());
        assertThat(recordOpt).isPresent();

        ExecutionRecord record = recordOpt.get();
        assertThat(record.evaluations()).isNotEmpty();

        // Verify query capability: "Which executions failed evaluation?"
        List<ExecutionRecord> evalFailures = repository.findExecutionsWithEvaluationFailures();
        assertThat(evalFailures).isNotEmpty();
        assertThat(evalFailures.stream().anyMatch(r -> r.executionId().equals(execution.executionId()))).isTrue();

        // If the report had any evaluation failure or pass, verify evaluation records are retrievable
        assertThat(record.evaluations().stream().anyMatch(e -> e.criterion() == EvaluationCriteria.FAITHFULNESS
                || e.criterion() == EvaluationCriteria.CALCULATION_FIDELITY
                || e.criterion() == EvaluationCriteria.SCENARIO_COMPLETENESS)).isTrue();
    }
}
