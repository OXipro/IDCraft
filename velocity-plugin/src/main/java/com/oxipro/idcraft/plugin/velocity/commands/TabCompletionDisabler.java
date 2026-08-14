package com.oxipro.idcraft.plugin.velocity.commands;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.oxipro.cmu.configlang.api.config.IConfigFile;
import com.oxipro.idcraft.plugin.velocity.IDCraftVelocityPlugin;
import com.oxipro.idcraft.plugin.velocity.authservers.AuthServersManager;
import com.oxipro.idcraft.plugin.velocity.configuration.paths.MainConfigPaths;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TabCompletionDisabler {

    private final AuthServersManager authServersManager;
    private final IConfigFile mainConfig;
    private final ProxyServer proxyServer;

    public TabCompletionDisabler(
            IDCraftVelocityPlugin plugin,
            AuthServersManager authServersManager
    ) {
        this.proxyServer = plugin.getProxyServer();
        this.authServersManager = authServersManager;
        this.mainConfig = plugin.getConfigManager().getMain();
    }

    @Subscribe
    public void onTabComplete(TabCompleteEvent event) {
        Predicate<String> shouldHide = buildFilter(event.getPlayer());
        if (shouldHide == null) return;

        event.getSuggestions().removeIf(suggestion -> {
            String command = suggestion.startsWith("/") ? suggestion.substring(1) : suggestion;
            return shouldHide.test(command.split(" ")[0]);
        });
    }

    @Subscribe
    public void onAvailableCommands(PlayerAvailableCommandsEvent event) {
        Predicate<String> shouldHide = buildFilter(event.getPlayer());
        if (shouldHide == null) return;

        RootCommandNode<?> root = event.getRootNode();
        for (CommandNode<?> child : new ArrayList<>(root.getChildren())) {
            String name = child.getName();
            boolean registeredOnProxy = proxyServer.getCommandManager().hasCommand(name);
            if (registeredOnProxy && shouldHide.test(name)) {
                root.removeChildByName(name);
            }
        }
    }

    private Predicate<String> buildFilter(Player player) {
        if (player.getCurrentServer().isEmpty()) return null;
        String serverName = player.getCurrentServer().get().getServerInfo().getName();
        if (!authServersManager.isAuthServer(serverName)) return null;
        if (!mainConfig.getBoolean(MainConfigPaths.AUTH_SERVERS_DISABLE_TAB_COMPLETION_ENABLED)) return null;

        String mode = mainConfig.getString(MainConfigPaths.AUTH_SERVERS_DISABLE_TAB_COMPLETION_MODE);
        if (mode == null) return null;

        return switch (mode) {
            case "ALL" -> name -> true;
            case "CUSTOM_CONFIG" -> {
                List<String> raw = mainConfig.getStringList(MainConfigPaths.AUTH_SERVERS_DISABLE_TAB_COMPLETION_CUSTOM_CONFIG);
                Set<String> hidden = raw.stream().map(String::toLowerCase).collect(Collectors.toSet());
                yield name -> {
                    String lower = name.toLowerCase();
                    String shortName = lower.contains(":") ? lower.substring(lower.indexOf(':') + 1) : lower;
                    return hidden.contains(lower) || hidden.contains(shortName);
                };
            }
            default -> null;
        };
    }
}