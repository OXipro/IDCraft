package com.oxipro.idcraft.api.account;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class Account {

    private final UUID uuid;
    private final String username;
    private final String usernameLower;
    private final String passwordHash;
    private final boolean premium;
    private final Instant registeredAt;
    private final Instant lastLoginAt;
    private final String lastIp;
    private final int failedAttempts;
    private final Instant lockedUntil;

    public Account(UUID uuid, String username, String usernameLower, String passwordHash, boolean premium,
                   Instant registeredAt, Instant lastLoginAt, String lastIp, int failedAttempts, Instant lockedUntil) {
        this.uuid = uuid;
        this.username = username;
        this.usernameLower = usernameLower;
        this.passwordHash = passwordHash;
        this.premium = premium;
        this.registeredAt = registeredAt;
        this.lastLoginAt = lastLoginAt;
        this.lastIp = lastIp;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
    }

    public static Account newAccount(UUID uuid, String username, String passwordHash, String ip) {
        return newAccount(uuid, username, passwordHash, ip, false);
    }

    public static Account newAccount(UUID uuid, String username, String passwordHash, String ip, boolean premium) {
        return new Account(uuid, username, username.toLowerCase(), passwordHash, premium,
                Instant.now(), null, ip, 0, null);
    }

    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    public Account withFailedAttempt(int maxAttempts, Duration lockDuration) {
        int attempts = failedAttempts + 1;
        Instant lockUntil = lockedUntil;
        if (attempts >= maxAttempts) {
            lockUntil = Instant.now().plus(lockDuration);
        }
        return new Account(uuid, username, usernameLower, passwordHash, premium, registeredAt,
                lastLoginAt, lastIp, attempts, lockUntil);
    }

    public Account afterSuccessfulLogin(String ip) {
        return new Account(uuid, username, usernameLower, passwordHash, premium, registeredAt,
                Instant.now(), ip, 0, null);
    }

    public Account clearFailedAttempts() {
        return new Account(uuid, username, usernameLower, passwordHash, premium, registeredAt,
                lastLoginAt, lastIp, 0, null);
    }

    public Account withPremium(boolean premium) {
        return new Account(uuid, username, usernameLower, passwordHash, premium, registeredAt,
                lastLoginAt, lastIp, failedAttempts, lockedUntil);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getUsernameLower() {
        return usernameLower;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isPremium() {
        return premium;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public String getLastIp() {
        return lastIp;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
