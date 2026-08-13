package com.oxipro.idcraft.minestom.auth.prompt.prompts;

import com.oxipro.cmu.configlang.api.language.ILanguage;
import com.oxipro.cmu.configlang.minestom.language.LanguageManager;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.minestom.auth.prompt.AuthPromptConfig;
import com.oxipro.idcraft.minestom.auth.prompt.IAuthPrompt;
import com.oxipro.idcraft.minestom.auth.prompt.LoginSubmission;
import com.oxipro.idcraft.minestom.auth.prompt.RegisterSubmission;
import com.oxipro.idcraft.minestom.language.LanguagePaths;
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

    private sealed interface Pending permits Pending.Login, Pending.Register, Pending.FactorSetup {
        record Login(Consumer<LoginSubmission> onSubmit, boolean needTotp) implements Pending {}
        record Register() implements Pending {}
        record FactorSetup(AuthFactor factor) implements Pending {}
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

    private final LanguageManager languageManager;
    private final AuthPromptConfig config;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final Map<UUID, RegisterState> registerStates = new ConcurrentHashMap<>();
    private final Map<UUID, Consumer<String>> activeFactorConsumer = new ConcurrentHashMap<>();

    public DialogAuthPrompt(LanguageManager languageManager, AuthPromptConfig config) {
        this.languageManager = languageManager;
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
        } else {
            showRegisterDialog(player, message);
        }
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
        return languageManager.getPlayerLanguage(player);
    }

    private ILanguage resolveRegisterLanguage(Player player) {
        if (config.isDetectLanguageBeforeRegister()) {
            return languageManager.detectPlayerLanguage(player);
        }
        return languageManager.getPlayerLanguage(player);
    }

    private void showLoginDialog(Player player, Component error, boolean needTotp) {
        ILanguage lang = resolveLoginLanguage(player);

        List<DialogBody> body = new ArrayList<>();
        body.add(plain(lang.getMessage(LanguagePaths.LOGIN_INTRO)));
        addError(body, error);

        List<DialogInput> inputs = new ArrayList<>();
        inputs.add(textField(FIELD_PASSWORD, lang.getMessage(LanguagePaths.LOGIN_PASSWORD_FIELD), ""));
        if (needTotp) {
            inputs.add(textField(FIELD_TOTP, lang.getMessage(LanguagePaths.TWO_FACTOR_CODE_FIELD), ""));
        }

        List<DialogActionButton> buttons = List.of(
                button(lang.getMessage(LanguagePaths.LOGIN_SUBMIT_BUTTON), LOGIN_SUBMIT)
        );

        show(player, buildDialog(lang.getMessage(LanguagePaths.LOGIN_TITLE), body, inputs, buttons));
    }

    private void showRegisterDialog(Player player, Component error) {
        RegisterState state = registerStates.get(player.getUuid());
        if (state == null) {
            return;
        }
        ILanguage lang = resolveRegisterLanguage(player);

        List<DialogBody> body = new ArrayList<>();
        body.add(plain(lang.getMessage(LanguagePaths.REGISTER_INTRO)));
        if (config.isFactorEnabled(AuthFactor.PASSWORD)) {
            body.add(plain(passwordHint(lang)));
        }
        addError(body, error);

        List<DialogInput> inputs = new ArrayList<>();
        if (config.isFactorEnabled(AuthFactor.PASSWORD)) {
            inputs.add(textField(FIELD_PASSWORD, lang.getMessage(LanguagePaths.REGISTER_PASSWORD_FIELD), ""));
            inputs.add(textField(FIELD_CONFIRM_PASSWORD, lang.getMessage(LanguagePaths.REGISTER_CONFIRM_FIELD), ""));
        }
        if (config.isFactorEnabled(AuthFactor.EMAIL)) {
            inputs.add(textField(FIELD_EMAIL, lang.getMessage(LanguagePaths.REGISTER_EMAIL_FIELD), ""));
        }

        List<DialogActionButton> buttons = new ArrayList<>();
        buttons.add(button(lang.getMessage(LanguagePaths.REGISTER_SUBMIT_BUTTON), REGISTER_SUBMIT));

        for (AuthFactor factor : AuthFactor.values()) {
            if (!factor.requiresSetupMenu() || !config.isFactorEnabled(factor)) {
                continue;
            }
            if (!state.factorSetupHandlers.containsKey(factor)) {
                continue;
            }
            String label = factorButtonLabel(lang, factor);
            if (state.completedFactors.contains(factor)) {
                label = "\u2714 " + label;
            }
            buttons.add(button(label, openKey(factor)));
        }

        show(player, buildDialog(lang.getMessage(LanguagePaths.REGISTER_TITLE), body, inputs, buttons));
    }

    private void showFactorSetupDialog(Player player, AuthFactor factor, Component error) {
        ILanguage lang = resolveRegisterLanguage(player);

        List<DialogBody> body = new ArrayList<>();
        body.add(plain(factorIntro(lang, factor)));
        addError(body, error);

        List<DialogInput> inputs = List.of(
                textField(FIELD_FACTOR_VALUE, factorFieldLabel(lang, factor), "")
        );

        List<DialogActionButton> buttons = List.of(
                button(lang.getMessage(LanguagePaths.FACTOR_SUBMIT_BUTTON), FACTOR_SUBMIT),
                button(lang.getMessage(LanguagePaths.FACTOR_CANCEL_BUTTON), FACTOR_CANCEL)
        );

        show(player, buildDialog(factorTitle(lang, factor), body, inputs, buttons));
    }

    private String passwordHint(ILanguage lang) {
        String hint = String.format(lang.getMessage(LanguagePaths.REGISTER_PASSWORD_HINT), config.getMinPasswordLength());
        hint = hint + " " + lang.getMessage(LanguagePaths.REGISTER_PASSWORD_NO_SPACES_HINT);
        if (config.isPasswordRegexEnabled()) {
            hint = hint + " " + lang.getMessage(LanguagePaths.REGISTER_PASSWORD_REGEX_HINT);
        }
        return hint;
    }

    private String factorTitle(ILanguage lang, AuthFactor factor) {
        if (factor == AuthFactor.TWO_FACTOR) {
            return lang.getMessage(LanguagePaths.TWO_FACTOR_TITLE);
        }
        return lang.getMessage(LanguagePaths.FACTOR_GENERIC_TITLE);
    }

    private String factorIntro(ILanguage lang, AuthFactor factor) {
        if (factor == AuthFactor.TWO_FACTOR) {
            return lang.getMessage(LanguagePaths.TWO_FACTOR_INTRO);
        }
        return lang.getMessage(LanguagePaths.FACTOR_GENERIC_INTRO);
    }

    private String factorFieldLabel(ILanguage lang, AuthFactor factor) {
        if (factor == AuthFactor.TWO_FACTOR) {
            return lang.getMessage(LanguagePaths.TWO_FACTOR_CODE_FIELD);
        }
        return lang.getMessage(LanguagePaths.FACTOR_GENERIC_FIELD);
    }

    private String factorButtonLabel(ILanguage lang, AuthFactor factor) {
        if (factor == AuthFactor.TWO_FACTOR) {
            return lang.getMessage(LanguagePaths.TWO_FACTOR_BUTTON);
        }
        return lang.getMessage(LanguagePaths.FACTOR_GENERIC_BUTTON);
    }

    private static void addError(List<DialogBody> body, Component error) {
        if (error != null) {
            body.add(new DialogBody.PlainMessage(error.color(NamedTextColor.RED), DialogBody.PlainMessage.DEFAULT_WIDTH));
        }
    }

    private static DialogBody.PlainMessage plain(String text) {
        return new DialogBody.PlainMessage(Component.text(text), DialogBody.PlainMessage.DEFAULT_WIDTH);
    }

    private static DialogInput.Text textField(String key, String label, String initial) {
        // 4th arg is labelVisible, not a password mask. Vanilla text inputs have no mask field.
        return new DialogInput.Text(key, DialogInput.DEFAULT_WIDTH, Component.text(label), true, initial, 64, null);
    }

    private static DialogActionButton button(String label, Key actionKey) {
        return new DialogActionButton(Component.text(label), null, DialogActionButton.DEFAULT_WIDTH, new DialogAction.DynamicCustom(actionKey, null));
    }

    private static Dialog buildDialog(String title, List<DialogBody> body, List<DialogInput> inputs, List<DialogActionButton> buttons) {
        DialogMetadata metadata = new DialogMetadata(Component.text(title), null, false, false, DialogAfterAction.CLOSE, body, inputs);
        // Client codec requires columns > 0; vanilla/Minestom default is 2.
        return new Dialog.MultiAction(metadata, buttons, null, 2);
    }

    private static void show(Player player, Dialog dialog) {
        player.sendPacket(new ShowDialogPacket(dialog));
    }
}
