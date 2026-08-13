package com.oxipro.idcraft.api.auth.decisions;

import com.oxipro.idcraft.api.auth.ClientAuthSource;
import com.oxipro.idcraft.api.auth.IAuthDecision;
import com.oxipro.idcraft.api.auth.PlayerAuthType;

import java.util.UUID;

// Send player to auth server for login/register
public class AuthDecisionRequiresAuth implements IAuthDecision {

    // null = do not rewrite profile UUID
    private final UUID assignedUuid;
    private final PlayerAuthType assignedType;
    private final ClientAuthSource clientSource;
    private final boolean requiresOnlineMode;

    public AuthDecisionRequiresAuth(
            UUID assignedUuid,
            PlayerAuthType assignedType,
            ClientAuthSource clientSource,
            boolean requiresOnlineMode
    ) {
        this.assignedUuid = assignedUuid;
        this.assignedType = assignedType;
        this.clientSource = clientSource;
        this.requiresOnlineMode = requiresOnlineMode;
    }

    @Override
    public UUID getUuid() {
        return assignedUuid;
    }

    public PlayerAuthType getAssignedType() {
        return assignedType;
    }

    public PlayerAuthType getType() {
        return assignedType;
    }

    public ClientAuthSource getClientSource() {
        return clientSource;
    }

    public boolean requiresOnlineMode() {
        return requiresOnlineMode;
    }
}
