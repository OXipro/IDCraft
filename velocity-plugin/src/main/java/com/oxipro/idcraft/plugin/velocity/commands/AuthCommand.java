package com.oxipro.idcraft.plugin.velocity.commands;

import com.oxipro.cmu.configlang.api.config.IConfigFile;
import com.oxipro.idcraft.api.auth.AuthVisitKind;
import com.oxipro.idcraft.core.utils.message.PlaceholderKeys;
import com.oxipro.idcraft.core.utils.message.Placeholders;
import com.oxipro.idcraft.plugin.velocity.IDCraftVelocityPlugin;
import com.oxipro.idcraft.plugin.velocity.auth.PlayerAuthContext;
import com.oxipro.idcraft.plugin.velocity.authservers.AuthServersManager;
import com.oxipro.idcraft.plugin.velocity.configuration.paths.MainConfigPaths;
import com.oxipro.idcraft.plugin.velocity.language.LanguagePaths;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthCommand implements SimpleCommand {

    private final IDCraftVelocityPlugin plugin;
    private final AuthServersManager authServersManager;
    private final ConcurrentHashMap<UUID, Long> lastUse = new ConcurrentHashMap<>();

    public AuthCommand(IDCraftVelocityPlugin plugin, AuthServersManager authServersManager) {
        this.plugin = plugin;
        this.authServersManager = authServersManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            return;
        }
        IConfigFile config = plugin.getConfigManager().getMain();
        if (!config.getBoolean(MainConfigPaths.ACCOUNT_DESK_ENABLED)) {
            player.sendMessage(plugin.getMessageUtil().message(player, LanguagePaths.ACCOUNT_DESK_DISABLED));
            return;
        }
        PlayerAuthContext authContext = plugin.getPlayerAuthContext();
        if (authContext.needsAuth(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageUtil().message(player, LanguagePaths.ACCOUNT_DESK_MUST_LOGIN));
            return;
        }
        if (authContext.isAccountDesk(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageUtil().message(player, LanguagePaths.ACCOUNT_DESK_ALREADY));
            return;
        }
        String currentServer = player.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse(null);
        if (currentServer != null && authServersManager.isAuthServer(currentServer)) {
            player.sendMessage(plugin.getMessageUtil().message(player, LanguagePaths.ACCOUNT_DESK_ALREADY));
            return;
        }
        int cooldown = config.getInt(MainConfigPaths.COMMANDS_AUTH_COOLDOWN);
        if (!player.hasPermission("idcraft.account.bypass-cooldown") && !tryCooldown(player.getUniqueId(), cooldown)) {
            long remaining = remainingSeconds(player.getUniqueId(), cooldown);
            player.sendMessage(plugin.getMessageUtil().message(
                    player,
                    LanguagePaths.ACCOUNT_DESK_COOLDOWN,
                    Placeholders.of(PlaceholderKeys.SECONDS, remaining)
            ));
            return;
        }
        RegisteredServer authServer = authServersManager.pickServer();
        if (authServer == null) {
            player.sendMessage(plugin.getMessageUtil().strangerMessage(player, LanguagePaths.ERROR_NO_AUTH_SERVER));
            return;
        }
        authContext.beginAccountDesk(player.getUniqueId(), currentServer);
        if (plugin.getMessagingProvider() != null) {
            plugin.getMessagingProvider().allowConnection(player.getUniqueId(), AuthVisitKind.ACCOUNT_DESK);
        }
        player.sendMessage(plugin.getMessageUtil().message(player, LanguagePaths.ACCOUNT_DESK_TRANSFERRING));
        player.createConnectionRequest(authServer).fireAndForget();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        String permission = plugin.getConfigManager().getMain().getString(MainConfigPaths.COMMANDS_AUTH_PERMISSION);
        if (permission == null || permission.isBlank()) {
            permission = "idcraft.command.auth";
        }
        return invocation.source().getPermissionValue(permission) != Tristate.FALSE;
    }

    private boolean tryCooldown(UUID uuid, int seconds) {
        if (seconds <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long previous = lastUse.get(uuid);
        if (previous != null && now - previous < seconds * 1000L) {
            return false;
        }
        lastUse.put(uuid, now);
        return true;
    }

    private long remainingSeconds(UUID uuid, int seconds) {
        Long previous = lastUse.get(uuid);
        if (previous == null || seconds <= 0) {
            return 0;
        }
        long remainingMs = seconds * 1000L - (System.currentTimeMillis() - previous);
        return remainingMs <= 0 ? 0 : (remainingMs + 999) / 1000;
    }
}
