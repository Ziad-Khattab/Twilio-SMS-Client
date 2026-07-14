package com.twilio.twilio_project;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import java.sql.Connection;
import java.sql.SQLException;

public class DBUtil {
    private static final HikariDataSource dataSource;

    static {
        String dbUrl = EnvLoader.get("DB_URL");
        String dbUser = EnvLoader.get("DB_USER");
        String dbPassword = EnvLoader.get("DB_PASSWORD");

        if (isBlank(dbUrl) || isBlank(dbUser) || isBlank(dbPassword)) {
            throw new IllegalStateException(
                    "Database not configured. Set DB_URL, DB_USER, and DB_PASSWORD in .env (see .env.example).");
        }

        dbUrl = stripEmbeddedCredentials(dbUrl);
        dbUrl = ensureSslMode(dbUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser.trim());
        config.setPassword(dbPassword);

        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30_000);
        config.setMaxLifetime(300_000);
        config.setIdleTimeout(120_000);
        config.setKeepaliveTime(30_000);

        dataSource = new HikariDataSource(config);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Removes user:password@ from JDBC URLs so Hikari uses DB_USER / DB_PASSWORD only.
     */
    static String stripEmbeddedCredentials(String jdbcUrl) {
        int schemeEnd = jdbcUrl.indexOf("://");
        if (schemeEnd < 0) {
            return jdbcUrl;
        }
        int credStart = schemeEnd + 3;
        int at = jdbcUrl.indexOf('@', credStart);
        if (at < 0) {
            return jdbcUrl;
        }
        String credentials = jdbcUrl.substring(credStart, at);
        if (credentials.contains(":")) {
            return jdbcUrl.substring(0, credStart) + jdbcUrl.substring(at + 1);
        }
        return jdbcUrl;
    }

    static String ensureSslMode(String jdbcUrl) {
        if (jdbcUrl.contains("sslmode=")) {
            return jdbcUrl;
        }
        return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
    }
}
