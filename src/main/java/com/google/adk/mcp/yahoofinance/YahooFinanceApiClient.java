package com.google.adk.mcp.yahoofinance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robust HTTP client for retrieving financial data from Yahoo Finance APIs.
 * Automatically handles cookie sessions, crumb authentication, and US class-share ticker normalization.
 */
public class YahooFinanceApiClient {
    private static final Logger logger = LoggerFactory.getLogger(YahooFinanceApiClient.class);

    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    private static final Pattern CLASS_SHARE_PATTERN =
            Pattern.compile("^\\s*([A-Za-z]{1,6})[./-]([A-Za-z])\\s*$");
    private static final Set<String> CLASS_SHARE_SUFFIXES = Set.of("A", "B");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String userAgent;

    private volatile String cachedCrumb;
    private volatile long crumbTimestampMs;
    private static final long CRUMB_TTL_MS = 3600_000L; // 1 hour

    public YahooFinanceApiClient() {
        this(DEFAULT_USER_AGENT, new ObjectMapper());
    }

    public YahooFinanceApiClient(String userAgent, ObjectMapper objectMapper) {
        this.userAgent = userAgent != null ? userAgent : DEFAULT_USER_AGENT;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();

        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(12))
                .build();
    }

    /**
     * Normalizes US class-share tickers to the hyphen form expected by Yahoo Finance.
     * E.g., "BRK.B" -> "BRK-B", "BF.B" -> "BF-B".
     * Exchange suffixes like "SHOP.TO", "RIO.L", "TCS.NS" are retained unchanged.
     */
    public static String normalizeTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return ticker;
        }
        String trimmed = ticker.trim();
        Matcher matcher = CLASS_SHARE_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            String suffix = matcher.group(2).toUpperCase();
            if (CLASS_SHARE_SUFFIXES.contains(suffix)) {
                return matcher.group(1).toUpperCase() + "-" + suffix;
            }
        }
        return trimmed;
    }

    /**
     * Retrieves or refreshes the Yahoo Finance authentication crumb.
     */
    public synchronized String getOrRefreshCrumb(boolean forceRefresh) throws IOException, InterruptedException {
        long now = System.currentTimeMillis();
        if (!forceRefresh && cachedCrumb != null && (now - crumbTimestampMs) < CRUMB_TTL_MS) {
            return cachedCrumb;
        }

        logger.debug("Acquiring Yahoo Finance session cookie and crumb...");

        // 1. Visit fc.yahoo.com to set initial cookies
        try {
            HttpRequest cookieReq = HttpRequest.newBuilder(URI.create("https://fc.yahoo.com"))
                    .header("User-Agent", userAgent)
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();
            httpClient.send(cookieReq, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            logger.debug("Cookie probe to fc.yahoo.com completed: {}", e.getMessage());
        }

        // 2. Query crumb endpoint
        String[] crumbUrls = new String[] {
                "https://query2.finance.yahoo.com/v1/test/getcrumb",
                "https://query1.finance.yahoo.com/v1/test/getcrumb"
        };

        for (String url : crumbUrls) {
            try {
                HttpRequest crumbReq = HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", userAgent)
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();

                HttpResponse<String> resp = httpClient.send(crumbReq, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200 && resp.body() != null && !resp.body().isBlank()) {
                    cachedCrumb = resp.body().trim();
                    crumbTimestampMs = System.currentTimeMillis();
                    logger.debug("Successfully obtained Yahoo Finance crumb.");
                    return cachedCrumb;
                }
            } catch (Exception e) {
                logger.warn("Failed retrieving crumb from {}: {}", url, e.getMessage());
            }
        }

        throw new IOException("Failed to obtain Yahoo Finance crumb authorization token.");
    }

    /**
     * Fetches chart metadata, prices, or dividend/split events.
     */
    public JsonNode fetchChart(String ticker, String interval, String range, String events) throws IOException, InterruptedException {
        String normalized = normalizeTicker(ticker);
        StringBuilder urlBuilder = new StringBuilder("https://query1.finance.yahoo.com/v8/finance/chart/")
                .append(URLEncoder.encode(normalized, StandardCharsets.UTF_8))
                .append("?interval=").append(interval != null ? interval : "1d")
                .append("&range=").append(range != null ? range : "1mo");

        if (events != null && !events.isBlank()) {
            urlBuilder.append("&events=").append(URLEncoder.encode(events, StandardCharsets.UTF_8));
        }

        HttpRequest req = HttpRequest.newBuilder(URI.create(urlBuilder.toString()))
                .header("User-Agent", userAgent)
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Yahoo Finance Chart API error: HTTP " + resp.statusCode() + " for ticker " + ticker);
        }

        return objectMapper.readTree(resp.body());
    }

    /**
     * Fetches quote summary modules with automatic crumb injection and retry on authorization failure.
     */
    public JsonNode fetchQuoteSummary(String ticker, String modules) throws IOException, InterruptedException {
        String normalized = normalizeTicker(ticker);
        String crumb = getOrRefreshCrumb(false);

        String url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/" +
                URLEncoder.encode(normalized, StandardCharsets.UTF_8) +
                "?crumb=" + URLEncoder.encode(crumb, StandardCharsets.UTF_8) +
                "&modules=" + modules;

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", userAgent)
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        // Check if crumb expired / unauthorized and retry once
        if (resp.statusCode() == 401 || (resp.body() != null && resp.body().contains("Invalid Crumb"))) {
            logger.info("Crumb invalid or expired. Refreshing crumb and retrying quoteSummary request...");
            crumb = getOrRefreshCrumb(true);
            String retryUrl = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/" +
                    URLEncoder.encode(normalized, StandardCharsets.UTF_8) +
                    "?crumb=" + URLEncoder.encode(crumb, StandardCharsets.UTF_8) +
                    "&modules=" + modules;

            HttpRequest retryReq = HttpRequest.newBuilder(URI.create(retryUrl))
                    .header("User-Agent", userAgent)
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();

            resp = httpClient.send(retryReq, HttpResponse.BodyHandlers.ofString());
        }

        if (resp.statusCode() != 200) {
            throw new IOException("Yahoo Finance QuoteSummary API error: HTTP " + resp.statusCode() + " for ticker " + ticker);
        }

        return objectMapper.readTree(resp.body());
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
