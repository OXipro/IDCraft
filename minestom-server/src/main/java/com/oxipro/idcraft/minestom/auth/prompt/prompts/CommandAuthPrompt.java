package com.oxipro.idcraft.minestom.auth.prompt.prompts;

import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.minestom.auth.prompt.AuthPromptConfig;
import com.oxipro.idcraft.minestom.auth.prompt.IAuthPrompt;
import com.oxipro.idcraft.minestom.auth.prompt.LoginSubmission;
import com.oxipro.idcraft.minestom.auth.prompt.PasswordChangeSubmission;
import com.oxipro.idcraft.minestom.auth.prompt.RegisterSubmission;
import com.oxipro.idcraft.minestom.language.LanguagePaths;
import com.oxipro.idcraft.minestom.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CommandAuthPrompt implements IAuthPrompt {

    private sealed interface Pending permits Pending.Login, Pending.Register, Pending.FactorSetup,
            Pending.Hub, Pending.Password, Pending.FactorEdit {
        record Login(Consumer<LoginSubmission> onSubmit) implements Pending {}
        record Register() implements Pending {}
        record FactorSetup(AuthFactor factor, Consumer<String> onSubmit) implements Pending {}
        record Hub(Consumer<AuthFactor> onOpen, Runnable onDone, Map<AuthFactor, Boolean> factors) implements Pending {}
        record Password(Consumer<PasswordChangeSubmission> onSubmit, Runnable onBack, boolean requireCurrent) implements Pending {}
        record FactorEdit(AuthFactor factor, BiConsumer<String, String> onSave, Consumer<String> onRemove) implements Pending {}
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

    private final MessageUtil messages;
    private final AuthPromptConfig config;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final Map<UUID, RegisterState> registerStates = new ConcurrentHashMap<>();
    private boolean commandsRegistered;

    public CommandAuthPrompt(MessageUtil messages, AuthPromptConfig config) {
        this.messages = messages;
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

        Command account = new Command("account");
        ArgumentWord accountAction = new ArgumentWord("action");
        ArgumentWord accountSub = new ArgumentWord("sub");
        ArgumentString accountValue = new ArgumentString("value");
        account.setDefaultExecutor((sender, context) -> {
            if (sender instanceof Player player && pending.get(player.getUuid()) instanceof Pending.Hub hub) {
                showHubHints(player, hub.factors());
            }
        });
        account.addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                return;
            }
            if (!"done".equalsIgnoreCase(context.get(accountAction))) {
                return;
            }
            if (pending.get(player.getUuid()) instanceof Pending.Hub hub) {
                hub.onDone().run();
            }
        }, accountAction);
        account.addSyntax((sender, context) -> handleAccount(sender instanceof Player p ? p : null,
                context.get(accountAction), context.get(accountSub), null), accountAction, accountSub);
        account.addSyntax((sender, context) -> handleAccount(sender instanceof Player p ? p : null,
                context.get(accountAction), context.get(accountSub), context.get(accountValue)),
                accountAction, accountSub, accountValue);
        MinecraftServer.getCommandManager().register(account);

        Command changePassword = new Command("changepassword");
        ArgumentString first = new ArgumentString("first");
        ArgumentString second = new ArgumentString("second");
        ArgumentString third = new ArgumentString("third");
        changePassword.addSyntax((sender, context) -> handleChangePassword(
                sender instanceof Player p ? p : null, null, context.get(first), context.get(second)), first, second);
        changePassword.addSyntax((sender, context) -> handleChangePassword(
                sender instanceof Player p ? p : null, context.get(first), context.get(second), context.get(third)),
                first, second, third);
        MinecraftServer.getCommandManager().register(changePassword);
    }

    private void handleAccount(Player player, String target, String sub, String value) {
        if (player == null) {
            return;
        }
        Pending state = pending.get(player.getUuid());
        if (state instanceof Pending.Hub hub) {
            if ("password".equalsIgnoreCase(target)) {
                hub.onOpen().accept(AuthFactor.PASSWORD);
                return;
            }
            AuthFactor factor = AuthFactor.fromConfigKey(target);
            if (factor != null) {
                hub.onOpen().accept(factor);
            }
            return;
        }
        if (state instanceof Pending.FactorEdit edit) {
            if ("remove".equalsIgnoreCase(sub) || "disable".equalsIgnoreCase(sub)) {
                edit.onRemove().accept(value);
                return;
            }
            if ("set".equalsIgnoreCase(sub) || "confirm".equalsIgnoreCase(sub) || "setup".equalsIgnoreCase(sub)) {
                edit.onSave().accept(value, null);
            }
        }
    }

    private void handleChangePassword(Player player, String current, String next, String confirm) {
        if (player == null) {
            return;
        }
        Pending state = pending.get(player.getUuid());
        if (state instanceof Pending.Hub hub) {
            hub.onOpen().accept(AuthFactor.PASSWORD);
            return;
        }
        if (state instanceof Pending.Password password) {
            if (password.requireCurrent()) {
                password.onSubmit().accept(new PasswordChangeSubmission(current, next, confirm));
            } else {
                password.onSubmit().accept(new PasswordChangeSubmission(null, current == null ? next : current, confirm));
            }
        }
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
        if (needTotp) {
            player.sendMessage(messages.message(player, LanguagePaths.LOGIN_COMMAND_HINT_2FA));
        } else {
            player.sendMessage(messages.message(player, LanguagePaths.LOGIN_COMMAND_HINT));
        }
    }

    @Override
    public void requestRegister(Player player, Consumer<RegisterSubmission> onSubmit, Map<AuthFactor, Consumer<String>> factorSetupHandlers) {
        registerStates.put(player.getUuid(), new RegisterState(onSubmit, factorSetupHandlers));
        pending.put(player.getUuid(), new Pending.Register());
        player.sendMessage(messages.message(player, LanguagePaths.REGISTER_COMMAND_HINT));
        if (config.isFactorEnabled(AuthFactor.TWO_FACTOR) && factorSetupHandlers.containsKey(AuthFactor.TWO_FACTOR)) {
            player.sendMessage(messages.message(player, LanguagePaths.TWO_FACTOR_COMMAND_HINT));
        }
    }

    @Override
    public void requestFactorSetup(Player player, AuthFactor factor, Consumer<String> onSubmit) {
        pending.put(player.getUuid(), new Pending.FactorSetup(factor, onSubmit));
        player.sendMessage(messages.message(player, LanguagePaths.TWO_FACTOR_COMMAND_SETUP_HINT));
    }

    @Override
    public void markFactorCompleted(Player player, AuthFactor factor) {
        RegisterState state = registerStates.get(player.getUuid());
        if (state == null) {
            return;
        }
        state.completedFactors.add(factor);
        pending.put(player.getUuid(), new Pending.Register());
        player.sendMessage(messages.message(player, LanguagePaths.FACTOR_COMPLETED_HINT));
    }

    @Override
    public void notifyError(Player player, Component message) {
        player.sendMessage(message);
    }

    @Override
    public void notifyInfo(Player player, Component message) {
        player.sendMessage(message);
    }

    @Override
    public void requestAccountHub(Player player, Map<AuthFactor, Boolean> factors, Consumer<AuthFactor> onOpen, Runnable onDone) {
        pending.put(player.getUuid(), new Pending.Hub(onOpen, onDone, factors));
        showHubHints(player, factors);
    }

    @Override
    public void requestPasswordChange(
            Player player,
            boolean requireCurrent,
            boolean createAccount,
            Consumer<PasswordChangeSubmission> onSubmit,
            Runnable onBack
    ) {
        pending.put(player.getUuid(), new Pending.Password(onSubmit, onBack, requireCurrent));
        player.sendMessage(messages.message(player, createAccount
                ? LanguagePaths.DESK_COMMAND_CREATE_HINT
                : LanguagePaths.DESK_COMMAND_PASSWORD_HINT));
    }

    @Override
    public void requestFactorEdit(
            Player player,
            AuthFactor factor,
            boolean enrolled,
            boolean requireCurrent,
            BiConsumer<String, String> onSave,
            Consumer<String> onRemove,
            Runnable onBack
    ) {
        pending.put(player.getUuid(), new Pending.FactorEdit(factor, onSave, onRemove));
        if (factor == AuthFactor.EMAIL) {
            player.sendMessage(messages.message(player, LanguagePaths.DESK_COMMAND_EMAIL_HINT));
        } else {
            player.sendMessage(messages.message(player, LanguagePaths.DESK_COMMAND_TOTP_HINT));
        }
    }

    @Override
    public void reset(Player player) {
        UUID uuid = player.getUuid();
        pending.remove(uuid);
        registerStates.remove(uuid);
    }

    private void showHubHints(Player player, Map<AuthFactor, Boolean> factors) {
        player.sendMessage(messages.message(player, LanguagePaths.DESK_COMMAND_HINT));
        if (factors.containsKey(AuthFactor.PASSWORD)) {
            player.sendMessage(messages.message(player, Boolean.TRUE.equals(factors.get(AuthFactor.PASSWORD))
                    ? LanguagePaths.DESK_COMMAND_PASSWORD_HINT
                    : LanguagePaths.DESK_COMMAND_CREATE_HINT));
        }
        if (factors.containsKey(AuthFactor.EMAIL)) {
            player.sendMessage(messages.message(player, LanguagePaths.DESK_COMMAND_EMAIL_HINT));
        }
        if (factors.containsKey(AuthFactor.TWO_FACTOR)) {
            player.sendMessage(messages.message(player, LanguagePaths.DESK_COMMAND_TOTP_HINT));
        }
        player.sendMessage(messages.message(player, LanguagePaths.DESK_COMMAND_DONE_HINT));
    }
}
