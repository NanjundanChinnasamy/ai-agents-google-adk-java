package com.google.adk.finance.v8;

import com.google.adk.finance.v8.guardrails.GuardrailResult;
import com.google.adk.finance.v8.guardrails.tool.ToolOperationGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolOperationGuardTest {

    private ToolOperationGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ToolOperationGuard();
    }

    @Test
    @DisplayName("Authorized analytical and research tools should be ALLOWED")
    void testPermittedResearchTools() {
        assertThat(guard.evaluate("get_stock_info").isAllowed()).isTrue();
        assertThat(guard.evaluate("get_stock_actions").isAllowed()).isTrue();
        assertThat(guard.evaluate("get_financial_statement").isAllowed()).isTrue();
        assertThat(guard.evaluate("get_recommendations").isAllowed()).isTrue();
        assertThat(guard.evaluate("load_customer_portfolio").isAllowed()).isTrue();
        assertThat(guard.evaluate("portfolio_math").isAllowed()).isTrue();
        assertThat(guard.evaluate("read_project_knowledge").isAllowed()).isTrue();
        assertThat(guard.evaluate("stockmarket_researcher").isAllowed()).isTrue();
        assertThat(guard.evaluate("run_sequential_research_workflow").isAllowed()).isTrue();
    }

    @Test
    @DisplayName("Transactional trading operations must be BLOCKED")
    void testTradingOperationsBlocked() {
        GuardrailResult r1 = guard.evaluate("execute_trade");
        assertThat(r1.isBlocked()).isTrue();
        assertThat(r1.reason()).contains("strictly prohibited");

        assertThat(guard.evaluate("place_order").isBlocked()).isTrue();
        assertThat(guard.evaluate("buy_stock").isBlocked()).isTrue();
        assertThat(guard.evaluate("sell_stock").isBlocked()).isTrue();
    }

    @Test
    @DisplayName("Banking transfers and account modifications must be BLOCKED")
    void testMoneyTransfersAndModificationsBlocked() {
        assertThat(guard.evaluate("transfer_funds").isBlocked()).isTrue();
        assertThat(guard.evaluate("withdraw_cash").isBlocked()).isTrue();
        assertThat(guard.evaluate("modify_account").isBlocked()).isTrue();
        assertThat(guard.evaluate("update_balance").isBlocked()).isTrue();
    }

    @Test
    @DisplayName("Arbitrary system command execution and credential dumping must be BLOCKED")
    void testSystemCommandsBlocked() {
        assertThat(guard.evaluate("execute_command").isBlocked()).isTrue();
        assertThat(guard.evaluate("shell_exec").isBlocked()).isTrue();
        assertThat(guard.evaluate("dump_env").isBlocked()).isTrue();
        assertThat(guard.evaluate("get_credentials").isBlocked()).isTrue();
    }

    @Test
    @DisplayName("Unregistered tool outside research whitelist must be BLOCKED")
    void testUnregisteredToolBlocked() {
        GuardrailResult r = guard.evaluate("random_unregistered_tool");
        assertThat(r.isBlocked()).isTrue();
        assertThat(r.reason()).contains("Access Denied");
    }
}
