package com.oxipro.idcraft.api.auth;

import java.util.UUID;

public class PlayerIdentity {

    private final UUID uuid;
    private final String username;
    private final PlayerAuthType type;

    public PlayerIdentity(UUID uuid, String username, PlayerAuthType type) {
        this.uuid = uuid;
        this.username = username;
        this.type = type;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public PlayerAuthType getType() {
        return type;
    }
}
