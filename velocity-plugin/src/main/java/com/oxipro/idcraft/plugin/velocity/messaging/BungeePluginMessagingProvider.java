package com.oxipro.idcraft.plugin.velocity.messaging;

import com.oxipro.idcraft.api.messaging.IMessagingProvider;
import com.oxipro.idcraft.api.messaging.MessagingChannels;
import com.oxipro.idcraft.api.messaging.MessagingOpcodes;
import com.oxipro.idcraft.api.messaging.handlers.IAuthServerMessagingHandler;
import com.oxipro.idcraft.api.messaging.handlers.IProxyMessagingHandler;
import com.oxipro.idcraft.plugin.velocity.IDCraftVelocityPlugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.configuration.PlayerConfigurationEvent;
import com.velocitypowered.api.event.player.configuration.PlayerEnteredConfigurationEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BungeePluginMessagingProvider implements IMessagingProvider {

    public static final MinecraftChannelIdentifier AUTH_CHANNEL =
            MinecraftChannelIdentifier.from(MessagingChannels.AUTH);
    public static final MinecraftChannelIdentifier CONNECT_CHANNEL =
            MinecraftChannelIdentifier.from(MessagingChannels.CONNECT);

    private final Set<UUID> pendingConnectAuth = ConcurrentHashMap.newKeySet();
    private final IDCraftVelocityPlugin plugin;
    private IProxyMessagingHandler proxyMessagingHandler;

    public BungeePluginMessagingProvider(IDCraftVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean connectForProxy(IProxyMessagingHandler pmh) {
        this.proxyMessagingHandler = pmh;
        plugin.getProxyServer().getChannelRegistrar().register(AUTH_CHANNEL, CONNECT_CHANNEL);
        return true;
    }

    @Subscribe
    public void onPluginMessageFromPlayer(PluginMessageEvent event) {
        if (!AUTH_CHANNEL.equals(event.getIdentifier())) {
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection backend)) {
            return;
        }

        if (!MessagingOpcodes.isAuthenticated(event.getData())) {
            return;
        }

        if (proxyMessagingHandler != null) {
            proxyMessagingHandler.handle(
                    backend.getServerInfo().getName(),
                    backend.getPlayer().getUniqueId()
            );
        }
    }

    @Override
    public boolean allowConnection(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        pendingConnectAuth.add(uuid);
        return true;
    }

    @Subscribe
    public void onPlayerEnteredConfiguration(PlayerEnteredConfigurationEvent event) {
        trySendConnectAuth(event.player(), event.server());
    }

    @Subscribe
    public void onPlayerConfiguration(PlayerConfigurationEvent event) {
        trySendConnectAuth(event.player(), event.server());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        trySendConnectAuth(event.getPlayer());
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        trySendConnectAuth(event.getPlayer());
    }

    private void trySendConnectAuth(Player player) {
        player.getCurrentServer().ifPresent(connection -> trySendConnectAuth(player, connection));
    }

    private void trySendConnectAuth(Player player, ServerConnection connection) {
        if (player == null || connection == null || !pendingConnectAuth.contains(player.getUniqueId())) {
            return;
        }
        try {
            if (connection.sendPluginMessage(CONNECT_CHANNEL, MessagingOpcodes.connectAuth())) {
                pendingConnectAuth.remove(player.getUniqueId());
            }
        } catch (RuntimeException ignored) {
            // Backend not ready yet; a later connect/config event will retry.
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        pendingConnectAuth.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public boolean authenticated(UUID uuid, String authServerName) {
        return false;
    }

    @Override
    public boolean connectForAuthServer(IAuthServerMessagingHandler asmh) {
        return false;
    }
}
