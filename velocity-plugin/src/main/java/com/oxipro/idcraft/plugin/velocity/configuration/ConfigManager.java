package com.oxipro.idcraft.plugin.velocity.configuration;

import com.oxipro.cmu.configlang.standalone.config.ConfigFile;
import com.oxipro.idcraft.api.configuration.ConfigsType;
import com.oxipro.idcraft.api.configuration.IConfigManager;
import com.oxipro.idcraft.plugin.velocity.IDCraftVelocityPlugin;
import com.oxipro.idcraft.plugin.velocity.configuration.defaultConfigs.DefaultMainConfig;

import java.io.File;

public class ConfigManager implements IConfigManager {

    private final ConfigFile mainConfig;
    private final ConfigFile cssdbConfig;

    public ConfigManager(IDCraftVelocityPlugin plugin) {
        this.mainConfig = new DefaultMainConfig(plugin);
        this.cssdbConfig = new ConfigFile(
                new File(plugin.getPluginData().toFile(), ConfigsType.CSSDB.filePath),
                plugin.getResourceAsStream(ConfigsType.CSSDB.filePath)
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
