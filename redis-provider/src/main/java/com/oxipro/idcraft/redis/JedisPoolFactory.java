package com.oxipro.idcraft.redis;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class JedisPoolFactory {

    private JedisPoolFactory() {
    }

    public static JedisPool create(RedisConnectionConfig config) {
        if (config.getUsername() != null) {
            return new JedisPool(new JedisPoolConfig(), config.getHost(), config.getPort(), 2000,
                    config.getUsername(), config.getPassword());
        }
        return new JedisPool(new JedisPoolConfig(), config.getHost(), config.getPort());
    }
}
