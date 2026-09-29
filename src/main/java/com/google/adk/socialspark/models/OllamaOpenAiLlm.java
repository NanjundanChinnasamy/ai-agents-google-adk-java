package com.google.adk.socialspark.models;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.BaseLlmConnection;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.adk.models.chat.ChatCompletionsHttpClient;
import com.google.genai.types.HttpOptions;
import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * An adapter connecting ADK agents to Ollama via the OpenAI Chat Completions endpoint (/v1/chat/completions).
 * Uses ADK's built-in ChatCompletionsHttpClient for request/response serialization, streaming, and tool support.
 */
public class OllamaOpenAiLlm extends BaseLlm {
    private static final Logger logger = LoggerFactory.getLogger(OllamaOpenAiLlm.class);
    private final ChatCompletionsHttpClient client;
    private final String endpointBaseUrl;

    public OllamaOpenAiLlm(String modelName, String baseUrl, String apiKey) {
        super(modelName);
        this.endpointBaseUrl = normalizeBaseUrl(baseUrl);

        Map<String, String> headers = new HashMap<>();
        if (apiKey != null && !apiKey.isBlank()) {
            headers.put("Authorization", "Bearer " + apiKey.trim());
        }

        HttpOptions.Builder optionsBuilder = HttpOptions.builder()
                .baseUrl(this.endpointBaseUrl);

        if (!headers.isEmpty()) {
            optionsBuilder.headers(headers);
        }

        logger.info("Initialized OllamaOpenAiLlm for model '{}' pointing to {}", modelName, this.endpointBaseUrl);
        this.client = new ChatCompletionsHttpClient(optionsBuilder.build());
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://ollama.com/v1";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (!trimmed.endsWith("/v1")) {
            trimmed = trimmed + "/v1";
        }
        return trimmed;
    }

    @Override
    public Flowable<LlmResponse> generateContent(LlmRequest llmRequest, boolean stream) {
        LlmRequest requestToSend = llmRequest;
        if (llmRequest.model().isEmpty() || llmRequest.model().get().isBlank()) {
            requestToSend = llmRequest.toBuilder().model(model()).build();
        }
        return client.complete(requestToSend, stream);
    }

    @Override
    public BaseLlmConnection connect(LlmRequest llmRequest) {
        throw new UnsupportedOperationException("Bidirectional live connection is not supported for OpenAI chat completions endpoint");
    }

    public String getEndpointBaseUrl() {
        return endpointBaseUrl;
    }
}
