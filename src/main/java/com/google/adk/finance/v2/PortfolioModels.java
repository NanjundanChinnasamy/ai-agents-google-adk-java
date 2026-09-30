package com.google.adk.finance.v2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain records and models for Week 1 — Version 2: Context & Holdings State Management.
 */
public final class PortfolioModels {

    /**
     * Customer record with composite primary key: (customerId, portfolioId).
     */
    public record CustomerRecord(
            String customerId,
            String portfolioId
    ) {}

    /**
     * Holding record representing a stock position within a portfolio.
     */
    public record PortfolioHoldingRecord(
            long id,
            String portfolioId,
            String symbol,
            String name,
            int quantity,
            double buyPrice,
            String currency,
            String boughtDate
    ) {
        public double totalInvested() {
            return quantity * buyPrice;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("portfolio_id", portfolioId);
            map.put("symbol", symbol);
            map.put("name", name);
            map.put("quantity", quantity);
            map.put("buy_price", buyPrice);
            map.put("currency", currency);
            map.put("bought_date", boughtDate);
            map.put("total_invested", totalInvested());
            return map;
        }
    }

    /**
     * Aggregated portfolio state for a customer.
     */
    public record CustomerPortfolio(
            String customerId,
            String portfolioId,
            List<PortfolioHoldingRecord> holdings
    ) {
        public double totalInvested() {
            return holdings.stream()
                    .mapToDouble(PortfolioHoldingRecord::totalInvested)
                    .sum();
        }

        public List<Map<String, Object>> holdingsAsMaps() {
            return holdings.stream()
                    .map(PortfolioHoldingRecord::toMap)
                    .toList();
        }

        /**
         * Formats holdings into a human-readable and model-friendly string for prompt templating.
         */
        public String toFormattedSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Customer ID: ").append(customerId)
              .append(" | Portfolio ID: ").append(portfolioId)
              .append(" | Total Holdings: ").append(holdings.size())
              .append("\n");

            sb.append(String.format("%-10s %-16s %-8s %-12s %-14s %-12s\n",
                    "Symbol", "Company", "Qty", "Buy Price", "Total (INR)", "Bought Date"));
            sb.append("-".repeat(76)).append("\n");

            for (PortfolioHoldingRecord h : holdings) {
                sb.append(String.format("%-10s %-16s %-8d %-12.2f %-14.2f %-12s\n",
                        h.symbol(), h.name(), h.quantity(), h.buyPrice(), h.totalInvested(), h.boughtDate()));
            }

            sb.append("-".repeat(76)).append("\n");
            sb.append(String.format("Total Invested Capital: %.2f INR\n", totalInvested()));
            return sb.toString();
        }
    }

    private PortfolioModels() {}
}
