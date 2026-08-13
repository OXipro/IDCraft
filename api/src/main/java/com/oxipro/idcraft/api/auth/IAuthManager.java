package com.oxipro.idcraft.api.auth;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Shared auth brain for proxy and auth-server.
 */
public interface IAuthManager {

    /**
     * Decide allow / deny / requires-auth at PreLogin (proxy).
     * Never writes sessions — call {@link #recordSession} only after full proof.
     *
     * @param username player name
     * @param address remote IP
     * @param isFloodgatePlayer true if Floodgate Bedrock player
     * @param connectionUuid UUID known at PreLogin (Floodgate UUID when bedrock)
     * @return decision for Velocity
     */
    IAuthDecision resolveConnection(
            String username,
            InetAddress address,
            boolean isFloodgatePlayer,
            UUID connectionUuid
    );

    /**
     * Register a new account (auth-server).
     *
     * @param uuid profile UUID already applied by proxy
     * @param username player name
     * @param password password
     * @param confirmPassword password confirm
     * @param ip remote IP string or null
     * @return result
     */
    AuthResult register(UUID uuid, String username, String password, String confirmPassword, String ip);

    /**
     * Login existing account (auth-server).
     * Fails if uuid does not match the account UUID.
     *
     * @param uuid profile UUID
     * @param username player name
     * @param password password
     * @param ip remote IP string or null
     * @return result
     */
    AuthResult login(UUID uuid, String username, String password, String ip);

    /**
     * True if a fully validated session exists (DB, lock, IP, support flags, username).
     *
     * @param uuid player UUID
     * @param username connecting username
     * @param ip remote IP string or null
     * @return true if session may resume auth
     */
    boolean hasActiveSession(UUID uuid, String username, String ip);

    /**
     * Persist session after identity is fully proven (post online-mode / login / register).
     * No-op for FLOODGATE or when sessions disabled / unsupported type.
     *
     * @param uuid player UUID
     * @param username player name
     * @param type assigned auth type
     * @param ip remote IP (required for session bind)
     */
    void recordSession(UUID uuid, String username, PlayerAuthType type, String ip);

    /**
     * Drop session for uuid.
     *
     * @param uuid player UUID
     */
    void invalidateSession(UUID uuid);
}
