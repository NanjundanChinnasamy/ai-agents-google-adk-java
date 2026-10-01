package com.google.adk.finance.v6;

import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v2.LoadCustomerPortfolioTool;
import com.google.adk.finance.v3.PortfolioMathTool;
import com.google.adk.finance.v5.YahooFinanceMcpClientManager;
import com.google.adk.finance.v6.subagents.FundamentalAnalysisAgentV6;
import com.google.adk.finance.v6.subagents.MarketResearchAgentV6;
import com.google.adk.finance.v6.subagents.PortfolioRiskAgentV6;
import com.google.adk.finance.v6.subagents.ReportWriterAgentV6;
import com.google.adk.finance.v6.subagents.ScenarioAnalystAgentV6;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.AgentTool;
import com.google.adk.tools.mcp.McpToolset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Week 3 — Version 6: Root Orchestrator Agent (Portfolio Director / Finance Advisor V6).
 * <p>
 * Core ADK & Multi-Agent Concepts Demonstrated:
 * <ul>
 *   <li><b>Parent-to-Sub-Agent Delegation</b>: The orchestrator does not perform specialized tasks directly;
 *       it routes domain inquiries to specialized sub-agents via {@link AgentTool}.</li>
 *   <li><b>Division of Labor across 5 Specialists</b>:
 *       <ol>
 *         <li>{@code stockmarket_researcher} ({@link MarketResearchAgentV6}): Google Search web grounding & breaking news attribution.</li>
 *         <li>{@code scenario_analyst} ({@link ScenarioAnalystAgentV6}): Bull, Bear, and Baseline macro scenario modeling & stress-testing.</li>
 *         <li>{@code report_writer} ({@link ReportWriterAgentV6}): Synthesizes multi-agent research into institutional decision-support reports.</li>
 *         <li>{@code fundamental_analysis_agent} ({@link FundamentalAnalysisAgentV6}): Yahoo Finance MCP fundamentals, ratios & statements.</li>
 *         <li>{@code portfolio_risk_agent} ({@link PortfolioRiskAgentV6}): Volatility, beta, concentration & portfolio risk framework.</li>
 *       </ol>
 *   </li>
 *   <li><b>Multi-Specialist Synthesis</b>: For complex multi-pillar questions, the parent invokes multiple sub-agents,
 *       aggregates their standardized reports, and synthesizes an institutional decision-support response.</li>
 *   <li><b>Failure Isolation & Resilience</b>: If one specialist fails or lacks data, the parent presents partial
 *       findings with explicit limitations without failing the entire analysis.</li>
 *   <li><b>Observability & Tracing</b>: Sub-agent selection, execution, and return are tracked via ADK callbacks.</li>
 * </ul>
 */
public final class FinanceAdvisorAgentV6 {
    private static final Logger logger = LoggerFactory.getLogger(FinanceAdvisorAgentV6.class);

    public static final String AGENT_NAME = "finance_advisor_v6";
    public static final String ROLE_NAME = "portfolio_director";

    public static final String INSTRUCTION = """
            You are portfolio_director (Finance Advisor v6), an institutional-grade portfolio orchestrator and decision-support director.
            Your fundamental architectural role is WORKFLOW ORCHESTRATION, DELEGATION, and USER ALIGNMENT.
            You do NOT conduct web searches, compute financial ratios, model scenarios, or draft reports directly.
            Instead, you analyze the user's inquiry, delegate specialized tasks to independent sub-agents, collect their structured reports, and synthesize a cohesive final response.

            Current Session State:
            - Customer ID: {customer_id?}
            - Portfolio ID: {portfolio_id?}
            - Active Portfolio Holdings:
            {portfolio_holdings?}

            Specialized Sub-Agent Delegation Matrix:
            1. `stockmarket_researcher`:
               - DELEGATE TO THIS SPECIALIST FOR: Real-time public-web developments, recent news, C-suite changes, corporate announcements, earnings release reactions, regulatory updates, macroeconomic events, and identifying why asset prices changed.
               - Invoked when the user asks: "What happened recently?", "Any news on X?", "Why did the price change?", "What are latest developments?"

            2. `scenario_analyst`:
               - DELEGATE TO THIS SPECIALIST FOR: Modeling forward-looking macro scenarios (Baseline, Bull, Bear), stress-testing portfolio sensitivity, rate hike shocks, multiple compression, and asset allocation stress simulations.
               - Invoked when the user asks: "Model bull and bear scenarios", "Stress-test my portfolio", "What happens if rates rise?", "Simulate market correction."

            3. `report_writer`:
               - DELEGATE TO THIS SPECIALIST FOR: Synthesizing multi-agent research notes, fundamentals, and scenario findings into an executive decision-support report featuring thesis, evidence matrix, scenario table, and risk flags.
               - Invoked when the user asks: "Generate an executive report", "Compile decision-support document", "Summarize findings into a full investment brief."

            4. `fundamental_analysis_agent`:
               - DELEGATE TO THIS SPECIALIST FOR: Structured empirical company financials, valuation multiples (P/E, PEG, P/B, EV/EBITDA), profitability margins (gross, operating, net), balance sheet cash and debt, historical financial statements, and Wall Street analyst price targets via Yahoo Finance MCP.
               - Invoked when the user asks: "What are the fundamentals?", "Show balance sheet / income statement", "What is the P/E and growth?"

            5. `portfolio_risk_agent`:
               - DELEGATE TO THIS SPECIALIST FOR: Investment risk assessment, market sensitivity (beta), volatility (52-week spread), balance sheet leverage risks, concentration risk, and portfolio diversification principles.
               - Invoked when the user asks: "What are the risks?", "How volatile is this stock?", "Assess my portfolio risk."

            6. Multi-Specialist Queries (Combined Inquiries):
               - When a user query requires multiple perspectives (e.g., "Give me a current view of Infosys: what has happened recently, how are the fundamentals, what are the key risks, and compare bull/bear scenarios?"), you MUST delegate to all relevant specialists:
                 Step a: Call `stockmarket_researcher` for recent developments and price change attribution.
                 Step b: Call `fundamental_analysis_agent` for structured valuation and financial health.
                 Step c: Call `portfolio_risk_agent` for volatility, leverage, and thesis risks.
                 Step d: Call `scenario_analyst` for forward-looking macro stress-testing.
                 Step e: Collect each sub-agent report and optionally call `report_writer` to compile the executive decision document.

            7. Customer Portfolio Ingestion & Arithmetic:
               - If the user provides a Customer ID (e.g. 1001 or 1002) or asks about their holdings, call `load_customer_portfolio` to register positions in session state.
               - For exact arithmetic (cost basis, PnL, return %), use `portfolio_math`.
               - Delegate fundamental and risk analysis of the portfolio holdings to the respective sub-agents.

            Failure Isolation & Graceful Degradation:
            - If any sub-agent returns an error, times out, or indicates that data was unavailable:
              1. Do NOT fail the entire request.
              2. Explicitly document the issue in a "Data Limitations & Specialist Warnings" section.
              3. Synthesize the findings from the remaining successful specialists.
              4. NEVER fabricate or hallucinate missing data.

            Response Synthesis Structure:
            Structure your final response clearly:
            * Executive Summary: Direct, concise answer addressing the core question.
            * Specialist Findings:
              - Recent Market Developments (Reported by stockmarket_researcher)
              - Fundamental & Valuation Profile (Reported by FundamentalAnalysisAgentV6)
              - Risk Assessment & Hazards (Reported by PortfolioRiskAgentV6)
              - Macro Scenarios & Stress-Testing (Reported by ScenarioAnalystAgentV6)
            * Strategic Synthesis: Objective synthesis connecting the news catalysts with underlying valuation and risk.
            * Data Limitations & Specialist Warnings: (Include if any specialist experienced limitations or failure).
            * Mandatory Regulatory Disclaimer:
              Always conclude your response with the exact disclaimer:
              "Disclaimer: This analysis is for informational and educational decision-support purposes only and does not constitute certified investment, legal, or tax advice."
            """;

    private static final Callbacks.BeforeAgentCallbackSync beforeAgentCallback = context -> {
        logger.info("[Parent Agent Lifecycle] Portfolio Director (FinanceAdvisorAgentV6) started processing user request.");
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync afterAgentCallback = context -> {
        logger.info("[Parent Agent Lifecycle] Portfolio Director (FinanceAdvisorAgentV6) completed response synthesis.");
        return Optional.empty();
    };

    private static final Callbacks.BeforeToolCallbackSync beforeToolCallback = (invocationContext, tool, args, toolContext) -> {
        String toolName = tool.name();
        if (toolName.contains("agent") || toolName.contains("research") || toolName.contains("fundamental") || toolName.contains("risk") || toolName.contains("scenario") || toolName.contains("writer")) {
            logger.info("[Delegation Decision] Portfolio Director delegating to specialist [{}] with args: {}", toolName, args);
        } else {
            logger.info("[Parent Tool Call] Portfolio Director invoking tool [{}] with args: {}", toolName, args);
        }
        return Optional.empty();
    };

    private static final Callbacks.AfterToolCallbackSync afterToolCallback = (invocationContext, tool, args, toolContext, result) -> {
        String toolName = tool.name();
        if (toolName.contains("agent") || toolName.contains("research") || toolName.contains("fundamental") || toolName.contains("risk") || toolName.contains("scenario") || toolName.contains("writer")) {
            logger.info("[Sub-Agent Result] Specialist [{}] completed task and returned report to Portfolio Director.", toolName);
        }
        return Optional.empty();
    };

    /**
     * Creates an instance of the parent {@link FinanceAdvisorAgentV6} wired with all 5 specialized sub-agents.
     *
     * @param mcpToolset the active McpToolset for Yahoo Finance tools
     * @return the configured {@link LlmAgent}
     */
    public static LlmAgent createFinanceAdvisorAgentV6(McpToolset mcpToolset) {
        String rootModelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm rootModel = AppConfig.createModel(rootModelName);
        logger.info("Initializing {} with orchestrator model: {}", AGENT_NAME, rootModel.model());

        List<Object> toolsAndToolsets = new ArrayList<>();

        // 1. Specialist Sub-Agent 1: Market Research (Google Search Grounding)
        LlmAgent marketResearchAgent = MarketResearchAgentV6.create();
        toolsAndToolsets.add(AgentTool.create(marketResearchAgent));
        logger.info("Registered sub-agent: [{}] into {}", marketResearchAgent.name(), AGENT_NAME);

        // 2. Specialist Sub-Agent 2: Fundamental Analysis (Yahoo Finance MCP)
        LlmAgent fundamentalAnalysisAgent = FundamentalAnalysisAgentV6.create(mcpToolset);
        toolsAndToolsets.add(AgentTool.create(fundamentalAnalysisAgent));
        logger.info("Registered sub-agent: [{}] into {}", fundamentalAnalysisAgent.name(), AGENT_NAME);

        // 3. Specialist Sub-Agent 3: Portfolio Risk (MCP Volatility + Risk Frameworks)
        LlmAgent portfolioRiskAgent = PortfolioRiskAgentV6.create(mcpToolset);
        toolsAndToolsets.add(AgentTool.create(portfolioRiskAgent));
        logger.info("Registered sub-agent: [{}] into {}", portfolioRiskAgent.name(), AGENT_NAME);

        // 4. Specialist Sub-Agent 4: Scenario Analyst (Bull, Bear, Baseline Macro Scenarios & Stress-Testing)
        LlmAgent scenarioAnalystAgent = ScenarioAnalystAgentV6.create(mcpToolset);
        toolsAndToolsets.add(AgentTool.create(scenarioAnalystAgent));
        logger.info("Registered sub-agent: [{}] into {}", scenarioAnalystAgent.name(), AGENT_NAME);

        // 5. Specialist Sub-Agent 5: Report Writer (Executive Decision-Support Report Synthesis)
        LlmAgent reportWriterAgent = ReportWriterAgentV6.create();
        toolsAndToolsets.add(AgentTool.create(reportWriterAgent));
        logger.info("Registered sub-agent: [{}] into {}", reportWriterAgent.name(), AGENT_NAME);

        // 6. Custom Java Tools: Portfolio State Ingestion & Math
        toolsAndToolsets.add(new LoadCustomerPortfolioTool());
        toolsAndToolsets.add(new PortfolioMathTool());

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("Portfolio Director - Institutional financial orchestrator delegating to specialized sub-agents: stockmarket_researcher, fundamental_analysis_agent, portfolio_risk_agent, scenario_analyst, and report_writer.")
                .model(rootModel)
                .instruction(INSTRUCTION)
                .tools(toolsAndToolsets)
                .beforeAgentCallbackSync(beforeAgentCallback)
                .afterAgentCallbackSync(afterAgentCallback)
                .beforeToolCallbackSync(beforeToolCallback)
                .afterToolCallbackSync(afterToolCallback)
                .build();
    }

    /**
     * Convenience factory method that starts a new YahooFinanceMcpClientManager and creates the agent.
     */
    public static LlmAgent createFinanceAdvisorAgentV6() {
        YahooFinanceMcpClientManager clientManager = new YahooFinanceMcpClientManager();
        clientManager.start();
        return createFinanceAdvisorAgentV6(clientManager.getMcpToolset());
    }

    private FinanceAdvisorAgentV6() {}
}
