package com.google.adk.socialspark.agui;

import java.util.List;
import java.util.Map;

public final class AgUiModels {

    public record RunAgentInput(
            String threadId,
            String runId,
            Map<String, Object> state,
            List<AgUiMessage> messages,
            List<Object> tools,
            List<String> forwardEvents
    ) {}

    public record AgUiMessage(
            String id,
            String role,
            Object content,
            String name,
            List<AgUiToolCall> toolCalls,
            String toolCallId
    ) {}

    public record AgUiToolCall(
            String id,
            String type,
            AgUiFunctionCall function
    ) {}

    public record AgUiFunctionCall(
            String name,
            String arguments
    ) {}

    public record AgentStateRequest(
            String threadId
    ) {}

    public record AgentStateResponse(
            boolean exists,
            Map<String, Object> state,
            List<AgUiMessage> messages
    ) {}

    public abstract static class AgUiEvent {
        private final String type;

        protected AgUiEvent(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public static class RunStarted extends AgUiEvent {
        private final String runId;
        private final String threadId;

        public RunStarted(String runId, String threadId) {
            super("RUN_STARTED");
            this.runId = runId;
            this.threadId = threadId;
        }

        public String getRunId() { return runId; }
        public String getThreadId() { return threadId; }
    }

    public static class StateSnapshot extends AgUiEvent {
        private final Map<String, Object> snapshot;

        public StateSnapshot(Map<String, Object> snapshot) {
            super("STATE_SNAPSHOT");
            this.snapshot = snapshot;
        }

        public Map<String, Object> getSnapshot() { return snapshot; }
    }

    public static class TextMessageStart extends AgUiEvent {
        private final String messageId;
        private final String role;

        public TextMessageStart(String messageId, String role) {
            super("TEXT_MESSAGE_START");
            this.messageId = messageId;
            this.role = role != null ? role : "assistant";
        }

        public String getMessageId() { return messageId; }
        public String getRole() { return role; }
    }

    public static class TextMessageContent extends AgUiEvent {
        private final String messageId;
        private final String delta;

        public TextMessageContent(String messageId, String delta) {
            super("TEXT_MESSAGE_CONTENT");
            this.messageId = messageId;
            this.delta = delta;
        }

        public String getMessageId() { return messageId; }
        public String getDelta() { return delta; }
    }

    public static class TextMessageEnd extends AgUiEvent {
        private final String messageId;

        public TextMessageEnd(String messageId) {
            super("TEXT_MESSAGE_END");
            this.messageId = messageId;
        }

        public String getMessageId() { return messageId; }
    }

    public static class ToolCallStart extends AgUiEvent {
        private final String toolCallId;
        private final String toolCallName;

        public ToolCallStart(String toolCallId, String toolCallName) {
            super("TOOL_CALL_START");
            this.toolCallId = toolCallId;
            this.toolCallName = toolCallName;
        }

        public String getToolCallId() { return toolCallId; }
        public String getToolCallName() { return toolCallName; }
    }

    public static class ToolCallArgs extends AgUiEvent {
        private final String toolCallId;
        private final String delta;

        public ToolCallArgs(String toolCallId, String delta) {
            super("TOOL_CALL_ARGS");
            this.toolCallId = toolCallId;
            this.delta = delta;
        }

        public String getToolCallId() { return toolCallId; }
        public String getDelta() { return delta; }
    }

    public static class ToolCallEnd extends AgUiEvent {
        private final String toolCallId;

        public ToolCallEnd(String toolCallId) {
            super("TOOL_CALL_END");
            this.toolCallId = toolCallId;
        }

        public String getToolCallId() { return toolCallId; }
    }

    public static class ToolCallResult extends AgUiEvent {
        private final String toolCallId;
        private final Object result;

        public ToolCallResult(String toolCallId, Object result) {
            super("TOOL_CALL_RESULT");
            this.toolCallId = toolCallId;
            this.result = result;
        }

        public String getToolCallId() { return toolCallId; }
        public Object getResult() { return result; }
    }

    public static class RunFinished extends AgUiEvent {
        private final String runId;
        private final String threadId;

        public RunFinished(String runId, String threadId) {
            super("RUN_FINISHED");
            this.runId = runId;
            this.threadId = threadId;
        }

        public String getRunId() { return runId; }
        public String getThreadId() { return threadId; }
    }

    public static class RunError extends AgUiEvent {
        private final String message;

        public RunError(String message) {
            super("RUN_ERROR");
            this.message = message;
        }

        public String getMessage() { return message; }
    }

    private AgUiModels() {}
}
