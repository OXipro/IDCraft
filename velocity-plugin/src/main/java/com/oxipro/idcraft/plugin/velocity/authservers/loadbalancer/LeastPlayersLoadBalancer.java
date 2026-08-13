package com.oxipro.idcraft.plugin.velocity.authservers.loadbalancer;

import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Comparator;
import java.util.List;

public class LeastPlayersLoadBalancer implements ILoadBalancer {

    @Override
    public RegisteredServer pickServer(List<RegisteredServer> servers) {
        RegisteredServer best = servers.stream()
                .min(Comparator.comparingInt(server -> server.getPlayersConnected().size()))
                .orElse(servers.get(0));
        return best;
    }
}