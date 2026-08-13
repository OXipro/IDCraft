package com.oxipro.idcraft.minestom.auth.prompt;

import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.minestom.auth.prompt.prompts.CommandAuthPrompt;
import com.oxipro.idcraft.minestom.auth.prompt.prompts.DialogAuthPrompt;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.Map;
import java.util.function.Consumer;

public class AuthPromptSelector implements IAuthPrompt {

    private final AuthPromptConfig config;
    private final DialogAuthPrompt dialogPrompt;
    private final CommandAuthPrompt commandPrompt;

    public AuthPromptSelector(
            AuthPromptConfig config,
            DialogAuthPrompt dialogPrompt,
            CommandAuthPrompt commandPrompt
    ) {
        this.config = config;
        this.dialogPrompt = dialogPrompt;
        this.commandPrompt = commandPrompt;
    }

    private IAuthPrompt select(Player player) {
        AuthPromptConfig.PromptType type = config.getPromptType();
        if (type == AuthPromptConfig.PromptType.COMMAND) {
            return commandPrompt;
        }
        if (type == AuthPromptConfig.PromptType.DIALOG) {
            return dialogPrompt;
        }
        // AUTO
        try {
            int protocol = player.getPlayerConnection().getProtocolVersion();
            if (protocol >= AuthPromptConfig.MIN_DIALOG_PROTOCOL) {
                return dialogPrompt;
            }
            return commandPrompt;
        } catch (Exception e) {
            return commandPrompt;
        }
    }

    @Override
    public void requestLogin(Player player, Consumer<LoginSubmission> onSubmit) {
        select(player).requestLogin(player, onSubmit, false);
    }

    @Override
    public void requestLogin(Player player, Consumer<LoginSubmission> onSubmit, boolean needTotp) {
        select(player).requestLogin(player, onSubmit, needTotp);
    }

    @Override
    public void requestRegister(
            Player player,
            Consumer<RegisterSubmission> onSubmit,
            Map<AuthFactor, Consumer<String>> factorSetupHandlers
    ) {
        select(player).requestRegister(player, onSubmit, factorSetupHandlers);
    }

    @Override
    public void requestFactorSetup(Player player, AuthFactor factor, Consumer<String> onSubmit) {
        select(player).requestFactorSetup(player, factor, onSubmit);
    }

    @Override
    public void markFactorCompleted(Player player, AuthFactor factor) {
        select(player).markFactorCompleted(player, factor);
    }

    @Override
    public void notifyError(Player player, Component message) {
        select(player).notifyError(player, message);
    }

    @Override
    public void reset(Player player) {
        dialogPrompt.reset(player);
        commandPrompt.reset(player);
    }
}
