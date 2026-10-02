package com.google.adk.finance.v11.workflows;

import java.util.Map;
import java.util.Optional;

/**
 * Deterministic lifecycle interception contract for V11 sequential workflow stages.
 */
public interface WorkflowStageHook {

    Optional<String> beforeStage(String stageName, Map<String, Object> stageContext);

    Optional<String> afterStage(String stageName, Map<String, Object> stageContext, String stageOutput, long durationMs);
}
