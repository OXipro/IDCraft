package com.oxipro.idcraft.plugin.velocity.configuration.paths;

public class MainConfigPaths {

    // Auth servers (backend routing)
    public static final String AUTH_SERVERS_LB = "auth-servers.lb";
    public static final String AUTH_SERVERS_PROVIDER = "auth-servers.provider";
    public static final String AUTH_SERVERS_SHULKER_TAG = "auth-servers.shulker-tag";
    public static final String AUTH_SERVERS_CONFIG = "auth-servers.config";
    public static final String AUTH_SERVERS_DISABLE_TAB_COMPLETION_ENABLED = "auth-servers.disable-tab-completion.enabled";
    public static final String AUTH_SERVERS_DISABLE_TAB_COMPLETION_MODE = "auth-servers.disable-tab-completion.mode";
    public static final String AUTH_SERVERS_DISABLE_TAB_COMPLETION_CUSTOM_CONFIG = "auth-servers.disable-tab-completion.custom-config";

    // Prefer CommonMainConfigPaths for shared auth keys

    public static final String FORWARDING_PREMIUM = "forwarding.premium";
    public static final String FORWARDING_FLOODGATE = "forwarding.floodgate";
    public static final String FORWARDING_CRACKS = "forwarding.cracks";

    public static final String COMMANDS_AUTH_ENABLED = "commands.auth.enabled";
    public static final String COMMANDS_AUTH_ALIASES = "commands.auth.aliases";
    public static final String COMMANDS_AUTH_PERMISSION = "commands.auth.permission";
    public static final String COMMANDS_AUTH_COOLDOWN = "commands.auth.cooldown-seconds";

    public static final String ACCOUNT_DESK_ENABLED = "account-desk.enabled";
    public static final String ACCOUNT_DESK_RETURN_PREVIOUS = "account-desk.return-to-previous-server";
    public static final String ACCOUNT_DESK_LOCK = "account-desk.lock-to-auth-server";
}
