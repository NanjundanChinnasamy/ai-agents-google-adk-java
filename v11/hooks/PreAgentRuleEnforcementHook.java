package com.google.adk.finance.v11.hooks;

import com.google.adk.agents.CallbackContext;
import com.google.adk.agents.Callbacks.BeforeAgentCallbackSync;
import com.google.adk.finance.v11.ruleloader.RuleLoader;
import com.google.genai.types.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Hook 4 — Pre-Agent Rule Enforcement Hook (NON-BLOCKING).
 */
public class PreAgentRuleEnforcementHook implements BeforeAgentCallbackSync {
    private static final Logger logger = LoggerFactory.getLogger(PreAgentRuleEnforcementHook.class);

    public static final String HOOK_NAME = "PreAgentRuleEnforcementHook";

    private final HookRegistry hookRegistry;
    private final RuleLoader ruleLoader;

    public PreAgentRuleEnforcementHook() {
        this(HookRegistry.getInstance(), RuleLoader.getInstance());
    }

    public PreAgentRuleEnforcementHook(HookRegistry hookRegistry, RuleLoader ruleLoader) {
        this.hookRegistry = Objects.requireNonNull(hookRegistry, "hookRegistry must not be null");
        this.ruleLoader = Objects.requireNonNull(ruleLoader, "ruleLoader must not be null");
    }

    @Override
    public Optional<Content> call(CallbackContext callbackContext) {
        long startTime = System.currentTimeMillis();

        try {
            Map<String, String> loadedRules = ruleLoader.loadAllRules();
            int ruleCount = loadedRules.size();

            callbackContext.state().put("v11_rules_loaded", true);
            callbackContext.state().put("v11_rules_count", ruleCount);

            long duration = System.currentTimeMillis() - startTime;
            hookRegistry.recordHookExecution(HookResult.success(
                    HOOK_NAME,
                    HookPolicy.NON_BLOCKING,
                    "V11 rules verified and loaded into agent state (" + ruleCount + " rules files)",
                    Map.of("ruleCount", ruleCount, "ruleFiles", loadedRules.keySet().toString()),
                    duration
            ));

            logger.info("[{}] Verified {} active V11 rules files in session state", HOOK_NAME, ruleCount);

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            logger.warn("[{}] Non-blocking rule verification warning: {}", HOOK_NAME, ex.getMessage());
            hookRegistry.recordHookExecution(HookResult.failure(
                    HOOK_NAME,
                    HookPolicy.NON_BLOCKING,
                    "Rule verification error: " + ex.getMessage(),
                    Map.of("error", ex.getMessage()),
                    duration
            ));
        }

        return Optional.empty();
    }
}
