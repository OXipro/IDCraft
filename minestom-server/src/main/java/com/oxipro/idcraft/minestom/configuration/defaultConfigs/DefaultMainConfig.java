package com.oxipro.idcraft.minestom.configuration.defaultConfigs;

import com.oxipro.cmu.configlang.standalone.config.ConfigFile;
import com.oxipro.idcraft.api.configuration.ConfigsType;
import com.oxipro.idcraft.core.configuration.defaultConfigs.CommonDefaultMainConfig;
import com.oxipro.idcraft.core.configuration.paths.CommonMainConfigPaths;
import com.oxipro.idcraft.minestom.IDCraftMinestomServer;
import com.oxipro.idcraft.minestom.configuration.paths.MainConfigPaths;

import java.io.File;
import java.util.List;

public class DefaultMainConfig extends ConfigFile {

    public DefaultMainConfig(IDCraftMinestomServer server) {
        super(
                new File(IDCraftMinestomServer.CONFIG_DIR, ConfigsType.CONFIG.filePath),
                server.getResourceAsStream(ConfigsType.CONFIG.filePath)
        );
        setDefaults();
    }

    private void setDefaults() {
        CommonDefaultMainConfig.applyCommonDefaults(this);

        addDefault(CommonMainConfigPaths.SERVER_NAME_VALUE, "idcraft-auth-1");

        addDefault(MainConfigPaths.NETWORK_MODE, "CONFIG");
        addDefault(MainConfigPaths.NETWORK_CONFIG_BIND_HOST, "0.0.0.0");
        addDefault(MainConfigPaths.NETWORK_CONFIG_BIND_PORT, 25565);
        addDefault(MainConfigPaths.NETWORK_CONFIG_PROXY_TYPE, "VELOCITY");
        addDefault(MainConfigPaths.NETWORK_CONFIG_PROXY_VELOCITY_SECRET, "VELOCITY_SECRET");

        addDefault(MainConfigPaths.AUTH_WORLD_ENABLED, true);
        addDefault(MainConfigPaths.AUTH_WORLD_TYPE, "VOID");
        addDefault(MainConfigPaths.AUTH_WORLD_ANVIL_PATH, "./authworld/");
        addDefault(MainConfigPaths.AUTH_WORLD_SPAWN_X, 0.5);
        addDefault(MainConfigPaths.AUTH_WORLD_SPAWN_Y, 64.0);
        addDefault(MainConfigPaths.AUTH_WORLD_SPAWN_Z, 0.5);
        addDefault(MainConfigPaths.AUTH_WORLD_SPAWN_YAW, 90.0);
        addDefault(MainConfigPaths.AUTH_WORLD_SPAWN_PITCH, 0.0);

        addDefault(MainConfigPaths.AUTH_PROMPT_TYPE, "DIALOG");
        addDefault(MainConfigPaths.AUTH_PROMPT_DIALOG_PHASE, "PLAYER_JOIN");

        addDefault(MainConfigPaths.AUTH_METHODS_REQUIRED, List.of("PASSWORD"));
        addDefault(MainConfigPaths.AUTH_METHODS_OPTIONAL, List.of("EMAIL", "2FA"));

        addDefault(MainConfigPaths.AUTH_METHODS_PASSWORD_MIN_LENGTH, 4);
        addDefault(MainConfigPaths.AUTH_METHODS_PASSWORD_REGEX_ENABLED, false);
        addDefault(MainConfigPaths.AUTH_METHODS_PASSWORD_REGEX_EXPRESSION,
                "^(?=.{12,128}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");

        addDefault(MainConfigPaths.AUTH_METHODS_EMAIL_PROVIDER, "SMTP");

        addDefault(MainConfigPaths.AUTH_METHODS_2FA_PROVIDER, "TOTP");
        addDefault(MainConfigPaths.AUTH_METHODS_2FA_TOTP, "");

        addDefault(MainConfigPaths.AUTH_LANGUAGE_DETECT_BEFORE_REGISTER, true);
    }
}
