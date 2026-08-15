package com.oxipro.idcraft.minestom.auth.factor;

import com.oxipro.idcraft.api.account.IAccountFactorRepository;
import com.oxipro.idcraft.api.auth.AuthErrorCode;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.api.auth.AuthResult;
import com.oxipro.idcraft.api.auth.factor.IAuthFactorHandler;

import java.util.UUID;

public final class TotpAuthFactor implements IAuthFactorHandler {

    private final IAccountFactorRepository factors;

    public TotpAuthFactor(IAccountFactorRepository factors) {
        this.factors = factors;
    }

    @Override
    public AuthFactor kind() {
        return AuthFactor.TWO_FACTOR;
    }

    @Override
    public boolean isEnrolled(UUID uuid) {
        return factors.find(uuid, AuthFactor.TWO_FACTOR) != null;
    }

    @Override
    public AuthResult verifyLogin(UUID uuid, String value) {
        String secret = factors.find(uuid, AuthFactor.TWO_FACTOR);
        if (secret == null) {
            return AuthResult.ok();
        }
        if (value == null || !secret.equals(value.trim())) {
            return AuthResult.failure(AuthErrorCode.FACTOR_INVALID);
        }
        return AuthResult.ok();
    }

    @Override
    public AuthResult enroll(UUID uuid, String value) {
        if (value == null || value.trim().isEmpty()) {
            return AuthResult.failure(AuthErrorCode.FACTOR_REQUIRED);
        }
        factors.save(uuid, AuthFactor.TWO_FACTOR, value.trim());
        return AuthResult.ok();
    }

    @Override
    public void clear(UUID uuid) {
        factors.delete(uuid, AuthFactor.TWO_FACTOR);
    }
}
