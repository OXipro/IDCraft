package com.oxipro.idcraft.plugin.velocity.messaging;

import com.oxipro.idcraft.api.auth.AuthVisitKind;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BungeePluginMessagingProvider implements IMessagingProvider {

    public static final MinecraftChannelIdentifier AUTH_CHANNEL =
            MinecraftChannelIdentifier.from(MessagingChannels.AUTH);
    public static final MinecraftChannelIdentifier CONNECT_CHANNEL =
            MinecraftChannelIdentifier.from(MessagingChannels.CONNECT);

    private final Map<UUID, AuthVisitKind> pendingConnect = new ConcurrentHashMap<>();
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
    public boolean allowConnection(UUID uuid, AuthVisitKind kind) {
        if (uuid == null) {
            return false;
        }
        pendingConnect.put(uuid, kind == null ? AuthVisitKind.LOGIN : kind);
        return true;
    }

    @Subscribe
    public void onPlayerEnteredConfiguration(PlayerEnteredConfigurationEvent event) {
        trySendConnect(event.player(), event.server());
    }

    @Subscribe
    public void onPlayerConfiguration(PlayerConfigurationEvent event) {
        trySendConnect(event.player(), event.server());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        trySendConnect(event.getPlayer());
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        trySendConnect(event.getPlayer());
    }

    private void trySendConnect(Player player) {
        player.getCurrentServer().ifPresent(connection -> trySendConnect(player, connection));
    }

    private void trySendConnect(Player player, ServerConnection connection) {
        if (player == null || connection == null) {
            return;
        }
        AuthVisitKind kind = pendingConnect.get(player.getUniqueId());
        if (kind == null) {
            return;
        }
        try {
            if (connection.sendPluginMessage(CONNECT_CHANNEL, MessagingOpcodes.connect(kind))) {
                pendingConnect.remove(player.getUniqueId());
            }
        } catch (RuntimeException ignored) {
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        pendingConnect.remove(event.getPlayer().getUniqueId());
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
