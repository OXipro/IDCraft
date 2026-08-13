package com.oxipro.idcraft.plugin.velocity.authservers.loadbalancer;

import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.List;

public class NoneLoadBalancer implements ILoadBalancer {

    @Override
    public RegisteredServer pickServer(List<RegisteredServer> servers) {
        return servers.getFirst();
    }
}