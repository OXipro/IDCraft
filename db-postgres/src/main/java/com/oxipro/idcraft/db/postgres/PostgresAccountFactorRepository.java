package com.oxipro.idcraft.db.postgres;

import com.oxipro.idcraft.api.account.IAccountFactorRepository;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class PostgresAccountFactorRepository implements IAccountFactorRepository {

    private static final String UPSERT_SQL =
            "INSERT INTO account_factors (player_uuid, factor, value) VALUES (?, ?, ?) "
                    + "ON CONFLICT (player_uuid, factor) DO UPDATE SET value = EXCLUDED.value";

    private final HikariDataSource dataSource;

    public PostgresAccountFactorRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Map<AuthFactor, String> findAll(UUID uuid) {
        Map<AuthFactor, String> result = new EnumMap<>(AuthFactor.class);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT factor, value FROM account_factors WHERE player_uuid = ?")) {
            ps.setObject(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuthFactor factor = AuthFactor.fromConfigKey(rs.getString("factor"));
                    if (factor != null) {
                        result.put(factor, rs.getString("value"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("account_factors.findAll failed", e);
        }
        return result;
    }

    @Override
    public String find(UUID uuid, AuthFactor factor) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT value FROM account_factors WHERE player_uuid = ? AND factor = ?")) {
            ps.setObject(1, uuid);
            ps.setString(2, factor.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("account_factors.find failed", e);
        }
    }

    @Override
    public UUID findUuidByEmail(String email) {
        if (email == null) {
            return null;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT player_uuid FROM account_factors "
                             + "WHERE factor = ? AND lower(value) = lower(?)")) {
            ps.setString(1, AuthFactor.EMAIL.name());
            ps.setString(2, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return (UUID) rs.getObject("player_uuid");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("account_factors.findUuidByEmail failed", e);
        }
    }

    @Override
    public void save(UUID uuid, AuthFactor factor, String value) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(UPSERT_SQL)) {
            ps.setObject(1, uuid);
            ps.setString(2, factor.name());
            ps.setString(3, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("account_factors.save failed", e);
        }
    }

    @Override
    public void delete(UUID uuid, AuthFactor factor) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "DELETE FROM account_factors WHERE player_uuid = ? AND factor = ?")) {
            ps.setObject(1, uuid);
            ps.setString(2, factor.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("account_factors.delete failed", e);
        }
    }

    @Override
    public void deleteAll(UUID uuid) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "DELETE FROM account_factors WHERE player_uuid = ?")) {
            ps.setObject(1, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("account_factors.deleteAll failed", e);
        }
    }
}
