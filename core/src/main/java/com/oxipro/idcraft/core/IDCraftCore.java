package com.oxipro.idcraft.core;

import com.oxipro.cmu.configlang.api.config.IConfigFile;
import com.oxipro.cssdb.CSSDB;
import com.oxipro.cssdb.config.DBConfig;
import com.oxipro.cssdb.database.Database;
import com.oxipro.cssdb.database.DatabaseFactory;
import com.oxipro.idcraft.api.account.IAccountFactorRepository;
import com.oxipro.idcraft.api.account.IAccountRepository;
import com.oxipro.idcraft.api.auth.IAuthManager;
import com.oxipro.idcraft.api.configuration.IConfigManager;
import com.oxipro.idcraft.api.mojang.IMojangVerifier;
import com.oxipro.idcraft.api.password.IPasswordHasher;
import com.oxipro.idcraft.api.session.ICacheProvider;
import com.oxipro.idcraft.api.support.servername.IServerNameProvider;
import com.oxipro.idcraft.api.support.servername.ServerNameSourceType;
import com.oxipro.idcraft.core.auth.AuthManager;
import com.oxipro.idcraft.core.auth.AuthManagerConfig;
import com.oxipro.idcraft.core.configuration.paths.CommonMainConfigPaths;
import com.oxipro.idcraft.core.logging.StartSummary;
import com.oxipro.idcraft.core.mojang.HttpMojangVerifier;
import com.oxipro.idcraft.core.password.Argon2PasswordHasher;
import com.oxipro.idcraft.core.support.servername.ConfigServerNameProvider;
import com.oxipro.idcraft.core.support.servername.EnvServerNameProvider;
import com.oxipro.idcraft.db.mysql.MySqlAccountFactorRepository;
import com.oxipro.idcraft.db.mysql.MySqlAccountRepository;
import com.oxipro.idcraft.db.mysql.MySqlConfig;
import com.oxipro.idcraft.db.mysql.MySqlDataSourceFactory;
import com.oxipro.idcraft.db.postgres.PostgresAccountFactorRepository;
import com.oxipro.idcraft.db.postgres.PostgresAccountRepository;
import com.oxipro.idcraft.db.postgres.PostgresConfig;
import com.oxipro.idcraft.db.postgres.PostgresDataSourceFactory;
import com.oxipro.idcraft.redis.RedisConnectionConfig;
import com.oxipro.idcraft.redis.providers.RedisCacheProvider;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class IDCraftCore {

    private final Logger logger;
    private final IConfigManager configManager;
    private final IConfigFile mainConfig;

    private IServerNameProvider serverNameProvider;
    private CSSDB cssdb;
    private ICacheProvider cache;
    private IAccountRepository accountRepository;
    private IAccountFactorRepository accountFactorRepository;
    private IMojangVerifier mojangVerifier;
    private IPasswordHasher passwordHasher;
    private AuthManagerConfig authManagerConfig;
    private IAuthManager authManager;
    private HikariDataSource accountDataSource;

    public IDCraftCore(Logger logger, IConfigManager configManager) {
        this.logger = logger;
        this.configManager = configManager;
        this.mainConfig = configManager.getMain();
    }

    public boolean init() {
        if (!initServerNameProvider()) {
            return false;
        }
        if (!initCSSDB()) {
            return false;
        }
        if (!initCache()) {
            return false;
        }
        if (!initAccountRepository()) {
            return false;
        }
        initMojangVerifier();
        initAuthManager();
        return true;
    }

    public void shutdown() {
        if (cache != null) {
            try {
                cache.disconnect();
            } catch (Exception e) {
                logger.warn("Failed to disconnect cache", e);
            }
        }
        if (accountDataSource != null) {
            try {
                accountDataSource.close();
            } catch (Exception e) {
                logger.warn("Failed to close account datasource", e);
            }
        }
    }

    private boolean initServerNameProvider() {
        String rawServerNameType = mainConfig.getString(CommonMainConfigPaths.SERVER_NAME_TYPE);
        if (rawServerNameType == null) {
            logger.error("No server-name.type set in config.yml");
            return false;
        }

        ServerNameSourceType type;
        try {
            type = ServerNameSourceType.valueOf(rawServerNameType.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.error("Unknown server name source: " + rawServerNameType);
            return false;
        }

        switch (type) {
            case ENV:
                this.serverNameProvider = new EnvServerNameProvider(mainConfig.getString(CommonMainConfigPaths.SERVER_NAME_ENV_VAR));
                break;
            case CONFIG:
                this.serverNameProvider = new ConfigServerNameProvider(mainConfig.getString(CommonMainConfigPaths.SERVER_NAME_VALUE));
                break;
            default:
                logger.error("Unknown server name source: " + rawServerNameType);
                return false;
        }

        return true;
    }

    private boolean initCSSDB() {
        try {
            DBConfig cssdbConfig = DBConfig.fromConfigFile(configManager.getCssdb());
            Database cssDB = DatabaseFactory.create(cssdbConfig);
            cssdb = new CSSDB(cssDB);
            cssdb.init();
            return true;
        } catch (Exception e) {
            logger.error("Failed to init CSSDB", e);
            return false;
        }
    }

    private boolean initCache() {
        String provider = mainConfig.getString(CommonMainConfigPaths.CACHE_PROVIDER);
        if (provider == null) {
            logger.error("cache.provider is missing in config.yml");
            return false;
        }

        switch (provider.toLowerCase()) {
            case "redis":
                String host = mainConfig.getString(CommonMainConfigPaths.CACHE_REDIS_HOST);
                int port = mainConfig.getInt(CommonMainConfigPaths.CACHE_REDIS_PORT);
                String user = mainConfig.getString(CommonMainConfigPaths.CACHE_REDIS_USER);
                String password = mainConfig.getString(CommonMainConfigPaths.CACHE_REDIS_PASSWORD);
                this.cache = new RedisCacheProvider(new RedisConnectionConfig(host, port, user, password));
                return true;
//            case "shulker":
//                this.cache = ShulkerRedisSessionCacheFactory.create();
//                return true;
            default:
                logger.error("Unknown cache.provider: " + provider);
                return false;
        }
    }

    private boolean initAccountRepository() {
        String provider = mainConfig.getString(CommonMainConfigPaths.DATABASE_PROVIDER);
        String host = mainConfig.getString(CommonMainConfigPaths.DATABASE_HOST);
        int port = mainConfig.getInt(CommonMainConfigPaths.DATABASE_PORT);
        String name = mainConfig.getString(CommonMainConfigPaths.DATABASE_NAME);
        String user = mainConfig.getString(CommonMainConfigPaths.DATABASE_USER);
        String password = mainConfig.getString(CommonMainConfigPaths.DATABASE_PASSWORD);

        if (provider == null) {
            logger.error("database.provider is missing in config.yml");
            return false;
        }

        try {
            switch (provider.toLowerCase()) {
                case "mysql":
                    accountDataSource = MySqlDataSourceFactory.create(new MySqlConfig(host, port, name, user, password, 10));
                    MySqlDataSourceFactory.applySchema(accountDataSource);
                    this.accountRepository = new MySqlAccountRepository(accountDataSource);
                    this.accountFactorRepository = new MySqlAccountFactorRepository(accountDataSource);
                    return true;
                case "postgres":
                    accountDataSource = PostgresDataSourceFactory.create(PostgresConfig.defaults(host, name, user, password));
                    PostgresDataSourceFactory.applySchema(accountDataSource);
                    this.accountRepository = new PostgresAccountRepository(accountDataSource);
                    this.accountFactorRepository = new PostgresAccountFactorRepository(accountDataSource);
                    return true;
                default:
                    logger.error("Unknown database.provider: " + provider);
                    return false;
            }
        } catch (Exception e) {
            logger.error("Failed to init account repository", e);
            return false;
        }
    }

    private void initMojangVerifier() {
        boolean enableCaching = mainConfig.getBoolean(CommonMainConfigPaths.AUTH_PREMIUM_CACHING_ENABLED);
        long positiveTtl = mainConfig.getLong(CommonMainConfigPaths.AUTH_PREMIUM_CACHING_POSITIVE_TTL);
        long negativeTtl = mainConfig.getLong(CommonMainConfigPaths.AUTH_PREMIUM_CACHING_NEGATIVE_TTL);

        mojangVerifier = new HttpMojangVerifier(enableCaching, Duration.ofSeconds(positiveTtl), Duration.ofSeconds(negativeTtl));
    }

    private void initAuthManager() {
        this.passwordHasher = new Argon2PasswordHasher();
        this.authManagerConfig = AuthManagerConfig.fromConfig(mainConfig).build();
        authManager = new AuthManager(
                accountRepository,
                cache,
                mojangVerifier,
                passwordHasher,
                authManagerConfig
        );
    }

    public IServerNameProvider getServerNameProvider() {
        return serverNameProvider;
    }

    public CSSDB getCssdb() {
        return cssdb;
    }

    public ICacheProvider getCache() {
        return cache;
    }

    public IAccountRepository getAccountRepository() {
        return accountRepository;
    }

    public IAccountFactorRepository getAccountFactorRepository() {
        return accountFactorRepository;
    }

    public IAuthManager getAuthManager() {
        return authManager;
    }

    public AuthManagerConfig getAuthManagerConfig() {
        return authManagerConfig;
    }

    public IMojangVerifier getMojangVerifier() {
        return mojangVerifier;
    }

    public IPasswordHasher getPasswordHasher() {
        return passwordHasher;
    }

    public List<String> commonStartSummaryLines() {
        List<String> lines = new ArrayList<>();
        String serverName = serverNameProvider == null ? "N/A" : serverNameProvider.getCurrentServerName();
        lines.add("Server name: '" + serverName + "' (" + StartSummary.className(serverNameProvider) + ")");
        lines.add("Cache: " + providerLabel(CommonMainConfigPaths.CACHE_PROVIDER) + " (" + StartSummary.className(cache) + ")");
        lines.add("Database: " + providerLabel(CommonMainConfigPaths.DATABASE_PROVIDER)
                + " (" + StartSummary.className(accountRepository) + ")");
        lines.add("CSSDB: " + StartSummary.className(cssdb) + " (" + StartSummary.packageVersion(CSSDB.class) + ")");
        lines.add("Mojang verifier: " + StartSummary.className(mojangVerifier));
        lines.add("Password hasher: " + StartSummary.className(passwordHasher));

        if (authManagerConfig != null) {
            lines.add("Auth mode: " + authManagerConfig.getAuthMode());
            lines.add("Session: enabled=" + authManagerConfig.getUseSessions()
                    + ", ttl=" + authManagerConfig.getSessionTtl().toMinutes() + "m"
                    + ", crack=" + authManagerConfig.isSessionSupportCrack()
                    + ", premium=" + authManagerConfig.isSessionSupportPremium());
            lines.add("Premium skip-auth: " + authManagerConfig.getSkipAuthPremium());
            lines.add("Floodgate skip-auth: " + authManagerConfig.getSkipAuthFloodgate());
            lines.add("Crack allow-on-premium-username: " + authManagerConfig.isAllowOnPremiumUsername());
            lines.add("Login cooldown: enabled=" + authManagerConfig.isLoginCooldownEnabled()
                    + ", maxFailed=" + authManagerConfig.getMaxFailedAttempts()
                    + ", duration=" + authManagerConfig.getLoginCooldown().toMinutes() + "m");
        }
        return lines;
    }

    private String providerLabel(String path) {
        String raw = mainConfig.getString(path);
        return raw == null || raw.isBlank() ? "N/A" : raw;
    }
}
