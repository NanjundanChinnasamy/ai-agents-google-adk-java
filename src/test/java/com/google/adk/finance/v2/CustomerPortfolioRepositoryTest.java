package com.google.adk.finance.v2;

import com.google.adk.finance.v2.PortfolioModels.CustomerPortfolio;
import com.google.adk.finance.v2.PortfolioModels.CustomerRecord;
import com.google.adk.finance.v2.PortfolioModels.PortfolioHoldingRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomerPortfolioRepositoryTest {

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        File dbFile = tempDir.resolve("test_portfolio.db").toFile();
        CustomerPortfolioRepository.setDbPath(dbFile.getAbsolutePath());
    }

    @Test
    void testCustomerTableAndCompositePrimaryKey() {
        Optional<CustomerRecord> c1 = CustomerPortfolioRepository.findCustomer("1001");
        assertThat(c1).isPresent();
        assertThat(c1.get().customerId()).isEqualTo("1001");
        assertThat(c1.get().portfolioId()).isEqualTo("100001");

        Optional<CustomerRecord> c2 = CustomerPortfolioRepository.findCustomer("1002");
        assertThat(c2).isPresent();
        assertThat(c2.get().customerId()).isEqualTo("1002");
        assertThat(c2.get().portfolioId()).isEqualTo("100002");
    }

    @Test
    void testSeedDataCustomer1001() {
        Optional<CustomerPortfolio> portfolioOpt = CustomerPortfolioRepository.findPortfolioByCustomerId("1001");
        assertThat(portfolioOpt).isPresent();

        CustomerPortfolio portfolio = portfolioOpt.get();
        assertThat(portfolio.customerId()).isEqualTo("1001");
        assertThat(portfolio.portfolioId()).isEqualTo("100001");
        assertThat(portfolio.holdings()).hasSize(2);

        // Reliance: 2 @ 1000 INR on 01-01-2025
        PortfolioHoldingRecord reliance = portfolio.holdings().stream()
                .filter(h -> h.symbol().equals("RELIANCE"))
                .findFirst()
                .orElse(null);
        assertThat(reliance).isNotNull();
        assertThat(reliance.name()).isEqualTo("Reliance");
        assertThat(reliance.quantity()).isEqualTo(2);
        assertThat(reliance.buyPrice()).isEqualTo(1000.0);
        assertThat(reliance.currency()).isEqualTo("INR");
        assertThat(reliance.boughtDate()).isEqualTo("01-01-2025");
        assertThat(reliance.totalInvested()).isEqualTo(2000.0);

        // TCS: 2 @ 1000 INR on 01-01-2025
        PortfolioHoldingRecord tcs = portfolio.holdings().stream()
                .filter(h -> h.symbol().equals("TCS"))
                .findFirst()
                .orElse(null);
        assertThat(tcs).isNotNull();
        assertThat(tcs.name()).isEqualTo("TCS");
        assertThat(tcs.quantity()).isEqualTo(2);
        assertThat(tcs.buyPrice()).isEqualTo(1000.0);
        assertThat(tcs.currency()).isEqualTo("INR");
        assertThat(tcs.boughtDate()).isEqualTo("01-01-2025");
        assertThat(tcs.totalInvested()).isEqualTo(2000.0);

        // Total Invested = 4000.0 INR
        assertThat(portfolio.totalInvested()).isEqualTo(4000.0);
    }

    @Test
    void testSeedDataCustomer1002() {
        Optional<CustomerPortfolio> portfolioOpt = CustomerPortfolioRepository.findPortfolioByCustomerId("1002");
        assertThat(portfolioOpt).isPresent();

        CustomerPortfolio portfolio = portfolioOpt.get();
        assertThat(portfolio.customerId()).isEqualTo("1002");
        assertThat(portfolio.portfolioId()).isEqualTo("100002");
        assertThat(portfolio.holdings()).hasSize(1);

        // Infosys: 10 @ 1500 INR on 10-08-2026
        PortfolioHoldingRecord infy = portfolio.holdings().getFirst();
        assertThat(infy.symbol()).isEqualTo("INFY");
        assertThat(infy.name()).isEqualTo("Infosys");
        assertThat(infy.quantity()).isEqualTo(10);
        assertThat(infy.buyPrice()).isEqualTo(1500.0);
        assertThat(infy.currency()).isEqualTo("INR");
        assertThat(infy.boughtDate()).isEqualTo("10-08-2026");
        assertThat(infy.totalInvested()).isEqualTo(15000.0);

        // Total Invested = 15000.0 INR
        assertThat(portfolio.totalInvested()).isEqualTo(15000.0);
    }

    @Test
    void testFindNonExistentCustomer() {
        Optional<CustomerPortfolio> portfolioOpt = CustomerPortfolioRepository.findPortfolioByCustomerId("9999");
        assertThat(portfolioOpt).isEmpty();
    }
}
