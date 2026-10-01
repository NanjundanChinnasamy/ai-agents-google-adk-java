package com.google.adk.finance.v8.guardrails.input;

import com.google.adk.finance.v8.guardrails.GuardrailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, pattern-based detector for trading and financial transaction intent in user input.
 * <p>
 * Finance Advisor V8 is a strictly read-only advisory and research system.
 * Any explicit user intent to execute trades, sell or buy securities, transfer funds, or place orders
 * must be detected and blocked at the input stage — before the LLM is even consulted — to provide
 * a clear, observable audit log entry and a safe explanatory refusal.
 * <p>
 * <b>Why This Layer Exists:</b><br>
 * The {@link PromptInjectionDetector} handles adversarial instruction overrides.
 * This detector handles <em>legitimate but out-of-scope user intent</em> —
 * requests that are not adversarial but simply exceed the agent's research-only mandate.
 * <p>
 * <b>Pattern Design:</b><br>
 * Each rule uses a relaxed proximity match: verb anchor + {@code .{0,30}?} lookahead + object anchor.
 * This tolerates multi-word modifier phrases between verb and object
 * (e.g. "sell ALL MY portfolio OF shares", "place a limit order for TCS").
 * The {@code ?} makes the intermediate match non-greedy to avoid false span captures.
 * <p>
 * <b>Detection Categories:</b>
 * <ul>
 *   <li><b>SELL_INTENT</b>: "sell all my shares", "liquidate my portfolio", "dispose of holdings"</li>
 *   <li><b>BUY_INTENT</b>: "buy 100 shares of Infosys", "purchase TCS stock", "acquire equity"</li>
 *   <li><b>ORDER_INTENT</b>: "place a market order", "submit a limit order", "create a trade"</li>
 *   <li><b>TRANSFER_INTENT</b>: "transfer funds", "wire money to", "send cash to account"</li>
 *   <li><b>CANCEL_ORDER_INTENT</b>: "cancel my order", "revoke my position"</li>
 * </ul>
 */
public class TradingIntentDetector {
    private static final Logger logger = LoggerFactory.getLogger(TradingIntentDetector.class);

    public record IntentMatch(String category, String snippet) {}

    private record CompiledRule(String category, Pattern pattern, String description) {}

    private final List<CompiledRule> rules;

    public TradingIntentDetector() {
        rules = List.of(
                // SELL_INTENT: "sell all my shares", "sell all my portfolio of shares"
                // Key fix: use .{0,30}? between verb and object to allow multi-word modifier phrases
                new CompiledRule(
                        "SELL_INTENT",
                        Pattern.compile(
                                "(?i)\\b(?:sell|selling|liquidate|liquidation|dispose|offload|exit)\\b.{0,35}?\\b(?:shares?|stocks?|portfolio|holdings?|equities|positions?)\\b",
                                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                        ),
                        "Attempt to sell or liquidate securities"
                ),
                // BUY_INTENT: "buy 100 shares of Infosys", "purchase TCS stock"
                new CompiledRule(
                        "BUY_INTENT",
                        Pattern.compile(
                                "(?i)\\b(?:buy|purchase|acquire|invest\\s+in|pick\\s+up)\\b.{0,30}?\\b(?:shares?|stocks?|units?|equities|lot)\\b",
                                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                        ),
                        "Attempt to buy or purchase securities"
                ),
                // ORDER_INTENT: "place a market order", "submit limit order", "execute trade"
                new CompiledRule(
                        "ORDER_INTENT",
                        Pattern.compile(
                                "(?i)\\b(?:place|submit|create|put\\s+in|make)\\b.{0,20}?\\b(?:order|trade|transaction)\\b",
                                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                        ),
                        "Attempt to place or execute a financial order"
                ),
                // TRANSFER_INTENT: "transfer funds", "wire money", "send cash to account"
                new CompiledRule(
                        "TRANSFER_INTENT",
                        Pattern.compile(
                                "(?i)\\b(?:transfer|wire|send|move|remit)\\b.{0,20}?\\b(?:funds?|money|cash|capital|balance|amount)\\b",
                                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                        ),
                        "Attempt to transfer or move funds"
                ),
                // CANCEL_ORDER_INTENT: "cancel my order", "revoke the trade"
                new CompiledRule(
                        "CANCEL_ORDER_INTENT",
                        Pattern.compile(
                                "(?i)\\b(?:cancel|revoke|withdraw)\\b.{0,20}?\\b(?:order|trade|transaction|position)\\b",
                                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                        ),
                        "Attempt to cancel an order or position"
                )
        );
    }

    /**
     * Evaluates the user input for explicit financial transaction intent.
     *
     * @param input the raw user text
     * @return {@link GuardrailResult#block} if trading intent is detected, otherwise {@link GuardrailResult#allow}
     */
    public GuardrailResult evaluate(String input) {
        if (input == null || input.isBlank()) {
            return GuardrailResult.allow("TradingIntentDetector", "Empty input.");
        }

        for (CompiledRule rule : rules) {
            Matcher m = rule.pattern().matcher(input);
            if (m.find()) {
                String snippet = m.group();
                logger.warn("[Security Alert] Trading intent detected. Category: {}, Matched: '{}'",
                        rule.category(), snippet);
                return GuardrailResult.block(
                        "TradingIntentDetector",
                        "Unauthorized Action Request: Finance Advisor V8 is a strictly read-only research and decision-support system. " +
                        "It does not have the authority to " + describeCategory(rule.category()) + ". " +
                        "If you wish to take action, please consult a licensed broker or your trading platform directly. " +
                        "I can, however, provide research, analysis, or portfolio performance summaries.",
                        Map.of("category", rule.category(), "matchedSnippet", snippet)
                );
            }
        }

        return GuardrailResult.allow("TradingIntentDetector", "No trading intent detected.");
    }

    private String describeCategory(String category) {
        return switch (category) {
            case "SELL_INTENT"        -> "sell, liquidate, or dispose of securities";
            case "BUY_INTENT"         -> "buy or purchase securities";
            case "ORDER_INTENT"       -> "place, submit, or execute financial orders";
            case "TRANSFER_INTENT"    -> "transfer, wire, or move funds";
            case "CANCEL_ORDER_INTENT"-> "cancel orders or positions";
            default                   -> "execute financial transactions";
        };
    }
}
