package com.google.adk.finance.v8.callbacks;

import com.google.adk.agents.Callbacks.BeforeToolCallbackSync;
import com.google.adk.agents.InvocationContext;
import com.google.adk.finance.v8.guardrails.GuardrailResult;
import com.google.adk.finance.v8.guardrails.tool.TickerValidator;
import com.google.adk.finance.v8.guardrails.tool.ToolOperationGuard;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * Google ADK lifecycle callback executing immediately before a tool is invoked.
 * <p>
 * Core Protections:
 * <ol>
 *   <li><b>Tool Operation Authorization:</b> Intercepts requested tools with {@link ToolOperationGuard}.
 *       If an unauthorized operation (such as trade execution, money transfer, or shell command) is attempted,
 *       the callback intercepts the execution and returns a blocking error map without calling the underlying tool.</li>
 *   <li><b>Ticker Symbol Validation:</b> If the tool accepts a financial symbol or ticker, validates the input
 *       using {@link TickerValidator} to prevent malformed queries, excessive lengths, or SQL/shell injection payloads.</li>
 * </ol>
 */
public class BeforeToolGuardrail implements BeforeToolCallbackSync {
    private static final Logger logger = LoggerFactory.getLogger(BeforeToolGuardrail.class);

    private final ToolOperationGuard operationGuard;
    private final TickerValidator tickerValidator;

    public BeforeToolGuardrail() {
        this(new ToolOperationGuard(), new TickerValidator());
    }

    public BeforeToolGuardrail(ToolOperationGuard operationGuard, TickerValidator tickerValidator) {
        this.operationGuard = operationGuard;
        this.tickerValidator = tickerValidator;
    }

    @Override
    public Optional<Map<String, Object>> call(
            InvocationContext invocationContext,
            BaseTool baseTool,
            Map<String, Object> input,
            ToolContext toolContext) {

        String toolName = baseTool.name();
        logger.info("[BeforeTool Callback] Intercepted tool call: '{}' with args: {}", toolName, input);

        // 1. Authorize tool operation (Priority 1)
        GuardrailResult opResult = operationGuard.evaluate(toolName);
        if (opResult.isBlocked()) {
            logger.warn("[GUARDRAIL] Tool operation blocked: {}", opResult.reason());
            return Optional.of(Map.of(
                    "status", "BLOCKED",
                    "error", opResult.reason(),
                    "guardrail", "ToolOperationGuard"
            ));
        }
        logger.info("[GUARDRAIL] Tool operation allowed: {}", toolName);

        // 2. Validate ticker symbol if present in arguments (Priority 2)
        if (input != null) {
            String ticker = null;
            if (input.containsKey("symbol")) {
                ticker = String.valueOf(input.get("symbol"));
            } else if (input.containsKey("ticker")) {
                ticker = String.valueOf(input.get("ticker"));
            }

            if (ticker != null && !ticker.isBlank()) {
                GuardrailResult tickerResult = tickerValidator.evaluate(ticker);
                if (tickerResult.isBlocked()) {
                    logger.warn("[GUARDRAIL] Tool operation blocked: Invalid ticker ({})", tickerResult.reason());
                    return Optional.of(Map.of(
                            "status", "BLOCKED",
                            "error", tickerResult.reason() + ". Please provide a valid ticker symbol (e.g., INFY.NS, RELIANCE.NS, AAPL).",
                            "guardrail", "TickerValidator"
                    ));
                }
                logger.info("[GUARDRAIL] Ticker validation passed: {}", ticker);
            }
        }

        // Allow tool execution to proceed normally
        return Optional.empty();
    }
}
