package com.oxipro.idcraft.minestom.network;

import java.util.Locale;

public enum NetworkMode {
    CONFIG,
    SHULKER;

    public static NetworkMode fromConfig(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalStateException("network.mode is missing in config.yml");
        }
        try {
            return NetworkMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Unknown network.mode: " + raw + " (expected CONFIG or SHULKER)", e);
        }
    }
}
