package com.oxipro.idcraft.plugin.velocity.listeners;

import com.oxipro.idcraft.api.auth.IAuthDecision;
import com.oxipro.idcraft.api.auth.IAuthManager;
import com.oxipro.idcraft.api.auth.PlayerAuthType;
import com.oxipro.idcraft.api.auth.decisions.AuthDecisionAllow;
import com.oxipro.idcraft.api.auth.decisions.AuthDecisionDeny;
import com.oxipro.idcraft.api.auth.decisions.AuthDecisionRequiresAuth;
import com.oxipro.idcraft.plugin.velocity.IDCraftVelocityPlugin;
import com.oxipro.idcraft.plugin.velocity.auth.PlayerAuthContext;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.util.UUID;

public class VelocityLoginListeners {

    private final IDCraftVelocityPlugin plugin;
    private final IAuthManager authManager;
    private final PlayerAuthContext authContext;

    public VelocityLoginListeners(
            IDCraftVelocityPlugin plugin,
            IAuthManager authManager,
            PlayerAuthContext authContext
    ) {
        this.plugin = plugin;
        this.authManager = authManager;
        this.authContext = authContext;
    }

    @Subscribe
    public EventTask onPreLogin(PreLoginEvent event) {
        if (!event.getResult().isAllowed()) {
            return null;
        }

        InboundConnection connection = event.getConnection();
        String username = event.getUsername();
        UUID connectionUuid = event.getUniqueId();

        boolean isFloodgate = false;
        if (plugin.getFloodgateApi() != null) {
            if (connectionUuid != null) {
                isFloodgate = plugin.getFloodgateApi().isFloodgatePlayer(connectionUuid);
            }
        }

        final boolean floodgate = isFloodgate;

        return EventTask.async(() -> {
            IAuthDecision decision = authManager.resolveConnection(
                    username,
                    connection.getRemoteAddress().getAddress(),
                    floodgate,
                    connectionUuid
            );

            if (decision instanceof AuthDecisionDeny deny) {
                authContext.removePending(connection);
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                        plugin.getMessageUtil().authDenyStranger(null, deny.getReason())));
                return;
            }

            authContext.putPending(connection, decision);

            boolean online = false;
            if (decision instanceof AuthDecisionAllow allow) {
                online = allow.requiresOnlineMode();
            } else if (decision instanceof AuthDecisionRequiresAuth requires) {
                online = requires.requiresOnlineMode();
            }

            if (online) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
            } else {
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
            }
        });
    }

    @Subscribe
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        InboundConnection connection = event.getConnection();

        IAuthDecision decision = authContext.getPendingDecision(connection).orElse(null);
        if (decision == null) {
            return;
        }

        // Only rewrite when AuthManager assigned an UUID
        UUID assigned = decision.getUuid();
        UUID finalUuid = event.getGameProfile().getId();

        if (assigned != null) {
            if (!assigned.equals(finalUuid)) {
                event.setGameProfile(new GameProfile(
                        assigned,
                        event.getUsername(),
                        event.getGameProfile().getProperties()
                ));
            }
            finalUuid = assigned;
        }

        authContext.promote(connection, finalUuid);
    }

    /**
     * Session write only after full login proof (incl. online-mode handshake).
     * Password path records session on auth-server login/register instead.
     */
    @Subscribe
    public void onLogin(LoginEvent event) {
        if (!event.getResult().isAllowed()) {
            return;
        }

        Player player = event.getPlayer();
        PlayerAuthContext.Entry entry = authContext.get(player.getUniqueId()).orElse(null);
        if (entry == null) {
            return;
        }

        // Auth-server path: session after password
        if (entry.needsAuth()) {
            return;
        }

        PlayerAuthType type = entry.getAuthType();
        if (type == PlayerAuthType.FLOODGATE) {
            return;
        }

        String ip = ipOf(player);
        if (ip == null) {
            return;
        }

        // Premium skip Allow, or session resume (TTL refresh)
        authManager.recordSession(player.getUniqueId(), player.getUsername(), type, ip);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        authContext.remove(event.getPlayer().getUniqueId());
    }

    private static String ipOf(Player player) {
        try {
            InetSocketAddress address = player.getRemoteAddress();
            if (address == null || address.getAddress() == null) {
                return null;
            }
            return address.getAddress().getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }
}
