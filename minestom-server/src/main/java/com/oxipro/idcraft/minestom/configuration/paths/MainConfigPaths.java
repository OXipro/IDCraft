package com.oxipro.idcraft.minestom.configuration.paths;

public final class MainConfigPaths {

    public static final String NETWORK_MODE = "network.mode";
    public static final String NETWORK_CONFIG_BIND_HOST = "network.config.bind.host";
    public static final String NETWORK_CONFIG_BIND_PORT = "network.config.bind.port";
    public static final String NETWORK_CONFIG_PROXY_TYPE = "network.config.proxy.type";
    public static final String NETWORK_CONFIG_PROXY_VELOCITY_SECRET = "network.config.proxy.velocity.secret";

    public static final String AUTH_WORLD_ENABLED = "auth.world.enabled";
    public static final String AUTH_WORLD_TYPE = "auth.world.type";
    public static final String AUTH_WORLD_ANVIL_PATH = "auth.world.anvil.path";
    public static final String AUTH_WORLD_SPAWN_X = "auth.world.spawn.x";
    public static final String AUTH_WORLD_SPAWN_Y = "auth.world.spawn.y";
    public static final String AUTH_WORLD_SPAWN_Z = "auth.world.spawn.z";
    public static final String AUTH_WORLD_SPAWN_YAW = "auth.world.spawn.yaw";
    public static final String AUTH_WORLD_SPAWN_PITCH = "auth.world.spawn.pitch";

    public static final String AUTH_PROMPT_TYPE = "auth.prompt.type";
    public static final String AUTH_PROMPT_DIALOG_PHASE = "auth.prompt.dialog.phase";

    public static final String AUTH_METHODS_REQUIRED = "auth.methods.required";
    public static final String AUTH_METHODS_OPTIONAL = "auth.methods.optional";

    public static final String AUTH_METHODS_PASSWORD_MIN_LENGTH = "auth.methods.password.min-length";
    public static final String AUTH_METHODS_PASSWORD_REGEX_ENABLED = "auth.methods.password.regex.enabled";
    public static final String AUTH_METHODS_PASSWORD_REGEX_EXPRESSION = "auth.methods.password.regex.expression";

    public static final String AUTH_METHODS_EMAIL_PROVIDER = "auth.methods.email.provider";

    public static final String AUTH_METHODS_2FA_PROVIDER = "auth.methods.2fa.provider";
    public static final String AUTH_METHODS_2FA_TOTP = "auth.methods.2fa.totp";

    public static final String AUTH_LANGUAGE_DETECT_BEFORE_REGISTER = "auth.language.detect-before-register";

    private MainConfigPaths() {
    }
}
