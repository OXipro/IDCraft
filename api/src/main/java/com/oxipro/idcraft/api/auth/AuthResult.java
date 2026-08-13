package com.oxipro.idcraft.api.auth;

public class AuthResult {

    private final boolean success;
    private final AuthErrorCode errorCode;

    private AuthResult(boolean success, AuthErrorCode errorCode) {
        this.success = success;
        this.errorCode = errorCode;
    }

    public static AuthResult ok() {
        return new AuthResult(true, null);
    }

    public static AuthResult failure(AuthErrorCode errorCode) {
        return new AuthResult(false, errorCode);
    }

    public boolean isSuccess() {
        return success;
    }

    // null when isSuccess() is true
    public AuthErrorCode getErrorCode() {
        return errorCode;
    }
}
