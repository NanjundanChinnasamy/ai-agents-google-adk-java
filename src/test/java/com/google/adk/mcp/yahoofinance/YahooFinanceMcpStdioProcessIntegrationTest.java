package com.google.adk.mcp.yahoofinance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class YahooFinanceMcpStdioProcessIntegrationTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Verify standalone mcp/yahoo-finance-mcp.jar runs over stdio and responds to MCP JSON-RPC")
    void testProcessHandshakeAndToolCall() throws Exception {
        File jarFile = new File("mcp/yahoo-finance-mcp.jar");
        assertThat(jarFile).exists();

        ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath());
        pb.redirectError(ProcessBuilder.Redirect.INHERIT); // Stderr to console
        Process process = pb.start();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            // 1. Send 'initialize'
            String initReq = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test-client\",\"version\":\"1.0.0\"}}}\n";
            writer.write(initReq);
            writer.flush();

            String initRespLine = reader.readLine();
            assertThat(initRespLine).isNotNull();
            JsonNode initJson = objectMapper.readTree(initRespLine);
            assertThat(initJson.path("result").path("serverInfo").path("name").asText()).isEqualTo("yahoo-finance-mcp");

            // Send standard MCP initialized notification
            String initializedNotif = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}\n";
            writer.write(initializedNotif);
            writer.flush();

            // 2. Send 'tools/list'
            String listReq = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}\n";
            writer.write(listReq);
            writer.flush();

            String listRespLine = reader.readLine();
            assertThat(listRespLine).isNotNull();
            JsonNode listJson = objectMapper.readTree(listRespLine);
            JsonNode tools = listJson.path("result").path("tools");
            assertThat(tools.isArray()).isTrue();
            assertThat(tools.size()).isEqualTo(4);

            // 3. Send 'tools/call' for get_stock_info
            String callReq1 = "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"get_stock_info\",\"arguments\":{\"ticker\":\"AAPL\"}}}\n";
            writer.write(callReq1);
            writer.flush();

            String callRespLine1 = reader.readLine();
            assertThat(callRespLine1).isNotNull();
            JsonNode callJson1 = objectMapper.readTree(callRespLine1);
            assertThat(callJson1.path("result").path("isError").asBoolean()).isFalse();
            String contentText1 = callJson1.path("result").path("content").get(0).path("text").asText();
            assertThat(contentText1).contains("\"symbol\" : \"AAPL\"");

            // 4. Send 'tools/call' for get_stock_actions
            String callReq2 = "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"get_stock_actions\",\"arguments\":{\"ticker\":\"AAPL\"}}}\n";
            writer.write(callReq2);
            writer.flush();

            String callRespLine2 = reader.readLine();
            assertThat(callRespLine2).isNotNull();
            JsonNode callJson2 = objectMapper.readTree(callRespLine2);
            assertThat(callJson2.path("result").path("isError").asBoolean()).isFalse();
            String contentText2 = callJson2.path("result").path("content").get(0).path("text").asText();
            assertThat(contentText2).contains("Dividends");

            // 5. Send 'tools/call' for get_financial_statement
            String callReq3 = "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\",\"params\":{\"name\":\"get_financial_statement\",\"arguments\":{\"ticker\":\"AAPL\",\"financial_type\":\"income_stmt\"}}}\n";
            writer.write(callReq3);
            writer.flush();

            String callRespLine3 = reader.readLine();
            assertThat(callRespLine3).isNotNull();
            JsonNode callJson3 = objectMapper.readTree(callRespLine3);
            assertThat(callJson3.path("result").path("isError").asBoolean()).isFalse();
            String contentText3 = callJson3.path("result").path("content").get(0).path("text").asText();
            assertThat(contentText3).contains("totalRevenue");

            // 6. Send 'tools/call' for get_recommendations
            String callReq4 = "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\",\"params\":{\"name\":\"get_recommendations\",\"arguments\":{\"ticker\":\"AAPL\",\"recommendation_type\":\"recommendations\"}}}\n";
            writer.write(callReq4);
            writer.flush();

            String callRespLine4 = reader.readLine();
            assertThat(callRespLine4).isNotNull();
            JsonNode callJson4 = objectMapper.readTree(callRespLine4);
            assertThat(callJson4.path("result").path("isError").asBoolean()).isFalse();
            String contentText4 = callJson4.path("result").path("content").get(0).path("text").asText();
            assertThat(contentText4).contains("strongBuy");
        } finally {
            process.destroyForcibly();
            process.waitFor(3, TimeUnit.SECONDS);
        }
    }
}
