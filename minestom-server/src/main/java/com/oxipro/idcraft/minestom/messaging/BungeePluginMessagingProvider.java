package com.oxipro.idcraft.minestom.messaging;

import com.oxipro.idcraft.api.messaging.IMessagingProvider;
import com.oxipro.idcraft.api.messaging.MessagingChannels;
import com.oxipro.idcraft.api.messaging.MessagingOpcodes;
import com.oxipro.idcraft.api.messaging.handlers.IAuthServerMessagingHandler;
import com.oxipro.idcraft.api.messaging.handlers.IProxyMessagingHandler;
import com.oxipro.idcraft.minestom.IDCraftMinestomServer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;

import java.util.UUID;

public class BungeePluginMessagingProvider implements IMessagingProvider {

    private final IDCraftMinestomServer server;
    private IAuthServerMessagingHandler asmh;
    private boolean listenerRegistered;

    public BungeePluginMessagingProvider(IDCraftMinestomServer server) {
        this.server = server;
    }

    @Override
    public boolean connectForAuthServer(IAuthServerMessagingHandler asmh) {
        this.asmh = asmh;
        if (!listenerRegistered) {
            MinecraftServer.getGlobalEventHandler().addListener(PlayerPluginMessageEvent.class, this::onPluginMessageReceived);
            listenerRegistered = true;
        }
        return true;
    }

    public void onPluginMessageReceived(PlayerPluginMessageEvent event) {
        if (!MessagingChannels.CONNECT.equals(event.getIdentifier())) {
            return;
        }
        if (!MessagingOpcodes.isConnectAuth(event.getMessage())) {
            return;
        }
        if (asmh != null) {
            asmh.handle(event.getPlayer().getUuid());
        }
    }

    @Override
    public boolean authenticated(UUID uuid, String authServerName) {
        Player player = findPlayer(uuid);
        if (player == null) {
            return false;
        }
        player.sendPacket(new PluginMessagePacket(MessagingChannels.AUTH, MessagingOpcodes.authenticated()));
        return true;
    }

    private static Player findPlayer(UUID uuid) {
        Player online = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);
        if (online != null) {
            return online;
        }
        for (Player player : MinecraftServer.getConnectionManager().getConfigPlayers()) {
            if (player.getUuid().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    @Override
    public boolean connectForProxy(IProxyMessagingHandler pmh) {
        return false;
    }

    @Override
    public boolean allowConnection(UUID uuid) {
        return false;
    }
}
