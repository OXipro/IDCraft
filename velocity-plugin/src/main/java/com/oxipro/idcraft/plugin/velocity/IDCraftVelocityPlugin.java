package com.oxipro.idcraft.plugin.velocity;

import com.google.inject.Inject;
import com.oxipro.cmu.configlang.api.language.ILanguage;
import com.oxipro.cmu.configlang.api.language.LanguageSettings;
import com.oxipro.cmu.configlang.standalone.config.ConfigFile;
import com.oxipro.cmu.configlang.velocity.ConfigLang;
import com.oxipro.cmu.configlang.velocity.language.LanguageManager;
import com.oxipro.idcraft.api.messaging.IMessagingProvider;
import com.oxipro.idcraft.api.platform.PlatformType;
import com.oxipro.idcraft.core.IDCraft;
import com.oxipro.idcraft.core.IDCraftCore;
import com.oxipro.idcraft.core.configuration.paths.CommonMainConfigPaths;
import com.oxipro.idcraft.core.logging.StartSummary;
import com.oxipro.idcraft.plugin.velocity.auth.PlayerAuthContext;
import com.oxipro.idcraft.plugin.velocity.authservers.AuthServersManager;
import com.oxipro.idcraft.plugin.velocity.commands.TabCompletionDisabler;
import com.oxipro.idcraft.plugin.velocity.configuration.ConfigManager;
import com.oxipro.idcraft.plugin.velocity.configuration.paths.MainConfigPaths;
import com.oxipro.idcraft.plugin.velocity.forwarding.ForwardingManager;
import com.oxipro.idcraft.plugin.velocity.language.defaultLanguage.English;
import com.oxipro.idcraft.plugin.velocity.listeners.VelocityLoginListeners;
import com.oxipro.idcraft.plugin.velocity.listeners.VelocityServerListeners;
import com.oxipro.idcraft.plugin.velocity.messaging.BungeePluginMessagingProvider;
import com.oxipro.idcraft.plugin.velocity.utils.MessageUtil;
import com.oxipro.idcraft.redis.RedisConnectionConfig;
import com.oxipro.idcraft.redis.providers.RedisMessagingProvider;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.geysermc.floodgate.api.FloodgateApi;
import org.slf4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class IDCraftVelocityPlugin implements IDCraft {

    private final Logger logger;
    private final IDCraftVelocityPlugin plugin;
    private final ProxyServer server;
    private final Path pluginData;

    private IDCraftCore core;
    private ConfigManager configManager;
    private ConfigFile mainConfig;
    private ConfigLang configLang;
    private LanguageManager languageManager;
    private IMessagingProvider messagingProvider;
    private FloodgateApi floodgateApi;
    private MessageUtil messageUtil;
    private AuthServersManager authServersManager;
    private PlayerAuthContext playerAuthContext;
    private ForwardingManager forwardingManager;

    @Inject
    public IDCraftVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path pluginData) {
        this.logger = logger;
        this.plugin = this;
        this.server = server;
        this.pluginData = pluginData;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        if (!checkDeps()) {
            return;
        }
        if (!initConfig()) {
            return;
        }

        this.core = new IDCraftCore(logger, configManager);
        if (!core.init()) {
            logger.error("IDCraft core failed to initialize");
            return;
        }

        if (!initMessagingProvider()) {
            logger.error("Messaging provider failed to initialize");
            return;
        }

        if (!initConfigLang()) {
            logger.error("Language system failed to initialize");
            return;
        }

        initUtils();
        initManagers();
        registerAll();
        startSummary();
        logger.info("IDCraft Velocity plugin enabled");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (messagingProvider != null) {
            messagingProvider.disconnect();
        }
        if (core != null) {
            core.shutdown();
        }
        logger.info("IDCraft has been shut down");
    }

    public boolean checkDeps() {
        boolean floodgatePresent = server.getPluginManager()
                .getPlugin("floodgate")
                .isPresent();

        if (floodgatePresent) {
            floodgateApi = FloodgateApi.getInstance();
        } else {
            logger.info("Floodgate API is not available");
        }
        return true;
    }

    public boolean initConfig() {
        this.configManager = new ConfigManager(plugin);
        this.mainConfig = configManager.getMain();
        String levelRaw = mainConfig.getString(CommonMainConfigPaths.LOGGER_LEVEL);
        Level level = levelRaw == null ? Level.INFO : Level.valueOf(levelRaw.toUpperCase(Locale.ROOT));
        Configurator.setLevel("com.oxipro.idcraft", level);
        return true;
    }

    public boolean initMessagingProvider() {
        String provider = mainConfig.getString(CommonMainConfigPaths.MESSAGING_PROVIDER);
        if (provider == null) {
            logger.error("messaging.provider is missing");
            return false;
        }

        switch (provider.toLowerCase(Locale.ROOT)) {
            case "bungee_plugin":
                this.messagingProvider = new BungeePluginMessagingProvider(plugin);
                return true;
            case "redis":
                String host = mainConfig.getString(CommonMainConfigPaths.MESSAGING_REDIS_HOST);
                int port = mainConfig.getInt(CommonMainConfigPaths.MESSAGING_REDIS_PORT);
                String user = mainConfig.getString(CommonMainConfigPaths.MESSAGING_REDIS_USER);
                String password = mainConfig.getString(CommonMainConfigPaths.MESSAGING_REDIS_PASSWORD);
                this.messagingProvider = new RedisMessagingProvider(
                        new RedisConnectionConfig(host, port, user, password),
                        PlatformType.PROXY
                );
                return true;
            default:
                logger.error("Unknown messaging.provider: {}", provider);
                return false;
        }
    }

    public boolean initConfigLang() {
        LanguageSettings languageSettings = new LanguageSettings.Builder()
                .ipLanguage(true)
                .clientLocale(true)
                .fallbackLocale(Locale.US)
                .build();

        this.configLang = new ConfigLang(
                pluginData.toFile(),
                core.getCssdb().getPlayerSettingCache(),
                core.getCssdb().getPlayerSettingsRepository(),
                languageSettings
        );

        Map<Locale, ILanguage> defaults = new HashMap<>();
        defaults.put(Locale.US, new English(plugin));
        configLang.init(defaults);
        this.languageManager = configLang.getLanguageManager();
        return languageManager != null;
    }

    public void initUtils() {
        this.messageUtil = new MessageUtil(languageManager);
    }

    public void initManagers() {
        authServersManager = new AuthServersManager(plugin);
        authServersManager.init();

        playerAuthContext = new PlayerAuthContext();
        forwardingManager = new ForwardingManager(plugin, playerAuthContext);

        if (messagingProvider != null) {
            messagingProvider.connectForProxy(forwardingManager);
        }
    }

    public void registerAll() {
        if (messagingProvider instanceof BungeePluginMessagingProvider) {
            registerEvents(messagingProvider);
        }

        if (mainConfig.getBoolean(MainConfigPaths.AUTH_SERVERS_DISABLE_TAB_COMPLETION_ENABLED)) {
            registerEvents(new TabCompletionDisabler(plugin, authServersManager));
        }

        VelocityServerListeners velocityServerListeners = new VelocityServerListeners(
                plugin, authServersManager, forwardingManager, playerAuthContext);

        registerEvents(
                velocityServerListeners,
                new VelocityLoginListeners(plugin, core.getAuthManager(), playerAuthContext)
        );
    }

    public void registerEvents(Object... listeners) {
        Arrays.stream(listeners).forEach(l -> server.getEventManager().register(plugin, l));
    }

    public void startSummary() {
        if (!StartSummary.enabled(mainConfig)) {
            return;
        }
        List<String> lines = new ArrayList<>();
        if (core != null) {
            lines.addAll(core.commonStartSummaryLines());
        }
        lines.add("Messaging: " + providerLabel(CommonMainConfigPaths.MESSAGING_PROVIDER)
                + " (" + StartSummary.className(messagingProvider) + ")");
        if (authServersManager != null) {
            String authServerNames = authServersManager.getAuthServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .collect(Collectors.joining(", "));
            if (authServerNames.isEmpty()) {
                authServerNames = "none";
            }
            lines.add("Auth servers: provider=" + authServersManager.getProvider()
                    + ", lb=" + authServersManager.getLoadBalancerType()
                    + " (" + StartSummary.className(authServersManager.getLoadBalancer()) + ")");
            lines.add("Auth servers registered: [" + authServerNames + "]");
        }
        lines.add("Forwarding: premium=" + providerLabel(MainConfigPaths.FORWARDING_PREMIUM)
                + ", floodgate=" + providerLabel(MainConfigPaths.FORWARDING_FLOODGATE)
                + ", cracks=" + providerLabel(MainConfigPaths.FORWARDING_CRACKS)
                + " (" + StartSummary.className(forwardingManager) + ")");
        lines.add("Floodgate: " + (floodgateApi != null ? "available" : "N/A (not installed)"));
        lines.add("Tab completion disabler: enabled="
                + mainConfig.getBoolean(MainConfigPaths.AUTH_SERVERS_DISABLE_TAB_COMPLETION_ENABLED)
                + ", mode=" + providerLabel(MainConfigPaths.AUTH_SERVERS_DISABLE_TAB_COMPLETION_MODE));
        StartSummary.log(logger, "IDCraft " + StartSummary.idcraftVersion() + " Velocity plugin has been enabled!", lines);
    }

    private String providerLabel(String path) {
        String raw = mainConfig.getString(path);
        return raw == null || raw.isBlank() ? "N/A" : raw;
    }

    public FloodgateApi getFloodgateApi() {
        return floodgateApi;
    }

    public ProxyServer getProxyServer() {
        return server;
    }

    public Path getPluginData() {
        return pluginData;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public PlayerAuthContext getPlayerAuthContext() {
        return playerAuthContext;
    }

    public ForwardingManager getForwardingManager() {
        return forwardingManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public IMessagingProvider getMessagingProvider() {
        return messagingProvider;
    }

    public InputStream getResourceAsStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }
}
