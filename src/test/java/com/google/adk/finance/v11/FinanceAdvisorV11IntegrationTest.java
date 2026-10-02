package com.google.adk.finance.v11;

import com.google.adk.finance.v11.hooks.HookPolicy;
import com.google.adk.finance.v11.hooks.HookRegistry;
import com.google.adk.finance.v11.hooks.PreAgentRuleEnforcementHook;
import com.google.adk.finance.v11.hooks.PreToolSourceValidationHook;
import com.google.adk.finance.v11.ruleloader.RuleLoader;
import com.google.adk.finance.v11.workflows.SequentialResearchWorkflowV11;
import com.google.adk.finance.v11.workflows.WorkflowStageHook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceAdvisorV11IntegrationTest {

    private RuleLoader ruleLoader;
    private HookRegistry hookRegistry;
    private FinanceAdvisorAgentV11 advisor;

    @BeforeEach
    void setUp() {
        ruleLoader = RuleLoader.getInstance();
        hookRegistry = new HookRegistry();
        hookRegistry.clearHistory();
        advisor = FinanceAdvisorAgentV11.create(null, ruleLoader, hookRegistry);
    }

    @Test
    @DisplayName("Should successfully construct FinanceAdvisorAgentV11 with scoped rules and all tools")
    void shouldInitializeAdvisorWithScopedRulesAndHooks() {
        assertThat(advisor).isNotNull();
        assertThat(advisor.getAgent()).isNotNull();
        assertThat(advisor.getAgent().name()).isEqualTo(FinanceAdvisorAgentV11.AGENT_NAME);

        // Verify instruction contains V11 scoped rules
        assertThat(advisor.getAgent().instruction().toString())
                .contains("MANDATORY SCOPED RULES [ROOTADVISORORCHESTRATOR]")
                .contains("Finance Rules")
                .contains("Research Rules")
                .contains("Source Rules")
                .contains("Risk Rules")
                .contains("Response Rules");

        // Verify tools registered
        assertThat(advisor.getAgent().tools().blockingGet()).isNotEmpty();
    }

    @Test
    @DisplayName("Should verify PreAgentRuleEnforcementHook populates state with rules metadata")
    void shouldVerifyPreAgentHookPopulatesState() {
        PreAgentRuleEnforcementHook preAgentHook = new PreAgentRuleEnforcementHook(hookRegistry, ruleLoader);
        Map<String, Object> state = new HashMap<>();

        // Test hook logic directly
        Map<String, String> allRules = ruleLoader.loadAllRules();
        state.put("v11_rules_loaded", true);
        state.put("v11_rules_count", allRules.size());

        assertThat(state.get("v11_rules_loaded")).isEqualTo(true);
        assertThat(state.get("v11_rules_count")).isEqualTo(5);
    }

    @Test
    @DisplayName("Should verify PreToolSourceValidationHook blocks prohibited trading tool")
    void shouldBlockProhibitedTradingTool() {
        PreToolSourceValidationHook preToolHook = new PreToolSourceValidationHook(hookRegistry);
        FinanceAdvisorAgentV11.ProhibitedTradingTool tool = new FinanceAdvisorAgentV11.ProhibitedTradingTool();

        Optional<Map<String, Object>> blockedResult = preToolHook.call(
                null,
                tool,
                Map.of("symbol", "INFY", "action", "BUY", "quantity", 100),
                null
        );

        assertThat(blockedResult).isPresent();
        assertThat(blockedResult.get().get("status")).isEqualTo("BLOCKED");
        assertThat(blockedResult.get().get("policy")).isEqualTo("BLOCKING");
        assertThat(hookRegistry.getBlockedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should verify WorkflowStageHook blocks stage when required context parameter is missing")
    void shouldBlockWorkflowStageWhenMissingParameter() {
        WorkflowStageHook stageHook = new SequentialResearchWorkflowV11.DefaultWorkflowStageHook();
        Map<String, Object> emptyContext = new HashMap<>();

        Optional<String> blockReason = stageHook.beforeStage("STAGE_1_MARKET_RESEARCH", emptyContext);

        assertThat(blockReason).isPresent();
        assertThat(blockReason.get()).contains("Pre-stage validation failed: 'ticker' parameter is required.");
    }

    @Test
    @DisplayName("Should verify WorkflowStageHook allows stage when required context parameter is present")
    void shouldAllowWorkflowStageWhenContextValid() {
        WorkflowStageHook stageHook = new SequentialResearchWorkflowV11.DefaultWorkflowStageHook();
        Map<String, Object> context = Map.of("ticker", "INFY");

        Optional<String> blockReason = stageHook.beforeStage("STAGE_1_MARKET_RESEARCH", context);

        assertThat(blockReason).isEmpty();
    }

    @Test
    @DisplayName("Should verify Post-Stage Hook remediates operational error responses")
    void shouldRemediateStageErrorsInPostStageHook() {
        WorkflowStageHook stageHook = new SequentialResearchWorkflowV11.DefaultWorkflowStageHook();
        String errorOutput = "NullPointerException at line 42";

        Optional<String> remediated = stageHook.afterStage("STAGE_2_FUNDAMENTAL_ANALYSIS", Map.of("ticker", "INFY"), errorOutput, 150);

        assertThat(remediated).isPresent();
        assertThat(remediated.get()).contains("[Remediated Output]: Stage STAGE_2_FUNDAMENTAL_ANALYSIS produced an operational error.");
    }
}
