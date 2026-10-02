package com.google.adk.finance.v10.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * SQLite Relational Implementation of {@link ExecutionRepository}.
 * <p>
 * Stores execution headers with indexed relational columns alongside complete JSON telemetry payloads
 * for fast indexed querying in SQLite WAL mode.
 */
public class SqliteExecutionRepository implements ExecutionRepository {
    private static final Logger logger = LoggerFactory.getLogger(SqliteExecutionRepository.class);

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:finance_portfolio.db";
    private final String dbUrl;
    private final Gson gson;

    public SqliteExecutionRepository() {
        this(DEFAULT_DB_URL);
    }

    public SqliteExecutionRepository(String dbUrl) {
        this.dbUrl = Objects.requireNonNull(dbUrl, "dbUrl must not be null");
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, type, ctx) -> Instant.parse(json.getAsString()))
                .registerTypeAdapterFactory(new JsonExecutionRepository.OptionalTypeAdapterFactory())
                .disableHtmlEscaping()
                .create();
        initTable();
    }

    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA busy_timeout = 30000;");
        }
        return conn;
    }

    private synchronized void initTable() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS agent_execution_v10 (
                    execution_id TEXT PRIMARY KEY,
                    user_id TEXT,
                    session_id TEXT,
                    started_at TEXT NOT NULL,
                    completed_at TEXT,
                    status TEXT NOT NULL,
                    agent_name TEXT NOT NULL,
                    duration_ms INTEGER,
                    event_count INTEGER,
                    has_guardrail_events INTEGER,
                    has_evaluation_failures INTEGER,
                    payload_json TEXT NOT NULL
                );
                CREATE INDEX IF NOT EXISTS idx_exec_v10_status ON agent_execution_v10(status);
                CREATE INDEX IF NOT EXISTS idx_exec_v10_started ON agent_execution_v10(started_at);
                CREATE INDEX IF NOT EXISTS idx_exec_v10_eval_fail ON agent_execution_v10(has_evaluation_failures);
                CREATE INDEX IF NOT EXISTS idx_exec_v10_guardrail ON agent_execution_v10(has_guardrail_events);
                """;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : ddl.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql.trim());
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to initialize SQLite table agent_execution_v10: {}", e.getMessage());
            throw new RuntimeException("Could not initialize SQLite execution table", e);
        }
    }

    @Override
    public synchronized void save(ExecutionRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        String json = gson.toJson(record);

        String sql = """
                INSERT OR REPLACE INTO agent_execution_v10 (
                    execution_id, user_id, session_id, started_at, completed_at,
                    status, agent_name, duration_ms, event_count,
                    has_guardrail_events, has_evaluation_failures, payload_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, record.executionId());
            stmt.setString(2, record.userId());
            stmt.setString(3, record.sessionId());
            stmt.setString(4, record.startedAt());
            stmt.setString(5, record.completedAt());
            stmt.setString(6, record.status());
            stmt.setString(7, record.agentName());
            stmt.setLong(8, record.metrics() != null ? record.metrics().totalExecutionDurationMs() : 0);
            stmt.setInt(9, record.events().size());
            stmt.setInt(10, record.hasGuardrailEvents() ? 1 : 0);
            stmt.setInt(11, record.hasEvaluationFailures() ? 1 : 0);
            stmt.setString(12, json);
            stmt.executeUpdate();
            logger.debug("Saved execution '{}' to SQLite", record.executionId());
        } catch (SQLException e) {
            logger.error("Failed to insert execution into SQLite: {}", e.getMessage());
            throw new RuntimeException("Failed to persist execution record in SQLite: " + record.executionId(), e);
        }
    }

    @Override
    public synchronized Optional<ExecutionRecord> findByExecutionId(String executionId) {
        if (executionId == null || executionId.isBlank()) return Optional.empty();
        String sql = "SELECT payload_json FROM agent_execution_v10 WHERE execution_id = ?;";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, executionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("payload_json");
                    return Optional.ofNullable(gson.fromJson(json, ExecutionRecord.class));
                }
            }
        } catch (SQLException e) {
            logger.error("Error querying execution '{}' from SQLite: {}", executionId, e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public synchronized List<ExecutionRecord> findAll() {
        return queryRecords("SELECT payload_json FROM agent_execution_v10 ORDER BY started_at DESC;");
    }

    @Override
    public synchronized List<ExecutionRecord> findRecentExecutions(int limit) {
        if (limit <= 0) return List.of();
        String sql = "SELECT payload_json FROM agent_execution_v10 ORDER BY started_at DESC LIMIT " + limit + ";";
        return queryRecords(sql);
    }

    @Override
    public synchronized List<ExecutionRecord> findByStatus(String status) {
        if (status == null || status.isBlank()) return List.of();
        String sql = "SELECT payload_json FROM agent_execution_v10 WHERE LOWER(status) = LOWER(?) ORDER BY started_at DESC;";
        List<ExecutionRecord> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(gson.fromJson(rs.getString("payload_json"), ExecutionRecord.class));
                }
            }
        } catch (SQLException e) {
            logger.error("Error querying by status in SQLite: {}", e.getMessage());
        }
        return list;
    }

    @Override
    public synchronized List<ExecutionRecord> findExecutionsWithEvaluationFailures() {
        return queryRecords("SELECT payload_json FROM agent_execution_v10 WHERE has_evaluation_failures = 1 ORDER BY started_at DESC;");
    }

    @Override
    public synchronized List<ExecutionRecord> findExecutionsWithGuardrailEvents() {
        return queryRecords("SELECT payload_json FROM agent_execution_v10 WHERE has_guardrail_events = 1 ORDER BY started_at DESC;");
    }

    @Override
    public synchronized int count() {
        String sql = "SELECT COUNT(*) FROM agent_execution_v10;";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting executions in SQLite: {}", e.getMessage());
        }
        return 0;
    }

    @Override
    public synchronized void clear() {
        String sql = "DELETE FROM agent_execution_v10;";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.error("Error clearing SQLite execution table: {}", e.getMessage());
        }
    }

    private List<ExecutionRecord> queryRecords(String sql) {
        List<ExecutionRecord> list = new ArrayList<>();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(gson.fromJson(rs.getString("payload_json"), ExecutionRecord.class));
            }
        } catch (SQLException e) {
            logger.error("Error executing query in SQLite: {}", e.getMessage());
        }
        return list;
    }
}
