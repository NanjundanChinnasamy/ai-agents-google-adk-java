package com.google.adk.finance.v10;

import com.google.adk.finance.v10.observability.EventType;
import com.google.adk.finance.v10.observability.ExecutionEvent;
import com.google.adk.finance.v10.observability.ExecutionTrace;
import com.google.adk.finance.v10.observability.FinanceAgentObserver;
import com.google.adk.finance.v10.observability.ObservabilityContext;
import com.google.adk.finance.v10.persistence.ExecutionRecord;
import com.google.adk.finance.v10.persistence.JsonExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V10 Observability: Deterministic Golden Trace Test")
public class GoldenTraceTest {

    @TempDir
    Path tempDir;

    private JsonExecutionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JsonExecutionRepository(tempDir);
    }

    @Test
    @DisplayName("Deterministic Golden Trace for controlled equity research sequence")
    void testGoldenTraceSequence() {
        String execId = "exec-golden-20261001-001";
        ObservabilityContext context = new ObservabilityContext(execId, "analyst-1", "session-golden", FinanceAdvisorAgentV10.AGENT_NAME);
        FinanceAgentObserver observer = new FinanceAgentObserver(context, repository);

        // Controlled golden sequence for: "Analyse Infosys using current financial information."
        observer.onAgentStarted(execId, "analyst-1", "session-golden", "Analyse Infosys using current financial information.");
        observer.onModelCallStarted(execId, "gemini-2.5-flash");
        observer.onModelCallCompleted(execId, "gemini-2.5-flash", 420, "UNKNOWN");

        observer.onToolCallStarted(execId, "YahooFinance", "YAHOO_FINANCE_MCP", Map.of("ticker", "INFY.NS"));
        observer.onToolCallCompleted(execId, "YahooFinance", "YAHOO_FINANCE_MCP", 310, "Price: 1910 INR, P/E: 24.5");

        observer.onModelCallStarted(execId, "gemini-2.5-flash");
        observer.onModelCallCompleted(execId, "gemini-2.5-flash", 650, "UNKNOWN");

        observer.onEvaluationCompleted(execId, "Faithfulness", "PASS", 1.0, "Verified against YahooFinance quote", 45);
        observer.onAgentCompleted(execId, "SUCCESS", "Infosys is trading at 1910 INR with sound fundamentals.", 1520);

        ExecutionTrace trace = observer.getLastTrace().orElseThrow();
        List<ExecutionEvent> events = trace.events();

        // Verify the chronological golden trace sequence
        assertThat(events).hasSize(9);
        assertThat(events.get(0).eventType()).isEqualTo(EventType.AGENT_STARTED);
        assertThat(events.get(1).eventType()).isEqualTo(EventType.MODEL_CALL_STARTED);
        assertThat(events.get(2).eventType()).isEqualTo(EventType.MODEL_CALL_COMPLETED);
        assertThat(events.get(3).eventType()).isEqualTo(EventType.TOOL_CALL_STARTED);
        assertThat(events.get(4).eventType()).isEqualTo(EventType.TOOL_CALL_COMPLETED);
        assertThat(events.get(5).eventType()).isEqualTo(EventType.MODEL_CALL_STARTED);
        assertThat(events.get(6).eventType()).isEqualTo(EventType.MODEL_CALL_COMPLETED);
        assertThat(events.get(7).eventType()).isEqualTo(EventType.EVALUATION_COMPLETED);
        assertThat(events.get(8).eventType()).isEqualTo(EventType.AGENT_COMPLETED);

        // Verify persistence of golden trace
        ExecutionRecord saved = repository.findByExecutionId(execId).orElseThrow();
        assertThat(saved.executionId()).isEqualTo(execId);
        assertThat(saved.status()).isEqualTo("SUCCESS");
    }
}
