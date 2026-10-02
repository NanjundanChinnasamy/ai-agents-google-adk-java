package com.google.adk.finance.v10.observability;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable structured event record representing a discrete lifecycle occurrence during an agent execution.
 * <p>
 * Security Invariant: Never stores raw unredacted personal, banking, or credential identifiers.
 */
public record ExecutionEvent(
        String executionId,
        String eventId,
        Instant timestamp,
        EventType eventType,
        String component,
        String agentName,
        Optional<Long> durationMs,
        String status,
        Optional<String> errorInfo,
        String safeSummary,
        Map<String, Object> metadata
) {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.of("UTC"));

    public ExecutionEvent {
        Objects.requireNonNull(executionId, "executionId must not be null");
        eventId = eventId == null ? "evt-" + UUID.randomUUID().toString().substring(0, 8) : eventId;
        timestamp = timestamp == null ? Instant.now() : timestamp;
        Objects.requireNonNull(eventType, "eventType must not be null");
        component = component == null ? eventType.defaultComponent() : component;
        agentName = agentName == null ? "finance_advisor_v10" : agentName;
        durationMs = durationMs == null ? Optional.empty() : durationMs;
        status = status == null ? "COMPLETED" : status;
        errorInfo = errorInfo == null ? Optional.empty() : errorInfo;
        safeSummary = safeSummary == null ? "" : safeSummary;
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    /**
     * Formats this event as a single line in a human-readable ASCII execution trace timeline.
     * Example: "20:00:01.110  TOOL_CALL_COMPLETED [TOOL] YahooFinance completed in 310ms (status=SUCCESS)"
     */
    public String toFormattedLine() {
        String timeStr = TIME_FORMATTER.format(timestamp);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-12s %-25s [%-10s] %s", timeStr, eventType.name(), component, safeSummary));

        durationMs.ifPresent(d -> sb.append(String.format(" (duration: %dms)", d)));
        if (!"SUCCESS".equalsIgnoreCase(status) && !"COMPLETED".equalsIgnoreCase(status) && !"STARTED".equalsIgnoreCase(status)) {
            sb.append(String.format(" [status=%s]", status));
        }
        errorInfo.ifPresent(err -> sb.append(String.format(" [error=%s]", err)));
        return sb.toString();
    }

    public static Builder builder(String executionId, EventType eventType) {
        return new Builder(executionId, eventType);
    }

    public static class Builder {
        private final String executionId;
        private final EventType eventType;
        private String eventId;
        private Instant timestamp = Instant.now();
        private String component;
        private String agentName = "finance_advisor_v10";
        private Long durationMs;
        private String status = "SUCCESS";
        private String errorInfo;
        private String safeSummary = "";
        private final Map<String, Object> metadata = new LinkedHashMap<>();

        public Builder(String executionId, EventType eventType) {
            this.executionId = Objects.requireNonNull(executionId, "executionId must not be null");
            this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
            this.component = eventType.defaultComponent();
        }

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder component(String component) {
            this.component = component;
            return this;
        }

        public Builder agentName(String agentName) {
            this.agentName = agentName;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder errorInfo(String errorInfo) {
            this.errorInfo = errorInfo;
            return this;
        }

        public Builder safeSummary(String safeSummary) {
            this.safeSummary = safeSummary;
            return this;
        }

        public Builder metadata(String key, Object value) {
            if (key != null && value != null) {
                this.metadata.put(key, value);
            }
            return this;
        }

        public Builder metadata(Map<String, Object> meta) {
            if (meta != null) {
                this.metadata.putAll(meta);
            }
            return this;
        }

        public ExecutionEvent build() {
            return new ExecutionEvent(
                    executionId,
                    eventId,
                    timestamp,
                    eventType,
                    component,
                    agentName,
                    Optional.ofNullable(durationMs),
                    status,
                    Optional.ofNullable(errorInfo),
                    safeSummary,
                    metadata
            );
        }
    }
}
