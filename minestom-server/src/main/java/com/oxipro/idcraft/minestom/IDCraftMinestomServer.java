package com.oxipro.idcraft.minestom;

import com.oxipro.cmu.configlang.api.language.ILanguage;
import com.oxipro.cmu.configlang.api.language.LanguageSettings;
import com.oxipro.cmu.configlang.minestom.ConfigLang;
import com.oxipro.cmu.configlang.minestom.language.LanguageManager;
import com.oxipro.cmu.configlang.standalone.config.ConfigFile;
import com.oxipro.cssdb.CSSDB;
import com.oxipro.idcraft.api.account.IAccountFactorRepository;
import com.oxipro.idcraft.api.account.IAccountRepository;
import com.oxipro.idcraft.api.auth.IAuthManager;
import com.oxipro.idcraft.api.messaging.IMessagingProvider;
import com.oxipro.idcraft.api.platform.PlatformType;
import com.oxipro.idcraft.api.session.ICacheProvider;
import com.oxipro.idcraft.api.support.servername.IServerNameProvider;
import com.oxipro.idcraft.core.IDCraftCore;
import com.oxipro.idcraft.core.configuration.LanguageSettingsConfig;
import com.oxipro.idcraft.core.configuration.paths.CommonMainConfigPaths;
import com.oxipro.idcraft.core.logging.StartSummary;
import com.oxipro.idcraft.api.auth.AuthVisitKind;
import com.oxipro.idcraft.minestom.auth.AuthFlowController;
import com.oxipro.idcraft.minestom.auth.ConnectAuthGate;
import com.oxipro.idcraft.minestom.auth.desk.AccountDeskConfig;
import com.oxipro.idcraft.minestom.auth.desk.AccountDeskController;
import com.oxipro.idcraft.minestom.auth.factor.AuthFactorRegistry;
import com.oxipro.idcraft.minestom.auth.prompt.AuthPromptConfig;
import com.oxipro.idcraft.minestom.auth.prompt.AuthPromptSelector;
import com.oxipro.idcraft.minestom.auth.prompt.IAuthPrompt;
import com.oxipro.idcraft.minestom.auth.prompt.prompts.CommandAuthPrompt;
import com.oxipro.idcraft.minestom.auth.prompt.prompts.DialogAuthPrompt;
import com.oxipro.idcraft.minestom.configuration.ConfigManager;
import com.oxipro.idcraft.minestom.configuration.paths.MainConfigPaths;
import com.oxipro.idcraft.minestom.language.LanguagePaths;
import com.oxipro.idcraft.minestom.language.defaultLanguage.English;
import com.oxipro.idcraft.minestom.language.defaultLanguage.French;
import com.oxipro.idcraft.minestom.utils.MessageUtil;
import com.oxipro.idcraft.minestom.messaging.BungeePluginMessagingProvider;
import com.oxipro.idcraft.minestom.network.NetworkConfig;
import com.oxipro.idcraft.minestom.network.NetworkMode;
import com.oxipro.idcraft.minestom.network.StandaloneNetworkBootstrap;
import com.oxipro.idcraft.minestom.world.AuthWorldFactory;
import com.oxipro.idcraft.minestom.world.IAuthWorldProvider;
import com.oxipro.idcraft.redis.RedisConnectionConfig;
import com.oxipro.idcraft.redis.providers.RedisMessagingProvider;
import com.oxipro.idcraft.support.shulker.minestom.ShulkerMinestomImpl;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.gamerule.GameRule;
import net.minestom.server.network.packet.server.configuration.UpdateEnabledFeaturesPacket;
import net.minestom.server.registry.StaticProtocolObject;
import net.minestom.server.tag.Tag;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class IDCraftMinestomServer {

    public static final File CONFIG_DIR = new File("config");

    private static final Logger LOGGER = LoggerFactory.getLogger(IDCraftMinestomServer.class);

    public static void main(String[] args) {
        new IDCraftMinestomServer().boot();
    }

    private final IDCraftMinestomServer server = this;

    private MinecraftServer minecraftServer;
    private NetworkConfig networkConfig;

    private ConfigManager configManager;
    private ConfigFile mainConfig;
    private ConfigLang configLang;
    private LanguageManager languageManager;
    private MessageUtil messageUtil;

    private IServerNameProvider serverNameProvider;
    private IMessagingProvider messagingProvider;
    private IAccountRepository accountRepository;
    private IAccountFactorRepository accountFactorRepository;
    private ICacheProvider cache;
    private IAuthManager authManager;

    private IAuthWorldProvider authWorld;
    private AuthPromptConfig promptConfig;
    private IAuthPrompt authPrompt;
    private AuthFlowController authFlowController;
    private AccountDeskConfig accountDeskConfig;
    private AccountDeskController accountDeskController;
    private final ConnectAuthGate connectAuthGate = new ConnectAuthGate();
    private final Map<UUID, ConnectAuthGate.Grant> visits = new ConcurrentHashMap<>();
    private final ExecutorService asyncAuthExecutor = Executors.newFixedThreadPool(4);

    private static final long CONNECT_AUTH_TIMEOUT_SECONDS = 30;
    private static final long CONFIG_AUTH_TIMEOUT_MINUTES = 15;
    private static final long POST_AUTH_TRANSFER_GRACE_MS = 15_000;

    private ShulkerMinestomImpl shulkerMinestom;

    private IDCraftCore core;

    private void boot() {
        long startedAt = System.nanoTime();
        initConfig();
        this.networkConfig = NetworkConfig.fromConfig(mainConfig);

        this.core = new IDCraftCore(LOGGER, configManager);
        if (!core.init()) {
            throw new IllegalStateException("IDCraft core failed to initialize");
        }
        if (!initMessagingProvider()) {
            throw new IllegalStateException("Messaging provider failed to init");
        }
        initConfigLang();
        initManagers();

        // 1) MinecraftServer.init (+ proxy) — CONFIG or SHULKER agent
        initNetwork();
        // 2) worlds / auth / events (need MinecraftServer already inited)
        initAuthUi();
        registerEvents();
        // 3) listen
        startNetwork();

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "idcraft-shutdown"));
        startSummary();
        double seconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;
        LOGGER.info("Done ({}s)!", String.format(Locale.ROOT, "%.3f", seconds));
    }

    private void initConfig() {
        if (!CONFIG_DIR.exists() && !CONFIG_DIR.mkdirs()) {
            throw new IllegalStateException("Cannot create config directory: " + CONFIG_DIR.getAbsolutePath());
        }
        this.configManager = new ConfigManager(server);
        this.mainConfig = configManager.getMain();
        String levelRaw = mainConfig.getString(CommonMainConfigPaths.LOGGER_LEVEL);
        Level level = levelRaw == null ? Level.INFO : Level.valueOf(levelRaw.toUpperCase(Locale.ROOT));
        Configurator.setLevel("com.oxipro.idcraft", level);
    }

    public boolean initMessagingProvider() {
        String provider = mainConfig.getString(CommonMainConfigPaths.MESSAGING_PROVIDER);
        if (provider == null) {
            return false;
        }
        switch (provider.toLowerCase(Locale.ROOT)) {
            case "bungee_plugin":
                this.messagingProvider = new BungeePluginMessagingProvider(server);
                return true;
            case "redis":
                String host = mainConfig.getString(CommonMainConfigPaths.MESSAGING_REDIS_HOST);
                int port = mainConfig.getInt(CommonMainConfigPaths.MESSAGING_REDIS_PORT);
                String user = mainConfig.getString(CommonMainConfigPaths.MESSAGING_REDIS_USER);
                String password = mainConfig.getString(CommonMainConfigPaths.MESSAGING_REDIS_PASSWORD);
                this.messagingProvider = new RedisMessagingProvider(
                        new RedisConnectionConfig(host, port, user, password),
                        PlatformType.AUTH_SERVER
                );
                return true;
            default:
                LOGGER.error("Unknown messaging.provider: {}", provider);
                return false;
        }
    }

    private void initConfigLang() {
        File languagesDir = new File(CONFIG_DIR, "languages");
        if (!languagesDir.exists() && !languagesDir.mkdirs()) {
            throw new IllegalStateException("Cannot create languages directory: " + languagesDir.getAbsolutePath());
        }

        LanguageSettings langSettings = LanguageSettingsConfig.fromConfig(mainConfig);
        configLang = new ConfigLang(
                CONFIG_DIR,
                core.getCssdb().getPlayerSettingCache(),
                core.getCssdb().getPlayerSettingsRepository(),
                langSettings
        );
        Map<Locale, ILanguage> defaultLangList = new HashMap<>();
        defaultLangList.put(Locale.US, new English(server));
        defaultLangList.put(Locale.FRANCE, new French(server));
        configLang.init(defaultLangList);
        languageManager = configLang.getLanguageManager();
        messageUtil = new MessageUtil(languageManager);
    }

    private void initManagers() {
        this.authManager = core.getAuthManager();
        this.accountRepository = core.getAccountRepository();
        this.accountFactorRepository = core.getAccountFactorRepository();
        this.cache = core.getCache();
        this.serverNameProvider = core.getServerNameProvider();
    }

    private void initNetwork() {
        if (networkConfig.getMode() == NetworkMode.SHULKER) {
            LOGGER.info("Network mode SHULKER: agent owns MinecraftServer.init, bind and proxy");
            shulkerMinestom = new ShulkerMinestomImpl();
            shulkerMinestom.initServer();
            // Agent already called MinecraftServer.init(); keep a handle for getters
            this.minecraftServer = new MinecraftServer();
            return;
        }
        this.minecraftServer = StandaloneNetworkBootstrap.init(networkConfig, LOGGER);
    }

    private void initAuthUi() {
        this.promptConfig = AuthPromptConfig.fromConfig(mainConfig);
        DialogAuthPrompt dialogPrompt = new DialogAuthPrompt(messageUtil, promptConfig);
        CommandAuthPrompt commandPrompt = new CommandAuthPrompt(messageUtil, promptConfig);
        this.authPrompt = new AuthPromptSelector(promptConfig, dialogPrompt, commandPrompt);

        dialogPrompt.registerListeners();
        commandPrompt.registerCommands();

        AuthFactorRegistry factorRegistry = new AuthFactorRegistry(
                promptConfig, authManager, accountRepository, accountFactorRepository);
        String serverName = serverNameProvider.getCurrentServerName();

        this.authFlowController = new AuthFlowController(
                authManager,
                accountRepository,
                authPrompt,
                promptConfig,
                factorRegistry,
                messageUtil,
                messagingProvider,
                serverName,
                asyncAuthExecutor
        );

        this.accountDeskConfig = AccountDeskConfig.fromConfig(mainConfig);
        this.accountDeskController = new AccountDeskController(
                authManager,
                accountRepository,
                factorRegistry,
                authPrompt,
                accountDeskConfig,
                messageUtil,
                messagingProvider,
                serverName,
                asyncAuthExecutor
        );

        if (messagingProvider != null) {
            messagingProvider.connectForAuthServer(connectAuthGate::allow);
        }

        this.authWorld = new AuthWorldFactory().create(mainConfig);
        long time = mainConfig.getLong(MainConfigPaths.AUTH_WORLD_TIME_VALUE);
        boolean advanceTime = mainConfig.getBoolean(MainConfigPaths.AUTH_WORLD_TIME_ADVANCE);
        authWorld.getInstance().setTime(time);
        authWorld.getInstance().setTag(Tag.Boolean("gamerule.advance_time"), advanceTime);

    }

    private void registerEvents() {
        GlobalEventHandler eventHandler = MinecraftServer.getGlobalEventHandler();

        eventHandler.addListener(AsyncPlayerConfigurationEvent.class, this::onPlayerConfiguration);

        eventHandler.addListener(PlayerSpawnEvent.class, event -> {
            if (!event.isFirstSpawn()) {
                return;
            }
            if (event.getInstance() != authWorld.getInstance()) {
                return;
            }
            Player player = event.getPlayer();
            if (shouldHoldDialogs(player)) {
                MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
                    if (player.isOnline()) {
                        player.startConfigurationPhase();
                    }
                });
                return;
            }
            startVisit(player);
        });

        eventHandler.addListener(PlayerDisconnectEvent.class, event -> {
            connectAuthGate.release(event.getPlayer().getUuid());
            visits.remove(event.getPlayer().getUuid());
            authFlowController.onDisconnect(event.getPlayer());
            accountDeskController.onDisconnect(event.getPlayer());
        });
    }

    private void onPlayerConfiguration(AsyncPlayerConfigurationEvent event) {
        Player player = event.getPlayer();
        boolean holdDialogs = shouldHoldDialogs(player);

        if (event.isFirstConfig() && messagingProvider != null) {
            ConnectAuthGate.Grant grant = connectAuthGate.awaitGrant(
                    player.getUuid(), CONNECT_AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (grant == null) {
                LOGGER.warn("No proxy CONNECT_AUTH for {} within {}s", player.getUsername(), CONNECT_AUTH_TIMEOUT_SECONDS);
                if (player.isOnline()) {
                    player.kick(connectDeniedMessage(player));
                }
                return;
            }
            visits.put(player.getUuid(), grant);
        }
        if (!player.isOnline()) {
            return;
        }

        if (holdDialogs && !event.isFirstConfig()) {
            runConfigurationDialogs(event, player);
            return;
        }

        assignAuthWorld(event);
    }

    private void startVisit(Player player) {
        ConnectAuthGate.Grant grant = visits.get(player.getUuid());
        AuthVisitKind kind = grant == null ? AuthVisitKind.LOGIN : grant.kind();
        if (kind == AuthVisitKind.ACCOUNT_DESK && accountDeskConfig.enabled()) {
            accountDeskController.start(player);
            return;
        }
        authFlowController.start(player, kind);
    }

    private boolean awaitVisit(Player player) {
        ConnectAuthGate.Grant grant = visits.get(player.getUuid());
        if (grant != null && grant.kind() == AuthVisitKind.ACCOUNT_DESK) {
            return accountDeskController.awaitTerminal(player, CONFIG_AUTH_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        }
        return authFlowController.awaitTerminal(player, CONFIG_AUTH_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    private boolean shouldHoldDialogs(Player player) {
        return promptConfig.shouldHoldInConfiguration(protocolVersion(player));
    }

    private void runConfigurationDialogs(AsyncPlayerConfigurationEvent event, Player player) {
        player.sendPacket(new UpdateEnabledFeaturesPacket(
                event.getFeatureFlags().stream().map(StaticProtocolObject::name).toList()
        ));

        startVisit(player);
        boolean finished = awaitVisit(player);
        if (!player.isOnline()) {
            return;
        }
        if (!finished) {
            player.kick(connectDeniedMessage(player));
            return;
        }

        long deadline = System.currentTimeMillis() + POST_AUTH_TRANSFER_GRACE_MS;
        while (player.isOnline() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (player.isOnline()) {
            assignAuthWorld(event);
        }
    }

    private void assignAuthWorld(AsyncPlayerConfigurationEvent event) {
        event.setSpawningInstance(authWorld.getInstance());
        event.getPlayer().setRespawnPoint(authWorld.getSpawn());
    }

    private Component connectDeniedMessage(Player player) {
        try {
            return messageUtil.message(player, LanguagePaths.ERROR_CONNECT_NOT_ALLOWED);
        } catch (Exception e) {
            return Component.text("You are not allowed to join this authentication server.");
        }
    }

    private static int protocolVersion(Player player) {
        try {
            return player.getPlayerConnection().getProtocolVersion();
        } catch (Exception e) {
            return 0;
        }
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
        if (networkConfig != null) {
            lines.add("Network: mode=" + networkConfig.getMode()
                    + ", bind=" + networkConfig.getBindHost() + ":" + networkConfig.getBindPort()
                    + ", proxy=" + networkConfig.getProxyType());
        }
        if (authWorld != null) {
            lines.add("World provider: " + authWorld.type() + " (" + StartSummary.className(authWorld) + ")");
        }
        if (promptConfig != null) {
            lines.add("Prompt: " + promptConfig.getPromptType()
                    + ", phase=" + promptConfig.getDialogPhase()
                    + " (" + StartSummary.className(authPrompt) + ")");
            lines.add("Auth methods required: " + promptConfig.getRequiredFactors());
            lines.add("Auth methods optional: " + promptConfig.getOptionalFactors());
            lines.add("Email provider: " + valueOrNa(promptConfig.getEmailProvider()));
            lines.add("2FA provider: " + valueOrNa(promptConfig.getTwoFactorProvider()));
            lines.add("Language detect-before-register: " + promptConfig.isDetectLanguageBeforeRegister());
        }
        if (accountDeskConfig != null) {
            lines.add("Account desk: enabled=" + accountDeskConfig.enabled());
        }
        StartSummary.log(LOGGER, "IDCraft " + StartSummary.idcraftVersion() + " auth server has been enabled!", lines);
    }

    private String providerLabel(String path) {
        String raw = mainConfig.getString(path);
        return raw == null || raw.isBlank() ? "N/A" : raw;
    }

    private static String valueOrNa(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private void startNetwork() {
        if (networkConfig.getMode() == NetworkMode.SHULKER) {
            LOGGER.info("Network mode SHULKER");
            shulkerMinestom.start();
            return;
        }
        StandaloneNetworkBootstrap.start(networkConfig, minecraftServer, LOGGER);
    }

    private void shutdown() {
        if (messagingProvider != null) {
            messagingProvider.disconnect();
        }
        if (core != null) {
            core.shutdown();
        }
        asyncAuthExecutor.shutdown();
        try {
            if (!asyncAuthExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncAuthExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            asyncAuthExecutor.shutdownNow();
        }
    }

    public MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    public InputStream getResourceAsStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }
}
