package com.google.adk.mcp.yahoofinance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YahooFinanceTickerNormalizationTest {

    @Test
    @DisplayName("Should normalize US share classes to hyphen format")
    void testNormalizeUsClassShares() {
        assertThat(YahooFinanceApiClient.normalizeTicker("BRK.B")).isEqualTo("BRK-B");
        assertThat(YahooFinanceApiClient.normalizeTicker("BRK/B")).isEqualTo("BRK-B");
        assertThat(YahooFinanceApiClient.normalizeTicker("brk.b")).isEqualTo("BRK-B");
        assertThat(YahooFinanceApiClient.normalizeTicker("BF.A")).isEqualTo("BF-A");
        assertThat(YahooFinanceApiClient.normalizeTicker("BF/B")).isEqualTo("BF-B");
        assertThat(YahooFinanceApiClient.normalizeTicker("BRK-B")).isEqualTo("BRK-B");
    }

    @Test
    @DisplayName("Should keep plain and international tickers unchanged")
    void testPreserveRegularAndInternationalTickers() {
        assertThat(YahooFinanceApiClient.normalizeTicker("AAPL")).isEqualTo("AAPL");
        assertThat(YahooFinanceApiClient.normalizeTicker("MSFT")).isEqualTo("MSFT");
        assertThat(YahooFinanceApiClient.normalizeTicker("SHOP.TO")).isEqualTo("SHOP.TO");
        assertThat(YahooFinanceApiClient.normalizeTicker("RIO.L")).isEqualTo("RIO.L");
        assertThat(YahooFinanceApiClient.normalizeTicker("RELIANCE.NS")).isEqualTo("RELIANCE.NS");
        assertThat(YahooFinanceApiClient.normalizeTicker("TCS.BO")).isEqualTo("TCS.BO");
        assertThat(YahooFinanceApiClient.normalizeTicker("7203.T")).isEqualTo("7203.T");
    }

    @Test
    @DisplayName("Should handle null and empty tickers safely")
    void testNullAndEmpty() {
        assertThat(YahooFinanceApiClient.normalizeTicker(null)).isNull();
        assertThat(YahooFinanceApiClient.normalizeTicker("")).isEqualTo("");
        assertThat(YahooFinanceApiClient.normalizeTicker("   ")).isEqualTo("   ");
    }
}
