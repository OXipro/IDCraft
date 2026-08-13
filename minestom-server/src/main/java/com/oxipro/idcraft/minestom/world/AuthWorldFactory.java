package com.oxipro.idcraft.minestom.world;

import com.oxipro.cmu.configlang.standalone.config.ConfigFile;
import com.oxipro.idcraft.minestom.world.providers.AnvilAuthWorldProvider;
import com.oxipro.idcraft.minestom.world.providers.VoidAuthWorldProvider;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class AuthWorldFactory {

    private final Map<String, Function<AuthWorldConfig, IAuthWorldProvider>> providers = new HashMap<>();

    public AuthWorldFactory() {
        register("ANVIL", AnvilAuthWorldProvider::new);
        register("VOID", VoidAuthWorldProvider::new);
    }

    public void register(String type, Function<AuthWorldConfig, IAuthWorldProvider> factory) {
        providers.put(type.toUpperCase(Locale.ROOT), factory);
    }

    public IAuthWorldProvider create(ConfigFile config) {
        AuthWorldConfig worldConfig = AuthWorldConfig.fromConfig(config);

        if (!worldConfig.isEnabled()) {
            return new VoidAuthWorldProvider(worldConfig);
        }

        Function<AuthWorldConfig, IAuthWorldProvider> factory = providers.get(worldConfig.getType());
        if (factory == null) {
            throw new IllegalStateException(
                    "Unknown auth.world.type: " + worldConfig.getType()
                            + " (registered: " + providers.keySet() + ")");
        }
        return factory.apply(worldConfig);
    }
}
