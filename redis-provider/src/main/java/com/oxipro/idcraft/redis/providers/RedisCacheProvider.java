package com.oxipro.idcraft.redis.providers;

import com.oxipro.idcraft.api.auth.PlayerAuthType;
import com.oxipro.idcraft.api.session.ICacheProvider;
import com.oxipro.idcraft.api.session.Session;
import com.oxipro.idcraft.redis.JedisPoolFactory;
import com.oxipro.idcraft.redis.RedisConnectionConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class RedisCacheProvider implements ICacheProvider {

    private static final String KEY_PREFIX = "idcraft:session:";
    private static final String USER_PREFIX = "idcraft:session:user:";

    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_IP = "ip";
    private static final String FIELD_AT = "at";

    private final JedisPool pool;

    public RedisCacheProvider(RedisConnectionConfig config) {
        this.pool = JedisPoolFactory.create(config);
    }

    @Override
    public Session get(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            Map<String, String> fields = jedis.hgetAll(KEY_PREFIX + uuid);
            if (fields == null || fields.isEmpty()) {
                return null;
            }
            return deserialize(uuid, fields);
        }
    }

    @Override
    public Session getByUsername(String username) {
        if (username == null) {
            return null;
        }
        try (Jedis jedis = pool.getResource()) {
            String raw = jedis.get(USER_PREFIX + username.toLowerCase(Locale.ROOT));
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(raw);
            } catch (IllegalArgumentException e) {
                return null;
            }
            return get(uuid);
        }
    }

    @Override
    public void put(Session session, Duration ttl) {
        String key = KEY_PREFIX + session.getUuid();
        long seconds = Math.max(1L, ttl.toSeconds());
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(key, serialize(session));
            jedis.expire(key, seconds);
            if (session.getUsername() != null) {
                String userKey = USER_PREFIX + session.getUsername().toLowerCase(Locale.ROOT);
                jedis.setex(userKey, seconds, session.getUuid().toString());
            }
        }
    }

    @Override
    public void invalidate(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            Session existing = get(uuid);
            jedis.del(KEY_PREFIX + uuid);
            if (existing != null && existing.getUsername() != null) {
                jedis.del(USER_PREFIX + existing.getUsername().toLowerCase(Locale.ROOT));
            }
        }
    }

    @Override
    public void disconnect() {
        try {
            pool.destroy();
        } catch (Exception ignored) {
        }
    }

    private static Map<String, String> serialize(Session session) {
        Map<String, String> fields = new HashMap<>();
        fields.put(FIELD_USERNAME, session.getUsername() == null ? "" : session.getUsername());
        fields.put(FIELD_TYPE, session.getType().name());
        fields.put(FIELD_IP, session.getIp() == null ? "" : session.getIp());
        fields.put(FIELD_AT, String.valueOf(session.getAuthenticatedAt().toEpochMilli()));
        return fields;
    }

    private static Session deserialize(UUID uuid, Map<String, String> fields) {
        String typeRaw = fields.get(FIELD_TYPE);
        if (typeRaw == null) {
            return null;
        }
        PlayerAuthType type;
        try {
            type = PlayerAuthType.valueOf(typeRaw);
        } catch (IllegalArgumentException e) {
            return null;
        }
        String ip = fields.get(FIELD_IP);
        String username = fields.get(FIELD_USERNAME);
        if (ip != null && ip.isEmpty()) {
            ip = null;
        }
        if (username != null && username.isEmpty()) {
            username = null;
        }
        String atRaw = fields.get(FIELD_AT);
        if (atRaw == null) {
            return null;
        }
        Instant authenticatedAt = Instant.ofEpochMilli(Long.parseLong(atRaw));
        return new Session(username, uuid, type, ip, authenticatedAt);
    }
}
