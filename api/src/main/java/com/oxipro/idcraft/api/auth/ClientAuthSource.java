package com.oxipro.idcraft.api.auth;

// What the connecting client is (detection), not what we assign
public enum ClientAuthSource {
    // Bedrock via Floodgate
    FLOODGATE,
    // Java name claimed on Mojang
    MOJANG,
    // Java offline / cracked path
    OFFLINE
}
