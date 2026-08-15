package com.oxipro.idcraft.minestom.auth.prompt.prompts;

import com.oxipro.cmu.configlang.api.language.ILanguage;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.core.utils.message.PlaceholderKeys;
import com.oxipro.idcraft.core.utils.message.Placeholders;
import com.oxipro.idcraft.minestom.auth.prompt.AuthPromptConfig;
import com.oxipro.idcraft.minestom.auth.prompt.IAuthPrompt;
import com.oxipro.idcraft.minestom.auth.prompt.LoginSubmission;
import com.oxipro.idcraft.minestom.auth.prompt.PasswordChangeSubmission;
import com.oxipro.idcraft.minestom.auth.prompt.RegisterSubmission;
import com.oxipro.idcraft.minestom.language.LanguagePaths;
import com.oxipro.idcraft.minestom.utils.MessageUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.dialog.*;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerConfigCustomClickEvent;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DialogAuthPrompt implements IAuthPrompt {

    private static final String FIELD_PASSWORD = "password";
    private static final String FIELD_CONFIRM_PASSWORD = "confirm_password";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_TOTP = "totp_code";
    private static final String FIELD_FACTOR_VALUE = "factor_value";

    private static final Key LOGIN_SUBMIT = Key.key("idcraft:auth_login_submit");
    private static final Key REGISTER_SUBMIT = Key.key("idcraft:auth_register_submit");
    private static final Key FACTOR_SUBMIT = Key.key("idcraft:auth_factor_submit");
    private static final Key FACTOR_CANCEL = Key.key("idcraft:auth_factor_cancel");
    private static final Key DESK_DONE = Key.key("idcraft:desk_done");
    private static final Key DESK_PASSWORD_SUBMIT = Key.key("idcraft:desk_password_submit");
    private static final Key DESK_PASSWORD_BACK = Key.key("idcraft:desk_password_back");
    private static final Key DESK_FACTOR_SAVE = Key.key("idcraft:desk_factor_save");
    private static final Key DESK_FACTOR_REMOVE = Key.key("idcraft:desk_factor_remove");
    private static final Key DESK_FACTOR_BACK = Key.key("idcraft:desk_factor_back");
    private static final String FIELD_CURRENT = "current_password";
    private static final String FIELD_NEW = "new_password";

    private sealed interface Pending permits Pending.Login, Pending.Register, Pending.FactorSetup,
            Pending.Hub, Pending.Password, Pending.FactorEdit {
        record Login(Consumer<LoginSubmission> onSubmit, boolean needTotp) implements Pending {}
        record Register() implements Pending {}
        record FactorSetup(AuthFactor factor) implements Pending {}
        record Hub(Consumer<AuthFactor> onOpen, Runnable onDone, Map<AuthFactor, Boolean> factors) implements Pending {}
        record Password(Consumer<PasswordChangeSubmission> onSubmit, Runnable onBack, boolean requireCurrent, boolean createAccount) implements Pending {}
        record FactorEdit(AuthFactor factor, boolean enrolled, boolean requireCurrent,
                          BiConsumer<String, String> onSave, Consumer<String> onRemove, Runnable onBack) implements Pending {}
    }

    private static final class RegisterState {
        private final Consumer<RegisterSubmission> onSubmit;
        private final Map<AuthFactor, Consumer<String>> factorSetupHandlers;
        private final Set<AuthFactor> completedFactors = EnumSet.noneOf(AuthFactor.class);

        private RegisterState(Consumer<RegisterSubmission> onSubmit, Map<AuthFactor, Consumer<String>> factorSetupHandlers) {
            this.onSubmit = onSubmit;
            this.factorSetupHandlers = factorSetupHandlers;
        }
    }

    private final MessageUtil messages;
    private final AuthPromptConfig config;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final Map<UUID, RegisterState> registerStates = new ConcurrentHashMap<>();
    private final Map<UUID, Consumer<String>> activeFactorConsumer = new ConcurrentHashMap<>();

    public DialogAuthPrompt(MessageUtil messages, AuthPromptConfig config) {
        this.messages = messages;
        this.config = config;
    }

    public void registerListeners() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerCustomClickEvent.class, event ->
                handleClick(event.getPlayer(), event.getKey(), event.getPayload()));
        MinecraftServer.getGlobalEventHandler().addListener(PlayerConfigCustomClickEvent.class, event ->
                handleClick(event.getPlayer(), event.getKey(), event.getPayload()));
        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, e -> reset(e.getPlayer()));
    }

    @Override
    public void requestLogin(Player player, Consumer<LoginSubmission> onSubmit) {
        requestLogin(player, onSubmit, false);
    }

    @Override
    public void requestLogin(Player player, Consumer<LoginSubmission> onSubmit, boolean needTotp) {
        pending.put(player.getUuid(), new Pending.Login(onSubmit, needTotp));
        showLoginDialog(player, null, needTotp);
    }

    @Override
    public void requestRegister(Player player, Consumer<RegisterSubmission> onSubmit, Map<AuthFactor, Consumer<String>> factorSetupHandlers) {
        registerStates.put(player.getUuid(), new RegisterState(onSubmit, factorSetupHandlers));
        pending.put(player.getUuid(), new Pending.Register());
        showRegisterDialog(player, null);
    }

    @Override
    public void requestFactorSetup(Player player, AuthFactor factor, Consumer<String> onSubmit) {
        pending.put(player.getUuid(), new Pending.FactorSetup(factor));
        activeFactorConsumer.put(player.getUuid(), onSubmit);
        showFactorSetupDialog(player, factor, null);
    }

    @Override
    public void markFactorCompleted(Player player, AuthFactor factor) {
        RegisterState state = registerStates.get(player.getUuid());
        if (state == null) {
            return;
        }
        state.completedFactors.add(factor);
        pending.put(player.getUuid(), new Pending.Register());
        showRegisterDialog(player, null);
    }

    @Override
    public void notifyError(Player player, Component message) {
        Pending state = pending.get(player.getUuid());
        if (state instanceof Pending.Login login) {
            showLoginDialog(player, message, login.needTotp());
        } else if (state instanceof Pending.FactorSetup factorSetup) {
            showFactorSetupDialog(player, factorSetup.factor(), message);
        } else if (state instanceof Pending.Hub hub) {
            showHub(player, hub.factors(), message);
        } else if (state instanceof Pending.Password password) {
            showPassword(player, password.requireCurrent(), password.createAccount(), message);
        } else if (state instanceof Pending.FactorEdit edit) {
            showFactorEdit(player, edit.factor(), edit.enrolled(), edit.requireCurrent(), message);
        } else {
            showRegisterDialog(player, message);
        }
    }

    @Override
    public void notifyInfo(Player player, Component message) {
        player.sendMessage(message);
    }

    @Override
    public void requestAccountHub(Player player, Map<AuthFactor, Boolean> factors, Consumer<AuthFactor> onOpen, Runnable onDone) {
        pending.put(player.getUuid(), new Pending.Hub(onOpen, onDone, factors));
        showHub(player, factors, null);
    }

    @Override
    public void requestPasswordChange(
            Player player,
            boolean requireCurrent,
            boolean createAccount,
            Consumer<PasswordChangeSubmission> onSubmit,
            Runnable onBack
    ) {
        pending.put(player.getUuid(), new Pending.Password(onSubmit, onBack, requireCurrent, createAccount));
        showPassword(player, requireCurrent, createAccount, null);
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
        pending.put(player.getUuid(), new Pending.FactorEdit(factor, enrolled, requireCurrent, onSave, onRemove, onBack));
        showFactorEdit(player, factor, enrolled, requireCurrent, null);
    }

    @Override
    public void reset(Player player) {
        UUID uuid = player.getUuid();
        pending.remove(uuid);
        registerStates.remove(uuid);
        activeFactorConsumer.remove(uuid);
    }

    private void handleClick(Player player, Key key, BinaryTag payload) {
        UUID uuid = player.getUuid();
        Pending state = pending.get(uuid);
        if (state == null) {
            return;
        }

        if (state instanceof Pending.Hub hub) {
            if (key.equals(DESK_DONE)) {
                hub.onDone().run();
                return;
            }
            AuthFactor opened = deskFactorFromKey(key);
            if (opened != null) {
                hub.onOpen().accept(opened);
            }
            return;
        }
        if (state instanceof Pending.Password password) {
            if (key.equals(DESK_PASSWORD_BACK)) {
                password.onBack().run();
                return;
            }
            if (key.equals(DESK_PASSWORD_SUBMIT)) {
                password.onSubmit().accept(new PasswordChangeSubmission(
                        readField(payload, FIELD_CURRENT),
                        readField(payload, FIELD_NEW),
                        readField(payload, FIELD_CONFIRM_PASSWORD)
                ));
            }
            return;
        }
        if (state instanceof Pending.FactorEdit edit) {
            if (key.equals(DESK_FACTOR_BACK)) {
                edit.onBack().run();
                return;
            }
            if (key.equals(DESK_FACTOR_REMOVE)) {
                edit.onRemove().accept(readField(payload, FIELD_CURRENT));
                return;
            }
            if (key.equals(DESK_FACTOR_SAVE)) {
                edit.onSave().accept(readField(payload, FIELD_FACTOR_VALUE), readField(payload, FIELD_CURRENT));
            }
            return;
        }

        if (key.equals(LOGIN_SUBMIT) && state instanceof Pending.Login login) {
            Map<AuthFactor, String> values = new EnumMap<>(AuthFactor.class);
            values.put(AuthFactor.PASSWORD, readField(payload, FIELD_PASSWORD));
            if (login.needTotp()) {
                values.put(AuthFactor.TWO_FACTOR, readField(payload, FIELD_TOTP));
            }
            login.onSubmit().accept(new LoginSubmission(values));
            return;
        }

        if (key.equals(REGISTER_SUBMIT) && state instanceof Pending.Register) {
            Map<AuthFactor, String> values = new EnumMap<>(AuthFactor.class);
            String confirm = null;
            if (config.isFactorEnabled(AuthFactor.PASSWORD)) {
                values.put(AuthFactor.PASSWORD, readField(payload, FIELD_PASSWORD));
                confirm = readField(payload, FIELD_CONFIRM_PASSWORD);
            }
            if (config.isFactorEnabled(AuthFactor.EMAIL)) {
                values.put(AuthFactor.EMAIL, readField(payload, FIELD_EMAIL));
            }
            RegisterState registerState = registerStates.get(uuid);
            if (registerState != null) {
                registerState.onSubmit.accept(new RegisterSubmission(values, confirm));
            }
            return;
        }

        if (key.equals(FACTOR_SUBMIT) && state instanceof Pending.FactorSetup) {
            Consumer<String> consumer = activeFactorConsumer.get(uuid);
            if (consumer != null) {
                consumer.accept(readField(payload, FIELD_FACTOR_VALUE));
            }
            return;
        }

        if (key.equals(FACTOR_CANCEL) && state instanceof Pending.FactorSetup) {
            pending.put(uuid, new Pending.Register());
            activeFactorConsumer.remove(uuid);
            showRegisterDialog(player, null);
            return;
        }

        if (state instanceof Pending.Register) {
            AuthFactor requested = factorFromOpenKey(key);
            if (requested != null) {
                openFactorSetupFromRegister(player, requested);
            }
        }
    }

    private void openFactorSetupFromRegister(Player player, AuthFactor factor) {
        RegisterState state = registerStates.get(player.getUuid());
        if (state == null) {
            return;
        }
        Consumer<String> handler = state.factorSetupHandlers.get(factor);
        if (handler == null) {
            return;
        }
        requestFactorSetup(player, factor, handler);
    }

    private static String readField(BinaryTag payload, String field) {
        if (payload instanceof CompoundBinaryTag compound) {
            return compound.getString(field, "");
        }
        return "";
    }

    private static Key openKey(AuthFactor factor) {
        return Key.key("idcraft:auth_open_factor_" + factor.name().toLowerCase(Locale.ROOT));
    }

    private static AuthFactor factorFromOpenKey(Key key) {
        for (AuthFactor factor : AuthFactor.values()) {
            if (factor.requiresSetupMenu() && key.equals(openKey(factor))) {
                return factor;
            }
        }
        return null;
    }

    private ILanguage resolveLoginLanguage(Player player) {
        return messages.languageOf(player);
    }

    private ILanguage resolveRegisterLanguage(Player player) {
        if (config.isDetectLanguageBeforeRegister()) {
            return messages.detectLanguage(player);
        }
        return messages.languageOf(player);
    }

    private Component text(Player player, ILanguage lang, String path) {
        return messages.message(player, lang, path);
    }

    private Component text(Player player, ILanguage lang, String path, Placeholders placeholders) {
        return messages.message(player, lang, path, placeholders);
    }

    private void showHub(Player player, Map<AuthFactor, Boolean> factors, Component error) {
        ILanguage lang = messages.languageOf(player);
        List<DialogBody> body = new ArrayList<>();
        boolean hasPassword = Boolean.TRUE.equals(factors.get(AuthFactor.PASSWORD));
        body.add(plain(text(player, lang, hasPassword
                ? LanguagePaths.DESK_HUB_INTRO
                : LanguagePaths.DESK_HUB_INTRO_NO_ACCOUNT)));
        addError(body, error);
        List<DialogActionButton> buttons = new ArrayList<>();
        for (Map.Entry<AuthFactor, Boolean> entry : factors.entrySet()) {
            Component label = factorButtonLabel(player, lang, entry.getKey());
            String status = entry.getValue()
                    ? LanguagePaths.DESK_HUB_FACTOR_LINKED
                    : LanguagePaths.DESK_HUB_FACTOR_OPEN;
            if (entry.getKey() == AuthFactor.PASSWORD) {
                label = text(player, lang, entry.getValue()
                        ? LanguagePaths.DESK_HUB_PASSWORD
                        : LanguagePaths.DESK_HUB_PASSWORD_CREATE);
            } else {
                label = label.append(Component.text(" | ")).append(text(player, lang, status));
            }
            buttons.add(button(label, deskFactorKey(entry.getKey())));
        }
        buttons.add(button(text(player, lang, LanguagePaths.DESK_HUB_DONE), DESK_DONE));
        show(player, buildDialog(text(player, lang, LanguagePaths.DESK_HUB_TITLE), body, List.of(), buttons));
    }

    private void showPassword(Player player, boolean requireCurrent, boolean createAccount, Component error) {
        ILanguage lang = messages.languageOf(player);
        List<DialogBody> body = new ArrayList<>();
        body.add(plain(text(player, lang, createAccount
                ? LanguagePaths.DESK_PASSWORD_CREATE_INTRO
                : LanguagePaths.DESK_PASSWORD_INTRO)));
        addError(body, error);
        List<DialogInput> inputs = new ArrayList<>();
        if (requireCurrent) {
            inputs.add(textField(FIELD_CURRENT, text(player, lang, LanguagePaths.DESK_PASSWORD_CURRENT), ""));
        }
        inputs.add(textField(FIELD_NEW, text(player, lang, LanguagePaths.DESK_PASSWORD_NEW), ""));
        inputs.add(textField(FIELD_CONFIRM_PASSWORD, text(player, lang, LanguagePaths.DESK_PASSWORD_CONFIRM), ""));
        List<DialogActionButton> buttons = List.of(
                button(text(player, lang, LanguagePaths.DESK_PASSWORD_SAVE), DESK_PASSWORD_SUBMIT),
                button(text(player, lang, LanguagePaths.DESK_PASSWORD_BACK), DESK_PASSWORD_BACK)
        );
        show(player, buildDialog(text(player, lang, LanguagePaths.DESK_PASSWORD_TITLE), body, inputs, buttons));
    }

    private void showFactorEdit(Player player, AuthFactor factor, boolean enrolled, boolean requireCurrent, Component error) {
        ILanguage lang = messages.languageOf(player);
        List<DialogBody> body = new ArrayList<>();
        body.add(plain(factorIntro(player, lang, factor)));
        addError(body, error);
        List<DialogInput> inputs = new ArrayList<>();
        if (requireCurrent) {
            inputs.add(textField(FIELD_CURRENT, text(player, lang, LanguagePaths.DESK_FACTOR_CURRENT), ""));
        }
        if (!enrolled) {
            inputs.add(textField(FIELD_FACTOR_VALUE, factorFieldLabel(player, lang, factor), ""));
        }
        List<DialogActionButton> buttons = new ArrayList<>();
        if (enrolled) {
            buttons.add(button(text(player, lang, LanguagePaths.DESK_FACTOR_REMOVE), DESK_FACTOR_REMOVE));
        } else {
            buttons.add(button(text(player, lang, LanguagePaths.DESK_FACTOR_ENROLL), DESK_FACTOR_SAVE));
        }
        buttons.add(button(text(player, lang, LanguagePaths.DESK_FACTOR_BACK), DESK_FACTOR_BACK));
        show(player, buildDialog(factorTitle(player, lang, factor), body, inputs, buttons));
    }

    private static Key deskFactorKey(AuthFactor factor) {
        return Key.key("idcraft:desk_open_" + factor.name().toLowerCase(Locale.ROOT));
    }

    private static AuthFactor deskFactorFromKey(Key key) {
        for (AuthFactor factor : AuthFactor.values()) {
            if (key.equals(deskFactorKey(factor))) {
                return factor;
            }
        }
        return null;
    }

    private void showLoginDialog(Player player, Component error, boolean needTotp) {
        ILanguage lang = resolveLoginLanguage(player);

        List<DialogBody> body = new ArrayList<>();
        body.add(plain(text(player, lang, LanguagePaths.LOGIN_INTRO)));
        addError(body, error);

        List<DialogInput> inputs = new ArrayList<>();
        inputs.add(textField(FIELD_PASSWORD, text(player, lang, LanguagePaths.LOGIN_PASSWORD_FIELD), ""));
        if (needTotp) {
            inputs.add(textField(FIELD_TOTP, text(player, lang, LanguagePaths.TWO_FACTOR_CODE_FIELD), ""));
        }

        List<DialogActionButton> buttons = List.of(
                button(text(player, lang, LanguagePaths.LOGIN_SUBMIT_BUTTON), LOGIN_SUBMIT)
        );

        show(player, buildDialog(text(player, lang, LanguagePaths.LOGIN_TITLE), body, inputs, buttons));
    }

    private void showRegisterDialog(Player player, Component error) {
        RegisterState state = registerStates.get(player.getUuid());
        if (state == null) {
            return;
        }
        ILanguage lang = resolveRegisterLanguage(player);

        List<DialogBody> body = new ArrayList<>();
        body.add(plain(text(player, lang, LanguagePaths.REGISTER_INTRO)));
        if (config.isFactorEnabled(AuthFactor.PASSWORD)) {
            body.add(plain(passwordHint(player, lang)));
        }
        addError(body, error);

        List<DialogInput> inputs = new ArrayList<>();
        if (config.isFactorEnabled(AuthFactor.PASSWORD)) {
            inputs.add(textField(FIELD_PASSWORD, text(player, lang, LanguagePaths.REGISTER_PASSWORD_FIELD), ""));
            inputs.add(textField(FIELD_CONFIRM_PASSWORD, text(player, lang, LanguagePaths.REGISTER_CONFIRM_FIELD), ""));
        }
        if (config.isFactorEnabled(AuthFactor.EMAIL)) {
            inputs.add(textField(FIELD_EMAIL, text(player, lang, LanguagePaths.REGISTER_EMAIL_FIELD), ""));
        }

        List<DialogActionButton> buttons = new ArrayList<>();
        buttons.add(button(text(player, lang, LanguagePaths.REGISTER_SUBMIT_BUTTON), REGISTER_SUBMIT));

        for (AuthFactor factor : AuthFactor.values()) {
            if (!factor.requiresSetupMenu() || !config.isFactorEnabled(factor)) {
                continue;
            }
            if (!state.factorSetupHandlers.containsKey(factor)) {
                continue;
            }
            Component label = factorButtonLabel(player, lang, factor);
            if (state.completedFactors.contains(factor)) {
                label = Component.text("\u2714 ").append(label);
            }
            buttons.add(button(label, openKey(factor)));
        }

        show(player, buildDialog(text(player, lang, LanguagePaths.REGISTER_TITLE), body, inputs, buttons));
    }

    private void showFactorSetupDialog(Player player, AuthFactor factor, Component error) {
        ILanguage lang = resolveRegisterLanguage(player);

        List<DialogBody> body = new ArrayList<>();
        body.add(plain(factorIntro(player, lang, factor)));
        addError(body, error);

        List<DialogInput> inputs = List.of(
                textField(FIELD_FACTOR_VALUE, factorFieldLabel(player, lang, factor), "")
        );

        List<DialogActionButton> buttons = List.of(
                button(text(player, lang, LanguagePaths.FACTOR_SUBMIT_BUTTON), FACTOR_SUBMIT),
                button(text(player, lang, LanguagePaths.FACTOR_CANCEL_BUTTON), FACTOR_CANCEL)
        );

        show(player, buildDialog(factorTitle(player, lang, factor), body, inputs, buttons));
    }

    private Component passwordHint(Player player, ILanguage lang) {
        Placeholders placeholders = Placeholders.of(PlaceholderKeys.MIN_LENGTH, config.getMinPasswordLength());
        Component hint = text(player, lang, LanguagePaths.REGISTER_PASSWORD_HINT, placeholders)
                .append(Component.space())
                .append(text(player, lang, LanguagePaths.REGISTER_PASSWORD_NO_SPACES_HINT, placeholders));
        if (config.isPasswordRegexEnabled()) {
            hint = hint.append(Component.space())
                    .append(text(player, lang, LanguagePaths.REGISTER_PASSWORD_REGEX_HINT, placeholders));
        }
        return hint;
    }

    private Component factorTitle(Player player, ILanguage lang, AuthFactor factor) {
        if (factor == AuthFactor.TWO_FACTOR) {
            return text(player, lang, LanguagePaths.TWO_FACTOR_TITLE);
        }
        if (factor == AuthFactor.EMAIL) {
            return text(player, lang, LanguagePaths.FACTOR_EMAIL_TITLE);
        }
        return text(player, lang, LanguagePaths.FACTOR_GENERIC_TITLE);
    }

    private Component factorIntro(Player player, ILanguage lang, AuthFactor factor) {
        if (factor == AuthFactor.TWO_FACTOR) {
            return text(player, lang, LanguagePaths.TWO_FACTOR_INTRO);
        }
        if (factor == AuthFactor.EMAIL) {
            return text(player, lang, LanguagePaths.FACTOR_EMAIL_INTRO);
        }
        return text(player, lang, LanguagePaths.FACTOR_GENERIC_INTRO);
    }

    private Component factorFieldLabel(Player player, ILanguage lang, AuthFactor factor) {
        if (factor == AuthFactor.TWO_FACTOR) {
            return text(player, lang, LanguagePaths.TWO_FACTOR_CODE_FIELD);
        }
        if (factor == AuthFactor.EMAIL) {
            return text(player, lang, LanguagePaths.FACTOR_EMAIL_FIELD);
        }
        return text(player, lang, LanguagePaths.FACTOR_GENERIC_FIELD);
    }

    private Component factorButtonLabel(Player player, ILanguage lang, AuthFactor factor) {
        if (factor == AuthFactor.TWO_FACTOR) {
            return text(player, lang, LanguagePaths.TWO_FACTOR_BUTTON);
        }
        if (factor == AuthFactor.EMAIL) {
            return text(player, lang, LanguagePaths.FACTOR_EMAIL_BUTTON);
        }
        return text(player, lang, LanguagePaths.FACTOR_GENERIC_BUTTON);
    }

    private static void addError(List<DialogBody> body, Component error) {
        if (error != null) {
            body.add(new DialogBody.PlainMessage(error.color(NamedTextColor.RED), DialogBody.PlainMessage.DEFAULT_WIDTH));
        }
    }

    private static DialogBody.PlainMessage plain(Component text) {
        return new DialogBody.PlainMessage(text, DialogBody.PlainMessage.DEFAULT_WIDTH);
    }

    private static DialogInput.Text textField(String key, Component label, String initial) {
        // 4th arg is labelVisible, not a password mask. Vanilla text inputs have no mask field.
        return new DialogInput.Text(key, DialogInput.DEFAULT_WIDTH, label, true, initial, 64, null);
    }

    private static DialogActionButton button(Component label, Key actionKey) {
        return new DialogActionButton(label, null, DialogActionButton.DEFAULT_WIDTH, new DialogAction.DynamicCustom(actionKey, null));
    }

    private static Dialog buildDialog(Component title, List<DialogBody> body, List<DialogInput> inputs, List<DialogActionButton> buttons) {
        DialogMetadata metadata = new DialogMetadata(title, null, false, false, DialogAfterAction.CLOSE, body, inputs);
        // Client codec requires columns > 0; vanilla/Minestom default is 2.
        return new Dialog.MultiAction(metadata, buttons, null, 2);
    }

    private static void show(Player player, Dialog dialog) {
        player.sendPacket(new ShowDialogPacket(dialog));
    }
}
