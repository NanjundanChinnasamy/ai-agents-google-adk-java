# Finance Advisor V4 — Domain Skills & Project Grounding Knowledge (`finance.v4`)

> **Milestone 4 Objective**: Extend Finance Advisor V3 by decoupling extensive institutional guidelines, valuation principles, and accounting rules from giant system instructions into **dynamic domain skills** (`SkillToolset`) and **curated project grounding knowledge** (`ProjectKnowledgeTool`) using Google ADK Java. Learn how to architect a **Three-Tier Knowledge Hierarchy** (Model Knowledge vs. Project Knowledge vs. Live Web Information), execute on-demand Markdown skills from `skills/finance/`, and produce transparent, fully cited decision-support reports.

---

## 1. High-Level Architecture & Knowledge Layering

```
                                  +─────────────────────────────────────────+
                                  |           Finance Advisor V4            |
                                  |        (com.google.adk.LlmAgent)        |
                                  +────────────────────┬────────────────────+
                                                       │
         ┌─────────────────────┬───────────────────────┼───────────────────────┬─────────────────────┐
         ▼                     ▼                       ▼                       ▼                     ▼
+─────────────────+   +─────────────────+   +─────────────────────+   +─────────────────+   +─────────────────+
|   SkillToolset  |   | ProjectKnowledge|   |    AgentTool(res)   |   |  PortfolioMath  |   | LoadCustomer    |
| (Local Skills)  |   | (Curated Docs)  |   | (Live Google Search)|   | (Deterministic  |   | PortfolioTool   |
| skills/finance/ |   | knowledge/*.md  |   | Real-time news &    |   |  PnL, weights,  |   | (SQLite Customer|
| on-demand load  |   | glossary, ratios|   | quarterly earnings  |   |  SMA & conc.)   |   |  state context) |
+─────────────────+   +─────────────────+   +─────────────────────+   +─────────────────+   +─────────────────+
```

---

## 2. The Three-Tier Knowledge Hierarchy

Rather than forcing the LLM to hallucinate rules or bloating the system prompt with hundreds of pages of documentation, V4 establishes three explicit knowledge layers:

```
+─────────────────────────────────────────────────────────────────────────────+
| LAYER 3: CURRENT WEB INFORMATION (Live Google Search / stockmarket_res.)    |
| - Real-time stock prices, quarterly earnings reports, recent corporate news |
| - High volatility, changing daily, requires external web verification      |
+──────────────────────────────────────┬──────────────────────────────────────+
                                       │
+──────────────────────────────────────▼──────────────────────────────────────+
| LAYER 2: PROJECT GROUNDING KNOWLEDGE (knowledge/ & skills/finance/)          |
| - Authoritative project-defined definitions, ratios, multiples, and rules   |
| - Static, curated, institutional frameworks; never asserts live prices      |
+──────────────────────────────────────┬──────────────────────────────────────+
                                       │
+──────────────────────────────────────▼──────────────────────────────────────+
| LAYER 1: GENERAL MODEL KNOWLEDGE (Internal LLM Weights)                      |
| - Baseline linguistic understanding and broad concepts ("What is a stock?") |
+─────────────────────────────────────────────────────────────────────────────+
```

### Knowledge Layering Rules:
1. **General Concepts**: Project Grounding Knowledge takes precedence over internal Model Knowledge.
2. **Current Events**: Live Google Search takes precedence over Project Knowledge for empirical numbers and breaking events.
3. **No False Grounding**: Never use project knowledge documents to assert a current empirical stock quote or quarterly EPS figure.

---

## 3. Dynamic Domain Skills (`SkillToolset` & `LocalSkillSource`)

Google ADK provides `SkillToolset`, which loads domain skills structured as folders containing a `SKILL.md` file with YAML frontmatter:

### The 6 Domain Skills in `skills/finance/`:
1. `skills/finance/finance-fundamentals/`: Core financial definitions, balance sheet mechanics, and liquidity ratios.
2. `skills/finance/fundamental-analysis/`: Holistic multi-pillar analysis evaluating revenue quality, margins, capital return (ROE/ROCE), and competitive moats.
3. `skills/finance/valuation/`: Contextual interpretation of multiples (P/E, forward P/E, EV/EBITDA, DCF principles), enforcing that high multiples do not automatically imply overvaluation.
4. `skills/finance/risk-management/`: Systematic vs. unsystematic risks, single-stock and sector concentration thresholds.
5. `skills/finance/portfolio-analysis/`: Asset allocation frameworks, correlation coefficients ($\rho$), and rebalancing mechanics.
6. `skills/finance/market-research/`: Structured 8-stage methodology for corporate filings, earnings surprise analysis, and guidance revisions.

Skills are registered via:
```java
SkillToolset skillToolset = new SkillToolset(new LocalSkillSource(Path.of("skills", "finance")));
```
This gives the agent native ADK tools to inspect available skills (`list_skills`) and load instructions on demand (`load_skill`).

---

## 4. Curated Grounding Knowledge (`ProjectKnowledgeTool`)

Curated institutional documents in `knowledge/` are accessible via the custom `ProjectKnowledgeTool` (`read_project_knowledge`):
- `knowledge/glossary.md`: Essential terminology (shares, market cap, enterprise value, free cash flow, CAGR).
- `knowledge/valuation-principles.md`: Multiples framework and intrinsic value principles.
- `knowledge/fundamental-analysis.md`: Operating performance, margin trends, balance sheet strength.
- `knowledge/risk-framework.md`: Taxonomy of financial hazards, drawdowns, and portfolio risk categories.
- `knowledge/portfolio-principles.md`: Asset allocation models, diversification benefits, and horizon alignment.
- `knowledge/market-research-framework.md`: Methodical corporate research lifecycle.

---

## 5. Grounding Transparency Report Structure

Responses in V4 follow a structured, multi-section format ensuring high transparency:
```markdown
### 1. Executive Summary
Concise, direct answer addressing the core inquiry.

### 2. Project Knowledge
Principles, definitions, and analytical frameworks retrieved from curated project resources.

### 3. Current Information (if applicable)
Empirical facts, recent earnings, or live news retrieved from Google Search (citing sources, dates, and figures).

### 4. Analysis & Interpretation
Objective synthesis applying the project frameworks to the current facts without speculative bias.

---
Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice.
```

---

## 6. Key Files & Implementation Reference

| File | Package Path | Role & Purpose |
|---|---|---|
| `FinanceAdvisorAgentV4Factory.java` | `src/main/java/com/google/adk/finance/v4/` | Root factory wiring `SkillToolset`, `ProjectKnowledgeTool`, `AgentTool(search)`, `PortfolioMathTool`, and `LoadCustomerPortfolioTool`. |
| `ProjectKnowledgeTool.java` | `src/main/java/com/google/adk/finance/tools/` | Custom `BaseTool` exposing `read_project_knowledge` to read curated Markdown documents from `knowledge/`. |
| `FinanceConsoleV4.java` | `src/main/java/com/google/adk/finance/v4/` | Interactive CLI with commands for `skills`, `knowledge`, `state`, `math`, and tool execution tracking. |
| `FinanceV4IntegrationTest.java` | `src/test/java/com/google/adk/finance/v4/` | Comprehensive test suite covering the 5 canonical learning tests and tool validations. |

---

## 7. Evolutionary Comparison: V3 vs. V4

| Capability | Finance Advisor V3 | Finance Advisor V4 |
|---|:---:|:---:|
| **Conversational Analyst Persona** | Yes | Yes |
| **SQLite Customer State Ingestion** | Yes | Yes |
| **Deterministic Java Math Tool** | Yes | Yes |
| **Google Search Grounding via AgentTool** | Yes | Yes |
| **Dynamic Domain Skills (`SkillToolset`)** | No | **Yes (6 modular skills in `skills/finance/`)** |
| **Project Grounding Knowledge (`knowledge/`)**| No | **Yes (6 curated documents via `ProjectKnowledgeTool`)**|
| **Three-Tier Knowledge Hierarchy** | Implicit | **Explicitly Enforced in Prompt & Architecture** |
| **Grounding Transparency Format** | Ad-hoc | **Structured 4-Section Output Contract** |
| **Model Context Protocol (MCP)** | No | No (Introduced in V5) |
| **Specialist Multi-Agent Delegation** | No | No (Introduced in V6) |

---

## 8. How to Run & Test Finance Advisor V4

### 8.1 Interactive CLI Console
Launch the interactive terminal:
```bash
# Windows
test-finance-v4.bat

# Linux / macOS
./test-finance-v4.sh
```

Within the console, use shortcut commands:
- `skills` : Lists all available domain skills in `skills/finance/`.
- `knowledge` : Lists all curated project grounding documents in `knowledge/`.
- `state` : Displays active customer session state variables.
- `help` : Shows sample questions covering skills, knowledge, and live search.
- `exit` : Terminates the session.

### 8.2 Executing Automated Tests
Run the complete V4 unit and integration test suite:
```bash
# Windows
gradlew.bat test --tests "com.google.adk.finance.v4.*"

# Linux / macOS
./gradlew test --tests "com.google.adk.finance.v4.*"
```
