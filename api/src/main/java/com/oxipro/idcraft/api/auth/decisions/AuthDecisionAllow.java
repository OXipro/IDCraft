package com.oxipro.idcraft.api.auth.decisions;

import com.oxipro.idcraft.api.auth.ClientAuthSource;
import com.oxipro.idcraft.api.auth.IAuthDecision;
import com.oxipro.idcraft.api.auth.PlayerAuthType;

import java.util.UUID;

// Player may join without auth-server password
public class AuthDecisionAllow implements IAuthDecision {

    // null = do not rewrite profile UUID (e.g. Floodgate keeps its own)
    private final UUID assignedUuid;
    private final PlayerAuthType assignedType;
    private final ClientAuthSource clientSource;
    private final boolean fromSession;
    private final boolean requiresOnlineMode;

    public AuthDecisionAllow(
            UUID assignedUuid,
            PlayerAuthType assignedType,
            ClientAuthSource clientSource,
            boolean fromSession,
            boolean requiresOnlineMode
    ) {
        this.assignedUuid = assignedUuid;
        this.assignedType = assignedType;
        this.clientSource = clientSource;
        this.fromSession = fromSession;
        this.requiresOnlineMode = requiresOnlineMode;
    }

    @Override
    public UUID getUuid() {
        return assignedUuid;
    }

    public PlayerAuthType getAssignedType() {
        return assignedType;
    }

    // Deprecated alias for callers still using getType()
    public PlayerAuthType getType() {
        return assignedType;
    }

    public ClientAuthSource getClientSource() {
        return clientSource;
    }

    public boolean isFromSession() {
        return fromSession;
    }

    // True when Mojang online-mode handshake is required
    public boolean requiresOnlineMode() {
        return requiresOnlineMode;
    }
}
