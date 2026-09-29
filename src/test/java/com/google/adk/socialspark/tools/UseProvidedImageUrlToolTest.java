package com.google.adk.socialspark.tools;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UseProvidedImageUrlToolTest {

    @Test
    void testInvalidUrlScheme() {
        Map<String, Object> result = UseProvidedImageUrlTool.validateUrl("ftp://example.com/image.png");
        assertThat(result.get("status")).isEqualTo("error");
        assertThat((String) result.get("error")).contains("Not a valid http(s) URL");
    }

    @Test
    void testEmptyUrl() {
        Map<String, Object> result = UseProvidedImageUrlTool.validateUrl("");
        assertThat(result.get("status")).isEqualTo("error");
        assertThat((String) result.get("error")).contains("Not a valid http(s) URL");
    }
}
