package com.oxipro.idcraft.api.auth;

import java.util.UUID;

/**
 * Result of {@link IAuthManager#resolveConnection}.
 * Callers use instanceof on Allow / Deny / RequiresAuth.
 */
public interface IAuthDecision {

    /**
     * @return UUID we assign for the profile, or null to keep the client/protocol UUID (Floodgate, deny)
     */
    UUID getUuid();
}
