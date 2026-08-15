package com.oxipro.idcraft.minestom.auth.prompt;

import com.oxipro.idcraft.api.auth.AuthFactor;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.Map;
import java.util.function.Consumer;

public interface IAuthPrompt {

    void requestLogin(Player player, Consumer<LoginSubmission> onSubmit);

    default void requestLogin(Player player, Consumer<LoginSubmission> onSubmit, boolean needTotp) {
        requestLogin(player, onSubmit);
    }

    void requestRegister(
            Player player,
            Consumer<RegisterSubmission> onSubmit,
            Map<AuthFactor, Consumer<String>> factorSetupHandlers
    );

    void requestFactorSetup(Player player, AuthFactor factor, Consumer<String> onSubmit);

    void markFactorCompleted(Player player, AuthFactor factor);

    void notifyError(Player player, Component message);

    void notifyInfo(Player player, Component message);

    void requestAccountHub(
            Player player,
            Map<AuthFactor, Boolean> factors,
            java.util.function.Consumer<AuthFactor> onOpen,
            Runnable onDone
    );

    void requestPasswordChange(
            Player player,
            boolean requireCurrent,
            boolean createAccount,
            java.util.function.Consumer<PasswordChangeSubmission> onSubmit,
            Runnable onBack
    );

    void requestFactorEdit(
            Player player,
            AuthFactor factor,
            boolean enrolled,
            boolean requireCurrent,
            java.util.function.BiConsumer<String, String> onSave,
            java.util.function.Consumer<String> onRemove,
            Runnable onBack
    );

    void reset(Player player);
}
