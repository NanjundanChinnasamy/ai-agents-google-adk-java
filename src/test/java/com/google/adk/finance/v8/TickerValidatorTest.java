package com.google.adk.finance.v8;

import com.google.adk.finance.v8.guardrails.GuardrailResult;
import com.google.adk.finance.v8.guardrails.tool.TickerValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TickerValidatorTest {

    private TickerValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TickerValidator();
    }

    @Test
    @DisplayName("Valid Indian and US tickers should be ALLOWED")
    void testValidTickers() {
        assertThat(validator.evaluate("INFY.NS").isAllowed()).isTrue();
        assertThat(validator.evaluate("RELIANCE.NS").isAllowed()).isTrue();
        assertThat(validator.evaluate("HDFCBANK.NS").isAllowed()).isTrue();
        assertThat(validator.evaluate("TCS.BO").isAllowed()).isTrue();
        assertThat(validator.evaluate("AAPL").isAllowed()).isTrue();
        assertThat(validator.evaluate("MSFT").isAllowed()).isTrue();
        assertThat(validator.evaluate("^NSEI").isAllowed()).isTrue();
    }

    @Test
    @DisplayName("SQL injection in ticker symbol must be BLOCKED")
    void testSqlInjectionBlocked() {
        GuardrailResult r1 = validator.evaluate("INFY'; DROP TABLE stocks; --");
        assertThat(r1.isBlocked()).isTrue();
        assertThat(r1.reason()).contains("Security Violation");

        GuardrailResult r2 = validator.evaluate("RELIANCE' OR '1'='1");
        assertThat(r2.isBlocked()).isTrue();
    }

    @Test
    @DisplayName("Shell injection in ticker symbol must be BLOCKED")
    void testShellInjectionBlocked() {
        GuardrailResult r = validator.evaluate("INFY | cat /etc/passwd");
        assertThat(r.isBlocked()).isTrue();
    }

    @Test
    @DisplayName("Empty, blank, or excessively long tickers must be BLOCKED")
    void testMalformedTickersBlocked() {
        assertThat(validator.evaluate("").isBlocked()).isTrue();
        assertThat(validator.evaluate("   ").isBlocked()).isTrue();
        assertThat(validator.evaluate(null).isBlocked()).isTrue();
        assertThat(validator.evaluate("VERYLONGSYMBOLNAMEEXCEEDING15CHARS.NS").isBlocked()).isTrue();
    }

    @Test
    @DisplayName("Normalizes valid ticker to uppercase")
    void testNormalization() {
        assertThat(validator.normalize("infy.ns")).isEqualTo("INFY.NS");
        assertThat(validator.normalize(" reliance.ns ")).isEqualTo("RELIANCE.NS");
    }
}
