package com.oxipro.idcraft.plugin.velocity.forwarding;

import com.oxipro.idcraft.api.auth.PlayerAuthType;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;

public interface IForwardingProvider {

    void handleInitialChoice(PlayerChooseInitialServerEvent event, PlayerAuthType authType);

    void handlePostAuth(Player player, String fromServer, PlayerAuthType authType);

}