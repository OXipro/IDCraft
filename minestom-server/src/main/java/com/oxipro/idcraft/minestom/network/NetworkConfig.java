package com.oxipro.idcraft.minestom.network;

import com.oxipro.cmu.configlang.standalone.config.ConfigFile;
import com.oxipro.idcraft.minestom.configuration.paths.MainConfigPaths;

public class NetworkConfig {

    private final NetworkMode mode;
    private final String bindHost;
    private final int bindPort;
    private final ProxyType proxyType;
    private final String velocitySecret;

    public NetworkConfig(
            NetworkMode mode,
            String bindHost,
            int bindPort,
            ProxyType proxyType,
            String velocitySecret
    ) {
        this.mode = mode;
        this.bindHost = bindHost;
        this.bindPort = bindPort;
        this.proxyType = proxyType;
        this.velocitySecret = velocitySecret;
    }

    public static NetworkConfig fromConfig(ConfigFile config) {
        NetworkMode mode = NetworkMode.fromConfig(config.getString(MainConfigPaths.NETWORK_MODE));

        String host = config.getString(MainConfigPaths.NETWORK_CONFIG_BIND_HOST);
        int port = readPort(config);
        ProxyType proxyType = ProxyType.fromConfig(config.getString(MainConfigPaths.NETWORK_CONFIG_PROXY_TYPE));
        String secret = config.getString(MainConfigPaths.NETWORK_CONFIG_PROXY_VELOCITY_SECRET);

        return new NetworkConfig(mode, host, port, proxyType, secret);
    }

    private static int readPort(ConfigFile config) {
        try {
            int port = config.getInt(MainConfigPaths.NETWORK_CONFIG_BIND_PORT);
            if (port > 0) {
                return port;
            }
        } catch (Exception ignored) {
            // try string
        }
        String raw = config.getString(MainConfigPaths.NETWORK_CONFIG_BIND_PORT);
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalStateException("network.config.bind.port is missing in config.yml");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("network.config.bind.port is not a valid integer: " + raw, e);
        }
    }

    public NetworkMode getMode() {
        return mode;
    }

    public String getBindHost() {
        return bindHost;
    }

    public int getBindPort() {
        return bindPort;
    }

    public ProxyType getProxyType() {
        return proxyType;
    }

    public String getVelocitySecret() {
        return velocitySecret;
    }
}
