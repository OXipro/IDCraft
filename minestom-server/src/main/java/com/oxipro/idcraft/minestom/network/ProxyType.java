package com.oxipro.idcraft.minestom.network;

import java.util.Locale;

public enum ProxyType {
    VELOCITY,
    BUNGEE,
    NONE;

    public static ProxyType fromConfig(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalStateException("network.config.proxy.type is missing in config.yml");
        }
        try {
            return ProxyType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Unknown network.config.proxy.type: " + raw + " (expected VELOCITY, BUNGEE or NONE)", e);
        }
    }
}
