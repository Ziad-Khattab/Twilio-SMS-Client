package com.twilio.twilio_project; // DB connection pool (HikariCP) + Flyway migrations

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.SQLException;

// Initializes a HikariCP connection pool using EnvLoader (profile-aware config).
// Runs Flyway migrations on startup to keep the schema in sync.
// Exposes getConnection() for all JDBC operations in the project.
// Uses DOCKER_/LOCAL_ profile prefix so the same .env works for both environments.
public class DBUtil {

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(EnvLoader.get("DB_URL"));
        config.setUsername(EnvLoader.get("DB_USER"));
        config.setPassword(EnvLoader.get("DB_PASSWORD"));

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(600000);
        config.setConnectionTimeout(10000);
        config.setLeakDetectionThreshold(60000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);

        // Run Flyway migrations automatically on application startup
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
