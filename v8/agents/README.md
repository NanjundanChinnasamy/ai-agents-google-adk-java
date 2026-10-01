# Finance Advisor V8 — Root Orchestrator Agent (`FinanceAdvisorAgentV8`)

## Role and Purpose

`FinanceAdvisorAgentV8` (Agent Name: `finance_advisor_v8`, Role: `guarded_finance_advisor`) represents the top-level orchestrator in Milestone 8.

Unlike earlier iterations where safety and compliance depended entirely on prompt phrasing ("please include a disclaimer"), V8 enforces an **enclosing protective boundary** through deterministic Google ADK lifecycle callbacks.

---

## Registered Callback Interceptors

1. **`BeforeAgentGuardrail`**:
   - Analyzes incoming user messages before any agent logic or planning executes.
   - Evaluates [`PromptInjectionDetector`](../guardrails/input/PromptInjectionDetector.java). If malicious override or secret extraction is detected, it terminates the invocation via `invocationContext.setEndInvocation(true)` and produces an immediate user-safe explanation.
   - Evaluates [`PiiDetector`](../guardrails/input/PiiDetector.java) and [`PiiSanitizer`](../guardrails/input/PiiSanitizer.java), redacting sensitive personal identifiers (bank accounts, cards, phone, email, SSN, PAN) into session state tokens.

2. **`BeforeModelGuardrail`**:
   - Inspects the outbound `LlmRequest` payload right before transmission to the Gemini LLM.
   - Ensures that wire payloads contain zero raw sensitive data.

3. **`BeforeToolGuardrail`**:
   - Inspects requested tool invocations before execution.
   - Evaluates [`ToolOperationGuard`](../guardrails/tool/ToolOperationGuard.java) to strictly prohibit transactional capabilities (such as buying/selling securities, transferring funds, or modifying accounts).
   - Evaluates [`TickerValidator`](../guardrails/tool/TickerValidator.java) to sanitize exchange symbols, enforce length constraints, and block SQL/shell injection strings.

4. **`AfterToolEvidenceCapture`**:
   - Intercepts tool execution results (from Yahoo Finance MCP, PortfolioMathTool, etc.).
   - Parses empirical quotes, prices, and metrics into the thread-safe [`EvidenceStore`](../../src/main/java/com/google/adk/finance/v8/evidence/EvidenceStore.java).

5. **`AfterModelGuardrail`**:
   - Intercepts model-generated outputs before they reach the user.
   - Evaluates [`OffensiveLanguageDetector`](../guardrails/output/OffensiveLanguageDetector.java) to detect and substitute toxic or abusive terms with safe defaults.
   - Evaluates [`HallucinationDetector`](../guardrails/output/HallucinationDetector.java) to audit asserted figures against facts captured in the `EvidenceStore`.
   - Evaluates [`ComplianceDisclaimerGuard`](../guardrails/output/ComplianceDisclaimerGuard.java) to guarantee the mandatory regulatory disclaimer is present.
