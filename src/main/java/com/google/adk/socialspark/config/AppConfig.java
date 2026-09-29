package com.google.adk.socialspark.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public final class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    public static String get(String key, String defaultValue) {
        String val = System.getenv(key);
        if (val == null || val.isBlank()) {
            val = dotenv.get(key);
        }
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key, "");
        if (val.isBlank()) {
            return defaultValue;
        }
        return val.equalsIgnoreCase("true") || val.equalsIgnoreCase("1");
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key, "");
        if (val.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer for {}: '{}', using default {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    // Google Cloud / GenAI
    public static final String GEMINI_API_KEY = get("GEMINI_API_KEY", get("GOOGLE_API_KEY", ""));
    public static final String GOOGLE_CLOUD_PROJECT = get("GOOGLE_CLOUD_PROJECT", "");
    public static final String GOOGLE_CLOUD_LOCATION = get("GOOGLE_CLOUD_LOCATION", "us-central1");
    public static final boolean GOOGLE_GENAI_USE_VERTEXAI = getBoolean("GOOGLE_GENAI_USE_VERTEXAI", false);

    // Ollama / OpenAI-Compatible Endpoint
    public static final String OLLAMA_API_KEY = get("OLLAMA_API_KEY", "");
    public static final String OLLAMA_BASE_URL = get("OLLAMA_BASE_URL", "https://ollama.com/v1");
    public static final String OLLAMA_DEFAULT_MODEL = get("OLLAMA_DEFAULT_MODEL", "gemma4:31b");

    // Models
    public static final String RESEARCH_MODEL = get("RESEARCH_MODEL", "gemini-2.0-flash");
    public static final String DRAFT_MODEL = get("DRAFT_MODEL", "gemma4:31b");
    public static final String ORCHESTRATOR_MODEL = get("ORCHESTRATOR_MODEL", "gemma4:31b");
    public static final String IMAGE_MODEL_ID = get("IMAGE_MODEL_ID", "gemini-3.1-flash-image");

    // Server & Execution
    public static final int PORT = getInt("PORT", 8000);
    public static final boolean DRY_RUN = getBoolean("DRY_RUN", true);
    public static final String BACKEND_PUBLIC_ORIGIN = get("BACKEND_PUBLIC_ORIGIN", "http://localhost:" + PORT);

    // Publishing & MCP
    public static final String LINKEDIN_ACCESS_TOKEN = get("LINKEDIN_ACCESS_TOKEN", "");
    public static final String BUFFER_API_KEY = get("BUFFER_API_KEY", "");
    public static final String POST_VIA = get("POST_VIA", "auto").trim().toLowerCase();
    public static final int BUFFER_REVIEW_DELAY_MINUTES = getInt("BUFFER_REVIEW_DELAY_MINUTES", 60);

    // Cloud Storage
    public static final String GCS_BUCKET_NAME = get("GCS_BUCKET_NAME", "");
    public static final boolean GCS_PUBLIC_BUCKET = getBoolean("GCS_PUBLIC_BUCKET", false);
    public static final int SIGNED_URL_EXPIRY_HOURS = getInt("SIGNED_URL_EXPIRY_HOURS", 168);

    // A2A Memory
    public static final String MEMORY_AGENT_CARD_URL = get("MEMORY_AGENT_CARD_URL", "");

    // Directories
    public static final File GALLERY_DIR = new File("gallery");
    public static final File SKILLS_DIR = new File("skills");
    public static final File MCP_DIR = new File("mcp");

    // Computed flags
    public static final boolean useLinkedIn = !POST_VIA.equals("buffer");
    public static final boolean useBuffer = !BUFFER_API_KEY.isBlank() && !POST_VIA.equals("linkedin");
    public static final boolean useGcs = !GCS_BUCKET_NAME.isBlank() || DRY_RUN;

    public static com.google.adk.models.BaseLlm createModel(String modelName) {
        String effectiveModel = (modelName != null && !modelName.isBlank()) ? modelName : RESEARCH_MODEL;

        // 1. Explicit Gemini model (e.g. for research_agent with GoogleSearchTool)
        if (effectiveModel.startsWith("gemini-") || effectiveModel.startsWith("google/")) {
            return createGeminiModel(effectiveModel);
        }

        // 2. Ollama / OpenAI-compatible endpoint
        if (!OLLAMA_API_KEY.isBlank() || !effectiveModel.startsWith("gemini-")) {
            return new com.google.adk.socialspark.models.OllamaOpenAiLlm(effectiveModel, OLLAMA_BASE_URL, OLLAMA_API_KEY);
        }

        return createGeminiModel(effectiveModel);
    }

    public static com.google.adk.models.BaseLlm createGeminiModel(String modelName) {
        try {
            if (GOOGLE_GENAI_USE_VERTEXAI) {
                com.google.genai.Client client = com.google.genai.Client.builder()
                        .project(GOOGLE_CLOUD_PROJECT)
                        .location(GOOGLE_CLOUD_LOCATION)
                        .vertexAI(true)
                        .build();
                return new com.google.adk.models.Gemini(modelName, client);
            } else if (!GEMINI_API_KEY.isBlank()) {
                com.google.genai.Client client = com.google.genai.Client.builder()
                        .apiKey(GEMINI_API_KEY)
                        .build();
                return new com.google.adk.models.Gemini(modelName, client);
            } else {
                return com.google.adk.models.LlmRegistry.getLlm(modelName);
            }
        } catch (Exception e) {
            logger.warn("Falling back to LlmRegistry for model {}: {}", modelName, e.getMessage());
            return com.google.adk.models.LlmRegistry.getLlm(modelName);
        }
    }

    private AppConfig() {}
}
