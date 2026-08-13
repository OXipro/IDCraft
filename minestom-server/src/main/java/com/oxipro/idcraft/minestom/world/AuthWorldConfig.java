package com.oxipro.idcraft.minestom.world;

import com.oxipro.cmu.configlang.standalone.config.ConfigFile;
import com.oxipro.idcraft.minestom.configuration.paths.MainConfigPaths;
import net.minestom.server.coordinate.Pos;

import java.util.Locale;

public class AuthWorldConfig {

    private final boolean enabled;
    private final String type;
    private final String anvilPath;
    private final Pos spawn;

    public AuthWorldConfig(boolean enabled, String type, String anvilPath, Pos spawn) {
        this.enabled = enabled;
        this.type = type;
        this.anvilPath = anvilPath;
        this.spawn = spawn;
    }

    public static AuthWorldConfig fromConfig(ConfigFile config) {
        boolean enabled = config.getBoolean(MainConfigPaths.AUTH_WORLD_ENABLED);

        String type = config.getString(MainConfigPaths.AUTH_WORLD_TYPE);
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalStateException("auth.world.type is missing in config.yml");
        }
        type = type.trim().toUpperCase(Locale.ROOT);

        String anvilPath = config.getString(MainConfigPaths.AUTH_WORLD_ANVIL_PATH);
        if (anvilPath == null || anvilPath.trim().isEmpty()) {
            throw new IllegalStateException("auth.world.anvil.path is missing in config.yml");
        }

        double x = config.getDouble(MainConfigPaths.AUTH_WORLD_SPAWN_X);
        double y = config.getDouble(MainConfigPaths.AUTH_WORLD_SPAWN_Y);
        double z = config.getDouble(MainConfigPaths.AUTH_WORLD_SPAWN_Z);
        float yaw = (float) config.getDouble(MainConfigPaths.AUTH_WORLD_SPAWN_YAW);
        float pitch = (float) config.getDouble(MainConfigPaths.AUTH_WORLD_SPAWN_PITCH);
        Pos spawn = new Pos(x, y, z, yaw, pitch);

        return new AuthWorldConfig(enabled, type, anvilPath.trim(), spawn);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getType() {
        return type;
    }

    public String getAnvilPath() {
        return anvilPath;
    }

    public Pos getSpawn() {
        return spawn;
    }
}
