package com.google.adk.finance.v9.evaluation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe Evidence Store for Version 9.
 * <p>
 * Captures, indexes, and normalizes evidence records emitted by tools across the agent run lifecycle.
 * The {@link FaithfulnessEvaluator} and {@link CalculationFidelityEvaluator} use this store as the
 * empirical source of truth for deterministic grounding audits.
 */
public class EvidenceStoreV9 {
    private static final Logger logger = LoggerFactory.getLogger(EvidenceStoreV9.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<EvidenceRecord> records = new CopyOnWriteArrayList<>();

    public EvidenceStoreV9() {}

    /**
     * Appends an existing {@link EvidenceRecord} directly into the store.
     */
    public void record(EvidenceRecord record) {
        if (record != null) {
            records.add(record);
            logger.info("[EvidenceStoreV9] Captured verified fact: [{}] {} - {} = {} (Ref: {})",
                    record.source(), record.ticker(), record.field(), record.value(), record.sourceReference());
        }
    }

    /**
     * Helper to record a numeric fact (e.g. quote, valuation metric, calculated PnL).
     */
    public void recordFact(String source, String tool, String ticker, String field, double numericValue, String reference, String rawContent) {
        record(EvidenceRecord.of(source, tool, ticker, field, numericValue, reference, rawContent));
    }

    /**
     * Helper to record a textual fact (e.g. news headline, rating, corporate development).
     */
    public void recordTextFact(String source, String tool, String ticker, String field, String textValue, String reference, String rawContent) {
        record(EvidenceRecord.ofText(source, tool, ticker, field, textValue, reference, rawContent));
    }

    /**
     * Inspects raw tool execution outputs and automatically parses and captures empirical evidence.
     *
     * @param toolName name of the tool executed (e.g., 'portfolio_math', 'get_stock_info', 'load_customer_portfolio')
     * @param input arguments passed into the tool
     * @param response output produced by the tool
     */
    public void capture(String toolName, Map<String, Object> input, Object response) {
        if (response == null) return;

        try {
            Map<String, Object> data = null;
            if (response instanceof Map<?, ?> map) {
                //noinspection unchecked
                data = (Map<String, Object>) map;
            } else if (response instanceof String str && str.trim().startsWith("{")) {
                data = OBJECT_MAPPER.readValue(str, new TypeReference<>() {});
            }

            if (data == null) {
                // Check if string response (e.g., search text)
                if (response instanceof String textResp && !textResp.isBlank()) {
                    String ticker = extractTickerFromInput(input);
                    record(EvidenceRecord.ofText("GOOGLE_SEARCH", toolName, ticker, "news_snippet", textResp, toolName, textResp));
                }
                return;
            }

            String source = determineSource(toolName);
            String ticker = extractTicker(input, data);

            // 1. Process Yahoo Finance MCP data
            if ("get_stock_info".equalsIgnoreCase(toolName) || "get_quote".equalsIgnoreCase(toolName) || data.containsKey("currentPrice")) {
                if (data.containsKey("currentPrice") && data.get("currentPrice") instanceof Number num) {
                    recordFact(source, toolName, ticker, "price", num.doubleValue(), toolName + "(" + ticker + ")", data.toString());
                } else if (data.containsKey("regularMarketPrice") && data.get("regularMarketPrice") instanceof Number num) {
                    recordFact(source, toolName, ticker, "price", num.doubleValue(), toolName + "(" + ticker + ")", data.toString());
                } else if (data.containsKey("price") && data.get("price") instanceof Number num) {
                    recordFact(source, toolName, ticker, "price", num.doubleValue(), toolName + "(" + ticker + ")", data.toString());
                }

                if (data.containsKey("pe_ratio") && data.get("pe_ratio") instanceof Number pe) {
                    recordFact(source, toolName, ticker, "pe_ratio", pe.doubleValue(), toolName + "(" + ticker + ")", data.toString());
                } else if (data.containsKey("trailingPE") && data.get("trailingPE") instanceof Number pe) {
                    recordFact(source, toolName, ticker, "pe_ratio", pe.doubleValue(), toolName + "(" + ticker + ")", data.toString());
                }

                if (data.containsKey("market_cap") && data.get("market_cap") instanceof Number mc) {
                    recordFact(source, toolName, ticker, "market_cap", mc.doubleValue(), toolName + "(" + ticker + ")", data.toString());
                }
            }

            // 2. Process PortfolioMathTool outputs
            if ("portfolio_math".equalsIgnoreCase(toolName)) {
                if (data.containsKey("pnl") && data.get("pnl") instanceof Number pnl) {
                    recordFact("PORTFOLIO_MATH", toolName, ticker, "pnl", pnl.doubleValue(), "PortfolioMathTool.calculate_pnl", data.toString());
                }
                if (data.containsKey("cost_basis") && data.get("cost_basis") instanceof Number cb) {
                    recordFact("PORTFOLIO_MATH", toolName, ticker, "cost_basis", cb.doubleValue(), "PortfolioMathTool.calculate_pnl", data.toString());
                }
                if (data.containsKey("current_value") && data.get("current_value") instanceof Number cv) {
                    recordFact("PORTFOLIO_MATH", toolName, ticker, "current_value", cv.doubleValue(), "PortfolioMathTool.calculate_pnl", data.toString());
                }
                if (data.containsKey("return_pct") && data.get("return_pct") instanceof Number rp) {
                    recordFact("PORTFOLIO_MATH", toolName, ticker, "return_pct", rp.doubleValue(), "PortfolioMathTool.calculate_pnl", data.toString());
                }
                if (data.containsKey("allocations") && data.get("allocations") instanceof List<?> allocs) {
                    for (Object allocObj : allocs) {
                        if (allocObj instanceof Map<?, ?> am) {
                            String posSymbol = String.valueOf(am.get("symbol"));
                            if (am.get("weight_pct") instanceof Number wp) {
                                recordFact("PORTFOLIO_MATH", toolName, posSymbol, "weight_pct", wp.doubleValue(), "PortfolioMathTool.calculate_allocation", allocObj.toString());
                            }
                        }
                    }
                }
            }

            // 3. Process Customer Portfolio Loader outputs
            if ("load_customer_portfolio".equalsIgnoreCase(toolName)) {
                if (data.containsKey("holdings") && data.get("holdings") instanceof List<?> holdings) {
                    for (Object hObj : holdings) {
                        if (hObj instanceof Map<?, ?> hm) {
                            String hSymbol = String.valueOf(hm.get("symbol"));
                            if (hm.get("buy_price") instanceof Number bp) {
                                recordFact("CUSTOMER_PORTFOLIO_DB", toolName, hSymbol, "buy_price", bp.doubleValue(), "CustomerPortfolioRepository", hObj.toString());
                            }
                            if (hm.get("quantity") instanceof Number qty) {
                                recordFact("CUSTOMER_PORTFOLIO_DB", toolName, hSymbol, "quantity", qty.doubleValue(), "CustomerPortfolioRepository", hObj.toString());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("[EvidenceStoreV9] Failed to capture tool response for {}: {}", toolName, e.getMessage());
        }
    }

    private String determineSource(String toolName) {
        if (toolName == null) return "UNKNOWN";
        if (toolName.contains("mcp") || toolName.startsWith("get_")) return "YAHOO_FINANCE_MCP";
        if (toolName.contains("math")) return "PORTFOLIO_MATH";
        if (toolName.contains("portfolio")) return "CUSTOMER_PORTFOLIO_DB";
        if (toolName.contains("search") || toolName.contains("research")) return "GOOGLE_SEARCH";
        return "TOOL";
    }

    private String extractTickerFromInput(Map<String, Object> input) {
        if (input == null) return "";
        if (input.containsKey("symbol")) return String.valueOf(input.get("symbol"));
        if (input.containsKey("ticker")) return String.valueOf(input.get("ticker"));
        if (input.containsKey("query")) {
            String q = String.valueOf(input.get("query"));
            for (String part : q.split("\\s+")) {
                if (part.matches("^[A-Z0-9.]+($|\\.NS|\\.BO)")) return part;
            }
        }
        return "";
    }

    private String extractTicker(Map<String, Object> input, Map<String, Object> data) {
        String ticker = extractTickerFromInput(input);
        if (ticker.isBlank() && data != null) {
            if (data.containsKey("symbol")) ticker = String.valueOf(data.get("symbol"));
            else if (data.containsKey("ticker")) ticker = String.valueOf(data.get("ticker"));
        }
        return ticker.trim().toUpperCase();
    }

    public List<EvidenceRecord> getAllRecords() {
        return Collections.unmodifiableList(records);
    }

    public List<EvidenceRecord> findRecordsByTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) return List.of();
        String normalized = ticker.trim().toUpperCase();
        List<EvidenceRecord> list = new ArrayList<>();
        for (EvidenceRecord r : records) {
            if (r.ticker().equalsIgnoreCase(normalized) || r.ticker().startsWith(normalized) || normalized.startsWith(r.ticker())) {
                list.add(r);
            }
        }
        return list;
    }

    public Optional<EvidenceRecord> findRecord(String ticker, String field) {
        if (ticker == null || field == null) return Optional.empty();
        String normTicker = ticker.trim().toUpperCase();
        String normField = field.trim().toLowerCase();
        for (EvidenceRecord r : records) {
            if ((r.ticker().equalsIgnoreCase(normTicker) || r.ticker().startsWith(normTicker) || normTicker.startsWith(r.ticker()))
                    && r.field().equalsIgnoreCase(normField)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    public void clear() {
        records.clear();
    }

    public int size() {
        return records.size();
    }
}
