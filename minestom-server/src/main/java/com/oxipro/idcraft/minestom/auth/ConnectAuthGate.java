package com.oxipro.idcraft.minestom.auth;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Holds a player in the Minecraft configuration phase until the proxy
 * confirms the join via Redis or plugin messaging ({@code CONNECT_AUTH}).
 */
public class ConnectAuthGate {

    private static final long ALLOW_TTL_MS = TimeUnit.SECONDS.toMillis(45);

    private final Map<UUID, Long> allowedAt = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Void>> waiters = new ConcurrentHashMap<>();

    public void allow(UUID uuid) {
        if (uuid == null) {
            return;
        }
        allowedAt.put(uuid, System.currentTimeMillis());
        CompletableFuture<Void> waiter = waiters.remove(uuid);
        if (waiter != null) {
            waiter.complete(null);
        }
    }

    public boolean await(UUID uuid, long timeout, TimeUnit unit) {
        if (uuid == null) {
            return false;
        }
        if (consume(uuid)) {
            return true;
        }
        CompletableFuture<Void> future = waiters.computeIfAbsent(uuid, id -> new CompletableFuture<>());
        if (consume(uuid)) {
            waiters.remove(uuid, future);
            return true;
        }
        try {
            future.get(timeout, unit);
            consume(uuid);
            return true;
        } catch (TimeoutException e) {
            waiters.remove(uuid, future);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            waiters.remove(uuid, future);
            return false;
        } catch (Exception e) {
            waiters.remove(uuid, future);
            return false;
        }
    }

    public void release(UUID uuid) {
        if (uuid == null) {
            return;
        }
        allowedAt.remove(uuid);
        CompletableFuture<Void> waiter = waiters.remove(uuid);
        if (waiter != null) {
            waiter.cancel(true);
        }
    }

    private boolean consume(UUID uuid) {
        Long at = allowedAt.remove(uuid);
        return at != null && System.currentTimeMillis() - at <= ALLOW_TTL_MS;
    }
}
