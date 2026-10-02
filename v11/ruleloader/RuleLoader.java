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

    public String loadRule(String ruleFileName) {
        return rulesCache.computeIfAbsent(ruleFileName, this::readRuleFromSource);
    }

    public Map<String, String> loadAllRules() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String ruleFile : ALL_RULE_FILES) {
            map.put(ruleFile, loadRule(ruleFile));
        }
        return Collections.unmodifiableMap(map);
    }

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

    public ScopedRules forFinanceAgent() {
        return getScopedRules("FinanceGeneral", List.of(FINANCE_RULES));
    }

    public ScopedRules forMarketResearchAgent() {
        return getScopedRules("MarketResearch", List.of(FINANCE_RULES, RESEARCH_RULES, SOURCE_RULES));
    }

    public ScopedRules forFundamentalAnalysisAgent() {
        return getScopedRules("FundamentalAnalysis", List.of(FINANCE_RULES, SOURCE_RULES));
    }

    public ScopedRules forRiskAnalysisAgent() {
        return getScopedRules("RiskAnalysis", List.of(FINANCE_RULES, RISK_RULES));
    }

    public ScopedRules forResponseSynthesisAgent() {
        return getScopedRules("ResponseSynthesis", List.of(FINANCE_RULES, RESPONSE_RULES));
    }

    public ScopedRules forRootAdvisorAgent() {
        return getScopedRules("RootAdvisorOrchestrator", ALL_RULE_FILES);
    }

    public void clearCache() {
        rulesCache.clear();
    }

    private String readRuleFromSource(String ruleFileName) {
        Path filePath = rulesDirectory.resolve(ruleFileName);
        if (Files.isRegularFile(filePath)) {
            try {
                logger.debug("Loading rule from disk: {}", filePath.toAbsolutePath());
                return Files.readString(filePath, StandardCharsets.UTF_8).trim();
            } catch (IOException e) {
                logger.warn("Failed reading rule file {}: {}", filePath, e.getMessage());
            }
        }

        Path fallbackDiskPath = Paths.get("v11", "rules", ruleFileName);
        if (!fallbackDiskPath.equals(filePath) && Files.isRegularFile(fallbackDiskPath)) {
            try {
                logger.debug("Loading rule from fallback disk path: {}", fallbackDiskPath.toAbsolutePath());
                return Files.readString(fallbackDiskPath, StandardCharsets.UTF_8).trim();
            } catch (IOException e) {
                logger.warn("Failed reading fallback rule file {}: {}", fallbackDiskPath, e.getMessage());
            }
        }

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
