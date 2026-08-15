package com.oxipro.idcraft.minestom.auth.desk;

import com.oxipro.cmu.configlang.standalone.config.ConfigFile;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.minestom.configuration.paths.MainConfigPaths;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class AccountDeskConfig {

    private final boolean enabled;
    private final Set<AuthFactor> manage;
    private final Set<AuthFactor> requireCurrentPassword;

    private AccountDeskConfig(boolean enabled, Set<AuthFactor> manage, Set<AuthFactor> requireCurrentPassword) {
        this.enabled = enabled;
        this.manage = manage;
        this.requireCurrentPassword = requireCurrentPassword;
    }

    public static AccountDeskConfig fromConfig(ConfigFile config) {
        Set<AuthFactor> manage = parseFactors(config.getStringList(MainConfigPaths.ACCOUNT_DESK_MANAGE));
        Set<AuthFactor> requireCurrent = parseFactors(config.getStringList(MainConfigPaths.ACCOUNT_DESK_REQUIRE_CURRENT_PASSWORD));
        return new AccountDeskConfig(
                config.getBoolean(MainConfigPaths.ACCOUNT_DESK_ENABLED),
                manage,
                requireCurrent
        );
    }

    private static Set<AuthFactor> parseFactors(List<String> raw) {
        Set<AuthFactor> result = EnumSet.noneOf(AuthFactor.class);
        if (raw == null) {
            return result;
        }
        for (String key : raw) {
            AuthFactor factor = AuthFactor.fromConfigKey(key);
            if (factor != null) {
                result.add(factor);
            }
        }
        return result;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean canManage(AuthFactor factor) {
        return factor != null && manage.contains(factor);
    }

    public boolean requiresCurrentPassword(AuthFactor factor) {
        return factor != null && requireCurrentPassword.contains(factor);
    }
}
