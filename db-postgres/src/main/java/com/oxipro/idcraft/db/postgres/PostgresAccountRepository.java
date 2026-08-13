package com.oxipro.idcraft.db.postgres;

import com.oxipro.idcraft.api.account.Account;
import com.oxipro.idcraft.api.account.IAccountRepository;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class PostgresAccountRepository implements IAccountRepository {

    private static final String INSERT_OR_UPDATE_SQL =
            "INSERT INTO accounts "
                    + "(uuid, username, username_lower, password_hash, is_premium, "
                    + "registered_at, last_login_at, last_ip, failed_attempts, locked_until) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT (uuid) DO UPDATE SET "
                    + "username = EXCLUDED.username, "
                    + "username_lower = EXCLUDED.username_lower, "
                    + "password_hash = EXCLUDED.password_hash, "
                    + "is_premium = EXCLUDED.is_premium, "
                    + "last_login_at = EXCLUDED.last_login_at, "
                    + "last_ip = EXCLUDED.last_ip, "
                    + "failed_attempts = EXCLUDED.failed_attempts, "
                    + "locked_until = EXCLUDED.locked_until";

    private final HikariDataSource dataSource;

    public PostgresAccountRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Account findByUuid(UUID uuid) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM accounts WHERE uuid = ?")) {
            ps.setObject(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("findByUuid failed", e);
        }
    }

    @Override
    public Account findByUsername(String username) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT * FROM accounts WHERE username_lower = ?")) {
            ps.setString(1, username.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("findByUsername failed", e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT 1 FROM accounts WHERE username_lower = ?")) {
            ps.setString(1, username.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("existsByUsername failed", e);
        }
    }

    @Override
    public void save(Account account) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(INSERT_OR_UPDATE_SQL)) {
            ps.setObject(1, account.getUuid());
            ps.setString(2, account.getUsername());
            ps.setString(3, account.getUsernameLower());
            ps.setString(4, account.getPasswordHash());
            ps.setBoolean(5, account.isPremium());
            ps.setTimestamp(6, Timestamp.from(account.getRegisteredAt()));
            ps.setTimestamp(7, account.getLastLoginAt() != null ? Timestamp.from(account.getLastLoginAt()) : null);
            ps.setString(8, account.getLastIp());
            ps.setInt(9, account.getFailedAttempts());
            ps.setTimestamp(10, account.getLockedUntil() != null ? Timestamp.from(account.getLockedUntil()) : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("save(Account) failed", e);
        }
    }

    @Override
    public void delete(UUID uuid) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM accounts WHERE uuid = ?")) {
            ps.setObject(1, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("delete(uuid) failed", e);
        }
    }

    private static Account mapRow(ResultSet rs) throws SQLException {
        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        Timestamp lockedUntil = rs.getTimestamp("locked_until");
        return new Account(
                (UUID) rs.getObject("uuid"),
                rs.getString("username"),
                rs.getString("username_lower"),
                rs.getString("password_hash"),
                rs.getBoolean("is_premium"),
                toInstant(rs.getTimestamp("registered_at")),
                lastLogin != null ? lastLogin.toInstant() : null,
                rs.getString("last_ip"),
                rs.getInt("failed_attempts"),
                lockedUntil != null ? lockedUntil.toInstant() : null
        );
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
