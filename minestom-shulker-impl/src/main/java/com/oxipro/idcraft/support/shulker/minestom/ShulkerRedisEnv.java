package com.oxipro.idcraft.support.shulker.minestom;

import com.oxipro.idcraft.redis.RedisConnectionConfig;

public final class ShulkerRedisEnv {

    private RedisConnectionConfig redisConnectionConfig;

    public ShulkerRedisEnv() {
        load();
    }

    public void load() {
        String host = require("SHULKER_REDIS_HOST");
        int port = Integer.parseInt(require("SHULKER_REDIS_PORT"));
        String username = System.getenv("SHULKER_REDIS_USERNAME");
        String password = System.getenv("SHULKER_REDIS_PASSWORD");
        redisConnectionConfig = new RedisConnectionConfig(host, port, username, password);
    }

    private String require(String envVar) {
        String value = System.getenv(envVar);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(envVar + " not set: is this pod running under the Shulker operator?");
        }
        return value;
    }

    public RedisConnectionConfig getConfig() {
        return redisConnectionConfig;
    }
}
