package com.oxipro.idcraft.minestom.configuration;

import com.oxipro.cmu.configlang.standalone.config.ConfigFile;
import com.oxipro.idcraft.api.configuration.ConfigsType;
import com.oxipro.idcraft.api.configuration.IConfigManager;
import com.oxipro.idcraft.minestom.IDCraftMinestomServer;
import com.oxipro.idcraft.minestom.configuration.defaultConfigs.DefaultMainConfig;

import java.io.File;

public class ConfigManager implements IConfigManager {

    private final ConfigFile mainConfig;
    private final ConfigFile cssdbConfig;

    public ConfigManager(IDCraftMinestomServer server) {
        File configDir = IDCraftMinestomServer.CONFIG_DIR;
        if (!configDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            configDir.mkdirs();
        }
        this.mainConfig = new DefaultMainConfig(server);
        this.cssdbConfig = new ConfigFile(
                new File(configDir, ConfigsType.CSSDB.filePath),
                server.getResourceAsStream(ConfigsType.CSSDB.filePath)
        );
    }

    @Override
    public ConfigFile getMain() {
        return mainConfig;
    }

    @Override
    public ConfigFile getCssdb() {
        return cssdbConfig;
    }

    @Override
    public void reloadAll() {
        mainConfig.reload();
        cssdbConfig.reload();
    }
}
