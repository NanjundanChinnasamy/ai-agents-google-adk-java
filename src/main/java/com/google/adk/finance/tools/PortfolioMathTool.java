package com.google.adk.finance.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shared Finance Tool: Portfolio Math & Financial Calculations.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Deterministic arithmetic execution offloaded from LLM to Java.</li>
 *   <li>Unrealized PnL, cost basis, and return percentage calculations.</li>
 *   <li>Portfolio allocation weights, concentration risk flagging, and Simple Moving Average (SMA).</li>
 * </ul>
 */
public class PortfolioMathTool extends BaseTool {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioMathTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final String TOOL_NAME = "portfolio_math";

    public PortfolioMathTool() {
        super(TOOL_NAME, "Performs deterministic financial math calculations: unrealized PnL, portfolio allocation weights, and technical indicators (SMA, price changes).");
    }

    @Override
    public Optional<FunctionDeclaration> declaration() {
        Map<String, Schema> properties = new HashMap<>();

        properties.put("operation", Schema.builder()
                .type(Type.Known.STRING)
                .description("The calculation to perform: 'calculate_pnl', 'calculate_allocation', or 'calculate_technical_indicator'.")
                .build());

        properties.put("symbol", Schema.builder()
                .type(Type.Known.STRING)
                .description("The stock ticker symbol (e.g. 'RELIANCE', 'TCS', 'AAPL').")
                .build());

        properties.put("buy_price", Schema.builder()
                .type(Type.Known.NUMBER)
                .description("The purchase price or cost basis per share.")
                .build());

        properties.put("current_price", Schema.builder()
                .type(Type.Known.NUMBER)
                .description("The current market price per share.")
                .build());

        properties.put("quantity", Schema.builder()
                .type(Type.Known.INTEGER)
                .description("The number of shares or units held.")
                .build());

        properties.put("positions_json", Schema.builder()
                .type(Type.Known.STRING)
                .description("JSON array of holding objects for allocation calculation, e.g. '[{\"symbol\":\"RELIANCE\",\"value\":2000.0},{\"symbol\":\"TCS\",\"value\":2000.0}]'.")
                .build());

        properties.put("indicator", Schema.builder()
                .type(Type.Known.STRING)
                .description("Technical indicator to compute: 'sma' (Simple Moving Average) or 'price_change'.")
                .build());

        properties.put("prices", Schema.builder()
                .type(Type.Known.ARRAY)
                .items(Schema.builder().type(Type.Known.NUMBER).build())
                .description("Array of historical closing prices for technical calculations.")
                .build());

        Schema parameters = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(properties)
                .required(List.of("operation"))
                .build();

        return Optional.of(FunctionDeclaration.builder()
                .name(TOOL_NAME)
                .description("Performs deterministic financial math calculations: unrealized PnL, portfolio allocation weights, and technical indicators.")
                .parameters(parameters)
                .build());
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
        String operation = (String) args.getOrDefault("operation", "");
        logger.info("Executing portfolio_math for operation='{}'", operation);

        try {
            Map<String, Object> result = switch (operation.toLowerCase().trim()) {
                case "calculate_pnl", "pnl" -> calculatePnl(args);
                case "calculate_allocation", "allocation" -> calculateAllocation(args);
                case "calculate_technical_indicator", "technical", "indicator" -> calculateTechnicalIndicator(args);
                default -> Map.of(
                        "status", "error",
                        "message", "Unknown operation '" + operation + "'. Supported operations: 'calculate_pnl', 'calculate_allocation', 'calculate_technical_indicator'."
                );
            };
            return Single.just(result);
        } catch (Exception e) {
            logger.error("Error executing portfolio math {}: {}", operation, e.getMessage(), e);
            return Single.just(Map.of(
                    "status", "error",
                    "operation", operation,
                    "message", "Calculation error: " + e.getMessage()
            ));
        }
    }

    public static Map<String, Object> calculatePnl(Map<String, Object> args) {
        String symbol = String.valueOf(args.getOrDefault("symbol", "UNKNOWN")).toUpperCase();
        double buyPrice = toDouble(args.get("buy_price"));
        double currentPrice = toDouble(args.get("current_price"));
        int quantity = toInt(args.get("quantity"), 1);

        double totalCost = buyPrice * quantity;
        double currentValue = currentPrice * quantity;
        double unrealizedPnl = currentValue - totalCost;
        double returnPct = totalCost > 0 ? (unrealizedPnl / totalCost) * 100.0 : 0.0;

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "success");
        res.put("operation", "calculate_pnl");
        res.put("symbol", symbol);
        res.put("quantity", quantity);
        res.put("buy_price", buyPrice);
        res.put("current_price", currentPrice);
        res.put("total_cost", round(totalCost));
        res.put("current_value", round(currentValue));
        res.put("unrealized_pnl", round(unrealizedPnl));
        res.put("return_pct", round(returnPct));
        res.put("is_profitable", unrealizedPnl >= 0);
        return res;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> calculateAllocation(Map<String, Object> args) {
        List<Map<String, Object>> positions = new ArrayList<>();

        if (args.containsKey("positions_json")) {
            String json = String.valueOf(args.get("positions_json"));
            try {
                positions = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                logger.warn("Failed to parse positions_json: {}", e.getMessage());
            }
        } else if (args.containsKey("positions") && args.get("positions") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    positions.add((Map<String, Object>) m);
                }
            }
        }

        if (positions.isEmpty()) {
            return Map.of("status", "error", "message", "No positions provided for allocation calculation.");
        }

        double totalValue = 0.0;
        for (Map<String, Object> pos : positions) {
            totalValue += toDouble(pos.get("value"));
        }

        List<Map<String, Object>> allocationList = new ArrayList<>();
        String maxConcentrationSymbol = "";
        double maxWeight = -1.0;

        for (Map<String, Object> pos : positions) {
            String sym = String.valueOf(pos.getOrDefault("symbol", "N/A"));
            double val = toDouble(pos.get("value"));
            double weight = totalValue > 0 ? (val / totalValue) * 100.0 : 0.0;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("symbol", sym);
            entry.put("value", round(val));
            entry.put("weight_pct", round(weight));
            allocationList.add(entry);

            if (weight > maxWeight) {
                maxWeight = weight;
                maxConcentrationSymbol = sym;
            }
        }

        boolean isConcentrated = maxWeight > 25.0; // Standard diversification threshold: >25% in a single position

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "success");
        res.put("operation", "calculate_allocation");
        res.put("total_portfolio_value", round(totalValue));
        res.put("positions_count", positions.size());
        res.put("allocations", allocationList);
        res.put("max_concentration_symbol", maxConcentrationSymbol);
        res.put("max_concentration_pct", round(maxWeight));
        res.put("concentration_risk_flag", isConcentrated);
        res.put("commentary", isConcentrated
                ? "Warning: Single position '" + maxConcentrationSymbol + "' exceeds recommended 25% concentration threshold."
                : "Portfolio allocation is diversified across holdings.");
        return res;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> calculateTechnicalIndicator(Map<String, Object> args) {
        String symbol = String.valueOf(args.getOrDefault("symbol", "UNKNOWN")).toUpperCase();
        String indicator = String.valueOf(args.getOrDefault("indicator", "sma")).toLowerCase();

        List<Double> priceList = new ArrayList<>();
        if (args.containsKey("prices") && args.get("prices") instanceof List<?> list) {
            for (Object p : list) {
                priceList.add(toDouble(p));
            }
        }

        if (priceList.isEmpty()) {
            return Map.of("status", "error", "message", "No price series provided for technical calculation.");
        }

        double sum = 0.0;
        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;

        for (double p : priceList) {
            sum += p;
            if (p < minPrice) minPrice = p;
            if (p > maxPrice) maxPrice = p;
        }

        double sma = sum / priceList.size();
        double firstPrice = priceList.getFirst();
        double lastPrice = priceList.getLast();
        double priceChange = lastPrice - firstPrice;
        double priceChangePct = firstPrice > 0 ? (priceChange / firstPrice) * 100.0 : 0.0;

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "success");
        res.put("operation", "calculate_technical_indicator");
        res.put("symbol", symbol);
        res.put("indicator", indicator);
        res.put("periods_analyzed", priceList.size());
        res.put("sma", round(sma));
        res.put("first_price", round(firstPrice));
        res.put("last_price", round(lastPrice));
        res.put("price_change", round(priceChange));
        res.put("price_change_pct", round(priceChangePct));
        res.put("min_price", round(minPrice));
        res.put("max_price", round(maxPrice));
        res.put("spread_range", round(maxPrice - minPrice));
        return res;
    }

    private static double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(obj).trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static int toInt(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(obj).replaceAll("\\.0$", "").trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
