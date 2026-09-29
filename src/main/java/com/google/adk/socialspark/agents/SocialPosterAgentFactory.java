package com.google.adk.socialspark.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.Callbacks;
import com.google.adk.agents.LlmAgent;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.socialspark.db.PostRepository;
import com.google.adk.tools.AgentTool;
import com.google.adk.tools.BaseToolset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SocialPosterAgentFactory {
    private static final Logger logger = LoggerFactory.getLogger(SocialPosterAgentFactory.class);

    private static final Callbacks.BeforeAgentCallbackSync initStageCallback = context -> {
        if (!context.state().containsKey("pipeline_stage")) {
            context.state().put("pipeline_stage", "idea");
        }
        return Optional.empty();
    };

    private static final Callbacks.AfterAgentCallbackSync stageAfterAgentCallback = context -> {
        Object stage = context.state().get("pipeline_stage");
        if ("researching".equals(stage) || "drafting".equals(stage)) {
            context.state().put("pipeline_stage", "awaiting_approval");
        }
        return Optional.empty();
    };

    private static final Callbacks.BeforeToolCallbackSync delayBufferPostCallback = (invocationContext, tool, args, toolContext) -> {
        if ("create_post".equals(tool.name()) && args.containsKey("channelId") && AppConfig.BUFFER_REVIEW_DELAY_MINUTES > 0) {
            Map<String, Object> modified = new HashMap<>(args);
            Instant due = Instant.now().plus(AppConfig.BUFFER_REVIEW_DELAY_MINUTES, ChronoUnit.MINUTES);
            String dueStr = DateTimeFormatter.ISO_INSTANT.format(due);
            modified.put("mode", "customScheduled");
            modified.put("dueAt", dueStr);
            logger.info("Buffer post delayed to {} (BUFFER_REVIEW_DELAY_MINUTES={}) for review", dueStr, AppConfig.BUFFER_REVIEW_DELAY_MINUTES);
            return Optional.of(modified);
        }
        return Optional.empty();
    };

    private static final Callbacks.AfterToolCallbackSync trackStageAndSavePostCallback = (invocationContext, tool, args, toolContext, result) -> {
        String toolName = tool.name();
        if ("research_agent".equals(toolName)) {
            toolContext.state().put("pipeline_stage", "researching");
        } else if ("draft_agent".equals(toolName)) {
            toolContext.state().put("pipeline_stage", "drafting");
        } else if ("memory_agent".equals(toolName)) {
            toolContext.state().put("pipeline_stage", "consulting_memory");
        } else if ("create_post".equals(toolName) || "linkedin_create_post".equals(toolName)) {
            if (result instanceof Map<?, ?> responseMap && !Boolean.TRUE.equals(responseMap.get("isError")) && !"error".equals(responseMap.get("status"))) {
                toolContext.state().put("pipeline_stage", "posted");
                String platform = args.containsKey("channelId") ? "Buffer" : "LinkedIn";
                String postUrl = (String) responseMap.get("post_url");
                if (postUrl == null) {
                    postUrl = (String) responseMap.get("url");
                }

                PostRepository.savePost(
                        platform,
                        (String) args.getOrDefault("text", ""),
                        postUrl,
                        (String) toolContext.state().get("current_image_path"),
                        (String) toolContext.state().get("current_image_url")
                );
                logger.info("Recorded successfully published post to SQLite store: platform={}", platform);
            }
        }
        return Optional.empty();
    };

    public static LlmAgent createRootAgent() {
        LlmAgent researchAgent = ResearchAgentFactory.createResearchAgent();
        LlmAgent draftAgent = DraftAgentFactory.createDraftAgent();
        BaseAgent memoryAgent = MemoryAgentFactory.createMemoryAgent();

        List<Object> toolsAndToolsets = new ArrayList<>();
        if (memoryAgent != null) {
            toolsAndToolsets.add(AgentTool.create(memoryAgent));
        }
        toolsAndToolsets.add(AgentTool.create(researchAgent));
        toolsAndToolsets.add(AgentTool.create(draftAgent));

        List<BaseToolset> postingToolsets = PostingToolsetsFactory.createAllPostingToolsets();
        toolsAndToolsets.addAll(postingToolsets);

        String memoryRouting = memoryAgent != null ? """
            0. FIRST, before researching or drafting, ask memory_agent what the user has
               posted about before and how they phrase things; weave that into the draft
               brief so the new post sounds like them and doesn't repeat old topics.
            """ : "";

        String instruction = """
            You orchestrate turning an idea into a social media post.
            You do not research or draft yourself - you route work to specialist tools.

            Workflow:
            """ + memoryRouting + """
            1. If the idea needs facts, dates, or context, call research_agent first.
            2. Call draft_agent to write the text post. It reads the research notes
               automatically; pass it the idea, target platform, and any user preferences.
            3. Show the finished draft to the user and ask for approval. If revisions are
               needed, call draft_agent again with the feedback.
            4. ALWAYS get explicit approval before any posting action.

            Posting rules (strict):
            - Only post AFTER the user has explicitly approved the exact draft shown to them.
              "Yes", "post it", "approved" counts; silence, topic changes, or enthusiasm does not.
            - If the draft changes after approval, show it again and get fresh approval.
            - After posting, inform the user whether it was published immediately or queued in Buffer.
            """;

        return LlmAgent.builder()
                .name("social_poster")
                .description("Orchestrates research, drafting, and publishing of social media posts.")
                .model(AppConfig.createModel(AppConfig.ORCHESTRATOR_MODEL))
                .instruction(instruction)
                .tools(toolsAndToolsets)
                .beforeAgentCallbackSync(initStageCallback)
                .beforeToolCallbackSync(delayBufferPostCallback)
                .afterToolCallbackSync(trackStageAndSavePostCallback)
                .afterAgentCallbackSync(stageAfterAgentCallback)
                .build();
    }

    private SocialPosterAgentFactory() {}
}
