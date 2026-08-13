package com.oxipro.idcraft.core.support.servername;

import com.oxipro.idcraft.api.support.servername.IServerNameProvider;

public class ConfigServerNameProvider implements IServerNameProvider {

    private final String serverName;

    public ConfigServerNameProvider(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public String getCurrentServerName() {
        return serverName;
    }
}
