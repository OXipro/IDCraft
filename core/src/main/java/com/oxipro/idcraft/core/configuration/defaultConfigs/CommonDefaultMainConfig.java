package com.oxipro.idcraft.core.configuration.defaultConfigs;

import com.oxipro.cmu.configlang.api.config.IConfigFile;
import com.oxipro.idcraft.core.configuration.paths.CommonMainConfigPaths;
import org.slf4j.event.Level;

public class CommonDefaultMainConfig {

    public static void applyCommonDefaults(IConfigFile c) {
        c.addDefault(CommonMainConfigPaths.SERVER_NAME_TYPE, "CONFIG");
        c.addDefault(CommonMainConfigPaths.SERVER_NAME_ENV_VAR, "HOSTNAME");
        c.addDefault(CommonMainConfigPaths.SERVER_NAME_VALUE, "server-1");

        c.addDefault(CommonMainConfigPaths.DATABASE_PROVIDER, "postgres");
        c.addDefault(CommonMainConfigPaths.DATABASE_HOST, "localhost");
        c.addDefault(CommonMainConfigPaths.DATABASE_PORT, 5432);
        c.addDefault(CommonMainConfigPaths.DATABASE_NAME, "idcraft");
        c.addDefault(CommonMainConfigPaths.DATABASE_USER, "idcraft");
        c.addDefault(CommonMainConfigPaths.DATABASE_PASSWORD, "");

        c.addDefault(CommonMainConfigPaths.CACHE_PROVIDER, "REDIS");
        c.addDefault(CommonMainConfigPaths.CACHE_REDIS_HOST, "localhost");
        c.addDefault(CommonMainConfigPaths.CACHE_REDIS_PORT, 6379);
        c.addDefault(CommonMainConfigPaths.CACHE_REDIS_USER, "default");
        c.addDefault(CommonMainConfigPaths.CACHE_REDIS_PASSWORD, "CHANGEME");

        c.addDefault(CommonMainConfigPaths.MESSAGING_PROVIDER, "BUNGEE_PLUGIN");
        c.addDefault(CommonMainConfigPaths.MESSAGING_REDIS_HOST, "localhost");
        c.addDefault(CommonMainConfigPaths.MESSAGING_REDIS_PORT, 6379);
        c.addDefault(CommonMainConfigPaths.MESSAGING_REDIS_USER, "default");
        c.addDefault(CommonMainConfigPaths.MESSAGING_REDIS_PASSWORD, "CHANGEME");

        c.addDefault(CommonMainConfigPaths.AUTH_MODE, "MIXED");
        c.addDefault(CommonMainConfigPaths.AUTH_SESSION_ENABLED, true);
        c.addDefault(CommonMainConfigPaths.AUTH_SESSION_TTL, 1440L);
        c.addDefault(CommonMainConfigPaths.AUTH_SESSION_SUPPORT_CRACK, true);
        c.addDefault(CommonMainConfigPaths.AUTH_SESSION_SUPPORT_PREMIUM, true);
        c.addDefault(CommonMainConfigPaths.AUTH_CRACK_ALLOW_ON_PREMIUM_USERNAME, false);
        c.addDefault(CommonMainConfigPaths.AUTH_PREMIUM_SKIP_AUTH, true);
        c.addDefault(CommonMainConfigPaths.AUTH_FLOODGATE_SKIP_AUTH, true);
        c.addDefault(CommonMainConfigPaths.AUTH_PREMIUM_CACHING_ENABLED, true);
        c.addDefault(CommonMainConfigPaths.AUTH_PREMIUM_CACHING_POSITIVE_TTL, 3600L);
        c.addDefault(CommonMainConfigPaths.AUTH_PREMIUM_CACHING_NEGATIVE_TTL, 600L);

        c.addDefault(CommonMainConfigPaths.AUTH_PASSWORD_MIN_LENGTH, 4);
        c.addDefault(CommonMainConfigPaths.AUTH_PASSWORD_REGEX_ENABLED, false);
        c.addDefault(CommonMainConfigPaths.AUTH_PASSWORD_REGEX_EXPRESSION,
                "^(?=.{12,128}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");
        c.addDefault(CommonMainConfigPaths.AUTH_LOGIN_COOLDOWN_ENABLED, true);
        c.addDefault(CommonMainConfigPaths.AUTH_LOGIN_COOLDOWN_MAX_FAILED, 5);
        c.addDefault(CommonMainConfigPaths.AUTH_LOGIN_COOLDOWN_MINUTES, 15L);

        c.addDefault(CommonMainConfigPaths.LOGGER_LEVEL, Level.INFO.toString());
        c.addDefault(CommonMainConfigPaths.LOGGER_SUMMARY, true);

        c.addDefault(CommonMainConfigPaths.LANGUAGE_CLIENT_LOCALE, true);
        c.addDefault(CommonMainConfigPaths.LANGUAGE_IP, true);
        c.addDefault(CommonMainConfigPaths.LANGUAGE_FALLBACK, "en_US");
    }
}
