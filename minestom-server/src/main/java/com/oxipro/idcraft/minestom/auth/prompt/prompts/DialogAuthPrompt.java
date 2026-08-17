package com.oxipro.idcraft.minestom.auth.prompt.prompts;

import com.oxipro.cmu.configlang.api.language.ILanguage;
import com.oxipro.cmu.configlang.api.language.Locales;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.core.utils.message.PlaceholderKeys;
import com.oxipro.idcraft.core.utils.message.Placeholders;
import com.oxipro.idcraft.minestom.auth.desk.AccountDeskConfig;
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
import net.minestom.server.network.ConnectionState;
import net.minestom.server.event.player.PlayerConfigCustomClickEvent;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;

import java.util.ArrayList;
import java.util.Comparator;
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
    private static final String FIELD_CURRENT = "current_password";
    private static final String FIELD_NEW = "new_password";

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
    private static final Key LANG_OPEN = Key.key("idcraft:lang_open");
    private static final Key LANG_BACK = Key.key("idcraft:lang_back");
    private static final String LANG_PICK_PREFIX = "idcraft:lang_pick_";

    private sealed interface Pending permits Pending.Login, Pending.Register, Pending.FactorSetup,
            Pending.Hub, Pending.Password, Pending.FactorEdit, Pending.LanguagePick {
        record Login(Consumer<LoginSubmission> onSubmit, boolean needTotp) implements Pending {}
        record Register() implements Pending {}
        record FactorSetup(AuthFactor factor) implements Pending {}
        record Hub(Consumer<AuthFactor> onOpen, Runnable onDone, Map<AuthFactor, Boolean> factors) implements Pending {}
        record Password(Consumer<PasswordChangeSubmission> onSubmit, Runnable onBack, boolean requireCurrent, boolean createAccount) implements Pending {}
        record FactorEdit(AuthFactor factor, boolean enrolled, boolean requireCurrent,
                          BiConsumer<String, String> onSave, Consumer<String> onRemove, Runnable onBack) implements Pending {}
        record LanguagePick(Pending returnTo) implements Pending {}
    }

    private static final class RegisterState {
        private final Consumer<RegisterSubmission> onSubmit;
        private final Map<AuthFactor, Consumer<String>> factorSetupHandlers;
        private final Set<AuthFactor> completedFactors = EnumSet.noneOf(AuthFactor.class);
        private String draftPassword = "";
        private String draftConfirm = "";
        private String draftEmail = "";

        private RegisterState(Consumer<RegisterSubmission> onSubmit, Map<AuthFactor, Consumer<String>> factorSetupHandlers) {
            this.onSubmit = onSubmit;
            this.factorSetupHandlers = factorSetupHandlers;
        }

        private void saveDraft(String password, String confirm, String email) {
            if (password != null && !password.isEmpty()) {
                draftPassword = password;
            }
            if (confirm != null && !confirm.isEmpty()) {
                draftConfirm = confirm;
            }
            if (email != null && !email.isEmpty()) {
                draftEmail = email;
            }
        }
    }

    private final MessageUtil messages;
    private final AuthPromptConfig config;
    private final AccountDeskConfig deskConfig;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final Map<UUID, RegisterState> registerStates = new ConcurrentHashMap<>();
    private final Map<UUID, Consumer<String>> activeFactorConsumer = new ConcurrentHashMap<>();
    private final Map<UUID, Component> pendingInfo = new ConcurrentHashMap<>();

    public DialogAuthPrompt(MessageUtil messages, AuthPromptConfig config, AccountDeskConfig deskConfig) {
        this.messages = messages;
        this.config = config;
        this.deskConfig = deskConfig;
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
        RegisterState existing = registerStates.get(player.getUuid());
        RegisterState state = new RegisterState(onSubmit, factorSetupHandlers);
        if (existing != null) {
            state.draftPassword = existing.draftPassword;
            state.draftConfirm = existing.draftConfirm;
            state.draftEmail = existing.draftEmail;
            state.completedFactors.addAll(existing.completedFactors);
        }
        registerStates.put(player.getUuid(), state);
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
        if (canSendChat(player)) {
            player.sendMessage(message);
            return;
        }
        // CONFIG phase cannot send SystemChatPacket; show on next dialog instead
        pendingInfo.put(player.getUuid(), message);
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
        pendingInfo.remove(uuid);
        messages.clearSessionLanguage(player);
    }

    private void handleClick(Player player, Key key, BinaryTag payload) {
        UUID uuid = player.getUuid();
        Pending state = pending.get(uuid);
        if (state == null) {
            return;
        }

        if (state instanceof Pending.LanguagePick pick) {
            if (key.equals(LANG_BACK)) {
                restorePending(player, pick.returnTo());
                return;
            }
            Locale picked = localeFromPickKey(key);
            if (picked != null) {
                messages.setPlayerLanguage(player, picked);
                restorePending(player, pick.returnTo());
            }
            return;
        }

        if (key.equals(LANG_OPEN) && (state instanceof Pending.Register || state instanceof Pending.Hub)) {
            if (state instanceof Pending.Register) {
                saveRegisterDraft(uuid, payload);
            }
            openLanguagePicker(player, state);
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
            saveRegisterDraft(uuid, payload);
            Map<AuthFactor, String> values = new EnumMap<>(AuthFactor.class);
            String confirm = null;
            RegisterState registerState = registerStates.get(uuid);
            if (config.isFactorEnabled(AuthFactor.PASSWORD)) {
                String password = registerState != null ? registerState.draftPassword : readField(payload, FIELD_PASSWORD);
                confirm = registerState != null ? registerState.draftConfirm : readField(payload, FIELD_CONFIRM_PASSWORD);
                values.put(AuthFactor.PASSWORD, password);
            }
            if (config.isFactorEnabled(AuthFactor.EMAIL)) {
                String email = registerState != null ? registerState.draftEmail : readField(payload, FIELD_EMAIL);
                values.put(AuthFactor.EMAIL, email);
            }
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
                saveRegisterDraft(uuid, payload);
                openFactorSetupFromRegister(player, requested);
            }
        }
    }

    private void saveRegisterDraft(UUID uuid, BinaryTag payload) {
        RegisterState state = registerStates.get(uuid);
        if (state == null || !(payload instanceof CompoundBinaryTag compound) || compound.size() == 0) {
            return;
        }
        state.saveDraft(
                compound.getString(FIELD_PASSWORD, ""),
                compound.getString(FIELD_CONFIRM_PASSWORD, ""),
                compound.getString(FIELD_EMAIL, "")
        );
    }

    private static boolean canSendChat(Player player) {
        try {
            return player.getPlayerConnection().getServerState() == ConnectionState.PLAY;
        } catch (Exception e) {
            return false;
        }
    }

    private void openLanguagePicker(Player player, Pending returnTo) {
        pending.put(player.getUuid(), new Pending.LanguagePick(returnTo));
        showLanguagePicker(player, returnTo);
    }

    private void restorePending(Player player, Pending returnTo) {
        pending.put(player.getUuid(), returnTo);
        if (returnTo instanceof Pending.Register) {
            showRegisterDialog(player, null);
        } else if (returnTo instanceof Pending.Hub hub) {
            showHub(player, hub.factors(), null);
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
        // languageOf prefers session pick / DB; detect only when no pick yet
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

    private Component languageButtonLabel(Player player, ILanguage lang, String path) {
        String fancy = lang != null ? lang.getFancyName() : null;
        if (fancy == null || fancy.isBlank()) {
            fancy = lang != null && lang.getLocale() != null ? lang.getLocale().toString() : "?";
        }
        return messages.message(player, lang, path, Placeholders.of(PlaceholderKeys.LANGUAGE, fancy));
    }

    private void showHub(Player player, Map<AuthFactor, Boolean> factors, Component error) {
        ILanguage lang = messages.languageOf(player);
        List<DialogBody> body = new ArrayList<>();
        boolean hasPassword = Boolean.TRUE.equals(factors.get(AuthFactor.PASSWORD));
        body.add(plain(text(player, lang, hasPassword
                ? LanguagePaths.DESK_HUB_INTRO
                : LanguagePaths.DESK_HUB_INTRO_NO_ACCOUNT)));
        Component info = pendingInfo.remove(player.getUuid());
        if (info != null) {
            body.add(plain(info));
        }
        addError(body, error);
        List<DialogActionButton> actions = new ArrayList<>();
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
                label = label.append(Component.text(" | ", NamedTextColor.DARK_GRAY)).append(text(player, lang, status));
            }
            actions.add(button(label, deskFactorKey(entry.getKey())));
        }
        if (deskConfig.languageSelector()) {
            actions.add(button(languageButtonLabel(player, lang, LanguagePaths.DESK_HUB_LANGUAGE), LANG_OPEN));
        }
        DialogActionButton done = button(text(player, lang, LanguagePaths.DESK_HUB_DONE), DESK_DONE);
        show(player, buildDialog(text(player, lang, LanguagePaths.DESK_HUB_TITLE), body, List.of(), actions, done));
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
        List<DialogActionButton> actions = List.of(
                button(text(player, lang, LanguagePaths.DESK_PASSWORD_SAVE), DESK_PASSWORD_SUBMIT)
        );
        DialogActionButton back = button(text(player, lang, LanguagePaths.DESK_PASSWORD_BACK), DESK_PASSWORD_BACK);
        show(player, buildDialog(text(player, lang, LanguagePaths.DESK_PASSWORD_TITLE), body, inputs, actions, back));
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
        List<DialogActionButton> actions = new ArrayList<>();
        if (enrolled) {
            actions.add(button(text(player, lang, LanguagePaths.DESK_FACTOR_REMOVE), DESK_FACTOR_REMOVE));
        } else {
            actions.add(button(text(player, lang, LanguagePaths.DESK_FACTOR_ENROLL), DESK_FACTOR_SAVE));
        }
        DialogActionButton back = button(text(player, lang, LanguagePaths.DESK_FACTOR_BACK), DESK_FACTOR_BACK);
        show(player, buildDialog(factorTitle(player, lang, factor), body, inputs, actions, back));
    }

    private void showLanguagePicker(Player player, Pending returnTo) {
        ILanguage lang = returnTo instanceof Pending.Register
                ? resolveRegisterLanguage(player)
                : messages.languageOf(player);
        List<DialogBody> body = new ArrayList<>();
        body.add(plain(text(player, lang, LanguagePaths.LANGUAGE_PICK_TITLE)));
        List<DialogActionButton> actions = new ArrayList<>();
        List<Locale> locales = new ArrayList<>(messages.languageManager().getLocales());
        locales.sort(Comparator.comparing(Locale::toString));
        for (Locale locale : locales) {
            ILanguage option = messages.languageManager().getLanguage(locale);
            String fancy = option != null ? option.getFancyName() : null;
            if (fancy == null || fancy.isBlank()) {
                fancy = locale.toString();
            }
            actions.add(button(Component.text(fancy), langPickKey(locale)));
        }
        DialogActionButton back = button(text(player, lang, LanguagePaths.LANGUAGE_PICK_BACK), LANG_BACK);
        show(player, buildDialog(text(player, lang, LanguagePaths.LANGUAGE_PICK_TITLE), body, List.of(), actions, back));
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

    private static Key langPickKey(Locale locale) {
        return Key.key(LANG_PICK_PREFIX + locale.toString().toLowerCase(Locale.ROOT));
    }

    private static Locale localeFromPickKey(Key key) {
        String value = key.asString();
        String bare = key.value();
        String raw;
        if (value.startsWith(LANG_PICK_PREFIX)) {
            raw = value.substring(LANG_PICK_PREFIX.length());
        } else if (bare.startsWith(LANG_PICK_PREFIX.substring("idcraft:".length()))) {
            raw = bare.substring(LANG_PICK_PREFIX.substring("idcraft:".length()).length());
        } else {
            return null;
        }
        try {
            return Locales.parse(raw);
        } catch (Exception e) {
            return null;
        }
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

        List<DialogActionButton> actions = List.of(
                button(text(player, lang, LanguagePaths.LOGIN_SUBMIT_BUTTON), LOGIN_SUBMIT)
        );

        show(player, buildDialog(text(player, lang, LanguagePaths.LOGIN_TITLE), body, inputs, actions, null));
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
            inputs.add(textField(FIELD_PASSWORD, text(player, lang, LanguagePaths.REGISTER_PASSWORD_FIELD), state.draftPassword));
            inputs.add(textField(FIELD_CONFIRM_PASSWORD, text(player, lang, LanguagePaths.REGISTER_CONFIRM_FIELD), state.draftConfirm));
        }
        if (config.isFactorEnabled(AuthFactor.EMAIL)) {
            inputs.add(textField(FIELD_EMAIL, text(player, lang, LanguagePaths.REGISTER_EMAIL_FIELD),
                    state.draftEmail != null ? state.draftEmail : ""));
        }

        List<DialogActionButton> actions = new ArrayList<>();
        actions.add(button(text(player, lang, LanguagePaths.REGISTER_SUBMIT_BUTTON), REGISTER_SUBMIT));

        for (AuthFactor factor : AuthFactor.values()) {
            if (!factor.requiresSetupMenu() || !config.isFactorEnabled(factor)) {
                continue;
            }
            if (!state.factorSetupHandlers.containsKey(factor)) {
                continue;
            }
            Component label = factorButtonLabel(player, lang, factor);
            if (state.completedFactors.contains(factor)) {
                label = Component.text("\u2714 ", NamedTextColor.GREEN).append(label);
            }
            actions.add(button(label, openKey(factor)));
        }

        if (config.isRegisterLanguageSelector()) {
            actions.add(button(languageButtonLabel(player, lang, LanguagePaths.REGISTER_LANGUAGE_BUTTON), LANG_OPEN));
        }

        show(player, buildDialog(text(player, lang, LanguagePaths.REGISTER_TITLE), body, inputs, actions, null));
    }

    private void showFactorSetupDialog(Player player, AuthFactor factor, Component error) {
        ILanguage lang = resolveRegisterLanguage(player);

        List<DialogBody> body = new ArrayList<>();
        body.add(plain(factorIntro(player, lang, factor)));
        addError(body, error);

        List<DialogInput> inputs = List.of(
                textField(FIELD_FACTOR_VALUE, factorFieldLabel(player, lang, factor), "")
        );

        List<DialogActionButton> actions = List.of(
                button(text(player, lang, LanguagePaths.FACTOR_SUBMIT_BUTTON), FACTOR_SUBMIT)
        );
        DialogActionButton cancel = button(text(player, lang, LanguagePaths.FACTOR_CANCEL_BUTTON), FACTOR_CANCEL);

        show(player, buildDialog(factorTitle(player, lang, factor), body, inputs, actions, cancel));
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
        return new DialogInput.Text(key, DialogInput.DEFAULT_WIDTH, label, true, initial, 64, null);
    }

    private static DialogActionButton button(Component label, Key actionKey) {
        return new DialogActionButton(label, null, DialogActionButton.DEFAULT_WIDTH, new DialogAction.DynamicCustom(actionKey, null));
    }

    private static Dialog buildDialog(
            Component title,
            List<DialogBody> body,
            List<DialogInput> inputs,
            List<DialogActionButton> actions,
            DialogActionButton exitAction
    ) {
        DialogMetadata metadata = new DialogMetadata(title, null, false, false, DialogAfterAction.CLOSE, body, inputs);
        return new Dialog.MultiAction(metadata, actions, exitAction, 2);
    }

    private static void show(Player player, Dialog dialog) {
        player.sendPacket(new ShowDialogPacket(dialog));
    }
}
