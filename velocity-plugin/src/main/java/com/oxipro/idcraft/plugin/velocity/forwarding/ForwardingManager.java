package com.oxipro.idcraft.plugin.velocity.forwarding;

import com.oxipro.cmu.configlang.api.config.IConfigFile;
import com.oxipro.idcraft.api.auth.PlayerAuthType;
import com.oxipro.idcraft.api.messaging.handlers.IProxyMessagingHandler;
import com.oxipro.idcraft.plugin.velocity.IDCraftVelocityPlugin;
import com.oxipro.idcraft.plugin.velocity.auth.PlayerAuthContext;
import com.oxipro.idcraft.plugin.velocity.configuration.paths.MainConfigPaths;
import com.oxipro.idcraft.plugin.velocity.forwarding.providers.OptimalChoiceForwardingProvider;
import com.oxipro.idcraft.plugin.velocity.forwarding.providers.VelocityReplicationForwardingProvider;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class ForwardingManager implements IProxyMessagingHandler {

    private final ProxyServer proxyServer;
    private final IConfigFile mainConfig;
    private final PlayerAuthContext authContext;
    private final Map<ForwardingType, IForwardingProvider> providers = new EnumMap<>(ForwardingType.class);

    public ForwardingManager(IDCraftVelocityPlugin plugin, PlayerAuthContext authContext) {
        this.proxyServer = plugin.getProxyServer();
        this.mainConfig = plugin.getConfigManager().getMain();
        this.authContext = authContext;

        providers.put(ForwardingType.VELOCITY_REPLICATION, new VelocityReplicationForwardingProvider(plugin));
        providers.put(ForwardingType.OPTIMAL_CHOICE, new OptimalChoiceForwardingProvider(plugin));
    }

    @Override
    public void handle(String fromServer, UUID playerId) {
        proxyServer.getPlayer(playerId).ifPresent(player -> {
            if (authContext.isAccountDesk(playerId)) {
                returnFromAccountDesk(player, fromServer);
                return;
            }
            authContext.clearNeedsAuth(playerId);
            PlayerAuthType authType = resolveAuthType(playerId);
            providerFor(authType).handlePostAuth(player, fromServer, authType);
        });
    }

    public void returnFromAccountDesk(Player player, String fromServer) {
        UUID playerId = player.getUniqueId();
        String previous = authContext.getPreviousServer(playerId).orElse(null);
        authContext.clearAccountDesk(playerId);
        boolean returnPrevious = true;
        try {
            returnPrevious = mainConfig.getBoolean(MainConfigPaths.ACCOUNT_DESK_RETURN_PREVIOUS);
        } catch (Exception ignored) {
        }
        if (returnPrevious && previous != null && !previous.isBlank()) {
            var target = proxyServer.getServer(previous);
            if (target.isPresent()) {
                player.createConnectionRequest(target.get()).fireAndForget();
                return;
            }
        }
        PlayerAuthType authType = resolveAuthType(playerId);
        providerFor(authType).handlePostAuth(player, fromServer, authType);
    }

    public void handleInitialChoice(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        PlayerAuthType authType = resolveAuthType(player.getUniqueId());
        providerFor(authType).handleInitialChoice(event, authType);
    }

    private IForwardingProvider providerFor(PlayerAuthType authType) {
        ForwardingType type = switch (authType) {
            case PREMIUM -> ForwardingType.valueOf(mainConfig.getString(MainConfigPaths.FORWARDING_PREMIUM));
            case FLOODGATE -> ForwardingType.valueOf(mainConfig.getString(MainConfigPaths.FORWARDING_FLOODGATE));
            case CRACKED -> ForwardingType.valueOf(mainConfig.getString(MainConfigPaths.FORWARDING_CRACKS));
        };
        return providers.getOrDefault(type, providers.get(ForwardingType.VELOCITY_REPLICATION));
    }

    private PlayerAuthType resolveAuthType(UUID playerId) {
        return authContext.getAuthType(playerId).orElse(PlayerAuthType.CRACKED);
    }
}
