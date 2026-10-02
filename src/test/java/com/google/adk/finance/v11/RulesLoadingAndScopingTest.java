package com.google.adk.finance.v11;

import com.google.adk.finance.v11.ruleloader.RuleLoader;
import com.google.adk.finance.v11.ruleloader.ScopedRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RulesLoadingAndScopingTest {

    private RuleLoader ruleLoader;

    @BeforeEach
    void setUp() {
        ruleLoader = RuleLoader.getInstance();
        ruleLoader.clearCache();
    }

    @Test
    @DisplayName("Should successfully load all 5 V11 rules files")
    void shouldLoadAllFiveRulesFiles() {
        Map<String, String> rules = ruleLoader.loadAllRules();
        assertThat(rules).hasSize(5);

        assertThat(rules).containsKeys(
                RuleLoader.FINANCE_RULES,
                RuleLoader.RESEARCH_RULES,
                RuleLoader.SOURCE_RULES,
                RuleLoader.RISK_RULES,
                RuleLoader.RESPONSE_RULES
        );

        for (Map.Entry<String, String> entry : rules.entrySet()) {
            assertThat(entry.getValue())
                    .as("Rule content for %s should not be blank", entry.getKey())
                    .isNotBlank()
                    .doesNotStartWith("# Error");
        }
    }

    @Test
    @DisplayName("Should verify specific rules content for source-selection mapping")
    void shouldVerifySourceRulesContent() {
        String sourceRules = ruleLoader.loadRule(RuleLoader.SOURCE_RULES);
        assertThat(sourceRules)
                .contains("Yahoo Finance MCP")
                .contains("Google Search")
                .contains("LoadCustomerPortfolioTool")
                .contains("PortfolioMathTool");
    }

    @Test
    @DisplayName("Should verify rule scoping for Market Research Agent")
    void shouldVerifyMarketResearchScoping() {
        ScopedRules scope = ruleLoader.forMarketResearchAgent();
        assertThat(scope.scopeName()).isEqualTo("MarketResearch");
        assertThat(scope.ruleFileNames()).containsExactly(
                RuleLoader.FINANCE_RULES,
                RuleLoader.RESEARCH_RULES,
                RuleLoader.SOURCE_RULES
        );

        String applied = scope.applyToInstruction("Base instruction");
        assertThat(applied)
                .contains("MANDATORY SCOPED RULES [MARKETRESEARCH]")
                .contains("Recency & Current Grounding")
                .contains("Source-Selection Mapping");
    }

    @Test
    @DisplayName("Should verify rule scoping for Fundamental Analysis Agent")
    void shouldVerifyFundamentalAnalysisScoping() {
        ScopedRules scope = ruleLoader.forFundamentalAnalysisAgent();
        assertThat(scope.scopeName()).isEqualTo("FundamentalAnalysis");
        assertThat(scope.ruleFileNames()).containsExactly(
                RuleLoader.FINANCE_RULES,
                RuleLoader.SOURCE_RULES
        );

        String applied = scope.applyToInstruction("Base instruction");
        assertThat(applied)
                .contains("MANDATORY SCOPED RULES [FUNDAMENTALANALYSIS]")
                .contains("Truthfulness & Non-Fabrication");
    }

    @Test
    @DisplayName("Should verify rule scoping for Risk Analysis Agent")
    void shouldVerifyRiskAnalysisScoping() {
        ScopedRules scope = ruleLoader.forRiskAnalysisAgent();
        assertThat(scope.scopeName()).isEqualTo("RiskAnalysis");
        assertThat(scope.ruleFileNames()).containsExactly(
                RuleLoader.FINANCE_RULES,
                RuleLoader.RISK_RULES
        );

        String applied = scope.applyToInstruction("Base instruction");
        assertThat(applied)
                .contains("MANDATORY SCOPED RULES [RISKANALYSIS]")
                .contains("Identification of Material Risks");
    }

    @Test
    @DisplayName("Should verify rule scoping for Response Synthesis Agent")
    void shouldVerifyResponseSynthesisScoping() {
        ScopedRules scope = ruleLoader.forResponseSynthesisAgent();
        assertThat(scope.scopeName()).isEqualTo("ResponseSynthesis");
        assertThat(scope.ruleFileNames()).containsExactly(
                RuleLoader.FINANCE_RULES,
                RuleLoader.RESPONSE_RULES
        );

        String applied = scope.applyToInstruction("Base instruction");
        assertThat(applied)
                .contains("MANDATORY SCOPED RULES [RESPONSESYNTHESIS]")
                .contains("Regulatory Notice & Disclaimer");
    }
}
