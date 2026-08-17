package com.oxipro.idcraft.minestom.auth.prompt;

import com.oxipro.cmu.configlang.standalone.config.ConfigFile;
import com.oxipro.idcraft.api.auth.AuthFactor;
import com.oxipro.idcraft.minestom.configuration.paths.MainConfigPaths;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AuthPromptConfig {

    public enum PromptType { DIALOG, COMMAND, AUTO }

    public enum DialogPhase { PLAYER_CONFIG, PLAYER_JOIN }

    public static final int MIN_DIALOG_PROTOCOL = 771;

    private final PromptType promptType;
    private final DialogPhase dialogPhase;
    private final Set<AuthFactor> requiredFactors;
    private final Set<AuthFactor> optionalFactors;
    private final int minPasswordLength;
    private final boolean passwordRegexEnabled;
    private final Pattern passwordPattern;
    private final String emailProvider;
    private final String twoFactorProvider;
    private final boolean detectLanguageBeforeRegister;
    private final boolean registerLanguageSelector;

    private AuthPromptConfig(Builder b) {
        this.promptType = b.promptType;
        this.dialogPhase = b.dialogPhase;
        this.requiredFactors = b.requiredFactors;
        this.optionalFactors = b.optionalFactors;
        this.minPasswordLength = b.minPasswordLength;
        this.passwordRegexEnabled = b.passwordRegexEnabled;
        this.passwordPattern = b.passwordPattern;
        this.emailProvider = b.emailProvider;
        this.twoFactorProvider = b.twoFactorProvider;
        this.detectLanguageBeforeRegister = b.detectLanguageBeforeRegister;
        this.registerLanguageSelector = b.registerLanguageSelector;
    }

    public static AuthPromptConfig fromConfig(ConfigFile config) {
        Builder b = new Builder();

        b.promptType = parseEnum(config.getString(MainConfigPaths.AUTH_PROMPT_TYPE), PromptType.class, PromptType.DIALOG);
        b.dialogPhase = parseEnum(config.getString(MainConfigPaths.AUTH_PROMPT_DIALOG_PHASE), DialogPhase.class, DialogPhase.PLAYER_JOIN);

        b.requiredFactors = parseFactors(config.getStringList(MainConfigPaths.AUTH_METHODS_REQUIRED));
        b.optionalFactors = parseFactors(config.getStringList(MainConfigPaths.AUTH_METHODS_OPTIONAL));
        b.optionalFactors.removeAll(b.requiredFactors);

        b.minPasswordLength = config.getInt(MainConfigPaths.AUTH_METHODS_PASSWORD_MIN_LENGTH);

        b.passwordRegexEnabled = config.getBoolean(MainConfigPaths.AUTH_METHODS_PASSWORD_REGEX_ENABLED);
        b.passwordPattern = compileRegex(b.passwordRegexEnabled, config.getString(MainConfigPaths.AUTH_METHODS_PASSWORD_REGEX_EXPRESSION));
        if (b.passwordRegexEnabled && b.passwordPattern == null) {
            b.passwordRegexEnabled = false;
        }

        b.emailProvider = config.getString(MainConfigPaths.AUTH_METHODS_EMAIL_PROVIDER);
        b.twoFactorProvider = config.getString(MainConfigPaths.AUTH_METHODS_2FA_PROVIDER);
        try {
            b.detectLanguageBeforeRegister = config.getBoolean(MainConfigPaths.AUTH_LANGUAGE_DETECT_BEFORE_REGISTER);
        } catch (Exception e) {
            b.detectLanguageBeforeRegister = true;
        }
        try {
            b.registerLanguageSelector = config.getBoolean(MainConfigPaths.AUTH_LANGUAGE_REGISTER_SELECTOR);
        } catch (Exception e) {
            b.registerLanguageSelector = true;
        }

        return b.build();
    }

    private static <E extends Enum<E>> E parseEnum(String raw, Class<E> type, E fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static Set<AuthFactor> parseFactors(List<String> raw) {
        Set<AuthFactor> result = EnumSet.noneOf(AuthFactor.class);
        if (raw == null) {
            return result;
        }
        for (String key : raw) {
            AuthFactor factor = AuthFactor.fromConfigKey(key);
            if (factor != null) {
                result.add(factor);
            }
        }
        return result;
    }

    private static Pattern compileRegex(boolean enabled, String expression) {
        if (!enabled || expression == null || expression.isEmpty()) {
            return null;
        }
        try {
            return Pattern.compile(expression);
        } catch (PatternSyntaxException e) {
            return null;
        }
    }

    private static class Builder {
        private PromptType promptType = PromptType.DIALOG;
        private DialogPhase dialogPhase = DialogPhase.PLAYER_JOIN;
        private Set<AuthFactor> requiredFactors = EnumSet.noneOf(AuthFactor.class);
        private Set<AuthFactor> optionalFactors = EnumSet.noneOf(AuthFactor.class);
        private int minPasswordLength = 4;
        private boolean passwordRegexEnabled = false;
        private Pattern passwordPattern = null;
        private String emailProvider = null;
        private String twoFactorProvider = null;
        private boolean detectLanguageBeforeRegister = true;
        private boolean registerLanguageSelector = true;

        private AuthPromptConfig build() {
            return new AuthPromptConfig(this);
        }
    }

    public PromptType getPromptType() { return promptType; }
    public DialogPhase getDialogPhase() { return dialogPhase; }

    public boolean usesDialogPrompt(int protocolVersion) {
        if (promptType == PromptType.COMMAND) {
            return false;
        }
        if (promptType == PromptType.DIALOG) {
            return true;
        }
        return protocolVersion >= MIN_DIALOG_PROTOCOL;
    }

    public boolean shouldHoldInConfiguration(int protocolVersion) {
        return dialogPhase == DialogPhase.PLAYER_CONFIG && usesDialogPrompt(protocolVersion);
    }

    public Set<AuthFactor> getRequiredFactors() { return requiredFactors; }
    public Set<AuthFactor> getOptionalFactors() { return optionalFactors; }

    public boolean isFactorEnabled(AuthFactor factor) {
        return requiredFactors.contains(factor) || optionalFactors.contains(factor);
    }

    public boolean isFactorRequired(AuthFactor factor) {
        return requiredFactors.contains(factor);
    }

    public int getMinPasswordLength() { return minPasswordLength; }
    public boolean isPasswordRegexEnabled() { return passwordRegexEnabled; }
    public Pattern getPasswordPattern() { return passwordPattern; }
    public String getEmailProvider() { return emailProvider; }
    public String getTwoFactorProvider() { return twoFactorProvider; }
    public boolean isDetectLanguageBeforeRegister() { return detectLanguageBeforeRegister; }
    public boolean isRegisterLanguageSelector() { return registerLanguageSelector; }
}
