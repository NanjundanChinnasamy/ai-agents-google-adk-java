package com.google.adk.finance.v10.persistence;

import java.util.List;
import java.util.Optional;

/**
 * Repository abstraction for persisting and retrieving Finance Advisor execution records.
 * Decouples agent observability from the underlying storage mechanism (JSON, SQLite, Memory).
 */
public interface ExecutionRepository {

    /**
     * Persists an execution record.
     */
    void save(ExecutionRecord record);

    /**
     * Retrieves an execution record by its unique collision-safe execution ID.
     */
    Optional<ExecutionRecord> findByExecutionId(String executionId);

    /**
     * Retrieves the most recent execution records up to the given limit.
     */
    List<ExecutionRecord> findRecentExecutions(int limit);

    /**
     * Retrieves executions filtered by status (e.g. "SUCCESS", "FAILED", "BLOCKED").
     */
    List<ExecutionRecord> findByStatus(String status);

    /**
     * Retrieves all executions that experienced at least one evaluation failure.
     */
    List<ExecutionRecord> findExecutionsWithEvaluationFailures();

    /**
     * Retrieves all executions that triggered at least one safety guardrail intervention.
     */
    List<ExecutionRecord> findExecutionsWithGuardrailEvents();

    /**
     * Retrieves all persisted executions.
     */
    List<ExecutionRecord> findAll();

    /**
     * Returns total count of persisted executions.
     */
    int count();

    /**
     * Clears all persisted executions (primarily for testing and reset).
     */
    void clear();
}
