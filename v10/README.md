# Milestone Version 10: Observability + Persistence

## 1. Executive Summary & Learning Objective

**Core Question:**
> *"Can I see what my agent did, understand why it did it, measure its execution, and retrieve the execution later?"*

Milestone Version 10 extends the Java Google Agent Development Kit (ADK) Finance Advisor with structured telemetry, execution tracing, telemetry metrics, and local persistence. Rather than treating an autonomous agent as an opaque black box, V10 provides complete end-to-end visibility into the agent's internal cognitive lifecycle: from user prompt ingestion, guardrail evaluation, model calls, tool executions, and evaluation assertions, to persistent trace archives.

### The Evolutionary Progression:
* **V8 (Guardrails + Callbacks):** *Was it allowed?* (Security, PII sanitization, trading blockades, compliance injection)
* **V9 (Evaluation + Failure Testing):** *Was it correct?* (Deterministic faithfulness, calculation fidelity, scenario completeness, LLM-as-a-judge)
* **V10 (Observability + Persistence):** *What exactly happened?* (End-to-end structured tracing, telemetry metrics, audit trails, and execution retrieval)

---

## 2. Core Conceptual Architecture

```
User
  │
  ▼
Agent (FinanceAdvisorAgentV10)
  │
  ▼
Events (Structured Event Stream)
  │
  ▼
Trace (ExecutionTrace)
  ├── Model calls (duration, status, token metrics)
  ├── Tool calls (Yahoo Finance MCP, Google Search, Math, DB)
  ├── Guardrails (PII detected/sanitized, injection blocked, disclaimer)
  ├── Errors (isolated business failure vs telemetry failure)
  └── Evaluations (V9 faithfulness, calculation fidelity, scenarios)
  │
  ▼
Metrics (ExecutionMetrics: latency, counts, tool breakdown, token metrics)
  │
  ▼
Persistence (JsonExecutionRepository / SqliteExecutionRepository)
  │
  ▼
Retrieve / Diagnose (findByExecutionId, findByStatus, findExecutionsWithEvaluationFailures)
```

---

## 3. The Critical Distinctions: Logging vs. Observability vs. Persistence vs. Evaluation

| Dimension | Core Question | Definition | Example in Finance Advisor |
|---|---|---|---|
| **Logging** | *"Something happened"* | Flat, unstructured text lines written to console or file stream. Difficult to query, lacks execution boundaries. | `logger.info("Executing tool yahoo_finance_quote")` |
| **Observability** | *"What happened, when, where, and why?"* | Structured, causal event streams tied together by a unique execution ID, tracking latency, status, and metadata. | `ToolCallEvent{executionId="exec-20261001-abc", tool="YahooFinance", duration=245ms, status=SUCCESS}` |
| **Persistence** | *"Can I inspect what happened later?"* | Storing structured execution records to durable storage (JSON/SQLite) for post-hoc auditing and querying. | `repository.findByExecutionId("exec-20261001-abc")` |
| **Evaluation** | *"Was the result good?"* | Independent scoring and assertion over agent output against ground truth or domain guidelines. | `Faithfulness: PASS, Scenario Completeness: FAIL` |

---

## 4. Execution ID: The Correlation Spine

Every request into `FinanceAdvisorAgentV10` generates a unique, collision-safe execution identifier:
```java
String executionId = "exec-20261001-a1b2c3d4"; // Format: exec-YYYYMMDD-<uuid8>
```

The execution ID connects all telemetry across the entire execution lifecycle:
```
exec-20261001-a1b2c3d4
 ├── AGENT_STARTED
 ├── BEFORE_AGENT (Guardrail check: PII & Injection)
 ├── GUARDRAIL_EVENT (PII sanitized: 1 match)
 ├── MODEL_CALL_STARTED (Model: gemini-2.5-flash)
 ├── TOOL_CALL_STARTED (Tool: yahoo_finance_quote, Ticker: INFY.NS)
 ├── TOOL_CALL_COMPLETED (Duration: 210ms, Status: SUCCESS)
 ├── MODEL_CALL_COMPLETED (Duration: 850ms, Tokens: UNKNOWN)
 ├── GUARDRAIL_EVENT (Regulatory disclaimer enforced)
 ├── EVALUATION_COMPLETED (Faithfulness: PASS)
 ├── EVALUATION_COMPLETED (Scenario Completeness: PASS)
 └── AGENT_COMPLETED (Status: SUCCESS, Total Duration: 1.15s)
```

---

## 5. Structured Events Model

Rather than plain text log strings, V10 uses strongly-typed, immutable Java records:

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
)
```

### Event Taxonomy (`EventType`):
* **Agent Lifecycle:** `AGENT_STARTED`, `AGENT_COMPLETED`, `AGENT_FAILED`
* **Model Lifecycle:** `MODEL_CALL_STARTED`, `MODEL_CALL_COMPLETED`, `MODEL_CALL_FAILED`
* **Tool Lifecycle:** `TOOL_CALL_STARTED`, `TOOL_CALL_COMPLETED`, `TOOL_CALL_FAILED`
* **Guardrails:** `GUARDRAIL_BLOCKED`, `GUARDRAIL_SANITIZED`, `GUARDRAIL_ENFORCED`
* **Evaluation:** `EVALUATION_STARTED`, `EVALUATION_COMPLETED`, `EVALUATION_FAILED`
* **Persistence & Errors:** `PERSISTENCE_STARTED`, `PERSISTENCE_COMPLETED`, `PERSISTENCE_FAILED`, `ERROR`

---

## 6. PII-Safe Observability & Data Protection

Financial data contains sensitive information (card numbers, bank accounts, Aadhaar/SSN, emails, phone numbers). Observability must **never** become an accidental leak channel or insecure secondary store.

### Safeguards Built into V10:
1. **Redaction Prior to Event Creation:** All incoming prompts and outgoing responses pass through `PiiDetector` and `PiiMasker` before trace creation.
2. **Metadata Sanitization:** Tool arguments are scrubbed. Tickers (`INFY.NS`) are recorded; raw user tokens or bank accounts are sanitized to `[REDACTED_ACCOUNT]`.
3. **Prompt Injection Payload Suppression:** When a prompt injection attack is detected, the event records:
   ```json
   {
     "eventType": "GUARDRAIL_BLOCKED",
     "guardrailType": "PROMPT_INJECTION",
     "riskScore": 0.98,
     "action": "BLOCKED"
   }
   ```
   The raw malicious payload is **not** persisted to disk.

---

## 7. Telemetry Metrics: Grounded Measurement Without Metric Hallucination

V10 tracks real, measured metrics:
* **Latency Metrics:** Granular breakdown of agent, model, tool, guardrail, and evaluation durations.
* **Tool Accounting:**
  ```
  Yahoo Finance MCP: Invocations: 2, Success: 2, Failure: 0, Avg Latency: 215ms
  Google Search:     Invocations: 1, Success: 1, Failure: 0, Avg Latency: 640ms
  ```
* **Token Usage Metric Invariant:**
  If the underlying ADK runtime or model endpoint does not return exact token counts on the wire, V10 explicitly records:
  ```json
  "tokenUsage": "UNKNOWN"
  ```
  **V10 never invents or hallucinates synthetic token counts.**

---

## 8. Persistence Abstraction & Storage Implementations

V10 decouples the agent from storage mechanics using `ExecutionRepository`:

```java
public interface ExecutionRepository {
    void save(ExecutionRecord record);
    Optional<ExecutionRecord> findByExecutionId(String executionId);
    List<ExecutionRecord> findRecentExecutions(int limit);
    List<ExecutionRecord> findByStatus(String status);
    List<ExecutionRecord> findExecutionsWithEvaluationFailures();
    List<ExecutionRecord> findExecutionsWithGuardrailEvents();
    List<ExecutionRecord> findAll();
    long count();
    void clear();
}
```

### Storage Engines Provided:
1. **`JsonExecutionRepository`**: Human-readable, isolated JSON files written to `v10/data/executions/{executionId}.json`. Includes custom `OptionalTypeAdapterFactory` for strict Java 21 module compliance.
2. **`SqliteExecutionRepository`**: Zero-dependency embedded database persistence using table `agent_execution_v10` in `finance_portfolio.db`, featuring indexed lookups and WAL (Write-Ahead Logging) mode.

---

## 9. Failure Observability: Business Failure vs. Telemetry Failure Isolation

V10 strictly isolates business/agent failure from telemetry/persistence failure:

* **Scenario A: Tool or MCP Fails**
  * Agent status: `FAILED` (or fallback)
  * Telemetry status: `SUCCESS` (the failure event `TOOL_CALL_FAILED` is captured and persisted with root-cause diagnostic information)
* **Scenario B: Storage or Disk Failure**
  * Agent status: `SUCCESS` (the user still receives their financial advisory response)
  * Persistence status: `FAILED` (logged and recorded as `PERSISTENCE_FAILED`, without crashing the agent execution flow)

---

## 10. Interactive CLI Commands (`FinanceConsoleV10`)

Run the interactive CLI:
```bash
./test-finance-v10.bat
# or
./test-finance-v10.sh
```

### Available Interactive Commands:
* `ask <question>`: Run advisory request and print response with execution ID.
* `trace`: Display complete human-readable ASCII timeline and metrics for the most recent run.
* `history`: List the last 10 persisted executions.
* `failed`: List all executions that failed.
* `guardrails`: List executions that triggered guardrail interventions.
* `eval-failures`: List executions that failed V9 evaluation criteria.
* `eval <test-case-id>`: Run a specific V9 golden case and persist execution trace with evaluation scores.
* `eval-all`: Run all 10 V9 golden cases with full observability.
* `inspect <executionId>`: Display the full stored trace for any execution ID.

---

## 11. Human-Readable ASCII Trace Output

```
================================================================================
                    FINANCE ADVISOR V10 EXECUTION TRACE
================================================================================
Execution ID: exec-20261001-a1b2c3d4
Agent:        FinanceAdvisorAgentV10
Status:       SUCCESS
Duration:     1150ms
Started:      2026-10-01T20:00:00.001Z
Completed:    2026-10-01T20:00:01.151Z

--------------------------------------------------------------------------------
EVENT TIMELINE
--------------------------------------------------------------------------------
[20:00:00.001] (+   0ms) [SUCCESS] AGENT_STARTED             | Execution initiated
[20:00:00.005] (+   4ms) [SUCCESS] GUARDRAIL_SANITIZED       | PII detected and redacted
[20:00:00.010] (+   5ms) [SUCCESS] MODEL_CALL_STARTED        | Calling gemini-2.5-flash
[20:00:00.120] (+ 110ms) [SUCCESS] TOOL_CALL_STARTED         | YahooFinance: quote (INFY.NS)
[20:00:00.330] (+ 210ms) [SUCCESS] TOOL_CALL_COMPLETED       | YahooFinance completed in 210ms
[20:00:00.950] (+ 620ms) [SUCCESS] MODEL_CALL_COMPLETED       | Model completed (Tokens: UNKNOWN)
[20:00:00.955] (+   5ms) [SUCCESS] GUARDRAIL_ENFORCED        | Regulatory disclaimer appended
[20:00:01.050] (+  95ms) [SUCCESS] EVALUATION_COMPLETED       | Faithfulness: PASS (1.00)
[20:00:01.150] (+ 100ms) [SUCCESS] AGENT_COMPLETED           | Execution finished with SUCCESS

--------------------------------------------------------------------------------
EXECUTION METRICS
--------------------------------------------------------------------------------
Total Duration:        1150 ms
Model Calls:           1 (Total Model Latency: 940 ms)
Tool Calls:            1 (Total Tool Latency: 210 ms)
Guardrail Interventions: 2
Evaluation Failures:   0

TOOL BREAKDOWN:
  - YahooFinance: 1 calls, 1 success, 0 failed, avg latency: 210 ms

EVALUATION SUMMARY:
  Total Criteria:      1 | Passes: 1 | Failures: 0 | Warnings: 0
  - Faithfulness:      PASS (Score: 1.00)
================================================================================
```

---

## 12. Verification & Test Suite

The V10 test suite rigorously verifies all observability and persistence invariants:
* `ObservabilityTest.java`: Unique execution IDs, lifecycle event emission, model observability, guardrail blocked tracking.
* `TraceCompletenessTest.java`: Chronological ordering, non-negative durations, ASCII timeline formatting.
* `PersistenceTest.java`: Persistence round-trip, query methods, SQLite parity.
* `PiiSafetyTest.java`: Verifies raw PII is **never** written to persisted disk files.
* `FailurePersistenceTest.java`: Tool failures recorded as `TOOL_CALL_FAILED`; evaluation failures retrievable via `findExecutionsWithEvaluationFailures()`.
* `MetricsTest.java`: Latency calculation, tool accounting, and `UNKNOWN` token handling.
* `GoldenTraceTest.java`: Deterministic 9-event execution sequence verification.
* `ObservabilityFailureTest.java`: Persistence failure isolation (agent succeeds even if telemetry storage fails).

Run all tests:
```bash
./gradlew test --tests "com.google.adk.finance.v10.*"
```
