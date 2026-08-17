package com.oxipro.idcraft.redis.providers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.oxipro.idcraft.api.auth.AuthVisitKind;
import com.oxipro.idcraft.api.messaging.IMessagingProvider;
import com.oxipro.idcraft.api.messaging.handlers.IAuthServerMessagingHandler;
import com.oxipro.idcraft.api.messaging.handlers.IProxyMessagingHandler;
import com.oxipro.idcraft.api.platform.PlatformType;
import com.oxipro.idcraft.api.messaging.MessagingChannels;
import com.oxipro.idcraft.redis.JedisPoolFactory;
import com.oxipro.idcraft.redis.RedisConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class RedisMessagingProvider implements IMessagingProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisMessagingProvider.class);

    private final JedisPool subPool;
    private final JedisPool pubPool;
    private final ExecutorService listenerPool = Executors.newCachedThreadPool();
    private final PlatformType platformType;
    private volatile JedisPubSub sub;

    public RedisMessagingProvider(RedisConnectionConfig redisConfiguration, PlatformType platformType) {
        this.subPool = JedisPoolFactory.create(redisConfiguration);
        this.pubPool = JedisPoolFactory.create(redisConfiguration);
        this.platformType = platformType;
    }

    @Override
    public boolean connectForProxy(IProxyMessagingHandler pmh) {
        if (platformType != PlatformType.PROXY) {
            LOGGER.warn("connectForProxy called on wrong platform type: {}", platformType);
            return false;
        }

        return subscribe(MessagingChannels.AUTH, (json) -> {
            if (!json.has("uuid") || !json.has("from")) {
                LOGGER.warn("Proxy message missing fields: {}", json);
                return;
            }

            try {
                String from = json.get("from").getAsString();
                UUID player = UUID.fromString(json.get("uuid").getAsString());
                pmh.handle(from, player);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid UUID in Proxy message: {}", json.get("uuid"));
            } catch (Exception e) {
                LOGGER.error("Error handling Proxy message", e);
            }
        });
    }

    @Override
    public boolean connectForAuthServer(IAuthServerMessagingHandler asmh) {
        if (platformType != PlatformType.AUTH_SERVER) {
            LOGGER.warn("connectForAuthServer called on wrong platform type: {}", platformType);
            return false;
        }

        return subscribe(MessagingChannels.CONNECT, (json) -> {
            if (!json.has("uuid")) {
                LOGGER.warn("AuthServer message missing 'uuid': {}", json);
                return;
            }

            try {
                UUID player = UUID.fromString(json.get("uuid").getAsString());
                AuthVisitKind kind = AuthVisitKind.fromName(
                        json.has("kind") ? json.get("kind").getAsString() : null);
                asmh.handle(player, kind);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid UUID in AuthServer message: {}", json.get("uuid"));
            } catch (Exception e) {
                LOGGER.error("Error handling AuthServer message", e);
            }
        });
    }

    private boolean subscribe(String channel, Consumer<JsonObject> handler) {
        sub = new JedisPubSub() {
            @Override
            public void onMessage(String ch, String message) {
                if (!channel.equals(ch)) {
                    return;
                }

                try {
                    JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                    handler.accept(json);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Malformed JSON on '{}': {}", channel, message);
                } catch (Exception e) {
                    LOGGER.error("Error processing message on '{}'", channel, e);
                }
            }
        };

        listenerPool.execute(() -> {
            try (Jedis jedis = subPool.getResource()) {
                jedis.subscribe(sub, channel);
            } catch (Exception e) {
                LOGGER.error("Subscription failed on '{}'", channel, e);
            } finally {
                listenerPool.shutdown();
            }
        });

        return true;
    }

    public boolean publish(String channel, String jsonMessage) {
        try (Jedis jedis = pubPool.getResource()) {
            jedis.publish(channel, jsonMessage);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to publish on '{}'", channel, e);
            return false;
        }
    }

    @Override
    public boolean authenticated(UUID uuid, String authServerName) {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("from", authServerName);
        return publish(MessagingChannels.AUTH, json.toString());
    }

    @Override
    public boolean allowConnection(UUID uuid, AuthVisitKind kind) {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("kind", (kind == null ? AuthVisitKind.LOGIN : kind).name());
        return publish(MessagingChannels.CONNECT, json.toString());
    }

    @Override
    public void disconnect() {
        if (sub != null) {
            try {
                sub.unsubscribe();
            } catch (Exception e) {
                LOGGER.warn("Error unsubscribing", e);
            }
            sub = null;
        }

        try {
            pubPool.destroy();
            subPool.destroy();
        } catch (Exception e) {
            LOGGER.error("Error destroying pools", e);
        }

        listenerPool.shutdownNow();
    }
}
