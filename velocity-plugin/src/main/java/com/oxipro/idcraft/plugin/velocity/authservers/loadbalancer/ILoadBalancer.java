package com.oxipro.idcraft.plugin.velocity.authservers.loadbalancer;

import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.List;

public interface ILoadBalancer {
    RegisteredServer pickServer(List<RegisteredServer> servers);
}

