package com.oxipro.idcraft.plugin.velocity.authservers;

import com.oxipro.idcraft.plugin.velocity.IDCraftVelocityPlugin;
import com.oxipro.idcraft.plugin.velocity.authservers.loadbalancer.LeastPlayersLoadBalancer;
import com.oxipro.idcraft.plugin.velocity.authservers.loadbalancer.ILoadBalancer;
import com.oxipro.idcraft.plugin.velocity.authservers.loadbalancer.NoneLoadBalancer;
import com.oxipro.idcraft.plugin.velocity.authservers.loadbalancer.OptimalChoiceLoadBalancer;
import com.oxipro.idcraft.plugin.velocity.authservers.loadbalancer.RoundRobinLoadBalancer;
import com.oxipro.idcraft.plugin.velocity.configuration.paths.MainConfigPaths;
import com.oxipro.idcraft.support.shulker.velocity.ShulkerVelocityAuthServersProvider;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuthServersManager {

    private final IDCraftVelocityPlugin plugin;
    private final List<RegisteredServer> authServers = new CopyOnWriteArrayList<>();

    private final LoadBalancerType loadBalancerType;
    private final ILoadBalancer loadBalancer;
    private final AuthServersProviders mode;

    public AuthServersManager(IDCraftVelocityPlugin plugin) {
        this.plugin = plugin;

        this.loadBalancerType = LoadBalancerType.fromString(
                plugin.getConfigManager().getMain().getString(MainConfigPaths.AUTH_SERVERS_LB));
        this.loadBalancer = createLoadBalancer(loadBalancerType);

        this.mode = AuthServersProviders.fromString(
                plugin.getConfigManager().getMain().getString(MainConfigPaths.AUTH_SERVERS_PROVIDER));
    }

    public void init() {
        authServers.clear();

        switch (mode) {
            case SHULKER -> initShulker();
            case CONFIG -> initConfig();
        }
    }

    private void initConfig() {
        List<String> configuredNames = plugin.getConfigManager().getMain()
                .getStringList(MainConfigPaths.AUTH_SERVERS_CONFIG);

        for (String name : configuredNames) {
            plugin.getProxyServer().getServer(name).ifPresent(authServers::add);
        }
    }

    private void initShulker() {
        String tag = plugin.getConfigManager().getMain().getString(MainConfigPaths.AUTH_SERVERS_SHULKER_TAG);

        for (String serverName : new ShulkerVelocityAuthServersProvider(tag).get()) {
            plugin.getProxyServer().getServer(serverName).ifPresent(authServers::add);
        }
    }

    public RegisteredServer pickServer() {
        List<RegisteredServer> snapshot = List.copyOf(authServers);

        if (snapshot.isEmpty()) {
            return null;
        }
        if (snapshot.size() == 1) {
            return snapshot.getFirst();
        }
        return loadBalancer.pickServer(snapshot);
    }

    public List<RegisteredServer> getAuthServers() {
        return Collections.unmodifiableList(authServers);
    }

    public boolean isAuthServer(RegisteredServer server) {
        return server != null && isAuthServer(server.getServerInfo().getName());
    }

    public boolean isAuthServer(String name) {
        if (name == null) {
            return false;
        }
        for (RegisteredServer server : authServers) {
            if (server.getServerInfo().getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private ILoadBalancer createLoadBalancer(LoadBalancerType type) {
        return switch (type) {
            case NONE -> new NoneLoadBalancer();
            case ROUND_ROBIN -> new RoundRobinLoadBalancer();
            case LEAST_PLAYERS -> new LeastPlayersLoadBalancer();
            case OPTIMAL_CHOICE -> new OptimalChoiceLoadBalancer();
        };
    }
}