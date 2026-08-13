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

    void reset(Player player);
}
