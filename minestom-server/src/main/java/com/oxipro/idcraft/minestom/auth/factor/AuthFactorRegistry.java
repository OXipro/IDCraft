package com.oxipro.idcraft.minestom.auth.factor;

import com.oxipro.idcraft.api.account.IAccountFactorRepository;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.api.auth.factor.IAuthFactorHandler;
import com.oxipro.idcraft.minestom.auth.prompt.AuthPromptConfig;

import java.util.EnumMap;
import java.util.Map;

public class AuthFactorRegistry {

    private final Map<AuthFactor, IAuthFactorHandler> handlers = new EnumMap<>(AuthFactor.class);

    public AuthFactorRegistry(AuthPromptConfig config, IAccountFactorRepository factorRepository) {
        if (factorRepository == null) {
            throw new IllegalStateException("IAccountFactorRepository is required for auth factors");
        }
        if (config.isFactorEnabled(AuthFactor.EMAIL)) {
            handlers.put(AuthFactor.EMAIL, new EmailFactorHandler(factorRepository));
        }
        if (config.isFactorEnabled(AuthFactor.TWO_FACTOR)) {
            handlers.put(AuthFactor.TWO_FACTOR, new TotpFactorHandler(factorRepository));
        }
    }

    public IAuthFactorHandler get(AuthFactor factor) {
        return handlers.get(factor);
    }

    public boolean has(AuthFactor factor) {
        return handlers.containsKey(factor);
    }
}
