package com.oxipro.idcraft.api.auth.decisions;

import com.oxipro.idcraft.api.auth.IAuthDecision;

import java.util.UUID;

public class AuthDecisionDeny implements IAuthDecision {

    public enum AuthDenyReason {
        PREMIUM_USERNAME_RESERVED,
        ACCOUNT_LOCKED,
        WIRED,
        // Premium lookup UNKNOWN
        IDENTITY_PROVIDER_UNAVAILABLE
    }

    private final AuthDenyReason reason;

    public AuthDecisionDeny(AuthDenyReason reason) {
        this.reason = reason;
    }

    @Override
    public UUID getUuid() {
        return null;
    }

    public AuthDenyReason getReason() {
        return reason;
    }
}
