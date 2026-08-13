package com.oxipro.idcraft.plugin.velocity.forwarding.providers;

import com.oxipro.idcraft.api.auth.PlayerAuthType;
import com.oxipro.idcraft.plugin.velocity.IDCraftVelocityPlugin;
import com.oxipro.idcraft.plugin.velocity.forwarding.IForwardingProvider;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;


public class OptimalChoiceForwardingProvider implements IForwardingProvider {

    private final IDCraftVelocityPlugin plugin;

    public OptimalChoiceForwardingProvider(IDCraftVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handleInitialChoice(PlayerChooseInitialServerEvent event, PlayerAuthType authType) {
        throw new UnsupportedOperationException("OPTIMAL_CHOICE api is not implemented yet");
    }

    @Override
    public void handlePostAuth(Player player, String fromServer, PlayerAuthType authType) {
        throw new UnsupportedOperationException("OPTIMAL_CHOICE api is not implemented yet");
    }
}