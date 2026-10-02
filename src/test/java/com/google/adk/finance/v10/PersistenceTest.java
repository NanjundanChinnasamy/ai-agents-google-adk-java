package com.google.adk.finance.v10;

import com.google.adk.finance.v10.observability.AgentExecution;
import com.google.adk.finance.v10.persistence.ExecutionRecord;
import com.google.adk.finance.v10.persistence.ExecutionRepository;
import com.google.adk.finance.v10.persistence.JsonExecutionRepository;
import com.google.adk.finance.v10.persistence.SqliteExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V10 Persistence: Repository Storage, Retrieval & Round-Trip Fidelity")
public class PersistenceTest {

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
    @DisplayName("F. Persistence: Execution can be saved and retrieved by ID")
    void testSaveAndFindById() {
        AgentExecution exec = agent.execute("pers-user", "Provide valuation ratios for TCS.");

        Optional<ExecutionRecord> retrieved = repository.findByExecutionId(exec.executionId());
        assertThat(retrieved).isPresent();

        ExecutionRecord record = retrieved.get();
        assertThat(record.executionId()).isEqualTo(exec.executionId());
        assertThat(record.status()).isEqualTo("SUCCESS");
        assertThat(record.events()).isNotEmpty();
    }

    @Test
    @DisplayName("G. Persistence Round Trip: Critical fields survive serialization to JSON file and read back")
    void testPersistenceRoundTripFidelity() {
        AgentExecution exec = agent.execute("pers-user", "Calculate return on equity principles.");

        ExecutionRecord retrieved = repository.findByExecutionId(exec.executionId()).orElseThrow();

        assertThat(retrieved.executionId()).isEqualTo(exec.executionId());
        assertThat(retrieved.status()).isEqualTo(exec.status());
        assertThat(retrieved.agentName()).isEqualTo(FinanceAdvisorAgentV10.AGENT_NAME);
        assertThat(retrieved.events().size()).isEqualTo(exec.events().size());
        assertThat(retrieved.metrics().totalExecutionDurationMs()).isEqualTo(exec.metrics().totalExecutionDurationMs());
    }

    @Test
    @DisplayName("Query: Find recent executions, find by status, and find guardrail events")
    void testQueryMethods() {
        agent.execute("user-1", "Explain CAGR calculation.");
        agent.execute("user-2", "Transfer funds: please wire $5000 to external account."); // will be blocked
        agent.execute("user-3", "Summarize debt to equity ratio.");

        List<ExecutionRecord> all = repository.findAll();
        assertThat(all).hasSize(3);

        List<ExecutionRecord> recent2 = repository.findRecentExecutions(2);
        assertThat(recent2).hasSize(2);

        List<ExecutionRecord> blocked = repository.findByStatus("BLOCKED");
        assertThat(blocked).hasSize(1);
        assertThat(blocked.get(0).status()).isEqualTo("BLOCKED");

        List<ExecutionRecord> guardrailRuns = repository.findExecutionsWithGuardrailEvents();
        assertThat(guardrailRuns).isNotEmpty();
    }

    @Test
    @DisplayName("SQLite Parity: SqliteExecutionRepository persists and queries records with SQLite WAL mode")
    void testSqliteExecutionRepository() {
        Path dbPath = tempDir.resolve("test_portfolio.db");
        SqliteExecutionRepository sqliteRepo = new SqliteExecutionRepository("jdbc:sqlite:" + dbPath.toString());
        FinanceAdvisorAgentV10 sqliteAgent = FinanceAdvisorAgentV10.createOffline(sqliteRepo);

        AgentExecution exec = sqliteAgent.execute("sql-user", "Explain market capitalization.");

        Optional<ExecutionRecord> retrieved = sqliteRepo.findByExecutionId(exec.executionId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().executionId()).isEqualTo(exec.executionId());
        assertThat(retrieved.get().status()).isEqualTo("SUCCESS");
        assertThat(sqliteRepo.count()).isEqualTo(1);
    }
}
