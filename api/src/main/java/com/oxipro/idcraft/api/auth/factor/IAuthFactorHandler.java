package com.oxipro.idcraft.api.auth.factor;

import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.api.auth.AuthResult;

import java.util.UUID;

public interface IAuthFactorHandler {

    AuthFactor kind();

    boolean isEnrolled(UUID uuid);

    AuthResult verifyLogin(UUID uuid, String value);

    AuthResult enroll(UUID uuid, String value);

    void clear(UUID uuid);

    default AuthResult unenroll(UUID uuid) {
        clear(uuid);
        return AuthResult.ok();
    }
}
