package com.oxipro.idcraft.api.account;

import com.oxipro.idcraft.api.auth.AuthFactor;

import java.util.Map;
import java.util.UUID;

/**
 * Persistent storage for non-password auth factors (email, 2FA secret, ...).
 * Email is stored plain. TOTP secret is stored plain (must be recoverable for verification).
 */
public interface IAccountFactorRepository {

    /**
     * @param uuid player uuid
     * @return factor -> value map (never null)
     */
    Map<AuthFactor, String> findAll(UUID uuid);

    /**
     * @param uuid player uuid
     * @param factor factor kind
     * @return stored value or null
     */
    String find(UUID uuid, AuthFactor factor);

    /**
     * Lookup player by email factor value (case-insensitive).
     *
     * @param email email address
     * @return player uuid or null
     */
    UUID findUuidByEmail(String email);

    /**
     * Insert or replace one factor value.
     *
     * @param uuid player uuid
     * @param factor factor kind
     * @param value plain value
     */
    void save(UUID uuid, AuthFactor factor, String value);

    /**
     * @param uuid player uuid
     * @param factor factor kind
     */
    void delete(UUID uuid, AuthFactor factor);

    /**
     * @param uuid player uuid
     */
    void deleteAll(UUID uuid);
}
