package com.oxipro.idcraft.api.mojang;

import java.util.UUID;

public class MojangProfile {

    private final UUID uuid;
    private final String username;

    public MojangProfile(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }
}
