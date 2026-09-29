package com.google.adk.socialspark.models;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.Gemini;
import com.google.adk.socialspark.config.AppConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OllamaOpenAiLlmTest {

    @Test
    void testUrlNormalization() {
        OllamaOpenAiLlm llm1 = new OllamaOpenAiLlm("gemma4:31b", "https://ollama.com", "test-key");
        assertThat(llm1.getEndpointBaseUrl()).isEqualTo("https://ollama.com/v1");

        OllamaOpenAiLlm llm2 = new OllamaOpenAiLlm("gemma4:31b", "https://ollama.com/v1/", "test-key");
        assertThat(llm2.getEndpointBaseUrl()).isEqualTo("https://ollama.com/v1");

        OllamaOpenAiLlm llm3 = new OllamaOpenAiLlm("gemma4:31b", "http://localhost:11434", "");
        assertThat(llm3.getEndpointBaseUrl()).isEqualTo("http://localhost:11434/v1");

        assertThat(llm1.model()).isEqualTo("gemma4:31b");
    }

    @Test
    void testHybridModelRouting() {
        // Gemini model routes to Gemini (used by research_agent)
        BaseLlm geminiModel = AppConfig.createModel("gemini-2.5-flash");
        assertThat(geminiModel).isInstanceOf(Gemini.class);

        // Ollama model routes to OllamaOpenAiLlm (used by draft_agent & orchestrator)
        BaseLlm ollamaModel = AppConfig.createModel("gemma4:31b");
        assertThat(ollamaModel).isInstanceOf(OllamaOpenAiLlm.class);
        assertThat(ollamaModel.model()).isEqualTo("gemma4:31b");
    }
}
