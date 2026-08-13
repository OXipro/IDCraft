package com.oxipro.idcraft.core.auth;

import com.oxipro.cmu.configlang.api.config.IConfigFile;
import com.oxipro.idcraft.api.auth.AuthMode;
import com.oxipro.idcraft.core.configuration.paths.CommonMainConfigPaths;

import java.time.Duration;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AuthManagerConfig {

    private final AuthMode authMode;
    private final boolean useSessions;
    private final Duration sessionTtl;
    private final boolean sessionSupportCrack;
    private final boolean sessionSupportPremium;
    private final boolean skipAuthFloodgate;
    private final boolean skipAuthPremium;
    private final boolean allowOnPremiumUsername;
    private final int minPasswordLength;
    private final boolean passwordRegexEnabled;
    private final String passwordRegex;
    private final Pattern passwordPattern;
    private final boolean loginCooldownEnabled;
    private final int maxFailedAttempts;
    private final Duration loginCooldown;

    private AuthManagerConfig(Builder b) {
        this.authMode = b.authMode;
        this.useSessions = b.useSessions;
        this.sessionTtl = b.sessionTtl;
        this.sessionSupportCrack = b.sessionSupportCrack;
        this.sessionSupportPremium = b.sessionSupportPremium;
        this.skipAuthFloodgate = b.skipAuthFloodgate;
        this.skipAuthPremium = b.skipAuthPremium;
        this.allowOnPremiumUsername = b.allowOnPremiumUsername;
        this.minPasswordLength = b.minPasswordLength;
        this.passwordRegexEnabled = b.passwordRegexEnabled;
        this.passwordRegex = b.passwordRegex;
        this.passwordPattern = b.passwordPattern;
        this.loginCooldownEnabled = b.loginCooldownEnabled;
        this.maxFailedAttempts = b.maxFailedAttempts;
        this.loginCooldown = b.loginCooldown;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder fromConfig(IConfigFile config) {
        String modeRaw = config.getString(CommonMainConfigPaths.AUTH_MODE);
        AuthMode mode = AuthMode.MIXED;
        if (modeRaw != null) {
            try {
                mode = AuthMode.valueOf(modeRaw.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                mode = AuthMode.MIXED;
            }
        }

        int minLen = config.getInt(CommonMainConfigPaths.AUTH_PASSWORD_MIN_LENGTH);
        if (minLen <= 0) {
            minLen = 4;
        }

        int maxFailed = config.getInt(CommonMainConfigPaths.AUTH_LOGIN_COOLDOWN_MAX_FAILED);
        if (maxFailed <= 0) {
            maxFailed = 5;
        }

        long cooldownMin = config.getLong(CommonMainConfigPaths.AUTH_LOGIN_COOLDOWN_MINUTES);
        if (cooldownMin <= 0) {
            cooldownMin = 15L;
        }

        return builder()
                .authMode(mode)
                .useSessions(config.getBoolean(CommonMainConfigPaths.AUTH_SESSION_ENABLED))
                .sessionTtl(Duration.ofMinutes(config.getLong(CommonMainConfigPaths.AUTH_SESSION_TTL)))
                .sessionSupportCrack(config.getBoolean(CommonMainConfigPaths.AUTH_SESSION_SUPPORT_CRACK))
                .sessionSupportPremium(config.getBoolean(CommonMainConfigPaths.AUTH_SESSION_SUPPORT_PREMIUM))
                .skipAuthFloodgate(config.getBoolean(CommonMainConfigPaths.AUTH_FLOODGATE_SKIP_AUTH))
                .skipAuthPremium(config.getBoolean(CommonMainConfigPaths.AUTH_PREMIUM_SKIP_AUTH))
                .allowOnPremiumUsername(config.getBoolean(CommonMainConfigPaths.AUTH_CRACK_ALLOW_ON_PREMIUM_USERNAME))
                .minPasswordLength(minLen)
                .passwordRegexEnabled(config.getBoolean(CommonMainConfigPaths.AUTH_PASSWORD_REGEX_ENABLED))
                .passwordRegex(config.getString(CommonMainConfigPaths.AUTH_PASSWORD_REGEX_EXPRESSION))
                .loginCooldownEnabled(config.getBoolean(CommonMainConfigPaths.AUTH_LOGIN_COOLDOWN_ENABLED))
                .maxFailedAttempts(maxFailed)
                .loginCooldown(Duration.ofMinutes(cooldownMin));
    }

    public static class Builder {
        private AuthMode authMode = AuthMode.MIXED;
        private boolean useSessions = true;
        private Duration sessionTtl = Duration.ofHours(24);
        private boolean sessionSupportCrack = true;
        private boolean sessionSupportPremium = true;
        private boolean skipAuthFloodgate = true;
        private boolean skipAuthPremium = true;
        private boolean allowOnPremiumUsername = false;
        private int minPasswordLength = 8;
        private boolean passwordRegexEnabled = false;
        private String passwordRegex = null;
        private Pattern passwordPattern = null;
        private boolean loginCooldownEnabled = true;
        private int maxFailedAttempts = 5;
        private Duration loginCooldown = Duration.ofMinutes(15);

        public Builder authMode(AuthMode v) {
            this.authMode = v;
            return this;
        }

        public Builder useSessions(boolean v) {
            this.useSessions = v;
            return this;
        }

        public Builder sessionTtl(Duration v) {
            this.sessionTtl = v;
            return this;
        }

        public Builder sessionSupportCrack(boolean v) {
            this.sessionSupportCrack = v;
            return this;
        }

        public Builder sessionSupportPremium(boolean v) {
            this.sessionSupportPremium = v;
            return this;
        }

        public Builder skipAuthFloodgate(boolean v) {
            this.skipAuthFloodgate = v;
            return this;
        }

        public Builder skipAuthPremium(boolean v) {
            this.skipAuthPremium = v;
            return this;
        }

        public Builder allowOnPremiumUsername(boolean v) {
            this.allowOnPremiumUsername = v;
            return this;
        }

        public Builder minPasswordLength(int v) {
            this.minPasswordLength = v;
            return this;
        }

        public Builder passwordRegexEnabled(boolean v) {
            this.passwordRegexEnabled = v;
            return this;
        }

        public Builder passwordRegex(String v) {
            this.passwordRegex = v;
            return this;
        }

        public Builder loginCooldownEnabled(boolean v) {
            this.loginCooldownEnabled = v;
            return this;
        }

        public Builder maxFailedAttempts(int v) {
            this.maxFailedAttempts = v;
            return this;
        }

        public Builder loginCooldown(Duration v) {
            this.loginCooldown = v;
            return this;
        }

        public AuthManagerConfig build() {
            this.passwordPattern = compileRegex(passwordRegexEnabled, passwordRegex);
            if (passwordRegexEnabled && passwordPattern == null) {
                this.passwordRegexEnabled = false;
            }
            return new AuthManagerConfig(this);
        }

        private static Pattern compileRegex(boolean enabled, String expression) {
            if (!enabled) {
                return null;
            }
            if (expression == null || expression.isEmpty()) {
                return null;
            }
            try {
                return Pattern.compile(expression);
            } catch (PatternSyntaxException e) {
                return null;
            }
        }
    }

    public AuthMode getAuthMode() {
        return authMode;
    }

    public boolean getUseSessions() {
        return useSessions;
    }

    public Duration getSessionTtl() {
        return sessionTtl;
    }

    public boolean isSessionSupportCrack() {
        return sessionSupportCrack;
    }

    public boolean isSessionSupportPremium() {
        return sessionSupportPremium;
    }

    public boolean getSkipAuthFloodgate() {
        return skipAuthFloodgate;
    }

    public boolean getSkipAuthPremium() {
        return skipAuthPremium;
    }

    public boolean isAllowOnPremiumUsername() {
        return allowOnPremiumUsername;
    }

    public int getMinPasswordLength() {
        return minPasswordLength;
    }

    public boolean isPasswordRegexEnabled() {
        return passwordRegexEnabled;
    }

    public String getPasswordRegex() {
        return passwordRegex;
    }

    public Pattern getPasswordPattern() {
        return passwordPattern;
    }

    public boolean isLoginCooldownEnabled() {
        return loginCooldownEnabled;
    }

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public Duration getLoginCooldown() {
        return loginCooldown;
    }
}
