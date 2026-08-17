package com.oxipro.idcraft.minestom.utils;

import com.oxipro.cmu.configlang.api.language.ILanguage;
import com.oxipro.cmu.configlang.minestom.language.LanguageManager;
import com.oxipro.idcraft.api.auth.AuthErrorCode;
import com.oxipro.idcraft.core.utils.message.MessageUtils;
import com.oxipro.idcraft.core.utils.message.PlaceholderKeys;
import com.oxipro.idcraft.core.utils.message.Placeholders;
import com.oxipro.idcraft.minestom.language.LanguagePaths;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.net.InetSocketAddress;
import java.util.Locale;

public class MessageUtil {

    private final LanguageManager languageManager;

    public MessageUtil(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    public Component message(Player player, String path) {
        return message(player, path, Placeholders.empty());
    }

    public Component message(Player player, String path, Placeholders placeholders) {
        return message(player, languageOf(player), path, placeholders);
    }

    public Component message(ILanguage language, String path) {
        return MessageUtils.fromLanguage(language, path);
    }

    public Component message(ILanguage language, String path, Placeholders placeholders) {
        return MessageUtils.fromLanguage(language, path, placeholders);
    }

    public Component message(Player player, ILanguage language, String path) {
        return message(player, language, path, Placeholders.empty());
    }

    public Component message(Player player, ILanguage language, String path, Placeholders placeholders) {
        return MessageUtils.fromLanguage(language, path, playerPlaceholders(player).merge(placeholders));
    }

    public Component authError(Player player, AuthErrorCode errorCode) {
        return message(player, pathFor(errorCode), Placeholders.of(PlaceholderKeys.REASON, errorCode.name()));
    }

    public ILanguage languageOf(Player player) {
        if (player != null) {
            return languageManager.getPlayerLanguage(player);
        }
        return fallbackLanguage();
    }

    public ILanguage detectLanguage(Player player) {
        if (player != null) {
            return languageManager.detectPlayerLanguage(player);
        }
        return fallbackLanguage();
    }

    private ILanguage fallbackLanguage() {
        Locale fallback = languageManager.getFallbackLocale();
        ILanguage language = languageManager.getLanguage(fallback);
        if (language != null) {
            return language;
        }
        return languageManager.getLanguage(Locale.US);
    }

    private static Placeholders playerPlaceholders(Player player) {
        if (player == null) {
            return Placeholders.empty();
        }
        Placeholders placeholders = Placeholders.of(
                PlaceholderKeys.PLAYER, player.getUsername(),
                PlaceholderKeys.PLAYER_UUID, player.getUuid()
        );
        try {
            InetSocketAddress address = (InetSocketAddress) player.getPlayerConnection().getRemoteAddress();
            if (address != null && address.getAddress() != null) {
                placeholders = placeholders.with(PlaceholderKeys.PLAYER_IP, address.getAddress().getHostAddress());
            }
        } catch (Exception ignored) {
            // remote address is optional
        }
        return placeholders;
    }

    private static String pathFor(AuthErrorCode errorCode) {
        return switch (errorCode) {
            case PASSWORD_TOO_SHORT -> LanguagePaths.ERROR_PASSWORD_TOO_SHORT;
            case PASSWORD_CONTAINS_SPACES -> LanguagePaths.ERROR_PASSWORD_CONTAINS_SPACES;
            case PASSWORD_REGEX_MISMATCH -> LanguagePaths.ERROR_PASSWORD_REGEX_MISMATCH;
            case PASSWORDS_DO_NOT_MATCH -> LanguagePaths.ERROR_PASSWORDS_DO_NOT_MATCH;
            case RATE_LIMITED -> LanguagePaths.ERROR_RATE_LIMITED;
            case ACCOUNT_ALREADY_EXISTS, PREMIUM_USERNAME_RESERVED -> LanguagePaths.ERROR_ACCOUNT_ALREADY_EXISTS;
            case ACCOUNT_NOT_FOUND -> LanguagePaths.ERROR_ACCOUNT_NOT_FOUND;
            case ACCOUNT_LOCKED -> LanguagePaths.ERROR_ACCOUNT_LOCKED;
            case FACTOR_INVALID -> LanguagePaths.ERROR_FACTOR_INVALID;
            case FACTOR_REQUIRED -> LanguagePaths.ERROR_FACTOR_REQUIRED;
            case FACTOR_NOT_ENROLLED -> LanguagePaths.ERROR_FACTOR_NOT_ENROLLED;
            case FACTOR_ALREADY_ENROLLED -> LanguagePaths.ERROR_FACTOR_ALREADY_ENROLLED;
            case PROVIDER_UNAVAILABLE -> LanguagePaths.ERROR_PROVIDER_UNAVAILABLE;
            case WRONG_PASSWORD, UUID_MISMATCH -> LanguagePaths.ERROR_WRONG_PASSWORD;
        };
    }
}
