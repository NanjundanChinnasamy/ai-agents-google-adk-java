# Finance Advisor V10 — Observability, Tracing & Telemetry Persistence (`finance.v10`)

> **Milestone 10 Objective**: Extend Finance Advisor V9 by implementing an end-to-end **Observability, Tracing, and Telemetry Persistence** architecture using Google ADK Java (`1.4.0`). While *"V8 protects the agent"* and *"V9 measures the agent"*, *"V10 answers: What exactly happened?"* Learn how to capture structured lifecycle events, correlate operations using an `executionId` spine, track precise component latencies without hallucinating token metrics, enforce PII-safe telemetry redaction, isolate business failures from persistence failures, and store durable execution traces to JSON and SQLite.

---

## 1. High-Level Architecture & Telemetry Pipeline

```
                                  User Inquiry / Portfolio Request
                                                  │
                                                  ▼
                                      +───────────────────────+
                                      | FinanceAdvisorAgentV10|
                                      | Generates Execution ID|
                                      | (exec-YYYYMMDD-uuid8) |
                                      +───────────┬───────────+
                                                  │
                                                  ▼ Emits Structured Events
                                      +───────────────────────+
                                      |  FinanceAgentObserver |
                                      | (Live Event Listener) |
                                      +───────────┬───────────+
                                                  │
                   ┌──────────────────────────────┼──────────────────────────────┐
                   ▼                              ▼                              ▼
        +─────────────────────+        +─────────────────────+        +─────────────────────+
        |     Guardrails      |        |     Model Calls     |        |     Tool Calls      |
        | - PII Sanitized     |        | - Gemini / Gemma    |        | - Yahoo Finance MCP |
        | - Injection Blocked |        | - Wire Latency      |        | - Google Search     |
        | - Compliance Guard  |        | - Token Metric Safe |        | - Deterministic Math|
        +──────────┬──────────+        +──────────┬──────────+        +──────────┬──────────+
                   │                              │                              │
                   └──────────────────────────────┼──────────────────────────────┘
                                                  │
                                                  ▼
                                      +───────────────────────+
                                      |    ExecutionTrace     |
                                      | - Full Event Timeline |
                                      | - Component Latencies |
                                      | - Safe Input / Output |
                                      | - Evaluation Audits   |
                                      +───────────┬───────────+
                                                  │
                                                  ▼
                                      +───────────────────────+
                                      |  ExecutionRepository  |
                                      | Dual Durable Storage: |
                                      | 1. JSON File Storage  |
                                      | 2. SQLite WAL Table   |
                                      +───────────┬───────────+
                                                  │
                                                  ▼
                                      Post-Hoc Audit & Retrieval
                                  (findByExecutionId, findByStatus)
```

---

## 2. The Four Pillars: Logging vs. Observability vs. Persistence vs. Evaluation

| Dimension | Core Question | Definition | Example in Finance Advisor |
|---|---|---|---|
| **Logging** | *"Something happened"* | Flat, unstructured text lines written to console. Difficult to query across requests. | `logger.info("Calling Yahoo Finance MCP")` |
| **Observability** | *"What happened, when, where, and why?"* | Structured, causal event streams tied together by a unique `executionId`, tracking duration and status. | `ToolCallEvent{execId="exec-01", tool="get_stock_info", duration=215ms, status=SUCCESS}` |
| **Persistence** | *"Can I inspect what happened later?"* | Storing structured execution records to durable storage (JSON/SQLite) for post-hoc auditing. | `repository.findByExecutionId("exec-01")` |
| **Evaluation** | *"Was the result good?"* | Independent scoring and assertion over agent output against ground truth or domain guidelines. | `Faithfulness: PASS, Calculation: PASS` |

---

## 3. The Execution ID Correlation Spine

Every request into `FinanceAdvisorAgentV10` generates a unique, collision-safe execution identifier:
```java
String executionId = "exec-20261001-a1b2c3d4"; // Format: exec-YYYYMMDD-<uuid8>
```
The execution ID correlates every action, event, metric, and evaluation report across the agent's internal lifecycle:
```
exec-20261001-a1b2c3d4
 ├── AGENT_STARTED
 ├── BEFORE_AGENT (Input Guardrail: PII detected & sanitized)
 ├── MODEL_CALL_STARTED (Model: gemini-2.5-flash)
 ├── TOOL_CALL_STARTED (Tool: get_stock_info, Symbol: INFY.NS)
 ├── TOOL_CALL_COMPLETED (Duration: 210ms, Status: SUCCESS)
 ├── MODEL_CALL_COMPLETED (Duration: 850ms, Tokens: UNKNOWN)
 ├── AFTER_MODEL (Compliance disclaimer verified)
 ├── EVALUATION_COMPLETED (Faithfulness: PASS, Fidelity: PASS)
 └── AGENT_COMPLETED (Status: SUCCESS, Total Duration: 1.15s)
```

---

## 4. Structured Event Model (`ExecutionEvent`)

Telemetry events are strongly typed, immutable Java records:
```java
public record ExecutionEvent(
    String eventId,
    String executionId,
    Instant timestamp,
    EventType eventType,
    String component,
    String agentName,
    Optional<String> toolName,
    Optional<String> modelName,
    long durationMs,
    String status,
    String safeSummary,
    Map<String, Object> safeMetadata,
    Optional<String> error
) {}
```

### Event Taxonomy (`EventType`):
- **Agent Lifecycle**: `AGENT_STARTED`, `AGENT_COMPLETED`, `AGENT_FAILED`.
- **Model Lifecycle**: `MODEL_CALL_STARTED`, `MODEL_CALL_COMPLETED`, `MODEL_CALL_FAILED`.
- **Tool Lifecycle**: `TOOL_CALL_STARTED`, `TOOL_CALL_COMPLETED`, `TOOL_CALL_FAILED`.
- **Guardrails**: `GUARDRAIL_BLOCKED`, `GUARDRAIL_SANITIZED`, `GUARDRAIL_ENFORCED`.
- **Evaluation**: `EVALUATION_STARTED`, `EVALUATION_COMPLETED`, `EVALUATION_FAILED`.
- **Persistence**: `PERSISTENCE_STARTED`, `PERSISTENCE_COMPLETED`, `PERSISTENCE_FAILED`.

---

## 5. PII-Safe Observability & Data Protection Invariants

Observability must **never** become an accidental leak channel or secondary store for private customer data:
1. **Redaction Prior to Event Creation**: Prompts and responses pass through `PiiDetector` and `PiiSanitizer` before trace generation.
2. **Metadata Sanitization**: Tickers (`INFY.NS`) are recorded; bank account numbers, credit cards, Aadhaar, PAN, emails, and phone numbers are replaced with privacy tokens (`[REDACTED_ACCOUNT]`, etc.).
3. **Prompt Injection Payload Suppression**: When a prompt injection attack is detected, the event records category, risk score, and action (`BLOCKED`). The raw malicious payload is suppressed.
4. **Token Metric Invariant**: If the underlying model endpoint does not return exact token counts, V10 explicitly records `"tokenUsage": "UNKNOWN"`. **V10 never invents or hallucinates synthetic token counts.**

---

## 6. Persistence Abstraction & Storage Implementations

V10 decouples telemetry collection from storage mechanics via `ExecutionRepository`:

```java
public interface ExecutionRepository {
    void save(ExecutionRecord record);
    Optional<ExecutionRecord> findByExecutionId(String executionId);
    List<ExecutionRecord> findRecentExecutions(int limit);
    List<ExecutionRecord> findByStatus(String status);
    List<ExecutionRecord> findExecutionsWithEvaluationFailures();
    List<ExecutionRecord> findExecutionsWithGuardrailEvents();
    long count();
}
```

### Storage Engines:
1. **`JsonExecutionRepository`**: Human-readable, isolated JSON files written to `v10/data/executions/{executionId}.json`.
2. **`SqliteExecutionRepository`**: Embedded relational database table `agent_execution_v10` in `finance_portfolio.db`, featuring indexed lookups and WAL mode.

---

## 7. Failure Isolation: Business Failure vs. Telemetry Failure

V10 strictly isolates agent/business failures from telemetry failures:
- **Scenario A: Tool or MCP Fails**: The agent reports tool failure, and telemetry records `TOOL_CALL_FAILED` with root-cause diagnostic information.
- **Scenario B: Storage or Disk Fails**: The user still receives their financial advisory response; telemetry captures `PERSISTENCE_FAILED` without crashing the user's turn.

---

## 8. Key Files & Implementation Reference

| File | Package Path | Role & Purpose |
|---|---|---|
| `FinanceAdvisorAgentV10.java` | `src/main/java/com/google/adk/finance/v10/` | Root factory creating `finance_advisor_v10` with execution tracking, observer wiring, and persistence. |
| `FinanceAgentObserver.java` | `src/main/java/com/google/adk/finance/v10/observability/` | Central observer listening to lifecycle events and building the `ExecutionTrace`. |
| `ExecutionEvent.java` | `src/main/java/com/google/adk/finance/v10/observability/` | Strongly typed immutable record representing an atomic lifecycle telemetry event. |
| `ExecutionTrace.java` | `src/main/java/com/google/adk/finance/v10/observability/` | Trace container correlating all events, latencies, safe summaries, and evaluation reports. |
| `ExecutionMetrics.java` | `src/main/java/com/google/adk/finance/v10/observability/` | Metrics container tracking total latency, tool breakdown, guardrail counts, and token usage. |
| `ExecutionRepository.java` | `src/main/java/com/google/adk/finance/v10/persistence/` | Common DAO interface for storing and querying execution records. |
| `JsonExecutionRepository.java` | `src/main/java/com/google/adk/finance/v10/persistence/` | File-based repository writing formatted JSON execution traces. |
| `SqliteExecutionRepository.java`| `src/main/java/com/google/adk/finance/v10/persistence/` | Embedded SQLite DAO persisting traces to `agent_execution_v10` in WAL mode. |
| `FinanceConsoleV10.java` | `src/main/java/com/google/adk/finance/v10/` | Interactive CLI with diagnostic commands (`trace`, `history`, `failed`, `eval-failures`, etc.). |
| `GoldenTraceTest.java` | `src/test/java/com/google/adk/finance/v10/` | Test verifying that normal runs emit all required lifecycle events in order. |
| `PiiSafetyTest.java` | `src/test/java/com/google/adk/finance/v10/` | Test verifying that persisted execution traces never contain raw sensitive PII. |
| `PersistenceTest.java` | `src/test/java/com/google/adk/finance/v10/` | Test verifying JSON and SQLite save and retrieval fidelity. |

---

## 9. How to Run & Test Finance Advisor V10

### 9.1 Interactive CLI Console
Launch the interactive terminal:
```bash
# Windows
test-finance-v10.bat

# Linux / macOS
./test-finance-v10.sh
```

Within the console, use diagnostic commands:
- `trace <id>`        : Displays full lifecycle event trace and latency breakdown for an execution ID.
- `history`           : Lists recent executions with status, duration, and guardrail flags.
- `failed`            : Lists executions that encountered tool, model, or guardrail failures.
- `eval-failures`     : Lists executions where evaluation criteria (faithfulness/fidelity) failed.
- `inspect <id>`      : Inspects stored evidence records captured for a specific execution.
- `quit`              : Terminates the console.

### 9.2 Executing Automated Tests
Execute the complete V10 observability and persistence test suite:
```bash
# Windows
gradlew.bat test --tests "com.google.adk.finance.v10.*"

# Linux / macOS
./gradlew test --tests "com.google.adk.finance.v10.*"
```
