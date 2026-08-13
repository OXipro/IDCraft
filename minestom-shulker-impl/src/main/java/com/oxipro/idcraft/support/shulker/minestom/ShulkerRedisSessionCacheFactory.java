package com.oxipro.idcraft.support.shulker.minestom;

import com.oxipro.idcraft.api.session.ICacheProvider;
import com.oxipro.idcraft.redis.providers.RedisCacheProvider;

public final class ShulkerRedisSessionCacheFactory {

    private ShulkerRedisSessionCacheFactory() {
    }

    public static ICacheProvider create() {
        return new RedisCacheProvider(new ShulkerRedisEnv().getConfig());
    }
}
