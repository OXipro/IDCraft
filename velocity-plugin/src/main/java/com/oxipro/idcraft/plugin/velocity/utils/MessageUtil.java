package com.oxipro.idcraft.plugin.velocity.utils;

import com.oxipro.cmu.configlang.api.language.ILanguage;
import com.oxipro.cmu.configlang.velocity.language.LanguageManager;
import com.oxipro.idcraft.api.auth.decisions.AuthDecisionDeny;
import com.oxipro.idcraft.core.utils.message.MessageUtils;
import com.oxipro.idcraft.core.utils.message.PlaceholderKeys;
import com.oxipro.idcraft.core.utils.message.Placeholders;
import com.oxipro.idcraft.plugin.velocity.language.LanguagePaths;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

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
        return MessageUtils.fromLanguage(resolveLanguage(player), path, playerPlaceholders(player).merge(placeholders));
    }

    public Component strangerMessage(Player player, String path) {
        return message(player, path);
    }

    public Component strangerMessage(Player player, String path, Placeholders placeholders) {
        return message(player, path, placeholders);
    }

    public Component authDenyStranger(Player player, AuthDecisionDeny.AuthDenyReason reason) {
        String path = LanguagePaths.ERROR_AUTH_DENIED_PREFIX + reason.name().toLowerCase(Locale.ROOT).replace('_', '-');
        return message(player, path, Placeholders.of(PlaceholderKeys.REASON, reason.name()));
    }

    public ILanguage resolveLanguage(Player player) {
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

    private static Placeholders playerPlaceholders(Player player) {
        if (player == null) {
            return Placeholders.empty();
        }
        Placeholders placeholders = Placeholders.of(
                PlaceholderKeys.PLAYER, player.getUsername(),
                PlaceholderKeys.PLAYER_UUID, player.getUniqueId()
        );
        InetSocketAddress address = player.getRemoteAddress();
        if (address != null && address.getAddress() != null) {
            placeholders = placeholders.with(PlaceholderKeys.PLAYER_IP, address.getAddress().getHostAddress());
        }
        return placeholders;
    }
}
