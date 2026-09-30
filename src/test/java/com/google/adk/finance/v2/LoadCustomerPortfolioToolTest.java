package com.google.adk.finance.v2;

import com.google.genai.types.FunctionDeclaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadCustomerPortfolioToolTest {

    private LoadCustomerPortfolioTool tool;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        File dbFile = tempDir.resolve("tool_test_portfolio.db").toFile();
        CustomerPortfolioRepository.setDbPath(dbFile.getAbsolutePath());
        tool = new LoadCustomerPortfolioTool();
    }

    @Test
    void testToolDeclaration() {
        Optional<FunctionDeclaration> declOpt = tool.declaration();
        assertThat(declOpt).isPresent();

        FunctionDeclaration decl = declOpt.get();
        assertThat(decl.name()).hasValue("load_customer_portfolio");
        assertThat(decl.description()).isPresent();
        assertThat(decl.parameters()).isPresent();
        assertThat(decl.parameters().get().properties()).isPresent();
        assertThat(decl.parameters().get().properties().get()).containsKey("customer_id");
        assertThat(decl.parameters().get().required()).hasValue(List.of("customer_id"));
    }

    @Test
    void testLoadCustomer1001() {
        Map<String, Object> args = Map.of("customer_id", "1001");
        Map<String, Object> result = tool.runAsync(args, null).blockingGet();

        assertThat(result).containsEntry("status", "success");
        assertThat(result).containsEntry("customer_id", "1001");
        assertThat(result).containsEntry("portfolio_id", "100001");
        assertThat(result).containsEntry("holdings_count", 2);
        assertThat(result).containsEntry("total_invested", 4000.0);
        assertThat(result).containsEntry("currency", "INR");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> holdings = (List<Map<String, Object>>) result.get("holdings");
        assertThat(holdings).hasSize(2);
        assertThat(holdings).anyMatch(h -> "RELIANCE".equals(h.get("symbol")) && Integer.valueOf(2).equals(h.get("quantity")));
        assertThat(holdings).anyMatch(h -> "TCS".equals(h.get("symbol")) && Integer.valueOf(2).equals(h.get("quantity")));
    }

    @Test
    void testLoadCustomer1002() {
        Map<String, Object> args = Map.of("customer_id", "1002");
        Map<String, Object> result = tool.runAsync(args, null).blockingGet();

        assertThat(result).containsEntry("status", "success");
        assertThat(result).containsEntry("customer_id", "1002");
        assertThat(result).containsEntry("portfolio_id", "100002");
        assertThat(result).containsEntry("holdings_count", 1);
        assertThat(result).containsEntry("total_invested", 15000.0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> holdings = (List<Map<String, Object>>) result.get("holdings");
        assertThat(holdings).hasSize(1);
        assertThat(holdings.getFirst()).containsEntry("symbol", "INFY");
        assertThat(holdings.getFirst()).containsEntry("name", "Infosys");
        assertThat(holdings.getFirst()).containsEntry("quantity", 10);
        assertThat(holdings.getFirst()).containsEntry("buy_price", 1500.0);
        assertThat(holdings.getFirst()).containsEntry("bought_date", "10-08-2026");
    }

    @Test
    void testLoadMissingOrUnknownCustomer() {
        Map<String, Object> args = Map.of("customer_id", "9999");
        Map<String, Object> result = tool.runAsync(args, null).blockingGet();

        assertThat(result).containsEntry("status", "error");
        assertThat(result.get("message").toString()).contains("9999");
    }

    @Test
    void testNumericCustomerIdConversion() {
        // Models might pass 1001 as integer or 1001.0
        Map<String, Object> args = Map.of("customer_id", 1001);
        Map<String, Object> result = tool.runAsync(args, null).blockingGet();

        assertThat(result).containsEntry("status", "success");
        assertThat(result).containsEntry("customer_id", "1001");
    }
}
