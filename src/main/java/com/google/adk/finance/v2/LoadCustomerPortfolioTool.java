package com.google.adk.finance.v2;

import com.google.adk.finance.v2.PortfolioModels.CustomerPortfolio;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Week 1 — Version 2 Custom Tool: Load Customer Portfolio.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Database querying from an ADK custom {@link BaseTool}.</li>
 *   <li>Dynamic state injection via {@link ToolContext#state()}.</li>
 *   <li>Structured return payloads for model grounding and subsequent reasoning.</li>
 * </ul>
 */
public class LoadCustomerPortfolioTool extends BaseTool {
    private static final Logger logger = LoggerFactory.getLogger(LoadCustomerPortfolioTool.class);

    public static final String TOOL_NAME = "load_customer_portfolio";

    public LoadCustomerPortfolioTool() {
        super(TOOL_NAME, "Loads customer portfolio holdings from SQLite database by customer ID and registers them into session state.");
    }

    @Override
    public Optional<FunctionDeclaration> declaration() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("customer_id", Schema.builder()
                .type(Type.Known.STRING)
                .description("The customer ID whose portfolio holdings to retrieve (e.g. '1001' or '1002').")
                .build());

        Schema parameters = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(properties)
                .required(List.of("customer_id"))
                .build();

        return Optional.of(FunctionDeclaration.builder()
                .name(TOOL_NAME)
                .description("Loads customer portfolio holdings from SQLite database by customer ID and registers them into session state.")
                .parameters(parameters)
                .build());
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
        Object rawId = args != null ? args.get("customer_id") : null;
        if (rawId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Missing required argument 'customer_id'.");
            return Single.just(error);
        }

        // Clean any floating point representations (e.g. 1001.0 -> 1001)
        String customerId = String.valueOf(rawId).replaceAll("\\.0$", "").trim();
        logger.info("Executing load_customer_portfolio for customerId={}", customerId);

        Optional<CustomerPortfolio> portfolioOpt = CustomerPortfolioRepository.findPortfolioByCustomerId(customerId);
        if (portfolioOpt.isEmpty()) {
            logger.warn("Customer ID '{}' not found in portfolio database.", customerId);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("customer_id", customerId);
            error.put("message", "Customer ID '" + customerId + "' was not found in the portfolio database. Available demo customers are 1001 and 1002.");
            return Single.just(error);
        }

        CustomerPortfolio portfolio = portfolioOpt.get();

        // Register directly into active session state so the prompt template and subsequent turns retain it
        if (toolContext != null && toolContext.state() != null) {
            toolContext.state().put("customer_id", portfolio.customerId());
            toolContext.state().put("portfolio_id", portfolio.portfolioId());
            toolContext.state().put("portfolio_holdings", portfolio.toFormattedSummary());
            toolContext.state().put("portfolio_holdings_list", portfolio.holdingsAsMaps());
            toolContext.state().put("total_invested", portfolio.totalInvested());
            toolContext.state().put("portfolio_loaded", true);
            logger.info("Successfully updated session state with portfolio for customerId={}", customerId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("customer_id", portfolio.customerId());
        result.put("portfolio_id", portfolio.portfolioId());
        result.put("holdings_count", portfolio.holdings().size());
        result.put("total_invested", portfolio.totalInvested());
        result.put("currency", "INR");
        result.put("holdings", portfolio.holdingsAsMaps());
        result.put("summary", portfolio.toFormattedSummary());
        result.put("message", "Portfolio " + portfolio.portfolioId() + " successfully loaded into session state for customer " + customerId + ".");

        return Single.just(result);
    }
}
