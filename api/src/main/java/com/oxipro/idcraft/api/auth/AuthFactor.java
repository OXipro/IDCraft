package com.oxipro.idcraft.api.auth;

import java.util.Locale;

// Configurable auth factor (distinct from PlayerAuthType / AuthMode)
public enum AuthFactor {
    PASSWORD(false),
    EMAIL(false),
    TWO_FACTOR(true);

    private final boolean requiresSetupMenu;

    AuthFactor(boolean requiresSetupMenu) {
        this.requiresSetupMenu = requiresSetupMenu;
    }

    public boolean requiresSetupMenu() {
        return requiresSetupMenu;
    }

    // Config keys: PASSWORD, EMAIL, 2FA
    public static AuthFactor fromConfigKey(String key) {
        if (key == null) {
            return null;
        }
        String k = key.trim().toUpperCase(Locale.ROOT);
        if ("PASSWORD".equals(k)) {
            return PASSWORD;
        }
        if ("EMAIL".equals(k)) {
            return EMAIL;
        }
        if ("2FA".equals(k) || "TWO_FACTOR".equals(k) || "TOTP".equals(k)) {
            return TWO_FACTOR;
        }
        return null;
    }
}
