package com.google.adk.finance.v11.ruleloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic loader for V11 rules files located under {@code v11/rules/}.
 * <p>
 * Supports file-system resolution from project root, classpath fallback, and thread-safe caching.
 */
public final class RuleLoader {
    private static final Logger logger = LoggerFactory.getLogger(RuleLoader.class);

    public static final String FINANCE_RULES = "finance-rules.md";
    public static final String RESEARCH_RULES = "research-rules.md";
    public static final String SOURCE_RULES = "source-rules.md";
    public static final String RISK_RULES = "risk-rules.md";
    public static final String RESPONSE_RULES = "response-rules.md";

    public static final List<String> ALL_RULE_FILES = List.of(
            FINANCE_RULES,
            RESEARCH_RULES,
            SOURCE_RULES,
            RISK_RULES,
            RESPONSE_RULES
    );

    private static final Path DEFAULT_RULES_DIR = Paths.get("v11", "rules");
    private final Path rulesDirectory;
    private final Map<String, String> rulesCache = new ConcurrentHashMap<>();

    private static final RuleLoader INSTANCE = new RuleLoader();

    public static RuleLoader getInstance() {
        return INSTANCE;
    }

    public RuleLoader() {
        this(DEFAULT_RULES_DIR);
    }

    public RuleLoader(Path rulesDirectory) {
        this.rulesDirectory = rulesDirectory;
    }

    /**
     * Loads the raw text of a specific rule file.
     *
     * @param ruleFileName file name (e.g. "finance-rules.md")
     * @return rule markdown contents
     */
    public String loadRule(String ruleFileName) {
        return rulesCache.computeIfAbsent(ruleFileName, this::readRuleFromSource);
    }

    /**
     * Loads all 5 V11 rule files into an ordered map.
     */
    public Map<String, String> loadAllRules() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String ruleFile : ALL_RULE_FILES) {
            map.put(ruleFile, loadRule(ruleFile));
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Creates a scoped rule bundle for an agent.
     *
     * @param scopeName identifier of the agent/scope (e.g. "MarketResearch")
     * @param ruleFileNames list of rule files to bundle
     * @return {@link ScopedRules}
     */
    public ScopedRules getScopedRules(String scopeName, List<String> ruleFileNames) {
        StringBuilder combined = new StringBuilder();
        for (String fileName : ruleFileNames) {
            String content = loadRule(fileName);
            if (!content.isBlank()) {
                if (!combined.isEmpty()) {
                    combined.append("\n\n---\n\n");
                }
                combined.append(content);
            }
        }
        return new ScopedRules(scopeName, ruleFileNames, combined.toString());
    }

    // --- Predefined Agent Scopes ---

    /**
     * General Finance Scope: applies baseline {@code finance-rules.md}.
     */
    public ScopedRules forFinanceAgent() {
        return getScopedRules("FinanceGeneral", List.of(FINANCE_RULES));
    }

    /**
     * Market Research Scope: applies {@code finance-rules.md}, {@code research-rules.md}, and {@code source-rules.md}.
     */
    public ScopedRules forMarketResearchAgent() {
        return getScopedRules("MarketResearch", List.of(FINANCE_RULES, RESEARCH_RULES, SOURCE_RULES));
    }

    /**
     * Fundamental Analysis Scope: applies {@code finance-rules.md} and {@code source-rules.md}.
     */
    public ScopedRules forFundamentalAnalysisAgent() {
        return getScopedRules("FundamentalAnalysis", List.of(FINANCE_RULES, SOURCE_RULES));
    }

    /**
     * Portfolio Risk Scope: applies {@code finance-rules.md} and {@code risk-rules.md}.
     */
    public ScopedRules forRiskAnalysisAgent() {
        return getScopedRules("RiskAnalysis", List.of(FINANCE_RULES, RISK_RULES));
    }

    /**
     * Response Synthesis Scope: applies {@code finance-rules.md} and {@code response-rules.md}.
     */
    public ScopedRules forResponseSynthesisAgent() {
        return getScopedRules("ResponseSynthesis", List.of(FINANCE_RULES, RESPONSE_RULES));
    }

    /**
     * Root Advisor Orchestrator: applies all rules for complete supervisory awareness.
     */
    public ScopedRules forRootAdvisorAgent() {
        return getScopedRules("RootAdvisorOrchestrator", ALL_RULE_FILES);
    }

    /**
     * Clears the in-memory cache (primarily for unit testing reload scenarios).
     */
    public void clearCache() {
        rulesCache.clear();
    }

    private String readRuleFromSource(String ruleFileName) {
        // 1. Try specified rules directory on disk
        Path filePath = rulesDirectory.resolve(ruleFileName);
        if (Files.isRegularFile(filePath)) {
            try {
                logger.debug("Loading rule from disk: {}", filePath.toAbsolutePath());
                return Files.readString(filePath, StandardCharsets.UTF_8).trim();
            } catch (IOException e) {
                logger.warn("Failed reading rule file {}: {}", filePath, e.getMessage());
            }
        }

        // 2. Try looking relative to current directory if rulesDirectory was customized
        Path fallbackDiskPath = Paths.get("v11", "rules", ruleFileName);
        if (!fallbackDiskPath.equals(filePath) && Files.isRegularFile(fallbackDiskPath)) {
            try {
                logger.debug("Loading rule from fallback disk path: {}", fallbackDiskPath.toAbsolutePath());
                return Files.readString(fallbackDiskPath, StandardCharsets.UTF_8).trim();
            } catch (IOException e) {
                logger.warn("Failed reading fallback rule file {}: {}", fallbackDiskPath, e.getMessage());
            }
        }

        // 3. Try classpath resource: /v11/rules/<filename>
        String resourcePath = "/v11/rules/" + ruleFileName;
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in != null) {
                logger.debug("Loading rule from classpath resource: {}", resourcePath);
                return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
        } catch (IOException e) {
            logger.warn("Failed reading rule classpath resource {}: {}", resourcePath, e.getMessage());
        }

        logger.error("Could not find rule file '{}' either on disk at '{}' or on classpath '{}'",
                ruleFileName, filePath, resourcePath);
        return "# Error: Rule file " + ruleFileName + " not found.";
    }
}
