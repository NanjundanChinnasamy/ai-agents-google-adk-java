package com.google.adk.finance.v10;

import com.google.adk.finance.v10.observability.AgentExecution;
import com.google.adk.finance.v10.observability.EventType;
import com.google.adk.finance.v10.observability.FinanceAgentObserver;
import com.google.adk.finance.v10.observability.ObservabilityContext;
import com.google.adk.finance.v10.persistence.ExecutionRecord;
import com.google.adk.finance.v10.persistence.ExecutionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V10 Observability: Resilience & Persistence Failure Isolation")
public class ObservabilityFailureTest {

    @Test
    @DisplayName("L. Persistence Failure Isolation: Agent succeeds even if telemetry persistence throws an error")
    void testPersistenceFailureIsolation() {
        // Failing mock repository that throws an IOException/RuntimeException on save
        ExecutionRepository failingRepository = new ExecutionRepository() {
            @Override
            public void save(ExecutionRecord record) {
                throw new RuntimeException("Disk full / I/O permission denied");
            }

            @Override
            public Optional<ExecutionRecord> findByExecutionId(String executionId) {
                return Optional.empty();
            }

            @Override
            public List<ExecutionRecord> findRecentExecutions(int limit) {
                return List.of();
            }

            @Override
            public List<ExecutionRecord> findByStatus(String status) {
                return List.of();
            }

            @Override
            public List<ExecutionRecord> findExecutionsWithEvaluationFailures() {
                return List.of();
            }

            @Override
            public List<ExecutionRecord> findExecutionsWithGuardrailEvents() {
                return List.of();
            }

            @Override
            public List<ExecutionRecord> findAll() {
                return List.of();
            }

            @Override
            public int count() {
                return 0;
            }

            @Override
            public void clear() {}
        };

        String execId = ObservabilityContext.generateExecutionId();
        ObservabilityContext context = new ObservabilityContext(execId, "user-resilient", "sess-resilient", FinanceAdvisorAgentV10.AGENT_NAME);
        FinanceAgentObserver observer = new FinanceAgentObserver(context, failingRepository);

        observer.onAgentStarted(execId, "user-resilient", "sess-resilient", "Calculate PnL");
        observer.onModelCallStarted(execId, "gemini-2.5-flash");
        observer.onModelCallCompleted(execId, "gemini-2.5-flash", 300, "UNKNOWN");

        // Complete agent: this calls save() which throws, but observer must catch it safely!
        observer.onAgentCompleted(execId, "SUCCESS", "Calculated profit is 20%.", 350);

        // Core Invariant Check:
        // 1. Agent execution status is still SUCCESS
        assertThat(observer.getLastTrace().orElseThrow().status()).isEqualTo("SUCCESS");

        // 2. Persistence status is tracked as FAILED
        assertThat(observer.isPersistenceSuccessful()).isFalse();

        // 3. PERSISTENCE_FAILED event was appended to the trace
        assertThat(context.events()).anyMatch(e -> e.eventType() == EventType.PERSISTENCE_FAILED);
    }
}
