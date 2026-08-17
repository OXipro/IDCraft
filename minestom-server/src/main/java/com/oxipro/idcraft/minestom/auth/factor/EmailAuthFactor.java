package com.oxipro.idcraft.minestom.auth.factor;

import com.oxipro.idcraft.api.account.IAccountFactorRepository;
import com.oxipro.idcraft.api.auth.AuthErrorCode;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.api.auth.AuthResult;
import com.oxipro.idcraft.api.auth.factor.IAuthFactorHandler;

import java.util.UUID;
import java.util.regex.Pattern;

public final class EmailAuthFactor implements IAuthFactorHandler {

    private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final IAccountFactorRepository factors;

    public EmailAuthFactor(IAccountFactorRepository factors) {
        this.factors = factors;
    }

    @Override
    public AuthFactor kind() {
        return AuthFactor.EMAIL;
    }

    @Override
    public boolean isEnrolled(UUID uuid) {
        return factors.find(uuid, AuthFactor.EMAIL) != null;
    }

    @Override
    public AuthResult verifyLogin(UUID uuid, String value) {
        return AuthResult.ok();
    }

    @Override
    public AuthResult enroll(UUID uuid, String value) {
        if (value == null || value.trim().isEmpty()) {
            return AuthResult.failure(AuthErrorCode.FACTOR_REQUIRED);
        }
        String email = value.trim();
        if (!SIMPLE_EMAIL.matcher(email).matches()) {
            return AuthResult.failure(AuthErrorCode.FACTOR_INVALID);
        }
        UUID existing = factors.findUuidByEmail(email);
        if (existing != null && !existing.equals(uuid)) {
            return AuthResult.failure(AuthErrorCode.FACTOR_INVALID);
        }
        factors.save(uuid, AuthFactor.EMAIL, email);
        return AuthResult.ok();
    }

    @Override
    public void clear(UUID uuid) {
        factors.delete(uuid, AuthFactor.EMAIL);
    }
}
