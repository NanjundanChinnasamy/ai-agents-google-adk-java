package com.google.adk.finance.v8.evidence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory Evidence Store capturing verified empirical facts directly from tool outputs.
 * <p>
 * During the agent run lifecycle, the {@link com.google.adk.finance.v8.callbacks.AfterToolEvidenceCapture}
 * intercepts tool responses (such as Yahoo Finance MCP {@code get_stock_info} or {@code portfolio_math})
 * and records verified figures (tickers, live market prices, currency, valuation metrics).
 * The {@link com.google.adk.finance.v8.guardrails.output.HallucinationDetector} later cross-references
 * LLM assertions against this evidence store to detect numerical hallucinations or fabricated quotes.
 */
public class EvidenceStore {
    private static final Logger logger = LoggerFactory.getLogger(EvidenceStore.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public record StockFact(
            String symbol,
            double price,
            String currency,
            Map<String, Object> metrics,
            String sourceTool,
            Instant recordedAt
    ) {}

    private final Map<String, StockFact> stockFacts = new ConcurrentHashMap<>();

    public EvidenceStore() {}

    /**
     * Explicitly records a verified stock fact into the evidence store.
     */
    public void recordStockFact(String symbol, double price, String currency, Map<String, Object> metrics, String sourceTool) {
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        String normalized = symbol.trim().toUpperCase();
        StockFact fact = new StockFact(
                normalized,
                price,
                currency == null ? "INR" : currency,
                metrics == null ? Collections.emptyMap() : metrics,
                sourceTool,
                Instant.now()
        );
        stockFacts.put(normalized, fact);
        logger.info("[Evidence Store] Captured fact for {}: price={} {}, source={}", normalized, price, fact.currency(), sourceTool);
    }

    /**
     * Inspects raw tool execution outputs and automatically parses and captures financial metrics.
     *
     * @param toolName name of the executed tool
     * @param input tool invocation arguments
     * @param response raw output from the tool
     */
    public void capture(String toolName, Map<String, Object> input, Object response) {
        if (response == null) {
            return;
        }

        try {
            Map<String, Object> data = null;
            if (response instanceof Map<?, ?> map) {
                //noinspection unchecked
                data = (Map<String, Object>) map;
            } else if (response instanceof String jsonStr && jsonStr.trim().startsWith("{")) {
                data = OBJECT_MAPPER.readValue(jsonStr, new TypeReference<>() {});
            }

            if (data == null) {
                return;
            }

            // Extract symbol from input or data
            String symbol = null;
            if (input != null) {
                if (input.containsKey("symbol")) symbol = String.valueOf(input.get("symbol"));
                else if (input.containsKey("ticker")) symbol = String.valueOf(input.get("ticker"));
            }
            if (symbol == null && data.containsKey("symbol")) {
                symbol = String.valueOf(data.get("symbol"));
            }

            if (symbol == null || symbol.isBlank()) {
                return;
            }

            // Extract price
            Double price = null;
            if (data.containsKey("currentPrice")) price = toDouble(data.get("currentPrice"));
            else if (data.containsKey("price")) price = toDouble(data.get("price"));
            else if (data.containsKey("regularMarketPrice")) price = toDouble(data.get("regularMarketPrice"));
            else if (data.containsKey("current_valuation")) price = toDouble(data.get("current_valuation"));

            String currency = "INR";
            if (data.containsKey("currency")) currency = String.valueOf(data.get("currency"));

            if (price != null) {
                recordStockFact(symbol, price, currency, data, toolName);
            }
        } catch (Exception e) {
            logger.debug("[Evidence Store] Could not parse tool response for evidence capture: {}", e.getMessage());
        }
    }

    public Optional<StockFact> getFact(String symbol) {
        if (symbol == null) return Optional.empty();
        return Optional.ofNullable(stockFacts.get(symbol.trim().toUpperCase()));
    }

    public Collection<StockFact> getAllFacts() {
        return Collections.unmodifiableCollection(stockFacts.values());
    }

    public void clear() {
        stockFacts.clear();
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s.replace(",", "").replace("₹", "").replace("$", "").trim());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
