package com.oxipro.idcraft.plugin.velocity.listeners;

import com.oxipro.idcraft.api.auth.AuthVisitKind;
import com.oxipro.idcraft.plugin.velocity.IDCraftVelocityPlugin;
import com.oxipro.idcraft.plugin.velocity.auth.PlayerAuthContext;
import com.oxipro.idcraft.plugin.velocity.authservers.AuthServersManager;
import com.oxipro.idcraft.plugin.velocity.forwarding.ForwardingManager;
import com.oxipro.idcraft.plugin.velocity.language.LanguagePaths;
import com.oxipro.idcraft.plugin.velocity.configuration.paths.MainConfigPaths;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

public class VelocityServerListeners {

    private final IDCraftVelocityPlugin plugin;
    private final AuthServersManager authServersManager;
    private final ForwardingManager forwardingManager;
    private final PlayerAuthContext authContext;

    public VelocityServerListeners(
            IDCraftVelocityPlugin plugin,
            AuthServersManager authServersManager,
            ForwardingManager forwardingManager,
            PlayerAuthContext authContext
    ) {
        this.plugin = plugin;
        this.authServersManager = authServersManager;
        this.forwardingManager = forwardingManager;
        this.authContext = authContext;
    }

    @Subscribe
    public void onChooseInitialServer(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();

        if (authContext.needsAuth(player.getUniqueId())) {
            RegisteredServer authServer = authServersManager.pickServer();
            if (authServer != null) {
                event.setInitialServer(authServer);
                if (plugin.getMessagingProvider() != null) {
                    boolean registered = plugin.getCore().getAccountRepository()
                            .existsByUsername(player.getUsername());
                    plugin.getMessagingProvider().allowConnection(
                            player.getUniqueId(),
                            registered ? AuthVisitKind.LOGIN : AuthVisitKind.REGISTER
                    );
                }
            } else {
                event.setInitialServer(null);
                player.disconnect(plugin.getMessageUtil().strangerMessage(player, LanguagePaths.ERROR_NO_AUTH_SERVER));
            }
            return;
        }

        forwardingManager.handleInitialChoice(event);
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        if (authContext.isAccountDesk(player.getUniqueId())) {
            if (!plugin.getConfigManager().getMain().getBoolean(MainConfigPaths.ACCOUNT_DESK_LOCK)) {
                return;
            }
            RegisteredServer destination = event.getResult().getServer().orElse(event.getOriginalServer());
            if (authServersManager.isAuthServer(destination)) {
                return;
            }
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            player.sendMessage(plugin.getMessageUtil().message(player, LanguagePaths.ACCOUNT_DESK_LOCKED));
            return;
        }
        if (!authContext.needsAuth(player.getUniqueId())) {
            return;
        }

        RegisteredServer destination = event.getResult().getServer().orElse(event.getOriginalServer());
        if (authServersManager.isAuthServer(destination)) {
            return;
        }

        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        Component message = plugin.getMessageUtil().strangerMessage(player, LanguagePaths.ERROR_SERVER_SWITCH_NOT_ALLOWED);
        if (event.getPreviousServer() == null) {
            player.disconnect(message);
        } else {
            player.sendMessage(message);
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        if (!authContext.isAccountDesk(player.getUniqueId())) {
            return;
        }
        if (!authServersManager.isAuthServer(event.getServer())) {
            authContext.clearAccountDesk(player.getUniqueId());
        }
    }

    @Subscribe(priority = Short.MIN_VALUE)
    public void onKickedFromServer(KickedFromServerEvent event) {
        Player player = event.getPlayer();
        if (authContext.isAccountDesk(player.getUniqueId())) {
            String previous = authContext.getPreviousServer(player.getUniqueId()).orElse(null);
            authContext.clearAccountDesk(player.getUniqueId());
            if (previous != null) {
                plugin.getProxyServer().getServer(previous).ifPresent(server ->
                        event.setResult(KickedFromServerEvent.RedirectPlayer.create(server)));
            }
            return;
        }
        if (!authContext.needsAuth(player.getUniqueId())) {
            return;
        }

        Component reason = event.getServerKickReason().orElseGet(() ->
                plugin.getMessageUtil().strangerMessage(player, LanguagePaths.ERROR_AUTH_SERVER_UNAVAILABLE));
        if (event.kickedDuringServerConnect() && authServersManager.isAuthServer(event.getServer())) {
            reason = plugin.getMessageUtil().strangerMessage(player, LanguagePaths.ERROR_AUTH_SERVER_UNAVAILABLE);
        }
        event.setResult(KickedFromServerEvent.DisconnectPlayer.create(reason));
    }
}
