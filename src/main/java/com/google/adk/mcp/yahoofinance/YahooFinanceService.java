package com.google.adk.mcp.yahoofinance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * High-level service implementing the 4 core financial analytical tools
 * mirroring the Python yahoo-finance-mcp reference implementation.
 */
public class YahooFinanceService {
    private static final Logger logger = LoggerFactory.getLogger(YahooFinanceService.class);
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final YahooFinanceApiClient apiClient;
    private final ObjectMapper objectMapper;

    public enum FinancialType {
        income_stmt("incomeStatementHistory", "incomeStatementHistory"),
        quarterly_income_stmt("incomeStatementHistoryQuarterly", "incomeStatementHistory"),
        balance_sheet("balanceSheetHistory", "balanceSheetStatements"),
        quarterly_balance_sheet("balanceSheetHistoryQuarterly", "balanceSheetStatements"),
        cashflow("cashflowStatementHistory", "cashflowStatements"),
        quarterly_cashflow("cashflowStatementHistoryQuarterly", "cashflowStatements");

        private final String moduleName;
        private final String statementArrayKey;

        FinancialType(String moduleName, String statementArrayKey) {
            this.moduleName = moduleName;
            this.statementArrayKey = statementArrayKey;
        }

        public String getModuleName() {
            return moduleName;
        }

        public String getStatementArrayKey() {
            return statementArrayKey;
        }

        public static Optional<FinancialType> fromString(String val) {
            if (val == null || val.isBlank() || val.equalsIgnoreCase("null")) {
                return Optional.of(income_stmt);
            }
            String cleaned = val.trim().toLowerCase().replace("-", "_");
            if (cleaned.equals("income_statement") || cleaned.equals("income") || cleaned.equals("pnl")) {
                return Optional.of(income_stmt);
            }
            if (cleaned.equals("quarterly_income_statement") || cleaned.equals("quarterly_income")) {
                return Optional.of(quarterly_income_stmt);
            }
            if (cleaned.equals("balance_sheet") || cleaned.equals("bs")) {
                return Optional.of(balance_sheet);
            }
            if (cleaned.equals("quarterly_balance_sheet")) {
                return Optional.of(quarterly_balance_sheet);
            }
            if (cleaned.equals("cash_flow") || cleaned.equals("cashflow_statement") || cleaned.equals("cashflow")) {
                return Optional.of(cashflow);
            }
            if (cleaned.equals("quarterly_cash_flow") || cleaned.equals("quarterly_cashflow")) {
                return Optional.of(quarterly_cashflow);
            }
            for (FinancialType type : values()) {
                if (type.name().equalsIgnoreCase(cleaned)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }

    public enum RecommendationType {
        recommendations,
        upgrades_downgrades;

        public static Optional<RecommendationType> fromString(String val) {
            if (val == null || val.isBlank() || val.equalsIgnoreCase("null")) {
                return Optional.of(recommendations);
            }
            String cleaned = val.trim().toLowerCase().replace("-", "_");
            if (cleaned.contains("upgrade") || cleaned.contains("downgrade")) {
                return Optional.of(upgrades_downgrades);
            }
            if (cleaned.contains("recommend")) {
                return Optional.of(recommendations);
            }
            for (RecommendationType type : values()) {
                if (type.name().equalsIgnoreCase(cleaned)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }

    public YahooFinanceService() {
        this(new YahooFinanceApiClient());
    }

    public YahooFinanceService(YahooFinanceApiClient apiClient) {
        this.apiClient = apiClient;
        this.objectMapper = apiClient.getObjectMapper();
    }

    /**
     * 1. get_stock_info:
     * Comprehensive stock information including quote, company profile, financial metrics,
     * valuation multiples, balance sheet highlights, and analyst coverage.
     */
    public String getStockInfo(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return "Error: ticker symbol is required.";
        }
        try {
            String modules = "summaryProfile,financialData,defaultKeyStatistics,summaryDetail,price";
            JsonNode qsRoot = apiClient.fetchQuoteSummary(ticker, modules);
            JsonNode resultNode = qsRoot.path("quoteSummary").path("result");

            if (resultNode.isMissingNode() || !resultNode.isArray() || resultNode.isEmpty()) {
                return "No stock info found for ticker " + ticker + ".";
            }

            JsonNode data = resultNode.get(0);
            ObjectNode infoMap = objectMapper.createObjectNode();

            // Extract from 'price' module
            JsonNode price = data.path("price");
            extractField(price, "symbol", infoMap);
            extractField(price, "shortName", infoMap);
            extractField(price, "longName", infoMap);
            extractField(price, "currency", infoMap);
            extractField(price, "quoteType", infoMap);
            extractField(price, "exchangeName", infoMap);
            extractField(price, "marketState", infoMap);
            extractRawOrValue(price, "regularMarketPrice", infoMap);
            extractRawOrValue(price, "regularMarketChange", infoMap);
            extractRawOrValue(price, "regularMarketChangePercent", infoMap);
            extractRawOrValue(price, "regularMarketVolume", infoMap);
            extractRawOrValue(price, "marketCap", infoMap);

            // Extract from 'summaryProfile' module
            JsonNode summaryProfile = data.path("summaryProfile");
            extractField(summaryProfile, "sector", infoMap);
            extractField(summaryProfile, "industry", infoMap);
            extractField(summaryProfile, "website", infoMap);
            extractField(summaryProfile, "city", infoMap);
            extractField(summaryProfile, "state", infoMap);
            extractField(summaryProfile, "country", infoMap);
            extractField(summaryProfile, "fullTimeEmployees", infoMap);
            extractField(summaryProfile, "longBusinessSummary", infoMap);

            // Extract from 'summaryDetail' module
            JsonNode summaryDetail = data.path("summaryDetail");
            extractRawOrValue(summaryDetail, "previousClose", infoMap);
            extractRawOrValue(summaryDetail, "open", infoMap);
            extractRawOrValue(summaryDetail, "dayLow", infoMap);
            extractRawOrValue(summaryDetail, "dayHigh", infoMap);
            extractRawOrValue(summaryDetail, "fiftyTwoWeekLow", infoMap);
            extractRawOrValue(summaryDetail, "fiftyTwoWeekHigh", infoMap);
            extractRawOrValue(summaryDetail, "fiftyDayAverage", infoMap);
            extractRawOrValue(summaryDetail, "twoHundredDayAverage", infoMap);
            extractRawOrValue(summaryDetail, "dividendRate", infoMap);
            extractRawOrValue(summaryDetail, "dividendYield", infoMap);
            extractRawOrValue(summaryDetail, "payoutRatio", infoMap);
            extractRawOrValue(summaryDetail, "trailingPE", infoMap);
            extractRawOrValue(summaryDetail, "forwardPE", infoMap);
            extractRawOrValue(summaryDetail, "beta", infoMap);

            // Extract from 'financialData' module
            JsonNode financialData = data.path("financialData");
            extractRawOrValue(financialData, "currentPrice", infoMap);
            extractRawOrValue(financialData, "targetHighPrice", infoMap);
            extractRawOrValue(financialData, "targetLowPrice", infoMap);
            extractRawOrValue(financialData, "targetMeanPrice", infoMap);
            extractRawOrValue(financialData, "targetMedianPrice", infoMap);
            extractField(financialData, "recommendationKey", infoMap);
            extractRawOrValue(financialData, "numberOfAnalystOpinions", infoMap);
            extractRawOrValue(financialData, "totalCash", infoMap);
            extractRawOrValue(financialData, "totalDebt", infoMap);
            extractRawOrValue(financialData, "totalRevenue", infoMap);
            extractRawOrValue(financialData, "debtToEquity", infoMap);
            extractRawOrValue(financialData, "revenueGrowth", infoMap);
            extractRawOrValue(financialData, "grossMargins", infoMap);
            extractRawOrValue(financialData, "ebitdaMargins", infoMap);
            extractRawOrValue(financialData, "operatingMargins", infoMap);
            extractRawOrValue(financialData, "profitMargins", infoMap);
            extractRawOrValue(financialData, "returnOnAssets", infoMap);
            extractRawOrValue(financialData, "returnOnEquity", infoMap);
            extractRawOrValue(financialData, "freeCashflow", infoMap);
            extractRawOrValue(financialData, "operatingCashflow", infoMap);

            // Extract from 'defaultKeyStatistics' module
            JsonNode defaultKeyStatistics = data.path("defaultKeyStatistics");
            extractRawOrValue(defaultKeyStatistics, "enterpriseValue", infoMap);
            extractRawOrValue(defaultKeyStatistics, "pegRatio", infoMap);
            extractRawOrValue(defaultKeyStatistics, "priceToBook", infoMap);
            extractRawOrValue(defaultKeyStatistics, "trailingEps", infoMap);
            extractRawOrValue(defaultKeyStatistics, "forwardEps", infoMap);
            extractRawOrValue(defaultKeyStatistics, "bookValue", infoMap);
            extractRawOrValue(defaultKeyStatistics, "sharesOutstanding", infoMap);

            if (infoMap.isEmpty() || !infoMap.has("symbol")) {
                return "No stock info found for ticker " + ticker + ".";
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(infoMap);
        } catch (Exception e) {
            logger.error("Error getting stock info for {}: {}", ticker, e.getMessage(), e);
            return "Error: getting stock information for " + ticker + ": " + e.getMessage();
        }
    }

    /**
     * 2. get_stock_actions:
     * Retrieves historical dividends and stock splits for a given ticker symbol.
     */
    public String getStockActions(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return "Error: ticker symbol is required.";
        }
        try {
            JsonNode chartRoot = apiClient.fetchChart(ticker, "1d", "5y", "div|split");
            JsonNode resultNode = chartRoot.path("chart").path("result");
            if (resultNode.isMissingNode() || !resultNode.isArray() || resultNode.isEmpty()) {
                return "[]";
            }

            JsonNode eventsNode = resultNode.get(0).path("events");
            if (eventsNode.isMissingNode() || eventsNode.isEmpty()) {
                return "[]";
            }

            Map<String, ObjectNode> actionsByDate = new TreeMap<>(Comparator.reverseOrder());

            // Process dividends
            JsonNode dividends = eventsNode.path("dividends");
            if (dividends.isObject()) {
                dividends.fields().forEachRemaining(entry -> {
                    JsonNode div = entry.getValue();
                    long timestamp = div.path("date").asLong(0);
                    if (timestamp == 0) timestamp = Long.parseLong(entry.getKey());
                    String dateStr = LocalDate.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.of("UTC"))
                            .format(ISO_DATE_FORMATTER);

                    double amount = div.path("amount").asDouble(0.0);
                    ObjectNode row = actionsByDate.computeIfAbsent(dateStr, d -> {
                        ObjectNode node = objectMapper.createObjectNode();
                        node.put("Date", d);
                        node.put("Dividends", 0.0);
                        node.put("Stock Splits", 0.0);
                        return node;
                    });
                    row.put("Dividends", amount);
                });
            }

            // Process splits
            JsonNode splits = eventsNode.path("splits");
            if (splits.isObject()) {
                splits.fields().forEachRemaining(entry -> {
                    JsonNode split = entry.getValue();
                    long timestamp = split.path("date").asLong(0);
                    if (timestamp == 0) timestamp = Long.parseLong(entry.getKey());
                    String dateStr = LocalDate.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.of("UTC"))
                            .format(ISO_DATE_FORMATTER);

                    double num = split.path("numerator").asDouble(1.0);
                    double den = split.path("denominator").asDouble(1.0);
                    double splitRatio = den != 0 ? (num / den) : 0.0;

                    ObjectNode row = actionsByDate.computeIfAbsent(dateStr, d -> {
                        ObjectNode node = objectMapper.createObjectNode();
                        node.put("Date", d);
                        node.put("Dividends", 0.0);
                        node.put("Stock Splits", 0.0);
                        return node;
                    });
                    row.put("Stock Splits", splitRatio);
                });
            }

            ArrayNode actionsArray = objectMapper.createArrayNode();
            actionsByDate.values().forEach(actionsArray::add);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actionsArray);
        } catch (Exception e) {
            logger.error("Error getting stock actions for {}: {}", ticker, e.getMessage(), e);
            return "Error: getting stock actions for " + ticker + ": " + e.getMessage();
        }
    }

    /**
     * 3. get_financial_statement:
     * Retrieves annual or quarterly income statement, balance sheet, or cashflow statement.
     */
    public String getFinancialStatement(String ticker, String financialType) {
        if (ticker == null || ticker.isBlank()) {
            return "Error: ticker symbol is required.";
        }
        Optional<FinancialType> typeOpt = FinancialType.fromString(financialType);
        if (typeOpt.isEmpty()) {
            return "Error: invalid financial type " + financialType +
                    ". Please use one of the following: income_stmt, quarterly_income_stmt, balance_sheet, quarterly_balance_sheet, cashflow, quarterly_cashflow.";
        }

        FinancialType type = typeOpt.get();
        try {
            JsonNode qsRoot = apiClient.fetchQuoteSummary(ticker, type.getModuleName());
            JsonNode resultNode = qsRoot.path("quoteSummary").path("result");
            if (resultNode.isMissingNode() || !resultNode.isArray() || resultNode.isEmpty()) {
                return "No financial statement data found for ticker " + ticker + ".";
            }

            JsonNode moduleNode = resultNode.get(0).path(type.getModuleName());
            JsonNode statements = moduleNode.path(type.getStatementArrayKey());
            if (statements.isMissingNode() || !statements.isArray() || statements.isEmpty()) {
                return "No financial statement data found for ticker " + ticker + ".";
            }

            ArrayNode resultsArray = objectMapper.createArrayNode();
            for (JsonNode stmt : statements) {
                ObjectNode row = objectMapper.createObjectNode();
                // Date extraction
                JsonNode endDateNode = stmt.path("endDate");
                if (endDateNode.isObject() && endDateNode.has("fmt")) {
                    row.put("date", endDateNode.path("fmt").asText());
                } else if (endDateNode.isObject() && endDateNode.has("raw")) {
                    String dateStr = LocalDate.ofInstant(
                            Instant.ofEpochSecond(endDateNode.path("raw").asLong()), ZoneId.of("UTC"))
                            .format(ISO_DATE_FORMATTER);
                    row.put("date", dateStr);
                } else {
                    row.put("date", endDateNode.asText());
                }

                // Copy all financial line items
                stmt.fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    if ("endDate".equals(key) || "maxAge".equals(key)) return;
                    extractRawOrValue(stmt, key, row);
                });

                resultsArray.add(row);
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultsArray);
        } catch (Exception e) {
            logger.error("Error getting financial statement for {}: {}", ticker, e.getMessage(), e);
            return "Error: getting financial statement for " + ticker + ": " + e.getMessage();
        }
    }

    /**
     * 4. get_recommendations:
     * Retrieves analyst recommendation trends or upgrade/downgrade history.
     */
    public String getRecommendations(String ticker, String recommendationType, Integer monthsBack) {
        if (ticker == null || ticker.isBlank()) {
            return "Error: ticker symbol is required.";
        }
        Optional<RecommendationType> typeOpt = RecommendationType.fromString(recommendationType);
        if (typeOpt.isEmpty()) {
            return "Error: invalid recommendation type " + recommendationType +
                    ". Please use one of the following: recommendations, upgrades_downgrades.";
        }

        int months = (monthsBack != null && monthsBack > 0) ? monthsBack : 12;

        try {
            if (typeOpt.get() == RecommendationType.recommendations) {
                JsonNode qsRoot = apiClient.fetchQuoteSummary(ticker, "recommendationTrend");
                JsonNode resultNode = qsRoot.path("quoteSummary").path("result");
                if (resultNode.isMissingNode() || !resultNode.isArray() || resultNode.isEmpty()) {
                    return "[]";
                }

                JsonNode trends = resultNode.get(0).path("recommendationTrend").path("trend");
                if (trends.isMissingNode() || !trends.isArray() || trends.isEmpty()) {
                    return "[]";
                }

                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(trends);
            } else {
                // upgrades_downgrades
                JsonNode qsRoot = apiClient.fetchQuoteSummary(ticker, "upgradeDowngradeHistory");
                JsonNode resultNode = qsRoot.path("quoteSummary").path("result");
                if (resultNode.isMissingNode() || !resultNode.isArray() || resultNode.isEmpty()) {
                    return "[]";
                }

                JsonNode historyNode = resultNode.get(0).path("upgradeDowngradeHistory").path("history");
                if (historyNode.isMissingNode() || !historyNode.isArray() || historyNode.isEmpty()) {
                    return "[]";
                }

                long cutoffSeconds = Instant.now().minus(Duration.ofDays(months * 30L)).getEpochSecond();
                List<JsonNode> filteredList = new ArrayList<>();
                for (JsonNode item : historyNode) {
                    long epoch = item.path("epochGradeDate").asLong(0);
                    if (epoch >= cutoffSeconds) {
                        filteredList.add(item);
                    }
                }

                // Sort descending by epochGradeDate
                filteredList.sort((a, b) -> Long.compare(b.path("epochGradeDate").asLong(0), a.path("epochGradeDate").asLong(0)));

                // Deduplicate by Firm (preserving the latest rating per firm)
                Set<String> seenFirms = new HashSet<>();
                ArrayNode outputArray = objectMapper.createArrayNode();

                for (JsonNode item : filteredList) {
                    String firm = item.path("firm").asText("");
                    if (!firm.isBlank() && seenFirms.add(firm.toLowerCase())) {
                        ObjectNode row = objectMapper.createObjectNode();
                        long epoch = item.path("epochGradeDate").asLong(0);
                        String gradeDate = Instant.ofEpochSecond(epoch).toString();
                        row.put("GradeDate", gradeDate);
                        row.put("Firm", firm);
                        row.put("ToGrade", item.path("toGrade").asText(""));
                        row.put("FromGrade", item.path("fromGrade").asText(""));
                        row.put("Action", item.path("action").asText(""));
                        outputArray.add(row);
                    }
                }

                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(outputArray);
            }
        } catch (Exception e) {
            logger.error("Error getting recommendations for {}: {}", ticker, e.getMessage(), e);
            return "Error: getting recommendations for " + ticker + ": " + e.getMessage();
        }
    }

    // Helper utilities for JSON extraction
    private void extractField(JsonNode source, String fieldName, ObjectNode target) {
        if (source != null && source.has(fieldName) && !source.get(fieldName).isNull()) {
            target.set(fieldName, source.get(fieldName));
        }
    }

    private void extractRawOrValue(JsonNode source, String fieldName, ObjectNode target) {
        if (source == null || !source.has(fieldName) || source.get(fieldName).isNull()) {
            return;
        }
        JsonNode node = source.get(fieldName);
        if (node.isObject() && node.has("raw")) {
            JsonNode raw = node.get("raw");
            if (raw.isNumber()) {
                target.set(fieldName, raw);
            } else if (raw.isBoolean()) {
                target.put(fieldName, raw.asBoolean());
            } else {
                target.put(fieldName, raw.asText());
            }
        } else {
            target.set(fieldName, node);
        }
    }
}
