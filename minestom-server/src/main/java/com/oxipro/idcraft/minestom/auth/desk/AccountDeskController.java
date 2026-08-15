package com.oxipro.idcraft.minestom.auth.desk;

import com.oxipro.idcraft.api.account.IAccountRepository;
import com.oxipro.idcraft.api.auth.AuthErrorCode;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.api.auth.AuthResult;
import com.oxipro.idcraft.api.auth.IAuthManager;
import com.oxipro.idcraft.api.auth.factor.IAuthFactorHandler;
import com.oxipro.idcraft.api.messaging.IMessagingProvider;
import com.oxipro.idcraft.minestom.auth.factor.AuthFactorRegistry;
import com.oxipro.idcraft.minestom.auth.prompt.IAuthPrompt;
import com.oxipro.idcraft.minestom.language.LanguagePaths;
import com.oxipro.idcraft.minestom.utils.MessageUtil;
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

public final class AccountDeskController {

    private final IAuthManager authManager;
    private final IAccountRepository accounts;
    private final AuthFactorRegistry factors;
    private final IAuthPrompt prompt;
    private final AccountDeskConfig config;
    private final MessageUtil messages;
    private final IMessagingProvider messaging;
    private final String authServerName;
    private final ExecutorService async;

    private final Set<UUID> steppedUp = ConcurrentHashMap.newKeySet();
    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> terminals = new ConcurrentHashMap<>();

    public AccountDeskController(
            IAuthManager authManager,
            IAccountRepository accounts,
            AuthFactorRegistry factors,
            IAuthPrompt prompt,
            AccountDeskConfig config,
            MessageUtil messages,
            IMessagingProvider messaging,
            String authServerName,
            ExecutorService async
    ) {
        this.authManager = authManager;
        this.accounts = accounts;
        this.factors = factors;
        this.prompt = prompt;
        this.config = config;
        this.messages = messages;
        this.messaging = messaging;
        this.authServerName = authServerName;
        this.async = async;
    }

    public void start(Player player) {
        UUID uuid = player.getUuid();
        terminals.computeIfAbsent(uuid, id -> new CompletableFuture<>());
        if (!active.add(uuid)) {
            return;
        }
        prompt.reset(player);
        showHub(player);
    }

    public void onDisconnect(Player player) {
        UUID uuid = player.getUuid();
        active.remove(uuid);
        steppedUp.remove(uuid);
        prompt.reset(player);
        signalTerminal(uuid);
        terminals.remove(uuid);
    }

    public boolean awaitTerminal(Player player, long timeout, TimeUnit unit) {
        CompletableFuture<Void> future = terminals.computeIfAbsent(player.getUuid(), id -> new CompletableFuture<>());
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

    private void showHub(Player player) {
        async.execute(() -> {
            Map<AuthFactor, Boolean> enrolled = new EnumMap<>(AuthFactor.class);
            for (IAuthFactorHandler handler : factors.handlers()) {
                if (config.canManage(handler.kind())) {
                    enrolled.put(handler.kind(), handler.isEnrolled(player.getUuid()));
                }
            }
            runMain(() -> prompt.requestAccountHub(
                    player,
                    enrolled,
                    factor -> openFactor(player, factor),
                    () -> complete(player)
            ));
        });
    }

    private void openFactor(Player player, AuthFactor factor) {
        if (factor == AuthFactor.PASSWORD) {
            openPassword(player);
            return;
        }
        async.execute(() -> {
            if (accounts.findByUuid(player.getUuid()) == null) {
                runMain(() -> {
                    prompt.notifyError(player, messages.message(player, LanguagePaths.DESK_NEED_ACCOUNT));
                    showHub(player);
                });
                return;
            }
            IAuthFactorHandler handler = factors.get(factor);
            if (handler == null) {
                runMain(() -> showHub(player));
                return;
            }
            boolean enrolled = handler.isEnrolled(player.getUuid());
            boolean requireCurrent = config.requiresCurrentPassword(factor)
                    && !steppedUp.contains(player.getUuid());
            runMain(() -> prompt.requestFactorEdit(
                    player,
                    factor,
                    enrolled,
                    requireCurrent,
                    (value, current) -> async.execute(() -> saveFactor(player, handler, value, current)),
                    current -> async.execute(() -> removeFactor(player, handler, current)),
                    () -> showHub(player)
            ));
        });
    }

    private void openPassword(Player player) {
        async.execute(() -> {
            boolean hasAccount = accounts.findByUuid(player.getUuid()) != null;
            boolean requireCurrent = hasAccount && config.requiresCurrentPassword(AuthFactor.PASSWORD);
            runMain(() -> prompt.requestPasswordChange(
                    player,
                    requireCurrent,
                    !hasAccount,
                    submission -> async.execute(() -> savePassword(player, submission, hasAccount)),
                    () -> showHub(player)
            ));
        });
    }

    private void savePassword(Player player, com.oxipro.idcraft.minestom.auth.prompt.PasswordChangeSubmission submission, boolean hasAccount) {
        AuthResult result;
        if (hasAccount) {
            result = authManager.changePassword(
                    player.getUuid(),
                    submission.currentPassword(),
                    submission.newPassword(),
                    submission.confirmPassword()
            );
        } else {
            result = authManager.createPassword(
                    player.getUuid(),
                    player.getUsername(),
                    submission.newPassword(),
                    submission.confirmPassword(),
                    ipOf(player)
            );
        }
        if (!result.isSuccess()) {
            runMain(() -> {
                prompt.notifyError(player, messages.authError(player, result.getErrorCode()));
                openPassword(player);
            });
            return;
        }
        steppedUp.add(player.getUuid());
        runMain(() -> {
            prompt.notifyInfo(player, messages.message(player, hasAccount
                    ? LanguagePaths.DESK_PASSWORD_CHANGED
                    : LanguagePaths.DESK_PASSWORD_CREATED));
            showHub(player);
        });
    }

    private void saveFactor(Player player, IAuthFactorHandler handler, String value, String current) {
        if (!stepUp(player, handler.kind(), current)) {
            return;
        }
        AuthResult result = handler.enroll(player.getUuid(), value);
        finishFactor(player, result, LanguagePaths.DESK_FACTOR_ENROLLED, handler.kind());
    }

    private void removeFactor(Player player, IAuthFactorHandler handler, String current) {
        if (!stepUp(player, handler.kind(), current)) {
            return;
        }
        AuthResult result = handler.unenroll(player.getUuid());
        finishFactor(player, result, LanguagePaths.DESK_FACTOR_REMOVED, handler.kind());
    }

    private boolean stepUp(Player player, AuthFactor factor, String current) {
        if (!config.requiresCurrentPassword(factor) || steppedUp.contains(player.getUuid())) {
            return true;
        }
        if (authManager.verifyPassword(player.getUuid(), current)) {
            steppedUp.add(player.getUuid());
            return true;
        }
        runMain(() -> {
            prompt.notifyError(player, messages.authError(player, AuthErrorCode.WRONG_PASSWORD));
            openFactor(player, factor);
        });
        return false;
    }

    private void finishFactor(Player player, AuthResult result, String okPath, AuthFactor factor) {
        if (!result.isSuccess()) {
            runMain(() -> {
                prompt.notifyError(player, messages.authError(player, result.getErrorCode()));
                openFactor(player, factor);
            });
            return;
        }
        runMain(() -> {
            prompt.notifyInfo(player, messages.message(player, okPath));
            showHub(player);
        });
    }

    private void complete(Player player) {
        prompt.reset(player);
        active.remove(player.getUuid());
        steppedUp.remove(player.getUuid());
        if (messaging != null) {
            messaging.authenticated(player.getUuid(), authServerName);
        }
        signalTerminal(player.getUuid());
    }

    private void signalTerminal(UUID uuid) {
        terminals.computeIfAbsent(uuid, id -> new CompletableFuture<>()).complete(null);
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
