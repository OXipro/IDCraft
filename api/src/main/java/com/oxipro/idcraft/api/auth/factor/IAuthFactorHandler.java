package com.oxipro.idcraft.api.auth.factor;

import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.api.auth.AuthResult;

import java.util.UUID;

/**
 * One auth factor backend (password, email, totp...).
 * UI collects values; this verifies / enrolls them.
 */
public interface IAuthFactorHandler {

    /**
     * @return factor kind this handler owns
     */
    AuthFactor kind();

    /**
     * Whether this player has enrolled the factor (login may require it).
     *
     * @param uuid player uuid
     * @return true if enrolled
     */
    boolean isEnrolled(UUID uuid);

    /**
     * Verify a login value for an enrolled factor.
     *
     * @param uuid player uuid
     * @param value user-supplied value
     * @return ok or failure code
     */
    AuthResult verifyLogin(UUID uuid, String value);

    /**
     * Enroll / store factor data at register (or setup).
     *
     * @param uuid player uuid
     * @param value user-supplied value
     * @return ok or failure code
     */
    AuthResult enroll(UUID uuid, String value);

    /**
     * Clear enrollment (account delete / reset).
     *
     * @param uuid player uuid
     */
    void clear(UUID uuid);
}
