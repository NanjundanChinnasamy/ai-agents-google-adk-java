package com.google.adk.socialspark.tools;

import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.Candidate;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class GenerateImageTool extends BaseTool {
    private static final Logger logger = LoggerFactory.getLogger(GenerateImageTool.class);

    public GenerateImageTool() {
        super("generate_image", "Generates an illustration image for a social media post.");
    }

    @Override
    public Optional<FunctionDeclaration> declaration() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("prompt", Schema.builder()
                .type(Type.Known.STRING)
                .description("A detailed visual description of the image to generate.")
                .build());

        Schema parameters = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(properties)
                .required(List.of("prompt"))
                .build();

        return Optional.of(FunctionDeclaration.builder()
                .name("generate_image")
                .description("Generates an illustration image for a social media post.")
                .parameters(parameters)
                .build());
    }

    public static Map<String, Object> generate(String prompt) {
        Map<String, Object> result = new HashMap<>();
        if (prompt == null || prompt.isBlank()) {
            result.put("status", "error");
            result.put("error", "Missing required prompt for image generation.");
            return result;
        }

        String model = AppConfig.IMAGE_MODEL_ID;

        try {
            Client client;
            if (AppConfig.GOOGLE_GENAI_USE_VERTEXAI) {
                client = Client.builder()
                        .project(AppConfig.GOOGLE_CLOUD_PROJECT)
                        .location(AppConfig.GOOGLE_CLOUD_LOCATION)
                        .vertexAI(true)
                        .build();
            } else if (!AppConfig.GEMINI_API_KEY.isBlank()) {
                client = Client.builder()
                        .apiKey(AppConfig.GEMINI_API_KEY)
                        .build();
            } else {
                client = new Client();
            }

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities("TEXT", "IMAGE")
                    .build();

            GenerateContentResponse response = client.models.generateContent(model, prompt, config);

            List<Candidate> candidates = response.candidates().orElse(List.of());
            if (candidates.isEmpty()) {
                result.put("status", "error");
                result.put("error", "Model returned no candidates (likely blocked by safety filters).");
                return result;
            }

            for (Candidate candidate : candidates) {
                List<Part> parts = candidate.content().flatMap(c -> c.parts()).orElse(List.of());
                for (Part part : parts) {
                    Optional<Blob> inlineBlob = part.inlineData();
                    if (inlineBlob.isPresent()) {
                        Blob blob = inlineBlob.get();
                        String mimeType = blob.mimeType().orElse("image/png");
                        if (mimeType.startsWith("image/")) {
                            AppConfig.GALLERY_DIR.mkdirs();
                            String ext = mimeType.contains("/") ? mimeType.substring(mimeType.lastIndexOf('/') + 1) : "png";
                            String fileName = "post-image-" + Instant.now().getEpochSecond() + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
                            File targetFile = new File(AppConfig.GALLERY_DIR, fileName);

                            byte[] bytes = blob.data().orElse(null);
                            if (bytes != null && bytes.length > 0) {
                                Files.write(targetFile.toPath(), bytes);
                                logger.info("Generated image saved to {}", targetFile.getAbsolutePath());
                                result.put("status", "success");
                                result.put("image_path", targetFile.getAbsolutePath());
                                return result;
                            }
                        }
                    }
                }
            }

            result.put("status", "error");
            result.put("error", "Model returned no image data.");
            return result;
        } catch (Exception e) {
            logger.error("Failed to generate image: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("error", e.getMessage() != null ? e.getMessage() : e.toString());
            return result;
        }
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
        String prompt = (String) args.get("prompt");
        return Single.just(generate(prompt));
    }
}
