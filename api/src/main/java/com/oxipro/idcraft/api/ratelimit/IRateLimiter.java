package com.oxipro.idcraft.api.ratelimit;

/**
 * Short-window rate limit (e.g. login attempts by IP).
 */
public interface IRateLimiter {

    /**
     * @param key rate-limit key (often IP)
     * @return true if the call is allowed
     */
    boolean tryAcquire(String key);

    /**
     * Release resources.
     */
    default void disconnect() {}
}
