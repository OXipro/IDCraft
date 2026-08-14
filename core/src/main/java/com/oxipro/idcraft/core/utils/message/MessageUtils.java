package com.oxipro.idcraft.core.utils.message;

import com.oxipro.cmu.configlang.api.language.ILanguage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Shared MiniMessage parser. Resolves {@code %PLACEHOLDER%} tokens first, then deserializes.
 */
public final class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private MessageUtils() {
    }

    public static Component parse(String input) {
        return parse(input, Placeholders.empty());
    }

    public static Component parse(String input, Placeholders placeholders) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String resolved = placeholders == null ? input : placeholders.apply(input);
        try {
            return MINI_MESSAGE.deserialize(resolved);
        } catch (Exception ignored) {
            return Component.text(resolved);
        }
    }

    public static Component fromLanguage(ILanguage language, String path) {
        return fromLanguage(language, path, Placeholders.empty());
    }

    public static Component fromLanguage(ILanguage language, String path, Placeholders placeholders) {
        if (language == null || path == null || path.isEmpty()) {
            return Component.empty();
        }
        String raw = language.getMessage(path);
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        return parse(raw, placeholders);
    }
}
