package com.oxipro.idcraft.plugin.velocity.utils;

import com.oxipro.cmu.configlang.api.language.ILanguage;
import com.oxipro.cmu.configlang.velocity.language.LanguageManager;
import com.oxipro.idcraft.api.auth.decisions.AuthDecisionDeny;
import com.oxipro.idcraft.plugin.velocity.language.LanguagePaths;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.Locale;

public class MessageUtil {

    private final LanguageManager languageManager;

    public MessageUtil(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    public Component strangerMessage(Player player, String path) {
        ILanguage language = resolveLanguage(player);
        return Component.text(language.getMessage(path));
    }

    public Component authDenyStranger(Player player, AuthDecisionDeny.AuthDenyReason reason) {
        String path = LanguagePaths.ERROR_AUTH_DENIED_PREFIX + reason.name().toLowerCase(Locale.ROOT).replace('_', '-');
        return strangerMessage(player, path);
    }

    private ILanguage resolveLanguage(Player player) {
        if (player != null) {
            return languageManager.detectPlayerLanguage(player);
        }
        Locale fallback = languageManager.getFallbackLocale();
        ILanguage language = languageManager.getLanguage(fallback);
        if (language != null) {
            return language;
        }
        return languageManager.getLanguage(Locale.US);
    }
}
