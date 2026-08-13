package com.oxipro.idcraft.core.support.servername;

import com.oxipro.idcraft.api.support.servername.IServerNameProvider;

public class EnvServerNameProvider implements IServerNameProvider {
    private final String envVar;

    public EnvServerNameProvider(String envVar) {
        this.envVar = envVar;
    }

    @Override
    public String getCurrentServerName() {
        String name = System.getenv(envVar);
        if (name == null) {
            throw new IllegalStateException("Environment variable " + envVar + " is not set");
        }
        return name;
    }
}
