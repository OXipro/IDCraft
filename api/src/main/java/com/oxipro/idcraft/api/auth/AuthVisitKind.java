package com.oxipro.idcraft.api.auth;

import java.util.Locale;

public enum AuthVisitKind {
    REGISTER((byte) 0),
    LOGIN((byte) 1),
    ACCOUNT_DESK((byte) 2);

    private final byte code;

    AuthVisitKind(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static AuthVisitKind fromCode(byte code) {
        for (AuthVisitKind kind : values()) {
            if (kind.code == code) {
                return kind;
            }
        }
        return LOGIN;
    }

    public static AuthVisitKind fromName(String name) {
        if (name == null || name.isBlank()) {
            return LOGIN;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return LOGIN;
        }
    }
}
