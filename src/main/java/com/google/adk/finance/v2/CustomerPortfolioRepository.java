package com.google.adk.finance.v2;

import com.google.adk.finance.v2.PortfolioModels.CustomerPortfolio;
import com.google.adk.finance.v2.PortfolioModels.CustomerRecord;
import com.google.adk.finance.v2.PortfolioModels.PortfolioHoldingRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite Repository for Portfolio & Holdings Management.
 * <p>
 * Manages two relational tables:
 * <ul>
 *   <li>{@code customer}: Composite primary key (customer_id, portfolio_id).</li>
 *   <li>{@code portfolio_holding}: Stock holdings per portfolio_id.</li>
 * </ul>
 * Automatically seeds initial test portfolios for customer 1001 and 1002.
 */
public final class CustomerPortfolioRepository {
    private static final Logger logger = LoggerFactory.getLogger(CustomerPortfolioRepository.class);

    private static final String DEFAULT_DB_FILE = "finance_portfolio.db";
    private static String dbUrl = "jdbc:sqlite:" + DEFAULT_DB_FILE;

    public static synchronized void setDbPath(String path) {
        dbUrl = "jdbc:sqlite:" + path;
        initDb();
    }

    public static synchronized void setDbUrl(String url) {
        dbUrl = url;
        initDb();
    }

    private static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA busy_timeout = 30000;");
        }
        return conn;
    }

    /**
     * Initializes database schema and seeds initial portfolio records if empty.
     */
    public static synchronized void initDb() {
        String createCustomerTable = """
                CREATE TABLE IF NOT EXISTS customer (
                    customer_id TEXT NOT NULL,
                    portfolio_id TEXT NOT NULL,
                    PRIMARY KEY (customer_id, portfolio_id)
                );
                """;

        String createHoldingTable = """
                CREATE TABLE IF NOT EXISTS portfolio_holding (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    portfolio_id TEXT NOT NULL,
                    symbol TEXT NOT NULL,
                    name TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    buy_price REAL NOT NULL,
                    currency TEXT NOT NULL DEFAULT 'INR',
                    bought_date TEXT NOT NULL
                );
                """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createCustomerTable);
            stmt.execute(createHoldingTable);
            logger.info("Initialized portfolio SQLite tables (customer, portfolio_holding).");

            // Seed if empty
            if (isCustomerTableEmpty(conn)) {
                seedDefaultData(conn);
            }
        } catch (SQLException e) {
            logger.error("Failed to initialize portfolio database: {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failure", e);
        }
    }

    private static boolean isCustomerTableEmpty(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM customer;";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    /**
     * Resets and re-seeds default test portfolio data.
     */
    public static synchronized void resetAndSeed() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM portfolio_holding;");
            stmt.execute("DELETE FROM customer;");
            seedDefaultData(conn);
            logger.info("Reset and re-seeded portfolio database.");
        } catch (SQLException e) {
            logger.error("Failed to reset and seed database: {}", e.getMessage(), e);
            throw new RuntimeException("Reset and seed failure", e);
        }
    }

    private static void seedDefaultData(Connection conn) throws SQLException {
        logger.info("Seeding default portfolio holdings for customer 1001 and 1002...");

        String insertCustomer = "INSERT INTO customer (customer_id, portfolio_id) VALUES (?, ?);";
        String insertHolding = """
                INSERT INTO portfolio_holding (portfolio_id, symbol, name, quantity, buy_price, currency, bought_date)
                VALUES (?, ?, ?, ?, ?, ?, ?);
                """;

        try (PreparedStatement custStmt = conn.prepareStatement(insertCustomer);
             PreparedStatement holdStmt = conn.prepareStatement(insertHolding)) {

            // Customer 1001 -> Portfolio 100001
            custStmt.setString(1, "1001");
            custStmt.setString(2, "100001");
            custStmt.executeUpdate();

            // Holding 1: Reliance (Qty: 2, Buy: 1000 INR, Date: 01-01-2025)
            holdStmt.setString(1, "100001");
            holdStmt.setString(2, "RELIANCE");
            holdStmt.setString(3, "Reliance");
            holdStmt.setInt(4, 2);
            holdStmt.setDouble(5, 1000.0);
            holdStmt.setString(6, "INR");
            holdStmt.setString(7, "01-01-2025");
            holdStmt.executeUpdate();

            // Holding 2: TCS (Qty: 2, Buy: 1000 INR, Date: 01-01-2025)
            holdStmt.setString(1, "100001");
            holdStmt.setString(2, "TCS");
            holdStmt.setString(3, "TCS");
            holdStmt.setInt(4, 2);
            holdStmt.setDouble(5, 1000.0);
            holdStmt.setString(6, "INR");
            holdStmt.setString(7, "01-01-2025");
            holdStmt.executeUpdate();

            // Customer 1002 -> Portfolio 100002
            custStmt.setString(1, "1002");
            custStmt.setString(2, "100002");
            custStmt.executeUpdate();

            // Holding 1: Infosys (Qty: 10, Buy: 1500 INR, Date: 10-08-2026)
            holdStmt.setString(1, "100002");
            holdStmt.setString(2, "INFY");
            holdStmt.setString(3, "Infosys");
            holdStmt.setInt(4, 10);
            holdStmt.setDouble(5, 1500.0);
            holdStmt.setString(6, "INR");
            holdStmt.setString(7, "10-08-2026");
            holdStmt.executeUpdate();

            logger.info("Successfully seeded customers 1001 and 1002 with portfolios 100001 and 100002.");
        }
    }

    /**
     * Looks up customer mapping by customer ID.
     */
    public static Optional<CustomerRecord> findCustomer(String customerId) {
        String sql = "SELECT customer_id, portfolio_id FROM customer WHERE customer_id = ? LIMIT 1;";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, customerId.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new CustomerRecord(
                            rs.getString("customer_id"),
                            rs.getString("portfolio_id")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding customer {}: {}", customerId, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Retrieves all holdings for a given portfolio ID.
     */
    public static List<PortfolioHoldingRecord> listHoldingsByPortfolioId(String portfolioId) {
        String sql = """
                SELECT id, portfolio_id, symbol, name, quantity, buy_price, currency, bought_date
                FROM portfolio_holding
                WHERE portfolio_id = ?
                ORDER BY symbol ASC;
                """;
        List<PortfolioHoldingRecord> holdings = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, portfolioId.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    holdings.add(new PortfolioHoldingRecord(
                            rs.getLong("id"),
                            rs.getString("portfolio_id"),
                            rs.getString("symbol"),
                            rs.getString("name"),
                            rs.getInt("quantity"),
                            rs.getDouble("buy_price"),
                            rs.getString("currency"),
                            rs.getString("bought_date")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error listing holdings for portfolio {}: {}", portfolioId, e.getMessage());
        }
        return holdings;
    }

    /**
     * Retrieves complete portfolio state for a customer.
     */
    public static Optional<CustomerPortfolio> findPortfolioByCustomerId(String customerId) {
        Optional<CustomerRecord> customerOpt = findCustomer(customerId);
        if (customerOpt.isEmpty()) {
            return Optional.empty();
        }

        CustomerRecord customer = customerOpt.get();
        List<PortfolioHoldingRecord> holdings = listHoldingsByPortfolioId(customer.portfolioId());
        return Optional.of(new CustomerPortfolio(customer.customerId(), customer.portfolioId(), holdings));
    }

    static {
        try {
            initDb();
        } catch (Exception e) {
            logger.warn("Initial DB init deferred or failed: {}", e.getMessage());
        }
    }

    private CustomerPortfolioRepository() {}
}
