package com.google.adk.socialspark.agents;

// import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
// import com.google.adk.models.LlmResponse;
import com.google.adk.skills.LocalSkillSource;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.socialspark.tools.CheckTextLengthTool;
// import com.google.adk.socialspark.tools.GenerateImageTool;
// import com.google.adk.socialspark.tools.UploadImageTool;
// import com.google.adk.socialspark.tools.UseProvidedImageUrlTool;
import com.google.adk.tools.skills.SkillToolset;
// import com.google.genai.types.Content;
// import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import java.io.File;
import java.util.ArrayList;
import java.util.List;
// import java.util.Map;
// import java.util.Optional;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

public final class DraftAgentFactory {
    private static final Logger logger = LoggerFactory.getLogger(DraftAgentFactory.class);
    // Image generation & state callbacks commented out to support text-only drafting:
    /*
    private static final Pattern IMAGE_PLACEHOLDER_RE = Pattern.compile("(?im)^\\s*\\(?\\s*image(?:\\s+description)?\\s*:\\s*(.+?)\\)?\\s*$");

    private static final Callbacks.AfterModelCallbackSync catchImagePlaceholderCallback = (callbackContext, llmResponse) -> {
        Optional<Content> contentOpt = llmResponse.content();
        if (contentOpt.isEmpty()) {
            return Optional.empty();
        }

        List<Part> parts = contentOpt.get().parts().orElse(List.of());
        for (Part part : parts) {
            if (part.functionCall().isPresent()) {
                return Optional.empty();
            }
        }

        if (callbackContext.state().containsKey("current_image_path")) {
            return Optional.empty();
        }

        StringBuilder sb = new StringBuilder();
        for (Part part : parts) {
            part.text().ifPresent(t -> sb.append(t).append("\n"));
        }

        String text = sb.toString();
        Matcher matcher = IMAGE_PLACEHOLDER_RE.matcher(text);
        if (matcher.find()) {
            String prompt = matcher.group(1).trim();
            if (!prompt.isBlank()) {
                logger.warn("draft_agent wrote an image placeholder instead of calling generate_image; converting to tool call: {}", prompt);
                Content replacementContent = Content.builder()
                        .role("model")
                        .parts(List.of(Part.fromFunctionCall("generate_image", Map.of("prompt", prompt))))
                        .build();

                LlmResponse replacementResponse = llmResponse.toBuilder()
                        .content(replacementContent)
                        .build();
                return Optional.of(replacementResponse);
            }
        }
        return Optional.empty();
    };

    private static final Callbacks.AfterToolCallbackSync trackImageStateCallback = (invocationContext, tool, args, toolContext, result) -> {
        if (!(result instanceof Map<?, ?> responseMap)) {
            return Optional.empty();
        }

        String toolName = tool.name();
        if ("generate_image".equals(toolName) && "success".equals(responseMap.get("status"))) {
            String imagePath = (String) responseMap.get("image_path");
            if (imagePath != null) {
                toolContext.state().put("current_image_path", imagePath);
                String filename = new File(imagePath).getName();
                toolContext.state().put("current_image_display_url", AppConfig.BACKEND_PUBLIC_ORIGIN + "/outputs/" + filename);
                toolContext.state().remove("current_image_url");
                logger.info("Tracked generated image state: {}", imagePath);
            }
        } else if ("upload_image".equals(toolName) && "success".equals(responseMap.get("status")) && !Boolean.TRUE.equals(responseMap.get("dry_run"))) {
            String url = (String) responseMap.get("url");
            if (url != null) {
                toolContext.state().put("current_image_url", url);
                logger.info("Tracked uploaded image URL: {}", url);
            }
        } else if ("use_provided_image_url".equals(toolName) && "success".equals(responseMap.get("status"))) {
            String url = (String) responseMap.get("url");
            if (url != null) {
                toolContext.state().remove("current_image_path");
                toolContext.state().put("current_image_display_url", url);
                toolContext.state().put("current_image_url", url);
                logger.info("Tracked user-provided image URL: {}", url);
            }
        }
        return Optional.empty();
    };
    */

    public static LlmAgent createDraftAgent() {
        List<Object> toolsAndToolsets = new ArrayList<>();

        if (AppConfig.SKILLS_DIR.exists() && AppConfig.SKILLS_DIR.isDirectory()) {
            try {
                SkillToolset skillToolset = new SkillToolset(new LocalSkillSource(AppConfig.SKILLS_DIR.toPath()));
                toolsAndToolsets.add(skillToolset);
            } catch (Exception e) {
                logger.warn("Could not load skills from {}: {}", AppConfig.SKILLS_DIR.getAbsolutePath(), e.getMessage());
            }
        }

        // Image generation, URL handling, and GCS upload tools commented out for text-only drafting:
        // toolsAndToolsets.add(new GenerateImageTool());
        // toolsAndToolsets.add(new UseProvidedImageUrlTool());
        toolsAndToolsets.add(new CheckTextLengthTool());
        // if (AppConfig.useGcs) {
        //     toolsAndToolsets.add(new UploadImageTool());
        // }

        return LlmAgent.builder()
                .name("draft_agent")
                .description("Writes and revises the social media post draft (text only).")
                .model(AppConfig.createModel(AppConfig.DRAFT_MODEL))
                .instruction("""
                    You write social media post drafts.

                    Research notes from an earlier step (may be empty):
                    {research_notes?}

                    Style guidance from the user's past posts (may be empty):
                    {memory_notes?}

                    Rules:
                    1. Load and follow the relevant skills before drafting: post-formatter
                       (structure), platform-style (rules for the target platform), and
                       brand-voice (tone).
                    2. Ground the draft in the research notes when they exist; never invent facts.
                    3. Focus purely on writing concise, engaging text for the target platform.
                       Do not generate images, reference image URLs, or include image placeholders.
                    4. Return ONLY the draft text - no commentary about approvals or posting;
                       the orchestrator handles that.
                    """)
                .tools(toolsAndToolsets)
                .outputKey("current_draft")
                // Image handling callbacks commented out for text-only drafting:
                // .afterModelCallbackSync(catchImagePlaceholderCallback)
                // .afterToolCallbackSync(trackImageStateCallback)
                .build();
    }

    private DraftAgentFactory() {}
}
