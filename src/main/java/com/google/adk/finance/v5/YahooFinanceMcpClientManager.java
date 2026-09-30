package com.google.adk.finance.v5;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.mcp.McpToolset;
import io.modelcontextprotocol.client.transport.ServerParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages the lifecycle and readiness of the Yahoo Finance MCP client and server connection.
 * <p>
 * Core responsibilities:
 * <ol>
 *   <li>Locates the compiled standalone Yahoo Finance MCP jar ({@code mcp/yahoo-finance-mcp.jar}).</li>
 *   <li>Configures stdio transport via {@link ServerParameters}.</li>
 *   <li>Instantiates {@link McpToolset} from Google ADK Java.</li>
 *   <li>Verifies server readiness by requesting tool discovery and checking expected tool schemas.</li>
 *   <li>Provides graceful shutdown via {@link AutoCloseable#close()} to prevent orphaned processes.</li>
 * </ol>
 */
public class YahooFinanceMcpClientManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(YahooFinanceMcpClientManager.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String DEFAULT_JAR_PATH = "mcp/yahoo-finance-mcp.jar";
    public static final List<String> EXPECTED_TOOLS = List.of(
            "get_stock_info",
            "get_stock_actions",
            "get_financial_statement",
            "get_recommendations"
    );

    private final Path jarPath;
    private final Duration readinessTimeout;
    private McpToolset mcpToolset;
    private List<BaseTool> discoveredTools = Collections.emptyList();
    private volatile boolean initialized = false;

    public YahooFinanceMcpClientManager() {
        this(Path.of(DEFAULT_JAR_PATH), Duration.ofSeconds(15));
    }

    public YahooFinanceMcpClientManager(Path jarPath, Duration readinessTimeout) {
        this.jarPath = jarPath;
        this.readinessTimeout = readinessTimeout;
    }

    /**
     * Initializes the MCP connection, starts the underlying Java MCP process via stdio,
     * and verifies that expected tools are successfully discovered.
     */
    public synchronized void start() {
        if (initialized) {
            logger.info("YahooFinanceMcpClientManager is already initialized.");
            return;
        }

        logger.info("[Lifecycle] Starting Yahoo Finance MCP Server from: {}", jarPath.toAbsolutePath());
        if (!Files.exists(jarPath)) {
            throw new IllegalStateException("Yahoo Finance MCP jar not found at " + jarPath.toAbsolutePath()
                    + ". Run './gradlew buildYahooFinanceMcpJar' to compile the standalone MCP jar first.");
        }

        String javaCmd = resolveJavaBinary();
        logger.info("[Lifecycle] Using Java runtime binary: {}", javaCmd);

        ServerParameters serverParams = ServerParameters.builder(javaCmd)
                .args(List.of("-jar", jarPath.toAbsolutePath().toString()))
                .build();

        logger.info("[Lifecycle] Connecting ADK McpToolset over STDIO transport...");
        this.mcpToolset = new McpToolset(serverParams, OBJECT_MAPPER, EXPECTED_TOOLS);

        // Verify readiness by calling getTools() within the timeout
        verifyReadiness();
        this.initialized = true;
        logger.info("[Lifecycle] Yahoo Finance MCP Server ready with {} tools.", discoveredTools.size());
    }

    private void verifyReadiness() {
        long startTime = System.currentTimeMillis();
        logger.info("[Readiness] Verifying MCP server readiness and discovering tools (timeout: {}s)...",
                readinessTimeout.toSeconds());

        try {
            List<BaseTool> tools = mcpToolset.getTools(null)
                    .timeout(readinessTimeout.toMillis(), TimeUnit.MILLISECONDS)
                    .toList()
                    .blockingGet();

            if (tools == null || tools.isEmpty()) {
                throw new IllegalStateException("MCP server responded but returned no tools.");
            }

            this.discoveredTools = new ArrayList<>(tools);
            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("[Readiness] Discovered {} MCP tools in {} ms:", tools.size(), elapsed);
            for (BaseTool tool : tools) {
                logger.info("  -> MCP Tool: [{}] - {}", tool.name(), tool.description());
            }

            // Verify all expected tools are present
            List<String> toolNames = tools.stream().map(BaseTool::name).toList();
            for (String expected : EXPECTED_TOOLS) {
                if (!toolNames.contains(expected)) {
                    logger.warn("[Readiness] Expected MCP tool '{}' was not found in discovered list: {}",
                            expected, toolNames);
                }
            }
        } catch (Exception e) {
            logger.error("[Readiness] Failed to establish MCP connection or discover tools within timeout: {}",
                    e.getMessage(), e);
            close();
            throw new IllegalStateException("Yahoo Finance MCP readiness verification failed: " + e.getMessage(), e);
        }
    }

    private String resolveJavaBinary() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isBlank()) {
            boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
            Path bin = Path.of(javaHome, "bin", isWindows ? "java.exe" : "java");
            if (Files.exists(bin)) {
                return bin.toAbsolutePath().toString();
            }
        }
        return "java";
    }

    public McpToolset getMcpToolset() {
        if (!initialized || mcpToolset == null) {
            start();
        }
        return mcpToolset;
    }

    public List<BaseTool> getDiscoveredTools() {
        return Collections.unmodifiableList(discoveredTools);
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public synchronized void close() {
        if (mcpToolset != null) {
            logger.info("[Lifecycle] Closing Yahoo Finance McpToolset and terminating child process...");
            try {
                mcpToolset.close();
            } catch (Exception e) {
                logger.warn("[Lifecycle] Exception while closing McpToolset: {}", e.getMessage());
            } finally {
                mcpToolset = null;
                initialized = false;
                logger.info("[Lifecycle] Yahoo Finance McpToolset closed cleanly.");
            }
        }
    }
}
