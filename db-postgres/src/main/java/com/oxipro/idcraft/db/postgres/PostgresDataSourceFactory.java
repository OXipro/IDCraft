package com.oxipro.idcraft.db.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class PostgresDataSourceFactory {

    private PostgresDataSourceFactory() {
    }

    public static HikariDataSource create(PostgresConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.jdbcUrl());
        hikariConfig.setUsername(config.getUser());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setMaximumPoolSize(config.getPoolSize());
        hikariConfig.setPoolName("idcraft-postgres");
        // Isolated plugin classloaders (Velocity) never register JDBC SPI drivers.
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        return new HikariDataSource(hikariConfig);
    }

    // idempotent (CREATE ... IF NOT EXISTS) - safe on every startup
    public static void applySchema(HikariDataSource dataSource) {
        String schema = readSchema();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(schema);
        } catch (SQLException e) {
            throw new IllegalStateException("Postgres schema bootstrap failed", e);
        }
    }

    private static String readSchema() {
        try (InputStream in = PostgresDataSourceFactory.class.getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql missing from db-postgres resources");
            }
            return new String(readAllBytes(in), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // InputStream#readAllBytes exists since Java 9, kept explicit for clarity
    private static byte[] readAllBytes(InputStream in) throws IOException {
        return in.readAllBytes();
    }
}
