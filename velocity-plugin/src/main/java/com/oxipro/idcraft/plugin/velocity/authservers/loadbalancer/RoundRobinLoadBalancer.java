package com.oxipro.idcraft.plugin.velocity.authservers.loadbalancer;

import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements ILoadBalancer {

    private final AtomicInteger cursor = new AtomicInteger(0);

    @Override
    public RegisteredServer pickServer(List<RegisteredServer> servers) {
        int index = Math.floorMod(cursor.getAndIncrement(), servers.size());
        return servers.get(index);
    }
}