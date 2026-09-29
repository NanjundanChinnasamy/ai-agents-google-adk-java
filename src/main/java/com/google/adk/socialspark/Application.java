package com.google.adk.socialspark;

import com.google.adk.socialspark.agui.AgUiEventTranslator;
import com.google.adk.socialspark.agui.AgUiModels;
import com.google.adk.socialspark.agui.SessionStore;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.socialspark.db.PostRecord;
import com.google.adk.socialspark.db.PostRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public static Javalin createServer(AgUiEventTranslator translator) {
        AgUiEventTranslator agUiTranslator = translator != null ? translator : new AgUiEventTranslator();

        AppConfig.GALLERY_DIR.mkdirs();
        PostRepository.initDb();

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    rule.reflectClientOrigin = true;
                    rule.allowCredentials = true;
                });
            });
            config.showJavalinBanner = false;
        });

        // 1. Health probe
        app.get("/healthz", ctx -> {
            ctx.contentType("application/json");
            ctx.result("{\"status\":\"ok\",\"service\":\"social-spark-adk-java\"}");
        });

        // 2. Durable post history
        app.get("/api/posts", ctx -> {
            List<PostRecord> posts = PostRepository.listPosts(50);
            Map<String, Object> resp = new HashMap<>();
            resp.put("posts", posts);
            ctx.contentType("application/json");
            ctx.result(gson.toJson(resp));
        });

        // 3. AG-UI thread state inspection & rehydration
        app.post("/agents/state", ctx -> {
            String body = ctx.body();
            AgUiModels.AgentStateRequest req = gson.fromJson(body, AgUiModels.AgentStateRequest.class);
            String threadId = req != null && req.threadId() != null ? req.threadId() : "";

            SessionStore.ThreadSession session = !threadId.isBlank() ? SessionStore.get(threadId) : null;
            AgUiModels.AgentStateResponse response;

            if (session != null) {
                response = new AgUiModels.AgentStateResponse(
                        true,
                        new HashMap<>(session.getState()),
                        new ArrayList<>(session.getMessages())
                );
            } else {
                response = new AgUiModels.AgentStateResponse(
                        false,
                        Map.of(),
                        List.of()
                );
            }

            ctx.contentType("application/json");
            ctx.result(gson.toJson(response));
        });

        // 4. AG-UI SSE stream endpoint
        app.post("/api/adk", ctx -> {
            String body = ctx.body();
            AgUiModels.RunAgentInput input = gson.fromJson(body, AgUiModels.RunAgentInput.class);

            ctx.async(() -> {
                HttpServletResponse res = ctx.res();
                res.setContentType("text/event-stream; charset=UTF-8");
                res.setHeader("Cache-Control", "no-cache");
                res.setHeader("Connection", "keep-alive");
                res.setHeader("X-Accel-Buffering", "no");

                PrintWriter writer = res.getWriter();
                try {
                    agUiTranslator.handleRun(input, line -> {
                        synchronized (writer) {
                            writer.write(line);
                            writer.flush();
                        }
                    });
                } catch (Exception e) {
                    logger.error("Error during SSE streaming: {}", e.getMessage(), e);
                }
            });
        });

        // 5. Gallery image serving
        app.get("/outputs/{filename}", ctx -> {
            String filename = ctx.pathParam("filename");
            File file = new File(AppConfig.GALLERY_DIR, filename);
            if (!file.exists()) {
                ctx.status(404).result("Image not found");
                return;
            }

            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) {
                mimeType = filename.endsWith(".jpg") || filename.endsWith(".jpeg") ? "image/jpeg" : "image/png";
            }
            ctx.contentType(mimeType);
            ctx.result(Files.readAllBytes(file.toPath()));
        });

        return app;
    }

    public static void main(String[] args) {
        int port = AppConfig.PORT;
        logger.info("Starting Social Spark Java ADK Server on port {}...", port);
        Javalin app = createServer(null);
        app.start(port);
        logger.info("Social Spark Java ADK Server is ready on http://localhost:{}", port);
    }
}
