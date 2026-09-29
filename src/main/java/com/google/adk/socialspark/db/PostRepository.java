package com.google.adk.socialspark.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class PostRepository {
    private static final Logger logger = LoggerFactory.getLogger(PostRepository.class);
    private static String dbUrl = "jdbc:sqlite:social_spark.db";

    public static synchronized void setDbPath(String path) {
        dbUrl = "jdbc:sqlite:" + path;
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

    public static synchronized void initDb() {
        String sql = """
            CREATE TABLE IF NOT EXISTS posts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                platform TEXT NOT NULL,
                text TEXT NOT NULL,
                image_path TEXT,
                image_url TEXT,
                post_url TEXT,
                created_at TEXT NOT NULL
            );
        """;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Initialized SQLite database schema successfully.");
        } catch (SQLException e) {
            logger.error("Failed to initialize database: {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failure", e);
        }
    }

    public static synchronized PostRecord savePost(
            String platform,
            String text,
            String postUrl,
            String imagePath,
            String imageUrl
    ) {
        String createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        String sql = """
            INSERT INTO posts (platform, text, image_path, image_url, post_url, created_at)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, platform);
            pstmt.setString(2, text);
            pstmt.setString(3, imagePath);
            pstmt.setString(4, imageUrl);
            pstmt.setString(5, postUrl);
            pstmt.setString(6, createdAt);

            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    return new PostRecord(id, platform, text, imagePath, imageUrl, postUrl, createdAt);
                } else {
                    throw new SQLException("Saving post failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to save post: {}", e.getMessage(), e);
            throw new RuntimeException("Save post failure", e);
        }
    }

    public static synchronized List<PostRecord> listPosts(int limit) {
        String sql = "SELECT id, platform, text, image_path, image_url, post_url, created_at FROM posts ORDER BY id DESC LIMIT ?;";
        List<PostRecord> posts = new ArrayList<>();

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    posts.add(new PostRecord(
                            rs.getLong("id"),
                            rs.getString("platform"),
                            rs.getString("text"),
                            rs.getString("image_path"),
                            rs.getString("image_url"),
                            rs.getString("post_url"),
                            rs.getString("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to list posts: {}", e.getMessage(), e);
        }
        return posts;
    }

    static {
        try {
            initDb();
        } catch (Exception e) {
            logger.warn("Initial DB init deferred or failed: {}", e.getMessage());
        }
    }

    private PostRepository() {}
}
