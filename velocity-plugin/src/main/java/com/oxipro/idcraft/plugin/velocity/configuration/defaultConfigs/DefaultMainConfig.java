package com.oxipro.idcraft.plugin.velocity.configuration.defaultConfigs;

import com.oxipro.cmu.configlang.standalone.config.ConfigFile;
import com.oxipro.idcraft.api.configuration.ConfigsType;
import com.oxipro.idcraft.core.configuration.defaultConfigs.CommonDefaultMainConfig;
import com.oxipro.idcraft.core.configuration.paths.CommonMainConfigPaths;
import com.oxipro.idcraft.plugin.velocity.IDCraftVelocityPlugin;
import com.oxipro.idcraft.plugin.velocity.configuration.paths.MainConfigPaths;

import java.io.File;
import java.util.List;

public class DefaultMainConfig extends ConfigFile {

    public DefaultMainConfig(IDCraftVelocityPlugin plugin) {
        super(
                new File(plugin.getPluginData().toFile(), ConfigsType.CONFIG.filePath),
                plugin.getResourceAsStream(ConfigsType.CONFIG.filePath)
        );
        setDefaults();
    }

    public void setDefaults() {
        CommonDefaultMainConfig.applyCommonDefaults(this);

        addDefault(CommonMainConfigPaths.SERVER_NAME_VALUE, "proxy-1");

        addDefault(MainConfigPaths.AUTH_SERVERS_LB, "NONE");
        addDefault(MainConfigPaths.AUTH_SERVERS_PROVIDER, "CONFIG");
        addDefault(MainConfigPaths.AUTH_SERVERS_SHULKER_TAG, "idcraft-auth");
        addDefault(MainConfigPaths.AUTH_SERVERS_CONFIG, List.of("idcraft-auth-1"));

        addDefault(MainConfigPaths.FORWARDING_PREMIUM, "VELOCITY_REPLICATION");
        addDefault(MainConfigPaths.FORWARDING_FLOODGATE, "VELOCITY_REPLICATION");
        addDefault(MainConfigPaths.FORWARDING_CRACKS, "VELOCITY_REPLICATION");
    }
}
