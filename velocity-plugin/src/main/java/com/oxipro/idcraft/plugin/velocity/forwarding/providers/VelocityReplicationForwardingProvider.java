package com.oxipro.idcraft.plugin.velocity.forwarding.providers;

import com.oxipro.cmu.configlang.api.config.IConfigFile;
import com.oxipro.idcraft.api.auth.PlayerAuthType;
import com.oxipro.idcraft.plugin.velocity.IDCraftVelocityPlugin;
import com.oxipro.idcraft.plugin.velocity.forwarding.IForwardingProvider;
import com.oxipro.idcraft.plugin.velocity.language.LanguagePaths;
import com.oxipro.idcraft.plugin.velocity.utils.MessageUtil;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class VelocityReplicationForwardingProvider implements IForwardingProvider {

    private final ProxyServer proxyServer;
    private final IConfigFile config;
    private final MessageUtil messageUtil;

    public VelocityReplicationForwardingProvider(IDCraftVelocityPlugin plugin) {
        this.proxyServer = plugin.getProxyServer();
        this.config = plugin.getConfigManager().getMain();
        this.messageUtil = plugin.getMessageUtil();
    }

    @Override
    public void handleInitialChoice(PlayerChooseInitialServerEvent event, PlayerAuthType authType) {
        Player player = event.getPlayer();
        Optional<RegisteredServer> target = resolveTarget(player);
        target.ifPresent(event::setInitialServer);
    }

    @Override
    public void handlePostAuth(Player player, String fromServer, PlayerAuthType authType) {
        Optional<RegisteredServer> target = resolveTarget(player);
        if (target.isEmpty()) {
            player.disconnect(messageUtil.message(player, LanguagePaths.ERROR_NO_AVAILABLE_SERVER));
            return;
        }
        player.createConnectionRequest(target.get()).fireAndForget();
    }


    private Optional<RegisteredServer> resolveTarget(Player player) {
        String virtualHost = player.getVirtualHost()
                .map(InetSocketAddress::getHostString)
                .orElse("")
                .toLowerCase(Locale.ROOT);

        List<String> candidates = proxyServer.getConfiguration().getForcedHosts().get(virtualHost);
        if (candidates == null || candidates.isEmpty()) {
            candidates = proxyServer.getConfiguration().getAttemptConnectionOrder();
        }
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        RegisteredServer current = player.getCurrentServer()
                .map(sc -> sc.getServer())
                .orElse(null);

        for (String name : candidates) {
            if (current != null && current.getServerInfo().getName().equalsIgnoreCase(name)) {
                continue;
            }
            Optional<RegisteredServer> server = proxyServer.getServer(name);
            if (server.isPresent()) {
                return server;
            }
        }
        return Optional.empty();
    }
}