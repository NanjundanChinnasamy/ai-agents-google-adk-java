# Finance Advisor V8 — Input & Output Guardrails + Lifecycle Callbacks

This directory documents **Finance Advisor V8**, which introduces **deterministic guardrails and Google ADK lifecycle callbacks** into the Finance Advisor application using the official **Google Agent Development Kit (ADK) for Java** (`com.google.adk:google-adk:1.4.0`).

---

## 1. Primary Learning Objective

> **Core Learning Objective:**
> Learn how deterministic guardrails and lifecycle callbacks can protect an autonomous agent before input is processed, before tools are executed, after the model responds, and before the final answer reaches the user.

---

## 2. The 5-Stage Interception Architecture

```
                         USER INPUT
                              │
                              ▼
                     INPUT GUARDRAILS
                     /              \
            PII Detection       Prompt Injection
            & Sanitization         Detection
                     \              /
                      ▼            ▼
                   BEFORE-AGENT CALLBACK
                              │
                              ▼
                    BEFORE-MODEL CALLBACK
                    (Wire-level PII scrub)
                              │
                              ▼
                        AGENT / MODEL
                              │
                              ▼
                     BEFORE-TOOL CALLBACK
                     /                  \
             Ticker Validation     Tool Operation
            (format, length, SQL)   Authorization
                     \                  /
                      ▼                ▼
                         MCP / SEARCH
                              │
                              ▼
                     AFTER-TOOL CALLBACK
                     (Evidence Store Fact Capture)
                              │
                              ▼
                            MODEL
                              │
                              ▼
                    AFTER-MODEL CALLBACK
                    /         |         \
              Offensive   Compliance  Hallucination
               Language   Disclaimer   Fact Audit
                    \         |         /
                     +--------+--------+
                              │
                              ▼
                        FINAL RESPONSE
```

---

## 3. Directory Structure

```
v8/
├── README.md                                  <- Main architectural documentation for V8
├── FinanceAdvisorAgentV8.java                 <- Root Guarded Orchestrator Agent (guarded_finance_advisor)
│
├── guardrails/
│   ├── GuardrailResult.java                   <- ALLOW, BLOCK, SANITIZE, WARN result contract
│   ├── input/
│   │   ├── PiiDetector.java                   <- Pattern detector for cards, accounts, emails, phones, SSN, PAN
│   │   ├── PiiSanitizer.java                  <- Redacts sensitive entities into safe tokens ([REDACTED_...])
│   │   └── PromptInjectionDetector.java       <- Scans for instruction overrides, secret extraction & jailbreaks
│   │
│   ├── tool/
│   │   ├── TickerValidator.java               <- Exchange symbol format, length, and SQL/shell injection validator
│   │   └── ToolOperationGuard.java            <- Restricts operations to read-only research; blocks trades/transfers
│   │
│   └── output/
│       ├── OffensiveLanguageDetector.java     <- Blocks profanity, abuse, harassment; returns safe fallback
│       ├── ComplianceDisclaimerGuard.java     <- Verifies and appends mandatory SEBI/SEC regulatory disclaimer
│       └── HallucinationDetector.java          <- Cross-references claimed numbers against empirical EvidenceStore
│
├── callbacks/
│   ├── BeforeAgentGuardrail.java              <- Intercepts user input, evaluates injection & sanitizes PII
│   ├── BeforeModelGuardrail.java              <- Wire-level PII scrubber before sending prompt to Gemini
│   ├── BeforeToolGuardrail.java               <- Enforces tool operation authorization & ticker validation
│   ├── AfterToolEvidenceCapture.java          <- Captures live tool outputs (price, currency, metrics) into EvidenceStore
│   └── AfterModelGuardrail.java               <- Verifies offensive language, hallucination, and disclaimer
│
├── agents/
│   └── README.md                              <- Specification of FinanceAdvisorAgentV8
│
├── sub-agents/
│   ├── GuardedMarketResearchAgentV8.java      <- Web search sub-agent with query guardrails
│   ├── MockTradingAgentV8.java                <- Controlled testbed agent with execute_trade tool
│   └── README.md                              <- Catalog of V8 sub-agents
│
└── workflows/
    └── README.md                              <- Explains relationship between V7 Workflows and V8 Guardrails
```

---

## 4. Google ADK Official Lifecycle Callback Mappings

All callbacks in Finance Advisor V8 are built strictly against the official Google ADK Java `1.4.0` callback interfaces:

| ADK Callback Interface | Interception Phase | V8 Implementation Class | Responsibility |
|---|---|---|---|
| `Callbacks.BeforeAgentCallbackSync` | Before Agent Execution | [`BeforeAgentGuardrail`](callbacks/BeforeAgentGuardrail.java) | Intercepts prompt injection attacks and redacts PII before agent logic begins. Can halt invocation via `invocationContext.setEndInvocation(true)`. |
| `Callbacks.BeforeModelCallbackSync` | Before LLM Call | [`BeforeModelGuardrail`](callbacks/BeforeModelGuardrail.java) | Rewrites `LlmRequest` contents to ensure zero unredacted PII is transmitted to the external model provider. |
| `Callbacks.BeforeToolCallbackSync` | Before Tool Run | [`BeforeToolGuardrail`](callbacks/BeforeToolGuardrail.java) | Validates tickers and authorizes tool calls. Returns override error map if blocked, preventing actual tool execution. |
| `Callbacks.AfterToolCallbackSync` | After Tool Run | [`AfterToolEvidenceCapture`](callbacks/AfterToolEvidenceCapture.java) | Extracts empirical quotes, prices, and metrics from tool outputs into the in-memory `EvidenceStore`. |
| `Callbacks.AfterModelCallbackSync` | After Model Generates | [`AfterModelGuardrail`](callbacks/AfterModelGuardrail.java) | Checks offensive language, model output PII, numeric fact consistency vs `EvidenceStore`, and appends regulatory disclaimer. |
| `Callbacks.AfterAgentCallbackSync` | After Agent Finishes | `FinanceAdvisorAgentV8` inline | Emits audit metrics, telemetry, and total evidence count. |

---

## 5. Evolutionary Progression (V1 through V8)

| Version | Focus | Core Concept |
|---|---|---|
| **V1** | Foundational Agent | Basic `LlmAgent`, instruction engineering, single-turn Q&A |
| **V2** | Context & Memory | Persistent session state, `{placeholder?}` templates, SQLite holdings |
| **V3** | Search Grounding | Deterministic Java tools + `GoogleSearchTool.INSTANCE` isolation |
| **V4** | Skills & Grounding | `LocalSkillSource`, `SkillToolset`, curated Markdown knowledge layers |
| **V5** | Tool Protocols (MCP) | Model Context Protocol (`McpToolset`) over stdio to Yahoo Finance |
| **V6** | Sub-Agent Delegation | Specialized division of labor (Researcher, Valuation, Risk, Synthesis) |
| **V7** | Workflow Orchestration | Deterministic execution chains: Sequential, Parallel fan-out/in, Loop |
| **V8** | Guardrails & Safety | Multi-stage lifecycle interception, PII scrubbing, injection & hallucination defense |

---

## 6. How to Run

### Interactive CLI Console
On Windows:
```cmd
test-finance-v8.bat
```
On Linux/macOS:
```bash
./test-finance-v8.sh
```

### Direct Gradle Command
```bash
./gradlew runFinanceV8 --console=plain
```

### Running the Test Suite
```bash
./gradlew test --tests com.google.adk.finance.v8.*
```

---

## 7. Guardrail Decision Model

Every guardrail evaluates incoming or outgoing content and maps to one of four deterministic outcomes:

```
ALLOW    -> Continue execution uninterrupted
SANITIZE -> Modify unsafe/sensitive content in-place -> Continue with sanitized payload
WARN     -> Continue execution while logging warning metadata / audit flags
BLOCK    -> Stop unsafe operation immediately -> Return safe fallback / end invocation
```

---

## 8. Guardrail Priority Execution Order

Guardrails execute in a strictly prioritized order:

1. **INPUT (`BeforeAgentCallback`)**:
   - PII Detection & Sanitization
   - Prompt Injection & Instruction Override Detection
   - Agent Planner
2. **TOOL (`BeforeToolCallback`)**:
   - Operation Authorization (Research allowed; Trades/Transfers blocked)
   - Ticker Validation (Format, length, SQL/command injection checks)
   - Tool Execution (External MCP / Search)
3. **OUTPUT (`AfterModelCallback`)**:
   - Output PII Redaction
   - Offensive Language Detection & Neutralization
   - Regulatory Compliance Disclaimer Enforcement
   - Factual Consistency & Hallucination Audit vs. `EvidenceStore`
   - Final Response to User

---

## 9. Failure Handling Matrix

| Failure Category | Detection Event | Lifecycle Action | Outcome Response |
|---|---|---|---|
| **User PII** | Account numbers, cards, phone, email | Detect $\rightarrow$ Sanitize $\rightarrow$ Continue | Redacted tokens (`[ACCOUNT_REDACTED]`); original PII never logged. |
| **Prompt Injection** | Instructions to ignore rules or dump secrets | Detect $\rightarrow$ Block $\rightarrow$ End Invocation | Safe rejection refusal; execution terminates cleanly. |
| **Invalid Ticker** | Malformed symbol (`INVALID-###`) or SQL injection | Detect $\rightarrow$ Block Tool Call $\rightarrow$ Error Map | Tool execution skipped; agent requests valid ticker. |
| **Unauthorized Tool** | Attempt to run `execute_trade`, `transfer_funds` | Detect $\rightarrow$ Block Tool Call $\rightarrow$ Error Map | Tool execution skipped; advisory read-only notice returned. |
| **Offensive Output** | Profanity, abusive terms | Detect $\rightarrow$ Mask / Sanitize | Offending tokens masked or safe synthesis substituted. |
| **Missing Disclaimer**| Analysis lacks mandatory compliance statement | Detect $\rightarrow$ Auto-append Disclaimer | Required regulatory statement appended automatically. |
| **Hallucinated Fact** | Model number differs from `EvidenceStore` quote | Detect $\rightarrow$ Flag Mismatch $\rightarrow$ Append Note | Audit discrepancy logged; corrective notice attached. |

---

## 10. Observability & Standardized Log Signatures

V8 emits deterministic audit logs without ever logging sensitive personal data:

- `[GUARDRAIL] PII detected`
- `[GUARDRAIL] Input sanitized`
- `[GUARDRAIL] Prompt injection detected`
- `[GUARDRAIL] Request blocked`
- `[GUARDRAIL] Ticker validation passed`
- `[GUARDRAIL] Tool operation allowed`
- `[GUARDRAIL] Tool operation blocked`
- `[GUARDRAIL] Output PII detected`
- `[GUARDRAIL] Offensive language detected`
- `[GUARDRAIL] Disclaimer verified`
- `[GUARDRAIL] Financial fact verified`
- `[GUARDRAIL] Financial fact mismatch`

---

## 11. Test Coverage

- **12 Canonical Lifecycle Tests**: Verified in [`FinanceV8CanonicalTests.java`](../src/test/java/com/google/adk/finance/v8/FinanceV8CanonicalTests.java).
- **8 Security Adversarial Tests**: Verified in [`FinanceV8SecurityTests.java`](../src/test/java/com/google/adk/finance/v8/FinanceV8SecurityTests.java).

For complete technical specifications, see [`docs/finance-agent-v8.md`](../docs/finance-agent-v8.md).
