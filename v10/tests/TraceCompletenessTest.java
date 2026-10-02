package com.google.adk.finance.v10;

import com.google.adk.finance.v10.observability.AgentExecution;
import com.google.adk.finance.v10.observability.ExecutionEvent;
import com.google.adk.finance.v10.observability.ExecutionTrace;
import com.google.adk.finance.v10.persistence.JsonExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V10 Observability: Trace Completeness & ASCII Timeline Verification")
public class TraceCompletenessTest {

    @TempDir
    Path tempDir;

    private FinanceAdvisorAgentV10 agent;

    @BeforeEach
    void setUp() {
        agent = FinanceAdvisorAgentV10.createOffline(new JsonExecutionRepository(tempDir));
    }

    @Test
    @DisplayName("Trace events must be chronologically ordered and share the same execution ID")
    void testTraceChronologyAndExecutionId() {
        AgentExecution execution = agent.execute("user-test", "Analyze Reliance portfolio risk.");
        ExecutionTrace trace = execution.trace();

        List<ExecutionEvent> events = trace.events();
        assertThat(events).hasSizeGreaterThanOrEqualTo(2);

        Instant previous = Instant.MIN;
        for (ExecutionEvent e : events) {
            assertThat(e.executionId()).isEqualTo(execution.executionId());
            assertThat(e.timestamp()).isAfterOrEqualTo(previous);
            previous = e.timestamp();
        }

        assertThat(trace.metrics().totalExecutionDurationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Human-readable trace output matches standard ASCII layout")
    void testAsciiTraceFormatting() {
        AgentExecution execution = agent.execute("user-test", "Show portfolio allocation principles.");
        String formatted = execution.renderTrace();

        assertThat(formatted)
                .contains("FINANCE ADVISOR V10 TRACE")
                .contains("Execution ID:")
                .contains(execution.executionId())
                .contains("EVENT TIMELINE")
                .contains("AGENT_STARTED")
                .contains("METRICS")
                .contains("Total duration:");
    }
}
