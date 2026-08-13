package com.oxipro.idcraft.api.mojang;

/**
 * Ternary premium lookup: never treat API failure as "not premium".
 */
public final class PremiumLookupResult {

    public enum Status {
        PREMIUM,
        NOT_PREMIUM,
        UNKNOWN
    }

    private final Status status;
    private final MojangProfile profile;

    private PremiumLookupResult(Status status, MojangProfile profile) {
        this.status = status;
        this.profile = profile;
    }

    public static PremiumLookupResult premium(MojangProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile required for PREMIUM");
        }
        return new PremiumLookupResult(Status.PREMIUM, profile);
    }

    public static PremiumLookupResult notPremium() {
        return new PremiumLookupResult(Status.NOT_PREMIUM, null);
    }

    public static PremiumLookupResult unknown() {
        return new PremiumLookupResult(Status.UNKNOWN, null);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isPremium() {
        return status == Status.PREMIUM;
    }

    public boolean isNotPremium() {
        return status == Status.NOT_PREMIUM;
    }

    public boolean isUnknown() {
        return status == Status.UNKNOWN;
    }

    /**
     * @return profile when PREMIUM, else null
     */
    public MojangProfile getProfile() {
        return profile;
    }
}
