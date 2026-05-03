package com.twilio.twilio_project;

import java.sql.Connection;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;

public class DBUtil {
    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();

        final String DB_URL = EnvLoader.get("DB_URL");
        final String DB_USER = EnvLoader.get("DB_USER");
        final String DB_PASSWORD = EnvLoader.get("DB_PASSWORD");

        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);

        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);

        config.setMaxLifetime(600000);
        config.setIdleTimeout(300000);

        config.setKeepaliveTime(30000);

        dataSource = new HikariDataSource(config);

    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void closeConnection(Connection conn) {

    }
}