package com.oxipro.idcraft.minestom.auth;

import com.oxipro.idcraft.api.account.IAccountRepository;
import com.oxipro.idcraft.api.auth.AuthErrorCode;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.api.auth.AuthResult;
import com.oxipro.idcraft.api.auth.IAuthManager;
import com.oxipro.idcraft.api.auth.factor.IAuthFactorHandler;
import com.oxipro.idcraft.minestom.auth.factor.AuthFactorRegistry;
import com.oxipro.idcraft.minestom.auth.prompt.AuthPromptConfig;
import com.oxipro.idcraft.minestom.auth.prompt.IAuthPrompt;
import com.oxipro.idcraft.minestom.auth.prompt.LoginSubmission;
import com.oxipro.idcraft.minestom.auth.prompt.RegisterSubmission;
import com.oxipro.idcraft.api.messaging.IMessagingProvider;
import com.oxipro.idcraft.minestom.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

import java.net.InetSocketAddress;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AuthFlowController {

    private enum Phase {
        IDLE,
        LOGIN,
        REGISTER,
        DONE
    }

    private final IAuthManager authManager;
    private final IAccountRepository accountRepository;
    private final IAuthPrompt authPrompt;
    private final AuthPromptConfig promptConfig;
    private final AuthFactorRegistry factorRegistry;
    private final MessageUtil messages;
    private final IMessagingProvider messagingProvider;
    private final String authServerName;
    private final ExecutorService asyncExecutor;

    private final Map<UUID, Phase> phases = new ConcurrentHashMap<>();
    private final Map<UUID, Map<AuthFactor, String>> registerSetupValues = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Void>> terminals = new ConcurrentHashMap<>();

    public AuthFlowController(
            IAuthManager authManager,
            IAccountRepository accountRepository,
            IAuthPrompt authPrompt,
            AuthPromptConfig promptConfig,
            AuthFactorRegistry factorRegistry,
            MessageUtil messages,
            IMessagingProvider messagingProvider,
            String authServerName,
            ExecutorService asyncExecutor
    ) {
        this.authManager = authManager;
        this.accountRepository = accountRepository;
        this.authPrompt = authPrompt;
        this.promptConfig = promptConfig;
        this.factorRegistry = factorRegistry;
        this.messages = messages;
        this.messagingProvider = messagingProvider;
        this.authServerName = authServerName;
        this.asyncExecutor = asyncExecutor;
    }

    public void start(Player player) {
        UUID uuid = player.getUuid();
        terminals.computeIfAbsent(uuid, id -> new CompletableFuture<>());
        if (phases.putIfAbsent(uuid, Phase.IDLE) != null) {
            return;
        }
        authPrompt.reset(player);

        asyncExecutor.execute(() -> {
            String ip = ipOf(player);
            if (authManager.hasActiveSession(uuid, player.getUsername(), ip)) {
                runMain(() -> complete(player));
                return;
            }
            boolean hasAccount = accountRepository.existsByUsername(player.getUsername());
            runMain(() -> {
                if (hasAccount) {
                    beginLogin(player);
                } else {
                    beginRegister(player);
                }
            });
        });
    }

    public void onDisconnect(Player player) {
        UUID uuid = player.getUuid();
        phases.remove(uuid);
        registerSetupValues.remove(uuid);
        authPrompt.reset(player);
        signalTerminal(uuid);
        terminals.remove(uuid);
    }

    public boolean awaitTerminal(Player player, long timeout, TimeUnit unit) {
        UUID uuid = player.getUuid();
        CompletableFuture<Void> future = terminals.computeIfAbsent(uuid, id -> new CompletableFuture<>());
        try {
            future.get(timeout, unit);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void beginLogin(Player player) {
        phases.put(player.getUuid(), Phase.LOGIN);
        boolean needTotp = false;
        IAuthFactorHandler totp = factorRegistry.get(AuthFactor.TWO_FACTOR);
        if (totp != null && totp.isEnrolled(player.getUuid())) {
            needTotp = true;
        }
        boolean finalNeedTotp = needTotp;
        authPrompt.requestLogin(player, submission -> asyncExecutor.execute(() -> handleLogin(player, submission, finalNeedTotp)), finalNeedTotp);
    }

    private void handleLogin(Player player, LoginSubmission submission, boolean needTotp) {
        String password = submission.get(AuthFactor.PASSWORD);
        String ip = ipOf(player);

        AuthResult result = authManager.login(player.getUuid(), player.getUsername(), password, ip);
        if (!result.isSuccess()) {
            runMain(() -> {
                authPrompt.notifyError(player, translateError(player, result.getErrorCode()));
                beginLogin(player);
            });
            return;
        }

        if (needTotp) {
            IAuthFactorHandler totp = factorRegistry.get(AuthFactor.TWO_FACTOR);
            String code = submission.get(AuthFactor.TWO_FACTOR);
            if (totp != null) {
                AuthResult totpResult = totp.verifyLogin(player.getUuid(), code);
                if (!totpResult.isSuccess()) {
                    runMain(() -> {
                        authPrompt.notifyError(player, translateError(player, totpResult.getErrorCode()));
                        beginLogin(player);
                    });
                    return;
                }
            }
        }

        runMain(() -> complete(player));
    }

    private void beginRegister(Player player) {
        phases.put(player.getUuid(), Phase.REGISTER);
        registerSetupValues.put(player.getUuid(), new EnumMap<>(AuthFactor.class));

        Map<AuthFactor, Consumer<String>> setupHandlers = new EnumMap<>(AuthFactor.class);
        if (promptConfig.isFactorEnabled(AuthFactor.TWO_FACTOR) && factorRegistry.has(AuthFactor.TWO_FACTOR)) {
            setupHandlers.put(AuthFactor.TWO_FACTOR, value -> asyncExecutor.execute(() -> {
                IAuthFactorHandler handler = factorRegistry.get(AuthFactor.TWO_FACTOR);
                AuthResult enroll = handler.enroll(player.getUuid(), value);
                runMain(() -> {
                    if (!enroll.isSuccess()) {
                        authPrompt.notifyError(player, translateError(player, enroll.getErrorCode()));
                        authPrompt.requestFactorSetup(player, AuthFactor.TWO_FACTOR, setupHandlers.get(AuthFactor.TWO_FACTOR));
                        return;
                    }
                    Map<AuthFactor, String> setup = registerSetupValues.get(player.getUuid());
                    if (setup != null) {
                        setup.put(AuthFactor.TWO_FACTOR, value);
                    }
                    authPrompt.markFactorCompleted(player, AuthFactor.TWO_FACTOR);
                });
            }));
        }

        authPrompt.requestRegister(player, submission -> asyncExecutor.execute(() -> handleRegister(player, submission)), setupHandlers);
    }

    private void handleRegister(Player player, RegisterSubmission submission) {
        Set<AuthFactor> required = promptConfig.getRequiredFactors();

        if (!required.contains(AuthFactor.PASSWORD) && !promptConfig.isFactorEnabled(AuthFactor.PASSWORD)) {
            runMain(() -> {
                authPrompt.notifyError(player, translateError(player, AuthErrorCode.FACTOR_REQUIRED));
                beginRegister(player);
            });
            return;
        }

        String password = submission.get(AuthFactor.PASSWORD);
        String confirm = submission.getConfirmPassword();
        String ip = ipOf(player);
        AuthResult result = authManager.register(
                player.getUuid(),
                player.getUsername(),
                password,
                confirm,
                ip
        );
        if (!result.isSuccess()) {
            runMain(() -> {
                authPrompt.notifyError(player, translateError(player, result.getErrorCode()));
                beginRegister(player);
            });
            return;
        }

        if (promptConfig.isFactorEnabled(AuthFactor.EMAIL) && factorRegistry.has(AuthFactor.EMAIL)) {
            String email = submission.get(AuthFactor.EMAIL);
            boolean requiredEmail = promptConfig.isFactorRequired(AuthFactor.EMAIL);
            if (requiredEmail || (email != null && !email.trim().isEmpty())) {
                AuthResult emailResult = factorRegistry.get(AuthFactor.EMAIL).enroll(player.getUuid(), email);
                if (!emailResult.isSuccess()) {
                    runMain(() -> {
                        authPrompt.notifyError(player, translateError(player, emailResult.getErrorCode()));
                        beginRegister(player);
                    });
                    return;
                }
            } else if (requiredEmail) {
                runMain(() -> {
                    authPrompt.notifyError(player, translateError(player, AuthErrorCode.FACTOR_REQUIRED));
                    beginRegister(player);
                });
                return;
            }
        }

        if (promptConfig.isFactorRequired(AuthFactor.TWO_FACTOR) && factorRegistry.has(AuthFactor.TWO_FACTOR)) {
            Map<AuthFactor, String> setup = registerSetupValues.get(player.getUuid());
            if (setup == null || !setup.containsKey(AuthFactor.TWO_FACTOR)) {
                runMain(() -> {
                    authPrompt.notifyError(player, translateError(player, AuthErrorCode.FACTOR_REQUIRED));
                    beginRegister(player);
                });
                return;
            }
        }

        runMain(() -> complete(player));
    }

    private void complete(Player player) {
        phases.put(player.getUuid(), Phase.DONE);
        authPrompt.reset(player);
        if (messagingProvider != null) {
            messagingProvider.authenticated(player.getUuid(), authServerName);
        }
        signalTerminal(player.getUuid());
    }

    private void signalTerminal(UUID uuid) {
        terminals.computeIfAbsent(uuid, id -> new CompletableFuture<>()).complete(null);
    }

    private Component translateError(Player player, AuthErrorCode errorCode) {
        return messages.authError(player, errorCode);
    }

    private void runMain(Runnable task) {
        MinecraftServer.getSchedulerManager().scheduleNextTick(task);
    }

    private static String ipOf(Player player) {
        try {
            InetSocketAddress address = (InetSocketAddress) player.getPlayerConnection().getRemoteAddress();
            return address.getAddress().getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }
}
