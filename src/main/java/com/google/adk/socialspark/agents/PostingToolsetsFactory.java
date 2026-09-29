package com.google.adk.socialspark.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.BaseToolset;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.StreamableHttpServerParameters;
import io.modelcontextprotocol.client.transport.ServerParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PostingToolsetsFactory {
    private static final Logger logger = LoggerFactory.getLogger(PostingToolsetsFactory.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private record CommandAndArgs(String command, List<String> args) {}

    private static CommandAndArgs resolvePythonCommand(String scriptPath) {
        String userHome = System.getProperty("user.home", "");
        List<String> candidates = List.of(
                "uv",
                userHome + "\\.local\\bin\\uv.exe",
                userHome + "\\AppData\\Roaming\\uv\\uv.exe"
        );

        for (String candidate : candidates) {
            try {
                Process p = new ProcessBuilder(candidate, "--version").start();
                if (p.waitFor() == 0) {
                    return new CommandAndArgs(candidate, List.of("run", "--with", "fastmcp", "--with", "httpx", "python", scriptPath));
                }
            } catch (Exception ignored) {
            }
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        String cmd = os.contains("win") ? "python" : "python3";
        return new CommandAndArgs(cmd, List.of(scriptPath));
    }

    public static BaseToolset createLinkedInToolset() {
        if (!AppConfig.useLinkedIn) return null;
        File script = new File(AppConfig.MCP_DIR, "linkedin_server.py");
        if (!script.exists()) {
            logger.warn("LinkedIn MCP server script not found at {}", script.getAbsolutePath());
            return null;
        }

        try {
            CommandAndArgs cmd = resolvePythonCommand(script.getAbsolutePath());
            Map<String, String> env = new HashMap<>();
            env.put("DRY_RUN", String.valueOf(AppConfig.DRY_RUN));
            env.put("LINKEDIN_ACCESS_TOKEN", AppConfig.LINKEDIN_ACCESS_TOKEN);

            ServerParameters params = ServerParameters.builder(cmd.command())
                    .args(cmd.args())
                    .env(env)
                    .build();

            return new McpToolset(params, objectMapper, List.of("get_profile"));
        } catch (Exception e) {
            logger.warn("Could not initialize LinkedIn read toolset: {}", e.getMessage());
            return null;
        }
    }

    public static BaseToolset createLinkedInPostToolset() {
        if (!AppConfig.useLinkedIn) return null;
        File script = new File(AppConfig.MCP_DIR, "linkedin_server.py");
        if (!script.exists()) return null;

        try {
            CommandAndArgs cmd = resolvePythonCommand(script.getAbsolutePath());
            Map<String, String> env = new HashMap<>();
            env.put("DRY_RUN", String.valueOf(AppConfig.DRY_RUN));
            env.put("LINKEDIN_ACCESS_TOKEN", AppConfig.LINKEDIN_ACCESS_TOKEN);

            ServerParameters params = ServerParameters.builder(cmd.command())
                    .args(cmd.args())
                    .env(env)
                    .build();

            return new McpToolset(params, objectMapper, List.of("create_post"));
        } catch (Exception e) {
            logger.warn("Could not initialize LinkedIn post toolset: {}", e.getMessage());
            return null;
        }
    }

    public static BaseToolset createBufferToolset() {
        if (!AppConfig.useBuffer) return null;

        try {
            Map<String, String> headers = Map.of("Authorization", "Bearer " + AppConfig.BUFFER_API_KEY);
            StreamableHttpServerParameters params = StreamableHttpServerParameters.builder()
                    .url("https://mcp.buffer.com/mcp")
                    .headers(headers)
                    .timeout(Duration.ofSeconds(30))
                    .readTimeout(Duration.ofSeconds(30))
                    .build();

            return new McpToolset(params, objectMapper, List.of("list_channels", "get_account"));
        } catch (Exception e) {
            logger.warn("Could not initialize Buffer read toolset: {}", e.getMessage());
            return null;
        }
    }

    public static BaseToolset createBufferPostToolset() {
        if (!AppConfig.useBuffer) return null;

        try {
            Map<String, String> headers = Map.of("Authorization", "Bearer " + AppConfig.BUFFER_API_KEY);
            StreamableHttpServerParameters params = StreamableHttpServerParameters.builder()
                    .url("https://mcp.buffer.com/mcp")
                    .headers(headers)
                    .timeout(Duration.ofSeconds(30))
                    .readTimeout(Duration.ofSeconds(30))
                    .build();

            return new McpToolset(params, objectMapper, List.of("create_post"));
        } catch (Exception e) {
            logger.warn("Could not initialize Buffer post toolset: {}", e.getMessage());
            return null;
        }
    }

    public static List<BaseToolset> createAllPostingToolsets() {
        List<BaseToolset> list = new ArrayList<>();
        BaseToolset liRead = createLinkedInToolset();
        if (liRead != null) list.add(liRead);

        BaseToolset liPost = createLinkedInPostToolset();
        if (liPost != null) list.add(liPost);

        BaseToolset bufRead = createBufferToolset();
        if (bufRead != null) list.add(bufRead);

        BaseToolset bufPost = createBufferPostToolset();
        if (bufPost != null) list.add(bufPost);

        return list;
    }

    private PostingToolsetsFactory() {}
}
