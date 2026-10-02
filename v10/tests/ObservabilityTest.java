package com.google.adk.finance.v10;

import com.google.adk.finance.v10.observability.AgentExecution;
import com.google.adk.finance.v10.observability.EventType;
import com.google.adk.finance.v10.observability.ExecutionEvent;
import com.google.adk.finance.v10.observability.ExecutionTrace;
import com.google.adk.finance.v10.observability.ObservabilityContext;
import com.google.adk.finance.v10.persistence.ExecutionRepository;
import com.google.adk.finance.v10.persistence.JsonExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V10 Observability: Execution ID & Structured Lifecycle Events")
public class ObservabilityTest {

    @TempDir
    Path tempDir;

    private ExecutionRepository repository;
    private FinanceAdvisorAgentV10 agent;

    @BeforeEach
    void setUp() {
        repository = new JsonExecutionRepository(tempDir);
        agent = FinanceAdvisorAgentV10.createOffline(repository);
    }

    @Test
    @DisplayName("A. Execution ID: Every execution receives a unique, collision-safe identifier")
    void testExecutionIdUniqueness() {
        Set<String> idSet = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            String id = ObservabilityContext.generateExecutionId();
            assertThat(id)
                    .startsWith("exec-")
                    .hasSizeGreaterThan(15);
            assertThat(idSet.add(id)).isTrue();
        }
    }

    @Test
    @DisplayName("B. Event Creation: Agent start produces AGENT_STARTED and completion produces AGENT_COMPLETED")
    void testAgentLifecycleEvents() {
        AgentExecution execution = agent.execute("user-1", "What is the P/E ratio of Infosys?");

        ExecutionTrace trace = execution.trace();
        List<ExecutionEvent> events = trace.events();

        assertThat(events).isNotEmpty();
        assertThat(events.get(0).eventType()).isEqualTo(EventType.AGENT_STARTED);
        assertThat(events.get(events.size() - 1).eventType()).isEqualTo(EventType.AGENT_COMPLETED);

        assertThat(events.get(0).executionId()).isEqualTo(execution.executionId());
        assertThat(events.get(events.size() - 1).executionId()).isEqualTo(execution.executionId());
    }

    @Test
    @DisplayName("C. Model Observability: Model call started and completed events are recorded")
    void testModelObservability() {
        AgentExecution execution = agent.execute("user-1", "Explain diversification principles.");

        List<ExecutionEvent> events = execution.trace().events();
        boolean hasModelStarted = events.stream().anyMatch(e -> e.eventType() == EventType.MODEL_CALL_STARTED);
        boolean hasModelCompleted = events.stream().anyMatch(e -> e.eventType() == EventType.MODEL_CALL_COMPLETED);

        assertThat(hasModelStarted).isTrue();
        assertThat(hasModelCompleted).isTrue();
    }

    @Test
    @DisplayName("D. Guardrail Observability: Trading attempt records structured TOOL_OPERATION_BLOCKED event")
    void testGuardrailEventObservability() {
        AgentExecution execution = agent.execute("user-1", "Execute trade: Buy 100 shares of Apple stock.");

        assertThat(execution.status()).isEqualTo("BLOCKED");
        List<ExecutionEvent> events = execution.trace().events();

        assertThat(events).anyMatch(e ->
                e.eventType() == EventType.TOOL_OPERATION_BLOCKED
                && "ToolOperationGuard".equals(e.metadata().get("guardrail")));
    }
}
