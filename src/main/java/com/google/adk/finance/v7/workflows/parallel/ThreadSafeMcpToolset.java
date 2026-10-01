package com.google.adk.finance.v7.workflows.parallel;

import com.google.adk.agents.ReadonlyContext;
import com.google.adk.models.LlmRequest;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.BaseToolset;
import com.google.adk.tools.ToolContext;
import com.google.adk.tools.mcp.McpToolset;
import com.google.genai.types.FunctionDeclaration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Week 4 — Version 7: Thread-Safe Wrapper for Google ADK {@link McpToolset}.
 * <p>
 * Problem Solved:
 * Official Google ADK {@link McpToolset} communicates with subprocesses (e.g. Yahoo Finance MCP)
 * over standard I/O (stdio) via Project Reactor's {@code Sinks.many().unicast().onBackpressureBuffer()}.
 * When multiple parallel workers concurrently call {@code McpToolset.getTools()} or invoke MCP tools,
 * the underlying {@code StdioClientTransport} throws:
 * <pre>
 * java.lang.RuntimeException: Failed to enqueue message
 *   at io.modelcontextprotocol.client.transport.StdioClientTransport.sendMessage(...)
 * </pre>
 * This occurs because unicast sinks reject concurrent emissions with {@code FAIL_NON_SERIALIZED}.
 * Additionally, {@code McpToolset} sets its internal session field to {@code null} upon error,
 * causing cascading initialization storms and connection destruction across threads.
 * <p>
 * This wrapper provides complete concurrency safety:
 * <ol>
 *   <li>Pre-discovers and caches MCP tool definitions sequentially before parallel execution starts.</li>
 *   <li>Wraps all MCP tools in {@link SynchronizedMcpTool}, serializing I/O over the shared stdio pipe.</li>
 *   <li>Allows workers to perform LLM reasoning and other work concurrently while protecting subprocess I/O.</li>
 * </ol>
 */
public class ThreadSafeMcpToolset implements BaseToolset, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ThreadSafeMcpToolset.class);

    private final BaseToolset delegate;
    private final Object lock = new Object();
    private volatile List<BaseTool> cachedSynchronizedTools;

    public ThreadSafeMcpToolset(BaseToolset delegate) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate toolset cannot be null");
    }

    /**
     * Safely wraps the toolset if not already wrapped.
     */
    public static BaseToolset wrap(BaseToolset toolset) {
        if (toolset == null || toolset instanceof ThreadSafeMcpToolset) {
            return toolset;
        }
        return new ThreadSafeMcpToolset(toolset);
    }

    /**
     * Pre-warms the MCP session and caches tool declarations sequentially.
     */
    public void warmUp() {
        warmUp(null);
    }

    /**
     * Pre-warms the MCP session with the given context and caches tool declarations sequentially.
     */
    public void warmUp(ReadonlyContext readonlyContext) {
        synchronized (lock) {
            if (cachedSynchronizedTools == null) {
                logger.info("[ThreadSafeMcpToolset] Pre-warming and caching MCP tools sequentially for parallel safety...");
                try {
                    List<BaseTool> rawTools = delegate.getTools(readonlyContext).toList().blockingGet();
                    List<BaseTool> wrapped = new ArrayList<>();
                    if (rawTools != null) {
                        for (BaseTool tool : rawTools) {
                            wrapped.add(new SynchronizedMcpTool(tool, lock));
                        }
                    }
                    cachedSynchronizedTools = Collections.unmodifiableList(wrapped);
                    logger.info("[ThreadSafeMcpToolset] Successfully cached {} synchronized MCP tools: {}",
                            cachedSynchronizedTools.size(),
                            cachedSynchronizedTools.stream().map(BaseTool::name).toList());
                } catch (Exception e) {
                    logger.warn("[ThreadSafeMcpToolset] Failed to pre-warm MCP tools: {}", e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public Flowable<BaseTool> getTools(ReadonlyContext readonlyContext) {
        return Flowable.defer(() -> {
            if (cachedSynchronizedTools == null) {
                warmUp(readonlyContext);
            }
            if (cachedSynchronizedTools != null) {
                return Flowable.fromIterable(cachedSynchronizedTools);
            }
            return Flowable.empty();
        });
    }

    @Override
    public Completable processLlmRequest(LlmRequest.Builder builder, ToolContext toolContext) {
        return delegate.processLlmRequest(builder, toolContext);
    }

    @Override
    public void close() throws Exception {
        synchronized (lock) {
            delegate.close();
        }
    }

    /**
     * Tool proxy that serializes execution on the shared stdio transport lock.
     */
    public static class SynchronizedMcpTool extends BaseTool {
        private static final Logger toolLogger = LoggerFactory.getLogger(SynchronizedMcpTool.class);
        private final BaseTool delegateTool;
        private final Object lock;

        public SynchronizedMcpTool(BaseTool delegateTool, Object lock) {
            super(delegateTool.name(), delegateTool.description(), delegateTool.longRunning());
            this.delegateTool = delegateTool;
            this.lock = lock;
        }

        @Override
        public Optional<FunctionDeclaration> declaration() {
            return delegateTool.declaration();
        }

        @Override
        public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
            return Single.fromCallable(() -> {
                synchronized (lock) {
                    toolLogger.debug("[SynchronizedMcpTool] Invoking '{}' on thread '{}' with args: {}",
                            name(), Thread.currentThread().getName(), args);
                    long start = System.currentTimeMillis();
                    try {
                        Map<String, Object> result = delegateTool.runAsync(args, toolContext).blockingGet();
                        long duration = System.currentTimeMillis() - start;
                        toolLogger.debug("[SynchronizedMcpTool] Completed '{}' in {}ms", name(), duration);
                        return result;
                    } catch (Exception e) {
                        long duration = System.currentTimeMillis() - start;
                        toolLogger.warn("[SynchronizedMcpTool] Tool '{}' call failed after {}ms: {}",
                                name(), duration, e.getMessage());
                        throw e;
                    }
                }
            });
        }
    }
}
