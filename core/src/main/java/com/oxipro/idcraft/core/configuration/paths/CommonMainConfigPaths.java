package com.oxipro.idcraft.core.configuration.paths;

public class CommonMainConfigPaths {
    // Server
    public static final String SERVER_NAME_TYPE = "server-name.type";
    public static final String SERVER_NAME_ENV_VAR = "server-name.env-var";
    public static final String SERVER_NAME_VALUE = "server-name.value";

    // db
    public static final String DATABASE_PROVIDER = "database.provider";
    public static final String DATABASE_HOST = "database.host";
    public static final String DATABASE_PORT = "database.port";
    public static final String DATABASE_NAME = "database.name";
    public static final String DATABASE_USER = "database.user";
    public static final String DATABASE_PASSWORD = "database.password";

    // Cache
    public static final String CACHE_PROVIDER = "cache.provider";
    public static final String CACHE_REDIS_HOST = "cache.redis.host";
    public static final String CACHE_REDIS_PORT = "cache.redis.port";
    public static final String CACHE_REDIS_USER = "cache.redis.user";
    public static final String CACHE_REDIS_PASSWORD = "cache.redis.password";

    // messaging
    public static final String MESSAGING_PROVIDER = "messaging.provider";
    public static final String MESSAGING_REDIS_HOST = "messaging.redis.host";
    public static final String MESSAGING_REDIS_PORT = "messaging.redis.port";
    public static final String MESSAGING_REDIS_USER = "messaging.redis.user";
    public static final String MESSAGING_REDIS_PASSWORD = "messaging.redis.password";

    // Auth mode: CRACKED | MIXED | RANDOM
    public static final String AUTH_MODE = "auth.mode";

    public static final String AUTH_SESSION_ENABLED = "auth.session.enabled";
    public static final String AUTH_SESSION_TTL = "auth.session.ttl";
    public static final String AUTH_SESSION_SUPPORT_CRACK = "auth.session.support.crack";
    public static final String AUTH_SESSION_SUPPORT_PREMIUM = "auth.session.support.premium";

    public static final String AUTH_CRACK_ALLOW_ON_PREMIUM_USERNAME = "auth.crack.allow-on-premium-username";
    public static final String AUTH_PREMIUM_SKIP_AUTH = "auth.premium.skip-auth";
    public static final String AUTH_FLOODGATE_SKIP_AUTH = "auth.floodgate.skip-auth";

    public static final String AUTH_PREMIUM_CACHING_ENABLED = "auth.premium.caching.enabled";
    public static final String AUTH_PREMIUM_CACHING_POSITIVE_TTL = "auth.premium.caching.ttl.positive";
    public static final String AUTH_PREMIUM_CACHING_NEGATIVE_TTL = "auth.premium.caching.ttl.negative";

    // Password policy (auth-server)
    public static final String AUTH_PASSWORD_MIN_LENGTH = "auth.methods.password.min-length";
    public static final String AUTH_PASSWORD_REGEX_ENABLED = "auth.methods.password.regex.enabled";
    public static final String AUTH_PASSWORD_REGEX_EXPRESSION = "auth.methods.password.regex.expression";

    public static final String AUTH_LOGIN_COOLDOWN_ENABLED = "auth.methods.login-cooldown.enabled";
    public static final String AUTH_LOGIN_COOLDOWN_MAX_FAILED = "auth.methods.login-cooldown.max-failed-attempts";
    public static final String AUTH_LOGIN_COOLDOWN_MINUTES = "auth.methods.login-cooldown.cooldown";

}
