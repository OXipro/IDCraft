package com.oxipro.idcraft.plugin.velocity.authservers;

public enum AuthServersProviders {
    CONFIG,
    SHULKER;

    public static AuthServersProviders fromString(String value) {
        if (value == null) return CONFIG;
        try {
            return AuthServersProviders.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CONFIG;
        }
    }
}

