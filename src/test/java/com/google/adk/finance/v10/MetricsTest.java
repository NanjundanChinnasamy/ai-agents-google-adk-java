package com.google.adk.finance.v10;

import com.google.adk.finance.v10.metrics.TokenMetrics;
import com.google.adk.finance.v10.observability.AgentExecution;
import com.google.adk.finance.v10.observability.ExecutionMetrics;
import com.google.adk.finance.v10.observability.FinanceAgentObserver;
import com.google.adk.finance.v10.observability.ObservabilityContext;
import com.google.adk.finance.v10.persistence.JsonExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V10 Metrics: Execution Latency, Tool Accounting & Token Handling")
public class MetricsTest {

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
    @DisplayName("E. Metrics: Total duration is measured and non-negative")
    void testDurationMeasurement() {
        AgentExecution execution = agent.execute("metrics-user", "Explain market beta.");
        ExecutionMetrics metrics = execution.metrics();

        assertThat(metrics.totalExecutionDurationMs()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.latencyMetrics().totalExecutionSeconds()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("Tool Metrics: Tracks invocation count, success count, and average latency per tool")
    void testToolAccountingMetrics() {
        String execId = ObservabilityContext.generateExecutionId();
        ObservabilityContext context = new ObservabilityContext(execId, "user-m", "sess-m", FinanceAdvisorAgentV10.AGENT_NAME);
        FinanceAgentObserver observer = new FinanceAgentObserver(context, repository);

        observer.onAgentStarted(execId, "user-m", "sess-m", "Query test");

        // Tool 1: Yahoo Finance called twice (successes: 200ms and 400ms -> avg 300ms)
        observer.onToolCallStarted(execId, "YahooFinance", "YAHOO_FINANCE_MCP", Map.of("ticker", "INFY"));
        observer.onToolCallCompleted(execId, "YahooFinance", "YAHOO_FINANCE_MCP", 200, "Price: 1800");

        observer.onToolCallStarted(execId, "YahooFinance", "YAHOO_FINANCE_MCP", Map.of("ticker", "TCS"));
        observer.onToolCallCompleted(execId, "YahooFinance", "YAHOO_FINANCE_MCP", 400, "Price: 3900");

        // Tool 2: Google Search called once (800ms)
        observer.onToolCallStarted(execId, "GoogleSearch", "GOOGLE_SEARCH", Map.of("query", "TCS earnings"));
        observer.onToolCallCompleted(execId, "GoogleSearch", "GOOGLE_SEARCH", 800, "Results found");

        observer.onAgentCompleted(execId, "SUCCESS", "Summary done", 1500);

        ExecutionMetrics metrics = observer.getLastTrace().orElseThrow().metrics();

        assertThat(metrics.toolCallCount()).isEqualTo(3);
        assertThat(metrics.toolMetricsMap()).containsKey("YahooFinance");
        assertThat(metrics.toolMetricsMap()).containsKey("GoogleSearch");

        var yahooMetric = metrics.toolMetricsMap().get("YahooFinance");
        assertThat(yahooMetric.invocationCount()).isEqualTo(2);
        assertThat(yahooMetric.successCount()).isEqualTo(2);
        assertThat(yahooMetric.failureCount()).isEqualTo(0);
        assertThat(yahooMetric.averageDurationMs()).isEqualTo(300);

        var searchMetric = metrics.toolMetricsMap().get("GoogleSearch");
        assertThat(searchMetric.invocationCount()).isEqualTo(1);
        assertThat(searchMetric.totalDurationMs()).isEqualTo(800);
    }

    @Test
    @DisplayName("Token Metrics: Accurately records UNKNOWN rather than fabricating token numbers")
    void testTokenMetricsHandling() {
        AgentExecution execution = agent.execute("token-user", "Define Price to Earnings ratio.");
        TokenMetrics tokens = execution.metrics().tokenMetrics();

        assertThat(tokens.inputTokens()).isEqualTo(TokenMetrics.UNKNOWN);
        assertThat(tokens.outputTokens()).isEqualTo(TokenMetrics.UNKNOWN);
        assertThat(tokens.totalTokens()).isEqualTo(TokenMetrics.UNKNOWN);
        assertThat(tokens.isAvailable()).isFalse();
    }
}
