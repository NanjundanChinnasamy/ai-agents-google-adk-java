# Finance Advisor V11 — Rules-Driven and Hook-Aware Agent (`finance.v11`)

> **Milestone 11 Objective**: Extend Finance Advisor V10 by establishing the architectural distinction between declarative behavioral guidance (**RULES**) and programmatic lifecycle interception (**HOOKS**) using Google ADK Java (`1.4.0`). Learn how to decompose monolithic system instructions into modular, scoped Markdown rules files (`v11/rules/`), enforce strict instruction precedence, implement blocking and non-blocking lifecycle hooks (`PreToolSourceValidationHook`, `PostToolObservationHook`, `ResponseValidationHook`, `PreAgentRuleEnforcementHook`), and orchestrate multi-stage workflows with deterministic stage hooks.

---

## 1. High-Level Architecture & Lifecycle Hooks Pipeline

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                    RULES                                    │
│   Persistent, deterministic Markdown instructions loaded from v11/rules/    │
│   (finance-rules.md, research-rules.md, source-rules.md, risk-rules.md)     │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Scoped to Agents via RuleLoader
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                    HOOKS                                    │
│   Programmatic lifecycle interception points executing deterministic code   │
│   before/after agent, tool, and model events to validate and enforce        │
└─────────────────────────────────────────────────────────────────────────────┘

                                LIFECYCLE PIPELINE:

               [PreAgentRuleEnforcementHook]
               (BeforeAgent, Non-Blocking: verifies rules loaded & turn ready)
                               │
                               ▼
                    FinanceAdvisorAgentV11
                               │
               ┌───────────────┴───────────────┐
               ▼                               ▼
          [Tool Call]                     [Model Call]
               │                               │
               ▼                               ▼
  [PreToolSourceValidationHook]        [ResponseValidationHook]
  (BeforeTool, BLOCKING:                (AfterModel, BLOCKING / REMEDIATING:
   validates ticker syntax,              validates source citations, non-empty
   blocks unauthorized trading)          content, auto-appends disclaimer)
               │                               │
               ▼                               ▼
    [Tool Execution: MCP / Math]         Final Safe Output
               │
               ▼
   [PostToolObservationHook]
   (AfterTool, NON-BLOCKING:
    records duration & telemetry)
```

---

## 2. The Architectural Hierarchy: Skills vs. Knowledge vs. Rules vs. Guardrails vs. Hooks

| Concept | Purpose | Location | Execution Time | Example |
|---|---|---|---|---|
| **Skill** | Reusable, multi-step procedural workflow for accomplishing a specific domain task. | `skills/finance/` | Dynamically loaded on demand via `load_skill` | `skills/finance/valuation` |
| **Knowledge** | Static reference facts, glossary definitions, and conceptual frameworks. | `knowledge/` | On-demand retrieval via `read_project_knowledge` | `knowledge/valuation-principles.md` |
| **Rule** | Mandatory behavioural constraints and boundaries that must never be violated. | `v11/rules/` | Statically loaded and scoped into agent instructions | `source-rules.md`, `finance-rules.md` |
| **Guardrail** | Perimeter defensive security mechanism blocking prompt injection and PII leaks. | `v8/guardrails/` | Programmatic pre/post execution checks | `PromptInjectionDetector` |
| **Hook** | Programmatic lifecycle interception point executing deterministic Java logic at exact execution boundaries. | `v11/hooks/` | Invoked synchronously by ADK lifecycle callbacks | `PreToolSourceValidationHook` |

---

## 3. Scoped Rules Architecture (`v11/rules/`)

Rather than maintaining giant, monolithic prompts, V11 isolates rules into modular, domain-specific Markdown files and scopes them per agent:

```
                          FinanceAdvisorAgentV11
                                     │
           ┌─────────────────────────┼─────────────────────────┐
           ▼                         ▼                         ▼
MarketResearchAgentV11    FundamentalAnalysisAgentV11   PortfolioRiskAgentV11
  ├── research-rules.md     ├── source-rules.md           ├── risk-rules.md
  ├── source-rules.md       └── finance-rules.md          └── finance-rules.md
  └── finance-rules.md
```

### The 5 Modular Rule Sets:
1. **`finance-rules.md`**: Truthfulness, empirical evidence requirement, strict non-fabrication of prices, non-advice boundary.
2. **`research-rules.md`**: Distinguishes between current web data and historical records; flags missing data gaps.
3. **`source-rules.md`**: Source authority mapping (Yahoo Finance MCP for quotes/multiples, Google Search for news, `PortfolioMathTool` for arithmetic). Strictly prohibits unauthorized transactional tools (`execute_trade`).
4. **`risk-rules.md`**: Systematic vs. unsystematic risks, volatility metrics, concentration thresholds (>25%), uncertainty disclosure.
5. **`response-rules.md`**: Structure conventions, separating factual data from model interpretation, mandatory disclaimer enforcement.

### Instruction Precedence Hierarchy:
1. **Safety Invariants & Programmatic Guardrails** (Non-overridable code checks)
2. **Rules Files** (Deterministic behavioral constraints scoped per agent)
3. **Agent System Instructions** (Core persona and operational role)
4. **Tool & MCP Results** (Empirical source ground-truth)
5. **Grounding Knowledge & Skills** (Procedural guides and reference definitions)
6. **User Requests** (Constrained within the bounds of higher tiers; cannot override rules)

---

## 4. Lifecycle Hooks Specification

Hooks implement explicit policy contracts (`HookPolicy.BLOCKING` vs. `HookPolicy.NON_BLOCKING`):

| Hook | Lifecycle Phase | ADK Callback | Policy | Behavior & Responsibility |
|---|---|---|---|---|
| **`PreToolSourceValidationHook`** | Before Tool Execution | `BeforeToolCallbackSync` | **BLOCKING** | Enforces `source-rules.md`: blocks unauthorized transactional operations (`execute_trade`, `place_order`), validates ticker format (`^[A-Z0-9.-]{1,12}$`), and validates math arguments. Returns error map on rejection. |
| **`PostToolObservationHook`** | After Tool Execution | `AfterToolCallbackSync` | **NON-BLOCKING** | Captures execution metadata (tool name, duration in ms, payload length, status) into `HookRegistry` for telemetry without persisting sensitive values. |
| **`ResponseValidationHook`** | After Model Generation | `AfterModelCallbackSync` | **BLOCKING / REMEDIATING** | Checks for non-empty output, verifies that external source data is properly attributed, checks for disclosed limitations, and automatically appends the regulatory disclaimer if omitted. |
| **`PreAgentRuleEnforcementHook`** | Before Agent Turn | `BeforeAgentCallbackSync` | **NON-BLOCKING** | Verifies that all required scoped rules files are loaded and registers readiness in session state. |

---

## 5. Workflows with Stage Hooks (`SequentialResearchWorkflowV11`)

V11 also integrates hooks into deterministic workflow pipelines:
- Pipeline: Company Research $\rightarrow$ Fundamental Analysis $\rightarrow$ Risk Analysis $\rightarrow$ Response Synthesis.
- `WorkflowStageHook`: Intercepts transitions between pipeline stages, logging stage entry, duration, and output validation before feeding data into the next stage via `{outputKey}` bindings.

---

## 6. Key Files & Implementation Reference

| File | Package Path | Role & Purpose |
|---|---|---|
| `FinanceAdvisorAgentV11.java` | `src/main/java/com/google/adk/finance/v11/` | Root factory creating `finance_advisor_v11` with scoped rules and registered lifecycle hooks. |
| `RuleLoader.java` | `src/main/java/com/google/adk/finance/v11/ruleloader/` | Deterministic parser and loader reading rules from classpath and filesystem. |
| `ScopedRules.java` | `src/main/java/com/google/adk/finance/v11/ruleloader/` | Scope container binding specific rule subsets to individual agents. |
| `HookPolicy.java` | `src/main/java/com/google/adk/finance/v11/hooks/` | Enum defining `BLOCKING` vs. `NON_BLOCKING` execution policies. |
| `HookRegistry.java` | `src/main/java/com/google/adk/finance/v11/hooks/` | Central registry coordinating hook executions, metrics, and telemetry. |
| `PreToolSourceValidationHook.java` | `src/main/java/com/google/adk/finance/v11/hooks/` | Blocking hook intercepting tool calls to validate tickers and block prohibited trades. |
| `PostToolObservationHook.java` | `src/main/java/com/google/adk/finance/v11/hooks/` | Non-blocking hook capturing post-tool execution durations and telemetry. |
| `ResponseValidationHook.java` | `src/main/java/com/google/adk/finance/v11/hooks/` | Blocking/remediating hook validating citations and enforcing disclaimers. |
| `PreAgentRuleEnforcementHook.java` | `src/main/java/com/google/adk/finance/v11/hooks/` | Pre-turn hook verifying active rule scopes. |
| `FinanceConsoleV11.java` | `src/main/java/com/google/adk/finance/v11/` | Interactive CLI terminal featuring `rules`, `hooks`, `telemetry`, and workflow test commands. |
| `RulesLoadingAndScopingTest.java` | `src/test/java/com/google/adk/finance/v11/` | Unit tests verifying rule parsing and agent scope bindings. |
| `PreToolValidationHookTest.java` | `src/test/java/com/google/adk/finance/v11/` | Unit tests verifying ticker validation and transactional tool blocking. |
| `PostToolObservationHookTest.java` | `src/test/java/com/google/adk/finance/v11/` | Unit tests verifying post-tool telemetry capture into `HookRegistry`. |
| `ResponseValidationHookTest.java` | `src/test/java/com/google/adk/finance/v11/` | Unit tests verifying disclaimer auto-injection and citation checks. |
| `FinanceAdvisorV11IntegrationTest.java` | `src/test/java/com/google/adk/finance/v11/` | Integration tests verifying end-to-end rules loading, hook execution, and agent synthesis. |

---

## 7. How to Run & Test Finance Advisor V11

### 7.1 Interactive CLI Console
Launch the interactive terminal:
```bash
# Windows
test-finance-v11.bat

# Linux / macOS
./test-finance-v11.sh
```

Within the console, use shortcut commands:
- `rules`     : Displays all active rules and scoped assignments per agent.
- `hooks`     : Lists all registered lifecycle hooks and their policies (`BLOCKING` vs. `NON_BLOCKING`).
- `telemetry` : Inspects hook invocation counts, average durations, and blocked attempts.
- `state`     : Inspects current customer portfolio session state.
- `help`      : Displays sample rule-governed questions.
- `exit`      : Terminates the console cleanly.

### 7.2 Executing Automated Tests
Execute the complete V11 rules and hooks test suite:
```bash
# Windows
gradlew.bat test --tests "com.google.adk.finance.v11.*"

# Linux / macOS
./gradlew test --tests "com.google.adk.finance.v11.*"
```
