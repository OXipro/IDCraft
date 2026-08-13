package com.oxipro.idcraft.api.session;

import com.oxipro.idcraft.api.auth.PlayerAuthType;

import java.time.Instant;
import java.util.UUID;

public class Session {

    private final String username;
    private final UUID uuid;
    private final PlayerAuthType type;
    private final String ip;
    private final Instant authenticatedAt;

    public Session(String username, UUID uuid, PlayerAuthType type, String ip, Instant authenticatedAt) {
        this.username = username;
        this.uuid = uuid;
        this.type = type;
        this.ip = ip;
        this.authenticatedAt = authenticatedAt;
    }

    public static Session authenticated(String username, UUID uuid, PlayerAuthType type, String ip) {
        return new Session(username, uuid, type, ip, Instant.now());
    }

    public UUID getUuid() {
        return uuid;
    }

    public PlayerAuthType getType() {
        return type;
    }

    public String getIp() {
        return ip;
    }

    public Instant getAuthenticatedAt() {
        return authenticatedAt;
    }

    public String getUsername() { return  username; }
}
