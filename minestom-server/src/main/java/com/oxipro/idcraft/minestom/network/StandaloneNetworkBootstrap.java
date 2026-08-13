package com.oxipro.idcraft.minestom.network;

import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import org.slf4j.Logger;

public final class StandaloneNetworkBootstrap {

    private StandaloneNetworkBootstrap() {
    }

    public static MinecraftServer init(NetworkConfig config, Logger logger) {
        Auth auth = buildAuth(config, logger);
        return MinecraftServer.init(auth);
    }

    public static void start(NetworkConfig config, MinecraftServer server, Logger logger) {
        String host = config.getBindHost();
        int port = config.getBindPort();
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalStateException("network.config.bind.host is missing in config.yml");
        }
        logger.info("Starting Minestom on {}:{} (CONFIG network mode)", host.trim(), port);
        server.start(host.trim(), port);
    }

    private static Auth buildAuth(NetworkConfig config, Logger logger) {
        ProxyType proxyType = config.getProxyType();
        if (proxyType == ProxyType.VELOCITY) {
            String secret = resolveVelocitySecret(config.getVelocitySecret());
            logger.info("Velocity modern forwarding enabled (CONFIG network mode)");
            return new Auth.Velocity(secret);
        }
        if (proxyType == ProxyType.BUNGEE) {
            logger.info("BungeeCord proxy middleware enabled (CONFIG network mode)");
            return new Auth.Bungee();
        }
        logger.info("No proxy middleware (network.config.proxy.type=NONE)");
        return new Auth.Offline();
    }

    // Config value may be the secret itself, or an env var name (e.g. VELOCITY_SECRET)
    private static String resolveVelocitySecret(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalStateException(
                    "network.config.proxy.velocity.secret is required when proxy.type is VELOCITY");
        }
        String trimmed = raw.trim();
        String fromEnv = System.getenv(trimmed);
        if (fromEnv != null && !fromEnv.isEmpty()) {
            return fromEnv;
        }
        if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
            String envName = trimmed.substring(2, trimmed.length() - 1).trim();
            String envVal = System.getenv(envName);
            if (envVal == null || envVal.isEmpty()) {
                throw new IllegalStateException(
                        "Environment variable for velocity secret is not set: " + envName);
            }
            return envVal;
        }
        return trimmed;
    }
}
