package com.oxipro.idcraft.plugin.velocity.authservers.loadbalancer;

import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.List;

public class OptimalChoiceLoadBalancer implements ILoadBalancer {

    private final ILoadBalancer fallback = new LeastPlayersLoadBalancer();

    @Override
    public RegisteredServer pickServer(List<RegisteredServer> servers) {
        return fallback.pickServer(servers);
    }
}