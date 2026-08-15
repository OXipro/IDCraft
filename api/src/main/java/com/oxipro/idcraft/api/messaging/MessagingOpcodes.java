package com.oxipro.idcraft.api.messaging;

import com.oxipro.idcraft.api.auth.AuthVisitKind;

public final class MessagingOpcodes {

    public static final byte AUTHENTICATED = 1;

    public static byte[] authenticated() {
        return new byte[]{AUTHENTICATED};
    }

    public static byte[] connect(AuthVisitKind kind) {
        AuthVisitKind resolved = kind == null ? AuthVisitKind.LOGIN : kind;
        return new byte[]{resolved.code()};
    }

    public static boolean isAuthenticated(byte[] data) {
        return data != null && data.length >= 1 && data[0] == AUTHENTICATED;
    }

    public static AuthVisitKind visitKind(byte[] data) {
        if (data == null || data.length < 1) {
            return AuthVisitKind.LOGIN;
        }
        return AuthVisitKind.fromCode(data[0]);
    }
}
