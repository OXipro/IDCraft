package com.oxipro.idcraft.db.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class MySqlDataSourceFactory {

    private MySqlDataSourceFactory() {
    }

    public static HikariDataSource create(MySqlConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.jdbcUrl());
        hikariConfig.setUsername(config.getUser());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setMaximumPoolSize(config.getPoolSize());
        hikariConfig.setPoolName("idcraft-mysql");
        // Isolated plugin classloaders (Velocity) never register JDBC SPI drivers.
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return new HikariDataSource(hikariConfig);
    }

    // idempotent (IF NOT EXISTS) - safe on every startup
    public static void applySchema(HikariDataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            String[] statements = readSchema().split(";");
            for (String sql : statements) {
                if (!sql.trim().isEmpty()) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("MySQL schema bootstrap failed", e);
        }
    }

    private static String readSchema() {
        try (InputStream in = MySqlDataSourceFactory.class.getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql missing from db-mysql resources");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
