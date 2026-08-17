package com.oxipro.idcraft.minestom.auth.factor;

import com.oxipro.idcraft.api.account.IAccountRepository;
import com.oxipro.idcraft.api.auth.AuthErrorCode;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.api.auth.AuthResult;
import com.oxipro.idcraft.api.auth.IAuthManager;
import com.oxipro.idcraft.api.auth.factor.IAuthFactorHandler;

import java.util.UUID;

public final class PasswordAuthFactor implements IAuthFactorHandler {

    private final IAuthManager authManager;
    private final IAccountRepository accounts;

    public PasswordAuthFactor(IAuthManager authManager, IAccountRepository accounts) {
        this.authManager = authManager;
        this.accounts = accounts;
    }

    @Override
    public AuthFactor kind() {
        return AuthFactor.PASSWORD;
    }

    @Override
    public boolean isEnrolled(UUID uuid) {
        return accounts.findByUuid(uuid) != null;
    }

    @Override
    public AuthResult verifyLogin(UUID uuid, String value) {
        if (authManager.verifyPassword(uuid, value)) {
            return AuthResult.ok();
        }
        return AuthResult.failure(AuthErrorCode.WRONG_PASSWORD);
    }

    @Override
    public AuthResult enroll(UUID uuid, String value) {
        return AuthResult.failure(AuthErrorCode.FACTOR_REQUIRED);
    }

    @Override
    public void clear(UUID uuid) {
    }

    @Override
    public AuthResult unenroll(UUID uuid) {
        return AuthResult.failure(AuthErrorCode.FACTOR_REQUIRED);
    }
}
