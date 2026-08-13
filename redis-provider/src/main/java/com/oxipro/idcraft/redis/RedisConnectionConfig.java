package com.oxipro.idcraft.redis;

public class RedisConnectionConfig {

    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public RedisConnectionConfig(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public static RedisConnectionConfig of(String host, int port) {
        return new RedisConnectionConfig(host, port, null, null);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
