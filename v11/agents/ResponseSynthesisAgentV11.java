package com.google.adk.finance.v11.agents;

import com.google.adk.agents.LlmAgent;
import com.google.adk.finance.v11.hooks.ResponseValidationHook;
import com.google.adk.finance.v11.ruleloader.RuleLoader;
import com.google.adk.finance.v11.ruleloader.ScopedRules;
import com.google.adk.models.BaseLlm;
import com.google.adk.socialspark.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * Specialized Response Synthesis Sub-Agent for V11.
 */
public final class ResponseSynthesisAgentV11 {
    private static final Logger logger = LoggerFactory.getLogger(ResponseSynthesisAgentV11.class);

    public static final String AGENT_NAME = "response_synthesis_agent_v11";

    public static final String BASE_INSTRUCTION = """
            You are response_synthesis_agent_v11, an institutional-grade investment research editor.
            Your role is to assemble disparate inputs from market research, fundamental analysis, and risk assessments
            into an institutional decision-support dossier.

            REPORT STRUCTURE:
            1. Executive Thesis (Summary of current status and developments)
            2. Verified Developments & News Grounding (Sourced via Google Search)
            3. Core Fundamentals & Valuation Multiples (Sourced via Yahoo Finance MCP)
            4. Risk & Scenario Analysis (Baseline, Upside, Stress-Test)
            5. Source Attribution & Limitations Summary
            6. Mandatory Regulatory Notice & Disclaimer
            """;

    public static LlmAgent create() {
        return create(RuleLoader.getInstance());
    }

    public static LlmAgent create(RuleLoader ruleLoader) {
        String modelName = AppConfig.get("FINANCE_MODEL", AppConfig.ORCHESTRATOR_MODEL);
        BaseLlm model = AppConfig.createModel(modelName);

        ScopedRules scopedRules = ruleLoader.forResponseSynthesisAgent();
        String fullInstruction = scopedRules.applyToInstruction(BASE_INSTRUCTION);

        logger.info("Initializing {} with model: {} and scoped rules: {}",
                AGENT_NAME, model.model(), scopedRules.ruleFileNames());

        return LlmAgent.builder()
                .name(AGENT_NAME)
                .description("V11 Response Synthesizer - Compiles verified multi-agent inputs into institutional report.")
                .model(model)
                .instruction(fullInstruction)
                .tools(Collections.emptyList())
                .afterModelCallbackSync(new ResponseValidationHook())
                .build();
    }

    private ResponseSynthesisAgentV11() {}
}
