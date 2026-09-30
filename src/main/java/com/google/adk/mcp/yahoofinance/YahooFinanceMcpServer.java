package com.google.adk.mcp.yahoofinance;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Official Java Model Context Protocol (MCP) Server for Yahoo Finance.
 * Implements 4 core financial tools:
 * 1. get_stock_info: Comprehensive stock metrics, price, valuation, balance sheet & company profile.
 * 2. get_stock_actions: Historical stock dividends and stock splits.
 * 3. get_financial_statement: Annual or quarterly income statements, balance sheets, and cash flow statements.
 * 4. get_recommendations: Analyst recommendation trends or firm upgrade/downgrade history.
 */
public class YahooFinanceMcpServer {
    static {
        System.setProperty("logback.statusListenerClass", "ch.qos.logback.core.status.NopStatusListener");
    }

    private static final Logger logger = LoggerFactory.getLogger(YahooFinanceMcpServer.class);

    private final YahooFinanceService service;
    private final McpJsonMapper jsonMapper;
    private final StdioServerTransportProvider transportProvider;
    private final McpSyncServer syncServer;

    public YahooFinanceMcpServer() {
        this(new YahooFinanceService(), System.in, System.out);
    }

    public YahooFinanceMcpServer(YahooFinanceService service, InputStream inputStream, OutputStream outputStream) {
        this.service = service;
        this.jsonMapper = new JacksonMcpJsonMapperSupplier().get();
        this.transportProvider = new StdioServerTransportProvider(this.jsonMapper, inputStream, outputStream);

        List<McpServerFeatures.SyncToolSpecification> toolSpecs = buildToolSpecifications();

        this.syncServer = McpServer.sync(this.transportProvider)
                .serverInfo("yahoo-finance-mcp", "1.0.0")
                .instructions("""
                    # Yahoo Finance MCP Server (Java)
                    This server provides access to financial data from Yahoo Finance for a given ticker symbol.
                    
                    Available tools:
                    - get_stock_info: Get comprehensive stock information including stock price & trading info, company information, financial metrics, earnings, margins, valuation, dividends, balance sheet, and analyst coverage.
                    - get_stock_actions: Get stock dividends and stock splits for a given ticker symbol.
                    - get_financial_statement: Get financial statement (income_stmt, quarterly_income_stmt, balance_sheet, quarterly_balance_sheet, cashflow, quarterly_cashflow).
                    - get_recommendations: Get recommendations or upgrades/downgrades for a given ticker symbol.
                    """)
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .tools(toolSpecs)
                .build();
    }

    private List<McpServerFeatures.SyncToolSpecification> buildToolSpecifications() {
        List<McpServerFeatures.SyncToolSpecification> list = new ArrayList<>();

        // 1. get_stock_info
        String stockInfoSchema = """
            {
              "type": "object",
              "properties": {
                "ticker": {
                  "type": "string",
                  "description": "The ticker symbol of the stock to get information for, e.g. 'AAPL', 'INFY', 'MSFT', 'RELIANCE.NS'"
                },
                "symbol": {
                  "type": "string",
                  "description": "Alias for ticker symbol, e.g. 'AAPL', 'INFY'"
                }
              },
              "required": ["ticker"]
            }
            """;

        McpSchema.Tool stockInfoTool = McpSchema.Tool.builder()
                .name("get_stock_info")
                .description("Get stock information for a given ticker symbol from yahoo finance. Include stock price & trading info, company information, financial metrics, earnings & revenue, margins & returns, dividends, balance sheet, analyst coverage, risk metrics, other.")
                .inputSchema(jsonMapper, stockInfoSchema)
                .build();

        list.add(new McpServerFeatures.SyncToolSpecification(stockInfoTool, (exchange, request) -> {
            try {
                Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
                String ticker = extractTicker(args);
                String result = service.getStockInfo(ticker);
                return McpSchema.CallToolResult.builder()
                        .addTextContent(result)
                        .isError(false)
                        .build();
            } catch (Exception e) {
                logger.error("Error in get_stock_info tool execution: {}", e.getMessage(), e);
                return McpSchema.CallToolResult.builder()
                        .addTextContent("Error: getting stock information: " + e.getMessage())
                        .isError(true)
                        .build();
            }
        }));

        // 2. get_stock_actions
        String stockActionsSchema = """
            {
              "type": "object",
              "properties": {
                "ticker": {
                  "type": "string",
                  "description": "The ticker symbol of the stock to get stock actions for, e.g. 'AAPL', 'INFY'"
                },
                "symbol": {
                  "type": "string",
                  "description": "Alias for ticker symbol, e.g. 'AAPL', 'INFY'"
                }
              },
              "required": ["ticker"]
            }
            """;

        McpSchema.Tool stockActionsTool = McpSchema.Tool.builder()
                .name("get_stock_actions")
                .description("Get stock dividends and stock splits for a given ticker symbol from yahoo finance.")
                .inputSchema(jsonMapper, stockActionsSchema)
                .build();

        list.add(new McpServerFeatures.SyncToolSpecification(stockActionsTool, (exchange, request) -> {
            try {
                Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
                String ticker = extractTicker(args);
                String result = service.getStockActions(ticker);
                return McpSchema.CallToolResult.builder()
                        .addTextContent(result)
                        .isError(false)
                        .build();
            } catch (Exception e) {
                logger.error("Error in get_stock_actions tool execution: {}", e.getMessage(), e);
                return McpSchema.CallToolResult.builder()
                        .addTextContent("Error: getting stock actions: " + e.getMessage())
                        .isError(true)
                        .build();
            }
        }));

        // 3. get_financial_statement
        String financialStmtSchema = """
            {
              "type": "object",
              "properties": {
                "ticker": {
                  "type": "string",
                  "description": "The ticker symbol of the stock to get financial statement for, e.g. 'AAPL', 'INFY'"
                },
                "symbol": {
                  "type": "string",
                  "description": "Alias for ticker symbol, e.g. 'AAPL', 'INFY'"
                },
                "financial_type": {
                  "type": "string",
                  "description": "The type of financial statement to get: 'income_stmt' (default), 'quarterly_income_stmt', 'balance_sheet', 'quarterly_balance_sheet', 'cashflow', 'quarterly_cashflow'",
                  "enum": [
                    "income_stmt",
                    "quarterly_income_stmt",
                    "balance_sheet",
                    "quarterly_balance_sheet",
                    "cashflow",
                    "quarterly_cashflow"
                  ]
                },
                "type": {
                  "type": "string",
                  "description": "Alias for financial_type"
                }
              },
              "required": ["ticker"]
            }
            """;

        McpSchema.Tool financialStmtTool = McpSchema.Tool.builder()
                .name("get_financial_statement")
                .description("Get financial statement for a given ticker symbol from yahoo finance. Choose from: income_stmt (default), quarterly_income_stmt, balance_sheet, quarterly_balance_sheet, cashflow, quarterly_cashflow.")
                .inputSchema(jsonMapper, financialStmtSchema)
                .build();

        list.add(new McpServerFeatures.SyncToolSpecification(financialStmtTool, (exchange, request) -> {
            try {
                Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
                String ticker = extractTicker(args);
                String financialType = extractFinancialType(args);
                String result = service.getFinancialStatement(ticker, financialType);
                return McpSchema.CallToolResult.builder()
                        .addTextContent(result)
                        .isError(false)
                        .build();
            } catch (Exception e) {
                logger.error("Error in get_financial_statement tool execution: {}", e.getMessage(), e);
                return McpSchema.CallToolResult.builder()
                        .addTextContent("Error: getting financial statement: " + e.getMessage())
                        .isError(true)
                        .build();
            }
        }));

        // 4. get_recommendations
        String recommendationsSchema = """
            {
              "type": "object",
              "properties": {
                "ticker": {
                  "type": "string",
                  "description": "The ticker symbol of the stock to get recommendations for, e.g. 'AAPL', 'INFY'"
                },
                "symbol": {
                  "type": "string",
                  "description": "Alias for ticker symbol, e.g. 'AAPL', 'INFY'"
                },
                "recommendation_type": {
                  "type": "string",
                  "description": "The type of recommendation to get: 'recommendations' (default) or 'upgrades_downgrades'",
                  "enum": ["recommendations", "upgrades_downgrades"]
                },
                "type": {
                  "type": "string",
                  "description": "Alias for recommendation_type: 'recommendations' or 'upgrades_downgrades'"
                },
                "months_back": {
                  "type": "integer",
                  "description": "The number of months back to get upgrades/downgrades for, default is 12."
                }
              },
              "required": ["ticker"]
            }
            """;

        McpSchema.Tool recommendationsTool = McpSchema.Tool.builder()
                .name("get_recommendations")
                .description("Get recommendations or upgrades/downgrades for a given ticker symbol from yahoo finance. Default type is 'recommendations'. Specify months_back (default 12) for upgrades/downgrades.")
                .inputSchema(jsonMapper, recommendationsSchema)
                .build();

        list.add(new McpServerFeatures.SyncToolSpecification(recommendationsTool, (exchange, request) -> {
            try {
                Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
                String ticker = extractTicker(args);
                String recommendationType = extractRecommendationType(args);
                Integer monthsBack = null;
                Object mbObj = args.get("months_back");
                if (mbObj instanceof Number num) {
                    monthsBack = num.intValue();
                } else if (mbObj instanceof String s && !s.isBlank()) {
                    try {
                        monthsBack = Integer.parseInt(s.trim());
                    } catch (NumberFormatException ignored) {}
                }

                String result = service.getRecommendations(ticker, recommendationType, monthsBack);
                return McpSchema.CallToolResult.builder()
                        .addTextContent(result)
                        .isError(false)
                        .build();
            } catch (Exception e) {
                logger.error("Error in get_recommendations tool execution: {}", e.getMessage(), e);
                return McpSchema.CallToolResult.builder()
                        .addTextContent("Error: getting recommendations: " + e.getMessage())
                        .isError(true)
                        .build();
            }
        }));

        return list;
    }

    private static String extractTicker(Map<String, Object> args) {
        if (args == null || args.isEmpty()) return null;
        if (args.containsKey("ticker") && args.get("ticker") != null) {
            String t = String.valueOf(args.get("ticker")).trim();
            if (!t.isBlank()) return t;
        }
        if (args.containsKey("symbol") && args.get("symbol") != null) {
            String s = String.valueOf(args.get("symbol")).trim();
            if (!s.isBlank()) return s;
        }
        return null;
    }

    private static String extractFinancialType(Map<String, Object> args) {
        if (args == null || args.isEmpty()) return "income_stmt";
        if (args.containsKey("financial_type") && args.get("financial_type") != null) {
            String ft = String.valueOf(args.get("financial_type")).trim();
            if (!ft.isBlank() && !ft.equalsIgnoreCase("null")) return ft;
        }
        if (args.containsKey("type") && args.get("type") != null) {
            String t = String.valueOf(args.get("type")).trim();
            if (!t.isBlank() && !t.equalsIgnoreCase("null")) return t;
        }
        return "income_stmt";
    }

    private static String extractRecommendationType(Map<String, Object> args) {
        if (args == null || args.isEmpty()) return "recommendations";
        if (args.containsKey("recommendation_type") && args.get("recommendation_type") != null) {
            String rt = String.valueOf(args.get("recommendation_type")).trim();
            if (!rt.isBlank() && !rt.equalsIgnoreCase("null")) return rt;
        }
        if (args.containsKey("type") && args.get("type") != null) {
            String t = String.valueOf(args.get("type")).trim();
            if (!t.isBlank() && !t.equalsIgnoreCase("null")) return t;
        }
        return "recommendations";
    }

    public McpSyncServer getSyncServer() {
        return syncServer;
    }

    public void close() {
        if (syncServer != null) {
            syncServer.close();
        }
    }

    public static void configureLoggingToStderr() {
        try {
            org.slf4j.ILoggerFactory factory = LoggerFactory.getILoggerFactory();
            if (factory instanceof ch.qos.logback.classic.LoggerContext context) {
                for (ch.qos.logback.classic.Logger l : context.getLoggerList()) {
                    java.util.Iterator<ch.qos.logback.core.Appender<ch.qos.logback.classic.spi.ILoggingEvent>> it = l.iteratorForAppenders();
                    while (it.hasNext()) {
                        ch.qos.logback.core.Appender<ch.qos.logback.classic.spi.ILoggingEvent> app = it.next();
                        if (app instanceof ch.qos.logback.core.ConsoleAppender<ch.qos.logback.classic.spi.ILoggingEvent> ca) {
                            ca.stop();
                            ca.setTarget("System.err");
                            ca.start();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void main(String[] args) {
        configureLoggingToStderr();

        // Direct all informational messages to System.err so System.out remains strictly clean for JSON-RPC
        System.err.println("Starting Yahoo Finance MCP server (Java)...");

        YahooFinanceMcpServer server = new YahooFinanceMcpServer();
        CountDownLatch shutdownLatch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Shutting down Yahoo Finance MCP server...");
            try {
                server.close();
            } catch (Exception ignored) {
            } finally {
                shutdownLatch.countDown();
            }
        }));

        System.err.println("Yahoo Finance MCP server running on stdio transport. Listening for JSON-RPC messages...");
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Yahoo Finance MCP server interrupted.");
        }
    }
}
