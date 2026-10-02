# Finance Advisor V11 — Rules-Driven and Hook-Aware Agent

## 1. Overview & Core Learning Objectives

**Finance Advisor V11** introduces two foundational capabilities into the autonomous Google ADK Java architecture:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                    RULES                                    │
│       = Persistent, deterministic instructions defining what the            │
│         Finance Advisor and its specialized sub-agents MUST follow          │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                    HOOKS                                    │
│       = Programmatic lifecycle interception points executing deterministic │
│         code before/after selected agent, tool, MCP, or model events         │
└─────────────────────────────────────────────────────────────────────────────┘
```

### The Architectural Hierarchy
To understand how rules and hooks fit into the wider multi-agent paradigm:

- **Skill**: A reusable, structured procedure or workflow for accomplishing a specific domain task (e.g. `skills/finance/valuation`).
- **Knowledge**: Domain facts, definitions, and conceptual grounding (e.g. `knowledge/valuation-principles.md`).
- **Rule**: Mandatory behavioural constraints and boundaries that must never be violated (`v11/rules/*.md`).
- **Guardrail**: Perimeter defensive security mechanisms preventing malicious prompt injection and PII leakage.
- **Hook**: Programmatic lifecycle interception points executing deterministic Java logic at exact execution boundaries.

---

## 2. Directory Layout

```
v11/
├── README.md                                # Comprehensive V11 documentation & guide
├── FinanceAdvisorAgentV11.java              # Root orchestrator agent factory & lifecycle wiring
├── FinanceConsoleV11.java                   # Interactive CLI console supporting rules & hooks inspection
├── rules/
│   ├── finance-rules.md                     # General truthfulness, non-fabrication, and factual boundaries
│   ├── research-rules.md                    # Current vs historical data, recency, and gap identification
│   ├── source-rules.md                      # Source selection (Yahoo Finance MCP vs Google Search)
│   ├── risk-rules.md                        # Risk identification, volatility distinctions, uncertainty
│   └── response-rules.md                    # Data vs interpretation, source context, transparent limitations
├── hooks/
│   ├── HookPolicy.java                      # Enum contract: BLOCKING vs NON_BLOCKING
│   ├── HookResult.java                      # Outcome record (status, reason, metadata, latency)
│   ├── HookRegistry.java                    # Central registry of active hooks and execution telemetry
│   ├── PreToolSourceValidationHook.java     # Hook 1: BLOCKING pre-tool validation against source rules
│   ├── PostToolObservationHook.java         # Hook 2: NON-BLOCKING post-tool telemetry & performance capture
│   ├── ResponseValidationHook.java          # Hook 3: BLOCKING/REMEDIATING final response validation
│   └── PreAgentRuleEnforcementHook.java     # Pre-agent lifecycle hook loading and checking active rules
├── ruleloader/
│   ├── RuleLoader.java                      # Deterministic parser & loader for Markdown rules files
│   └── ScopedRules.java                     # Rule scope container mapping rules to agents
├── agents/
│   ├── MarketResearchAgentV11.java          # Scoped with research-rules.md & source-rules.md
│   ├── FundamentalAnalysisAgentV11.java     # Scoped with source-rules.md & finance-rules.md
│   ├── PortfolioRiskAgentV11.java           # Scoped with risk-rules.md & finance-rules.md
│   └── ResponseSynthesisAgentV11.java       # Scoped with response-rules.md
├── sub-agents/
│   └── README.md                            # Documentation on sub-agent rule scoping
└── workflows/
    ├── SequentialResearchWorkflowV11.java   # 4-stage sequential pipeline with pre/post stage hooks
    └── WorkflowStageHook.java               # Lifecycle hook interface for workflow stage transitions
```

---

## 3. Rules Architecture & Scoping

V11 implements **Scoped Rules** rather than monolithic instructions:

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

### Instruction Precedence Hierarchy
To guarantee safety and determinism:
1. **Safety Invariants & Programmatic Guardrails** (Non-overridable code checks)
2. **Rules Files** (Deterministic behavioral constraints scoped per agent)
3. **Agent System Instructions** (Core persona and operational role)
4. **Tool & MCP Results** (Empirical source ground-truth)
5. **Grounding Knowledge & Skills** (Procedural guides and reference definitions)
6. **User Requests** (Constrained within the bounds of higher tiers; cannot override rules)

---

## 4. Lifecycle Hooks Specification

| Hook | Lifecycle Event | ADK Interface | Policy | Description |
|---|---|---|---|---|
| **PreToolSourceValidationHook** | Before Tool Execution | `BeforeToolCallbackSync` | **BLOCKING** | Enforces `source-rules.md`: blocks prohibited operations (e.g. trading), validates ticker symbols, and validates math parameters. |
| **PostToolObservationHook** | After Tool Execution | `AfterToolCallbackSync` | **NON-BLOCKING** | Captures execution metadata (tool name, category, duration in ms, payload length, status) into `HookRegistry` without secrets. |
| **ResponseValidationHook** | After Model Generation | `AfterModelCallbackSync` | **BLOCKING / REMEDIATING** | Checks for non-empty responses, verifies source context when external data was retrieved, discloses limitations, and appends regulatory disclaimers. |
| **PreAgentRuleEnforcementHook** | Before Agent Turn | `BeforeAgentCallbackSync` | **NON-BLOCKING** | Verifies active V11 rule files and records turn readiness into session state. |

---

## 5. Verification & Testing

### Running Interactive CLI Console
```powershell
.\test-finance-v11.bat
```
Or via Gradle:
```powershell
.\gradlew.bat runFinanceV11 --console=plain -q
```

### Executing Unit and Integration Tests
```powershell
.\gradlew.bat test --tests "com.google.adk.finance.v11.*"
```
