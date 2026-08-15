package com.oxipro.idcraft.api.auth;

// Platform maps each code to language strings
public enum AuthErrorCode {
    PASSWORD_TOO_SHORT,
    PASSWORD_CONTAINS_SPACES,
    PASSWORD_REGEX_MISMATCH,
    PASSWORDS_DO_NOT_MATCH,
    FACTOR_INVALID,
    FACTOR_REQUIRED,
    RATE_LIMITED,
    ACCOUNT_ALREADY_EXISTS,
    ACCOUNT_NOT_FOUND,
    ACCOUNT_LOCKED,
    WRONG_PASSWORD,
    FACTOR_NOT_ENROLLED,
    FACTOR_ALREADY_ENROLLED,
    PREMIUM_USERNAME_RESERVED,
    // Proxy assigned UUID does not match account UUID
    UUID_MISMATCH,
    // Mojang/NameMC/etc unavailable — refuse to guess
    PROVIDER_UNAVAILABLE
}
