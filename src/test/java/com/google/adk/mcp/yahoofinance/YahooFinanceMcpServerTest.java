package com.google.adk.mcp.yahoofinance;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YahooFinanceMcpServerTest {

    @Test
    @DisplayName("Should initialize server and register exactly the 4 required tools")
    void testServerToolRegistration() {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        YahooFinanceMcpServer server = new YahooFinanceMcpServer(new YahooFinanceService(), in, out);
        try {
            List<McpSchema.Tool> tools = server.getSyncServer().listTools();
            assertThat(tools).isNotNull();
            assertThat(tools).hasSize(4);

            List<String> toolNames = tools.stream().map(McpSchema.Tool::name).toList();
            assertThat(toolNames).containsExactlyInAnyOrder(
                    "get_stock_info",
                    "get_stock_actions",
                    "get_financial_statement",
                    "get_recommendations"
            );

            // Verify schemas
            for (McpSchema.Tool tool : tools) {
                assertThat(tool.inputSchema()).isNotNull();
                assertThat(tool.inputSchema().type()).isEqualTo("object");
                assertThat(tool.inputSchema().required()).contains("ticker");
            }
        } finally {
            server.close();
        }
    }
}
