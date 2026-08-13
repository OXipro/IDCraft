package com.oxipro.idcraft.api.mojang;

/**
 * Resolves whether a username is a premium Mojang account.
 * Implementations may chain Mojang API, cache, NameMC, etc.
 * Must never map transport failures to NOT_PREMIUM.
 */
public interface IMojangVerifier {

    /**
     * @param username raw player name
     * @return PREMIUM, NOT_PREMIUM, or UNKNOWN (API/cache failure)
     */
    PremiumLookupResult lookup(String username);
}
