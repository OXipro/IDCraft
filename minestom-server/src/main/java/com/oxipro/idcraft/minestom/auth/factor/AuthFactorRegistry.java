package com.oxipro.idcraft.minestom.auth.factor;

import com.oxipro.idcraft.api.account.IAccountFactorRepository;
import com.oxipro.idcraft.api.account.IAccountRepository;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.api.auth.IAuthManager;
import com.oxipro.idcraft.api.auth.factor.IAuthFactorHandler;
import com.oxipro.idcraft.minestom.auth.prompt.AuthPromptConfig;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class AuthFactorRegistry {

    private final Map<AuthFactor, IAuthFactorHandler> handlers = new EnumMap<>(AuthFactor.class);

    public AuthFactorRegistry(
            AuthPromptConfig config,
            IAuthManager authManager,
            IAccountRepository accounts,
            IAccountFactorRepository factors
    ) {
        handlers.put(AuthFactor.PASSWORD, new PasswordAuthFactor(authManager, accounts));
        if (config.isFactorEnabled(AuthFactor.EMAIL)) {
            handlers.put(AuthFactor.EMAIL, new EmailAuthFactor(factors));
        }
        if (config.isFactorEnabled(AuthFactor.TWO_FACTOR)) {
            handlers.put(AuthFactor.TWO_FACTOR, new TotpAuthFactor(factors));
        }
    }

    public IAuthFactorHandler get(AuthFactor factor) {
        return handlers.get(factor);
    }

    public Collection<IAuthFactorHandler> handlers() {
        return handlers.values();
    }

    public boolean has(AuthFactor factor) {
        return handlers.containsKey(factor);
    }
}
