package com.google.adk.socialspark.agui;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.socialspark.agents.SocialPosterAgentFactory;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.Part;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class AgUiEventTranslator {
    private static final Logger logger = LoggerFactory.getLogger(AgUiEventTranslator.class);
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private final Runner runner;

    public AgUiEventTranslator() {
        LlmAgent rootAgent = SocialPosterAgentFactory.createRootAgent();
        this.runner = new InMemoryRunner(rootAgent, "social_poster");
    }

    public AgUiEventTranslator(Runner runner) {
        this.runner = runner;
    }

    private void sendEvent(Consumer<String> sseEmitter, AgUiModels.AgUiEvent event) {
        String json = gson.toJson(event);
        sseEmitter.accept("data: " + json + "\n\n");
    }

    public Content extractUserContent(AgUiModels.AgUiMessage message) {
        Object raw = message.content();
        List<Part> parts = new ArrayList<>();

        if (raw instanceof String str) {
            parts.add(Part.fromText(str));
        } else if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String type = (String) map.get("type");
                    if ("text".equals(type) || map.containsKey("text")) {
                        String text = (String) map.get("text");
                        if (text != null && !text.isBlank()) {
                            parts.add(Part.fromText(text));
                        }
                    } else if ("image_url".equals(type) || "image".equals(type)) {
                        Object imgUrlObj = map.get("image_url");
                        String url = null;
                        if (imgUrlObj instanceof Map<?, ?> imgMap) {
                            url = (String) imgMap.get("url");
                        } else if (map.get("url") instanceof String directUrl) {
                            url = directUrl;
                        }

                        String data = (String) map.get("data");
                        Object mimeObj = map.get("mimeType");
                        String mimeType = mimeObj instanceof String s ? s : "image/png";

                        if (url != null && url.startsWith("data:")) {
                            int commaIdx = url.indexOf(',');
                            if (commaIdx != -1) {
                                String meta = url.substring(5, commaIdx);
                                String base64Data = url.substring(commaIdx + 1);
                                String mime = meta.split(";")[0];
                                byte[] bytes = Base64.getDecoder().decode(base64Data);
                                parts.add(Part.fromBytes(bytes, mime));
                            }
                        } else if (data != null) {
                            byte[] bytes = Base64.getDecoder().decode(data);
                            parts.add(Part.fromBytes(bytes, mimeType));
                        }
                    }
                }
            }
        } else if (raw != null) {
            parts.add(Part.fromText(raw.toString()));
        }

        if (parts.isEmpty()) {
            parts.add(Part.fromText(""));
        }

        return Content.builder()
                .role("user")
                .parts(parts)
                .build();
    }

    public void handleRun(AgUiModels.RunAgentInput input, Consumer<String> sseEmitter) {
        String threadId = (input.threadId() != null && !input.threadId().isBlank())
                ? input.threadId()
                : UUID.randomUUID().toString();
        String runId = (input.runId() != null && !input.runId().isBlank())
                ? input.runId()
                : UUID.randomUUID().toString();

        SessionStore.ThreadSession session = SessionStore.getOrCreate(threadId);

        if (input.state() != null) {
            session.getState().putAll(input.state());
        }

        sendEvent(sseEmitter, new AgUiModels.RunStarted(runId, threadId));
        sendEvent(sseEmitter, new AgUiModels.StateSnapshot(new HashMap<>(session.getState())));

        AgUiModels.AgUiMessage lastUserMsg = null;
        if (input.messages() != null) {
            for (AgUiModels.AgUiMessage msg : input.messages()) {
                if ("user".equalsIgnoreCase(msg.role())) {
                    lastUserMsg = msg;
                }
            }
        }

        if (lastUserMsg != null) {
            session.getMessages().add(lastUserMsg);
        }

        Content userContent = lastUserMsg != null ? extractUserContent(lastUserMsg) : Content.builder().role("user").parts(List.of(Part.fromText(""))).build();
        String assistantMessageId = "msg-" + UUID.randomUUID().toString().substring(0, 8);
        StringBuilder accumulatedText = new StringBuilder();
        boolean[] textStarted = new boolean[]{false};

        try {
            ensureSession(runner, "devcamp-user", threadId, session.getState());
            RunConfig runConfig = RunConfig.builder().build();
            Flowable<Event> eventFlow = runner.runAsync(
                    "devcamp-user",
                    threadId,
                    userContent,
                    runConfig,
                    session.getState()
            );

            eventFlow.blockingForEach(event -> {
                if (event.actions() != null) {
                    Map<String, Object> delta = event.actions().stateDelta();
                    if (delta != null && !delta.isEmpty()) {
                        session.getState().putAll(delta);
                        sendEvent(sseEmitter, new AgUiModels.StateSnapshot(new HashMap<>(session.getState())));
                    }
                }

                if (event.content().isPresent()) {
                    Content content = event.content().get();
                    List<Part> parts = content.parts().orElse(List.of());
                    for (Part part : parts) {
                        part.text().ifPresent(chunk -> {
                            if (!chunk.isEmpty()) {
                                if (!textStarted[0]) {
                                    sendEvent(sseEmitter, new AgUiModels.TextMessageStart(assistantMessageId, "assistant"));
                                    textStarted[0] = true;
                                }
                                accumulatedText.append(chunk);
                                sendEvent(sseEmitter, new AgUiModels.TextMessageContent(assistantMessageId, chunk));
                            }
                        });
                    }
                }

                for (FunctionCall call : event.functionCalls()) {
                    String callId = call.id().orElse("call-" + UUID.randomUUID().toString().substring(0, 8));
                    String callName = call.name().orElse("");
                    String argsJson = gson.toJson(call.args().orElse(Map.of()));

                    sendEvent(sseEmitter, new AgUiModels.ToolCallStart(callId, callName));
                    sendEvent(sseEmitter, new AgUiModels.ToolCallArgs(callId, argsJson));
                    sendEvent(sseEmitter, new AgUiModels.ToolCallEnd(callId));
                }

                for (FunctionResponse resp : event.functionResponses()) {
                    String callId = resp.id().orElse("");
                    Map<String, Object> respMap = resp.response().orElse(Map.of());
                    sendEvent(sseEmitter, new AgUiModels.ToolCallResult(callId, respMap));
                }
            });

            if (textStarted[0]) {
                sendEvent(sseEmitter, new AgUiModels.TextMessageEnd(assistantMessageId));
                session.getMessages().add(new AgUiModels.AgUiMessage(
                        assistantMessageId,
                        "assistant",
                        accumulatedText.toString(),
                        null,
                        null,
                        null
                ));
            }

            sendEvent(sseEmitter, new AgUiModels.StateSnapshot(new HashMap<>(session.getState())));
            sendEvent(sseEmitter, new AgUiModels.RunFinished(runId, threadId));

        } catch (Throwable t) {
            logger.error("Error executing agent run: {}", t.getMessage(), t);
            if (textStarted[0]) {
                sendEvent(sseEmitter, new AgUiModels.TextMessageEnd(assistantMessageId));
            }
            sendEvent(sseEmitter, new AgUiModels.RunError(t.getMessage() != null ? t.getMessage() : "Execution failure"));
            sendEvent(sseEmitter, new AgUiModels.RunFinished(runId, threadId));
        }
    }

    private void ensureSession(Runner runner, String userId, String sessionId, Map<String, Object> state) {
        try {
            runner.sessionService()
                    .getSession(runner.appName(), userId, sessionId, Optional.empty())
                    .switchIfEmpty(runner.sessionService().createSession(runner.appName(), userId, state != null ? state : Map.of(), sessionId))
                    .blockingGet();
        } catch (Exception e) {
            logger.debug("Session already exists or initialized: {}", e.getMessage());
        }
    }
}
