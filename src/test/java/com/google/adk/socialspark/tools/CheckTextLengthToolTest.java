package com.google.adk.socialspark.tools;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckTextLengthToolTest {

    @Test
    void testWithinLimit() {
        Map<String, Object> result = CheckTextLengthTool.checkLength("Short tweet", 280);
        assertThat(result.get("status")).isEqualTo("success");
        assertThat(result.get("length")).isEqualTo(11);
        assertThat(result.get("limit")).isEqualTo(280);
        assertThat(result.get("within_limit")).isEqualTo(true);
        assertThat(result.get("over_by")).isEqualTo(0);
    }

    @Test
    void testExactLimit() {
        String exact = "a".repeat(280);
        Map<String, Object> result = CheckTextLengthTool.checkLength(exact, 280);
        assertThat(result.get("length")).isEqualTo(280);
        assertThat(result.get("within_limit")).isEqualTo(true);
        assertThat(result.get("over_by")).isEqualTo(0);
    }

    @Test
    void testOverLimit() {
        String over = "a".repeat(295);
        Map<String, Object> result = CheckTextLengthTool.checkLength(over, 280);
        assertThat(result.get("length")).isEqualTo(295);
        assertThat(result.get("within_limit")).isEqualTo(false);
        assertThat(result.get("over_by")).isEqualTo(15);
    }
}
