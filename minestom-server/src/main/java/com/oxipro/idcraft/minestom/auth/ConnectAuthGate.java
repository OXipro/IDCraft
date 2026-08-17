package com.oxipro.idcraft.minestom.auth;

import com.oxipro.idcraft.api.auth.AuthVisitKind;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ConnectAuthGate {

    private static final long ALLOW_TTL_MS = TimeUnit.SECONDS.toMillis(45);

    public record Grant(AuthVisitKind kind, long allowedAt) {
        public Grant(AuthVisitKind kind) {
            this(kind == null ? AuthVisitKind.LOGIN : kind, System.currentTimeMillis());
        }

        public boolean expired() {
            return System.currentTimeMillis() - allowedAt > ALLOW_TTL_MS;
        }
    }

    private final Map<UUID, Grant> allowed = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Grant>> waiters = new ConcurrentHashMap<>();

    public void allow(UUID uuid, AuthVisitKind kind) {
        if (uuid == null) {
            return;
        }
        Grant grant = new Grant(kind);
        allowed.put(uuid, grant);
        CompletableFuture<Grant> waiter = waiters.remove(uuid);
        if (waiter != null) {
            waiter.complete(grant);
        }
    }

    public Grant awaitGrant(UUID uuid, long timeout, TimeUnit unit) {
        if (uuid == null) {
            return null;
        }
        Grant existing = consume(uuid);
        if (existing != null) {
            return existing;
        }
        CompletableFuture<Grant> future = waiters.computeIfAbsent(uuid, id -> new CompletableFuture<>());
        existing = consume(uuid);
        if (existing != null) {
            waiters.remove(uuid, future);
            return existing;
        }
        try {
            Grant grant = future.get(timeout, unit);
            consume(uuid);
            return grant;
        } catch (TimeoutException e) {
            waiters.remove(uuid, future);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            waiters.remove(uuid, future);
            return null;
        } catch (Exception e) {
            waiters.remove(uuid, future);
            return null;
        }
    }

    public void release(UUID uuid) {
        if (uuid == null) {
            return;
        }
        allowed.remove(uuid);
        CompletableFuture<Grant> waiter = waiters.remove(uuid);
        if (waiter != null) {
            waiter.cancel(true);
        }
    }

    private Grant consume(UUID uuid) {
        Grant grant = allowed.remove(uuid);
        if (grant == null || grant.expired()) {
            return null;
        }
        return grant;
    }
}
