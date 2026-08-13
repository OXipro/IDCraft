package com.oxipro.idcraft.api.messaging;

public final class MessagingOpcodes {

    public static final byte AUTHENTICATED = 1;
    public static final byte CONNECT_AUTH = 2;

    public static byte[] authenticated() {
        return new byte[]{AUTHENTICATED};
    }

    public static byte[] connectAuth() {
        return new byte[]{CONNECT_AUTH};
    }

    public static boolean isAuthenticated(byte[] data) {
        return data != null && data.length >= 1 && data[0] == AUTHENTICATED;
    }

    public static boolean isConnectAuth(byte[] data) {
        return data != null && data.length >= 1 && data[0] == CONNECT_AUTH;
    }
}
