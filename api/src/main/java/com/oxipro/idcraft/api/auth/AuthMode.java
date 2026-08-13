package com.oxipro.idcraft.api.auth;

// how is idcraft configured to apply settings on users
public enum AuthMode {
    // Always offline UUID
    CRACKED,
    // Premium Mojang UUID if premium name, else offline
    MIXED,
    // Fresh random UUID until an account exists
    RANDOM
}
