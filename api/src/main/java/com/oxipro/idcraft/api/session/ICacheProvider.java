package com.oxipro.idcraft.api.session;

import java.time.Duration;
import java.util.UUID;

/**
 * Short-lived session store (local or Redis).
 * Sessions are keyed by UUID; optional username index for reconnect when Mojang is down.
 */
public interface ICacheProvider {

    /**
     * @param uuid player UUID
     * @return session or null
     */
    Session get(UUID uuid);

    /**
     * Lookup by username (case-insensitive). Used when premium UUID is unknown (provider down).
     *
     * @param username player name
     * @return session or null
     */
    Session getByUsername(String username);

    /**
     * Store session and username index. TTL applies to both.
     *
     * @param session session to store
     * @param ttl time to live
     */
    void put(Session session, Duration ttl);

    /**
     * @param uuid player UUID
     */
    void invalidate(UUID uuid);

    /**
     * Release resources.
     */
    void disconnect();
}
