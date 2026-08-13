package com.oxipro.idcraft.minestom.auth.factor;

import com.oxipro.idcraft.api.account.IAccountFactorRepository;
import com.oxipro.idcraft.api.auth.AuthErrorCode;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.api.auth.AuthResult;
import com.oxipro.idcraft.api.auth.factor.IAuthFactorHandler;

import java.util.UUID;

// Secret stored plain in DB (must stay recoverable for TOTP verification).
// Real TOTP algorithm can replace verifyLogin later without changing storage.
public class TotpFactorHandler implements IAuthFactorHandler {

    private final IAccountFactorRepository factorRepository;

    public TotpFactorHandler(IAccountFactorRepository factorRepository) {
        this.factorRepository = factorRepository;
    }

    @Override
    public AuthFactor kind() {
        return AuthFactor.TWO_FACTOR;
    }

    @Override
    public boolean isEnrolled(UUID uuid) {
        return factorRepository.find(uuid, AuthFactor.TWO_FACTOR) != null;
    }

    @Override
    public AuthResult verifyLogin(UUID uuid, String value) {
        String secret = factorRepository.find(uuid, AuthFactor.TWO_FACTOR);
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
        factorRepository.save(uuid, AuthFactor.TWO_FACTOR, value.trim());
        return AuthResult.ok();
    }

    @Override
    public void clear(UUID uuid) {
        factorRepository.delete(uuid, AuthFactor.TWO_FACTOR);
    }
}
