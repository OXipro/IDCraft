package com.oxipro.idcraft.minestom.auth.prompt.prompts;

import com.oxipro.cmu.configlang.api.language.ILanguage;
import com.oxipro.cmu.configlang.minestom.language.LanguageManager;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.minestom.auth.prompt.AuthPromptConfig;
import com.oxipro.idcraft.minestom.auth.prompt.IAuthPrompt;
import com.oxipro.idcraft.minestom.auth.prompt.LoginSubmission;
import com.oxipro.idcraft.minestom.auth.prompt.RegisterSubmission;
import com.oxipro.idcraft.minestom.language.LanguagePaths;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CommandAuthPrompt implements IAuthPrompt {

    private sealed interface Pending permits Pending.Login, Pending.Register, Pending.FactorSetup {
        record Login(Consumer<LoginSubmission> onSubmit) implements Pending {}
        record Register() implements Pending {}
        record FactorSetup(AuthFactor factor, Consumer<String> onSubmit) implements Pending {}
    }

    private static final class RegisterState {
        private final Consumer<RegisterSubmission> onSubmit;
        private final Map<AuthFactor, Consumer<String>> factorSetupHandlers;
        private final Set<AuthFactor> completedFactors = EnumSet.noneOf(AuthFactor.class);
        private String pendingEmail;

        private RegisterState(Consumer<RegisterSubmission> onSubmit, Map<AuthFactor, Consumer<String>> factorSetupHandlers) {
            this.onSubmit = onSubmit;
            this.factorSetupHandlers = factorSetupHandlers;
        }
    }

    private final LanguageManager languageManager;
    private final AuthPromptConfig config;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final Map<UUID, RegisterState> registerStates = new ConcurrentHashMap<>();
    private boolean commandsRegistered;

    public CommandAuthPrompt(LanguageManager languageManager, AuthPromptConfig config) {
        this.languageManager = languageManager;
        this.config = config;
    }

    public void registerCommands() {
        if (commandsRegistered) {
            return;
        }
        commandsRegistered = true;

        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, e -> reset(e.getPlayer()));

        Command login = new Command("login");
        ArgumentString password = new ArgumentString("password");
        ArgumentString totp = new ArgumentString("totp");
        login.addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                return;
            }
            Pending state = pending.get(player.getUuid());
            if (!(state instanceof Pending.Login loginState)) {
                return;
            }
            Map<AuthFactor, String> values = new EnumMap<>(AuthFactor.class);
            values.put(AuthFactor.PASSWORD, context.get(password));
            loginState.onSubmit().accept(new LoginSubmission(values));
        }, password);
        login.addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                return;
            }
            Pending state = pending.get(player.getUuid());
            if (!(state instanceof Pending.Login loginState)) {
                return;
            }
            Map<AuthFactor, String> values = new EnumMap<>(AuthFactor.class);
            values.put(AuthFactor.PASSWORD, context.get(password));
            values.put(AuthFactor.TWO_FACTOR, context.get(totp));
            loginState.onSubmit().accept(new LoginSubmission(values));
        }, password, totp);
        MinecraftServer.getCommandManager().register(login);

        Command register = new Command("register");
        ArgumentString pass = new ArgumentString("password");
        ArgumentString confirm = new ArgumentString("confirm");
        ArgumentString email = new ArgumentString("email");
        register.addSyntax((sender, context) -> handleRegister(sender, context.get(pass), context.get(confirm), null), pass, confirm);
        register.addSyntax((sender, context) -> handleRegister(sender, context.get(pass), context.get(confirm), context.get(email)), pass, confirm, email);
        MinecraftServer.getCommandManager().register(register);

        Command twoFa = new Command("2fa");
        ArgumentString action = new ArgumentString("action");
        ArgumentString value = new ArgumentString("value");
        twoFa.addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                return;
            }
            String act = context.get(action);
            if ("setup".equalsIgnoreCase(act)) {
                openTotpSetup(player);
            }
        }, action);
        twoFa.addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                return;
            }
            String act = context.get(action);
            if (!"confirm".equalsIgnoreCase(act) && !"setup".equalsIgnoreCase(act)) {
                return;
            }
            Pending state = pending.get(player.getUuid());
            if (state instanceof Pending.FactorSetup setup) {
                setup.onSubmit().accept(context.get(value));
            }
        }, action, value);
        MinecraftServer.getCommandManager().register(twoFa);
    }

    private void handleRegister(net.minestom.server.command.CommandSender sender, String pass, String confirm, String email) {
        if (!(sender instanceof Player player)) {
            return;
        }
        Pending state = pending.get(player.getUuid());
        if (!(state instanceof Pending.Register)) {
            return;
        }
        RegisterState reg = registerStates.get(player.getUuid());
        if (reg == null) {
            return;
        }
        Map<AuthFactor, String> values = new EnumMap<>(AuthFactor.class);
        if (config.isFactorEnabled(AuthFactor.PASSWORD)) {
            values.put(AuthFactor.PASSWORD, pass);
        }
        if (config.isFactorEnabled(AuthFactor.EMAIL) && email != null) {
            values.put(AuthFactor.EMAIL, email);
            reg.pendingEmail = email;
        } else if (reg.pendingEmail != null) {
            values.put(AuthFactor.EMAIL, reg.pendingEmail);
        }
        reg.onSubmit.accept(new RegisterSubmission(values, confirm));
    }

    private void openTotpSetup(Player player) {
        RegisterState reg = registerStates.get(player.getUuid());
        if (reg == null) {
            return;
        }
        Consumer<String> handler = reg.factorSetupHandlers.get(AuthFactor.TWO_FACTOR);
        if (handler == null) {
            return;
        }
        requestFactorSetup(player, AuthFactor.TWO_FACTOR, handler);
    }

    @Override
    public void requestLogin(Player player, Consumer<LoginSubmission> onSubmit) {
        requestLogin(player, onSubmit, false);
    }

    @Override
    public void requestLogin(Player player, Consumer<LoginSubmission> onSubmit, boolean needTotp) {
        pending.put(player.getUuid(), new Pending.Login(onSubmit));
        ILanguage lang = languageManager.getPlayerLanguage(player);
        if (needTotp) {
            player.sendMessage(Component.text(lang.getMessage(LanguagePaths.LOGIN_COMMAND_HINT_2FA)));
        } else {
            player.sendMessage(Component.text(lang.getMessage(LanguagePaths.LOGIN_COMMAND_HINT)));
        }
    }

    @Override
    public void requestRegister(Player player, Consumer<RegisterSubmission> onSubmit, Map<AuthFactor, Consumer<String>> factorSetupHandlers) {
        registerStates.put(player.getUuid(), new RegisterState(onSubmit, factorSetupHandlers));
        pending.put(player.getUuid(), new Pending.Register());
        ILanguage lang = languageManager.getPlayerLanguage(player);
        player.sendMessage(Component.text(lang.getMessage(LanguagePaths.REGISTER_COMMAND_HINT)));
        if (config.isFactorEnabled(AuthFactor.TWO_FACTOR) && factorSetupHandlers.containsKey(AuthFactor.TWO_FACTOR)) {
            player.sendMessage(Component.text(lang.getMessage(LanguagePaths.TWO_FACTOR_COMMAND_HINT)));
        }
    }

    @Override
    public void requestFactorSetup(Player player, AuthFactor factor, Consumer<String> onSubmit) {
        pending.put(player.getUuid(), new Pending.FactorSetup(factor, onSubmit));
        ILanguage lang = languageManager.getPlayerLanguage(player);
        player.sendMessage(Component.text(lang.getMessage(LanguagePaths.TWO_FACTOR_COMMAND_SETUP_HINT)));
    }

    @Override
    public void markFactorCompleted(Player player, AuthFactor factor) {
        RegisterState state = registerStates.get(player.getUuid());
        if (state == null) {
            return;
        }
        state.completedFactors.add(factor);
        pending.put(player.getUuid(), new Pending.Register());
        ILanguage lang = languageManager.getPlayerLanguage(player);
        player.sendMessage(Component.text(lang.getMessage(LanguagePaths.FACTOR_COMPLETED_HINT)));
    }

    @Override
    public void notifyError(Player player, Component message) {
        player.sendMessage(message);
    }

    @Override
    public void reset(Player player) {
        UUID uuid = player.getUuid();
        pending.remove(uuid);
        registerStates.remove(uuid);
    }
}
