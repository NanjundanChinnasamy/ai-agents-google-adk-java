# Finance Advisor V1 — Foundational Agent & Session (`finance.v1`)

> **Milestone 1 Objective**: Introduce the Google Agent Development Kit (ADK) for Java by implementing a foundational conversational financial analyst agent (`finance_advisor_v1`). Learn how to instantiate an `LlmAgent`, configure instructions and personas, abstract underlying models (Gemini vs. Ollama/OpenAI), manage multi-turn session lifecycle using `InMemoryRunner` and `SessionService`, and handle real-time streaming tokens via reactive RxJava `Flowable<Event>`.

---

## 1. High-Level Architecture & Flow

```
                                  User Prompt
                         ("Explain growth vs value")
                                      │
                                      ▼
                      +───────────────────────────────+
                      |       FinanceConsoleV1        |
                      |   (Interactive CLI Console)   |
                      +───────────────┬───────────────+
                                      │
                                      ▼
                      +───────────────────────────────+
                      |        InMemoryRunner         |
                      | - SessionService (User/Sess)  |
                      | - RunConfig & State Registry  |
                      +───────────────┬───────────────+
                                      │
                                      ▼
                      +───────────────────────────────+
                      |      finance_advisor_v1       |
                      |  (com.google.adk.LlmAgent)    |
                      | Model: gemini / gemma         |
                      | Instruction: Persona & Rules  |
                      +───────────────┬───────────────+
                                      │
                                      ▼
                      +───────────────────────────────+
                      |    Reactive RxJava Stream     |
                      | Flowable<Event> -> Text Chunks|
                      +───────────────┬───────────────+
                                      │
                                      ▼
                             Streaming Response
                      (with Mandatory Disclaimer)
```

---

## 2. Core Google ADK Concepts Demonstrated

### 2.1 Declarative Agent Definition (`LlmAgent`)
In the Google ADK, an agent is an autonomous entity configured declaratively via its builder. At Milestone 1, the agent has no external tools or skills; it relies on structured system prompts and LLM reasoning:
```java
return LlmAgent.builder()
        .name(AGENT_NAME)
        .description("Foundational conversational financial analyst for portfolio, asset, and market inquiries.")
        .model(model)
        .instruction(INSTRUCTION)
        .build();
```

### 2.2 Model Abstraction (`BaseLlm`)
The agent decouples the underlying model provider through `AppConfig.createModel()`. The system seamlessly supports:
- **Cloud Models**: `gemini-2.5-flash`, `gemini-2.5-pro` (via Google GenAI SDK).
- **Local Models**: `gemma4:31b`, `llama3.3:70b` (via Ollama or OpenAI-compatible endpoints).

### 2.3 Instruction Engineering & Persona Constraints
Financial applications require strict boundaries:
1. **Domain Taxonomy**: Explicit definitions of asset classes (Equities, Fixed Income, Commodities, Cash), fundamental metrics (P/E, P/B, EV/EBITDA, FCF), macro drivers (CPI, Fed rate hikes, yield curves), and risk metrics (Sharpe ratio, Beta, standard deviation).
2. **Neutrality & Fact vs. Speculation**: Explicit guidelines separating empirical market facts from analyst expectations.
3. **Non-Advice Invariant**: Strict prohibition against issuing personalized buy/sell commands.
4. **Mandatory Disclaimer Enforcement**: Every response concludes with:
   > *"Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice."*

### 2.4 Execution Runner & Session Lifecycle (`InMemoryRunner` & `SessionService`)
Execution is coordinated by `Runner`:
- `runner.sessionService().createSession(...)` establishes persistent conversation state keyed by `appName`, `userId`, and `sessionId`.
- `runner.runAsync(...)` dispatches the user's `Content` parts and emits a reactive stream of `Event` objects.

### 2.5 Reactive Token Streaming (`Flowable<Event>`)
ADK uses RxJava 3 `Flowable` to stream response chunks as they arrive from the model:
```java
Flowable<Event> eventFlow = runner.runAsync(userId, sessionId, userContent, runConfig, state);
eventFlow.blockingForEach(event -> {
    if (event.content().isPresent()) {
        Content content = event.content().get();
        for (Part part : content.parts().orElse(List.of())) {
            part.text().ifPresent(System.out::print);
        }
    }
});
```

---

## 3. Key Files & Implementation Reference

| File | Package Path | Role & Purpose |
|---|---|---|
| `FinanceAgentV1Factory.java` | `src/main/java/com/google/adk/finance/v1/` | Factory defining `finance_advisor_v1`, configuring persona instructions, financial frameworks, and binding the model. |
| `FinanceConsoleV1.java` | `src/main/java/com/google/adk/finance/v1/` | Interactive CLI terminal demonstrating session creation, reactive streaming, one-shot queries, and command loop. |
| `FinanceV1IntegrationTest.java` | `src/test/java/com/google/adk/finance/v1/` | Integration tests verifying agent configuration, instruction attributes, and session lifecycle. |

---

## 4. What V1 Deliberately Does NOT Contain

To isolate the foundational agent and session concepts, V1 deliberately omits:
- ❌ **External Tools**: No deterministic arithmetic or database access (introduced in V2 & V3).
- ❌ **Web Search Grounding**: No live news or market data retrieval (introduced in V3).
- ❌ **Model Context Protocol (MCP)**: No external tool servers (introduced in V5).
- ❌ **Multi-Agent Specialist Delegation**: Single agent persona only (introduced in V6).
- ❌ **Guardrails & Callbacks**: No programmatic PII redaction or prompt injection filtering (introduced in V8).

---

## 5. How to Run & Test Finance Advisor V1

### 5.1 Interactive CLI Console
Launch the interactive terminal:
```bash
# Windows
test-finance-v1.bat

# Linux / macOS
./test-finance-v1.sh
```

Or pass a one-shot query directly:
```bash
test-finance-v1.bat "Explain the impact of Federal Reserve rate hikes on growth vs dividend stocks"
```

### 5.2 Executing Automated Integration Tests
Execute the unit and integration test suite:
```bash
# Windows
gradlew.bat test --tests com.google.adk.finance.v1.FinanceV1IntegrationTest

# Linux / macOS
./gradlew test --tests com.google.adk.finance.v1.FinanceV1IntegrationTest
```
