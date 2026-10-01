package com.google.adk.finance.v8.guardrails.output;

import com.google.adk.finance.v8.evidence.EvidenceStore;
import com.google.adk.finance.v8.guardrails.GuardrailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Output guardrail verifying factual and numerical consistency between LLM generated statements
 * and empirical data recorded in the {@link EvidenceStore}.
 * <p>
 * Core Verification Capabilities:
 * <ol>
 *   <li><b>Ticker & Price Verification:</b> Extracts claimed share prices for companies and compares
 *       them against verified market quotes from Yahoo Finance MCP or portfolio records.</li>
 *   <li><b>Discrepancy Threshold:</b> Flags assertions exceeding a tolerance margin (default: 1.0%)
 *       as unverified hallucinations or mathematical errors.</li>
 *   <li><b>Observable Audit Reporting:</b> Attaches an explicit audit warning if discrepancies are detected,
 *       preventing ungrounded figures from being presented as verified facts.</li>
 * </ol>
 */
public class HallucinationDetector {
    private static final Logger logger = LoggerFactory.getLogger(HallucinationDetector.class);

    private final EvidenceStore evidenceStore;
    private final double tolerancePercentage;

    public record FactCheckItem(
            String entity,
            String symbol,
            double claimedValue,
            double verifiedValue,
            String metric,
            boolean matches,
            double discrepancyPercent
    ) {}

    // Regex matching statements like: "trading at ₹1,500", "price of 1850", "current price is ₹1,500.00"
    private static final Pattern PRICE_CLAIM_PATTERN = Pattern.compile(
            "(?i)(?:trading\\s+at|trades\\s+at|current\\s+price\\s+is|price\\s+is|price\\s+of|priced\\s+at|valued\\s+at)\\s*(?:₹|\\$|INR|USD)?\\s*([0-9]+(?:,[0-9]{3})*(?:\\.[0-9]+)?)"
    );

    // Common aliases mapping company names to standard tickers
    private static final Map<String, String> COMPANY_TO_TICKER = Map.of(
            "infosys", "INFY.NS",
            "infy", "INFY.NS",
            "reliance", "RELIANCE.NS",
            "tcs", "TCS.NS",
            "hdfc", "HDFCBANK.NS",
            "hdfc bank", "HDFCBANK.NS"
    );

    public HallucinationDetector(EvidenceStore evidenceStore) {
        this(evidenceStore, 0.01); // 1.0% tolerance
    }

    public HallucinationDetector(EvidenceStore evidenceStore, double tolerancePercentage) {
        this.evidenceStore = evidenceStore;
        this.tolerancePercentage = tolerancePercentage;
    }

    /**
     * Inspects candidate text against recorded evidence in the store.
     *
     * @param text model output text
     * @return list of {@link FactCheckItem} verification results
     */
    public List<FactCheckItem> checkFacts(String text) {
        if (text == null || text.isBlank() || evidenceStore.getAllFacts().isEmpty()) {
            return List.of();
        }

        List<FactCheckItem> items = new ArrayList<>();

        for (EvidenceStore.StockFact fact : evidenceStore.getAllFacts()) {
            String symbol = fact.symbol();
            String rootTicker = symbol.contains(".") ? symbol.substring(0, symbol.indexOf('.')) : symbol;

            // Check if model text mentions this company or ticker
            boolean mentionsStock = text.toUpperCase().contains(symbol) || text.toUpperCase().contains(rootTicker);
            if (!mentionsStock) {
                // Check aliases
                for (Map.Entry<String, String> entry : COMPANY_TO_TICKER.entrySet()) {
                    if (entry.getValue().equalsIgnoreCase(symbol) && text.toLowerCase().contains(entry.getKey())) {
                        mentionsStock = true;
                        break;
                    }
                }
            }

            if (!mentionsStock) {
                continue;
            }

            // Extract price claims
            Matcher matcher = PRICE_CLAIM_PATTERN.matcher(text);
            while (matcher.find()) {
                String priceStr = matcher.group(1).replace(",", "").trim();
                try {
                    double claimedPrice = Double.parseDouble(priceStr);
                    double verifiedPrice = fact.price();

                    double diff = Math.abs(claimedPrice - verifiedPrice) / verifiedPrice;
                    boolean matches = diff <= tolerancePercentage;

                    items.add(new FactCheckItem(
                            symbol,
                            symbol,
                            claimedPrice,
                            verifiedPrice,
                            "currentPrice",
                            matches,
                            diff * 100.0
                    ));
                } catch (NumberFormatException ignored) {}
            }
        }

        return items;
    }

    /**
     * Evaluates text and returns a {@link GuardrailResult}.
     *
     * @param text model generated response
     * @return {@link GuardrailResult#allow} if verified, or {@link GuardrailResult#block} or {@link GuardrailResult#warn} if mismatch found
     */
    public GuardrailResult evaluate(String text) {
        List<FactCheckItem> factChecks = checkFacts(text);
        if (factChecks.isEmpty()) {
            return GuardrailResult.allow("HallucinationDetector", "No specific tool-grounded numerical claims to audit.");
        }

        List<FactCheckItem> mismatches = factChecks.stream().filter(f -> !f.matches()).toList();
        if (mismatches.isEmpty()) {
            logger.info("[Hallucination Guardrail] All {} numerical assertion(s) verified against tool evidence.", factChecks.size());
            return GuardrailResult.allow(
                    "HallucinationDetector",
                    "All numerical figures verified against empirical tool evidence.",
                    Map.of("verifiedCount", factChecks.size())
            );
        }

        FactCheckItem mismatch = mismatches.get(0);
        logger.warn("[Hallucination Guardrail] Factual mismatch detected for {}: claimed={}, verified={}, discrepancy={}%",
                mismatch.symbol(), mismatch.claimedValue(), mismatch.verifiedValue(), String.format("%.2f", mismatch.discrepancyPercent()));

        String alertWarning = String.format(
                "\n\n> [!CAUTION]\n> **Factual Audit Alert**: The generated analysis contains an unverified figure for %s " +
                "(Claimed: %.2f, Verified Tool Data: %.2f, Discrepancy: %.2f%%). Unverified claims have been flagged.",
                mismatch.symbol(), mismatch.claimedValue(), mismatch.verifiedValue(), mismatch.discrepancyPercent()
        );

        return GuardrailResult.block(
                "HallucinationDetector",
                "Numerical hallucination or discrepancy detected for " + mismatch.symbol() +
                ": Model claimed " + mismatch.claimedValue() + ", but verified tool evidence recorded " + mismatch.verifiedValue() + ".",
                Map.of(
                        "mismatchCount", mismatches.size(),
                        "symbol", mismatch.symbol(),
                        "claimedPrice", mismatch.claimedValue(),
                        "verifiedPrice", mismatch.verifiedValue(),
                        "alertWarning", alertWarning
                )
        );
    }
}
