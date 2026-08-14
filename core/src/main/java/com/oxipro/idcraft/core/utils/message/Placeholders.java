package com.oxipro.idcraft.core.utils.message;

import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PAPI-style {@code %KEY%} replacements applied before MiniMessage parsing.
 * Values are escaped so they cannot inject MiniMessage tags.
 */
public final class Placeholders {

    public static final Placeholders EMPTY = new Placeholders(Map.of());

    private static final Pattern TOKEN = Pattern.compile("%([A-Za-z0-9_]+)%");
    private static final MiniMessage ESCAPE = MiniMessage.miniMessage();

    private final Map<String, String> values;

    private Placeholders(Map<String, String> values) {
        this.values = values;
    }

    public static Placeholders empty() {
        return EMPTY;
    }

    public static Placeholders of(String key, Object value) {
        return empty().with(key, value);
    }

    public static Placeholders of(String key1, Object value1, String key2, Object value2) {
        return of(key1, value1).with(key2, value2);
    }

    public static Placeholders of(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        return of(key1, value1, key2, value2).with(key3, value3);
    }

    /**
     * Alternating key/value pairs. Keys may be written as {@code PLAYER} or {@code %PLAYER%}.
     */
    public static Placeholders of(Object... pairs) {
        if (pairs == null || pairs.length == 0) {
            return EMPTY;
        }
        if ((pairs.length & 1) != 0) {
            throw new IllegalArgumentException("Placeholders.of expects key/value pairs");
        }
        Placeholders result = EMPTY;
        for (int i = 0; i < pairs.length; i += 2) {
            Object key = pairs[i];
            if (key == null) {
                throw new IllegalArgumentException("Placeholder key at index " + i + " is null");
            }
            result = result.with(key.toString(), pairs[i + 1]);
        }
        return result;
    }

    public Placeholders with(String key, Object value) {
        Objects.requireNonNull(key, "key");
        Map<String, String> copy = new LinkedHashMap<>(values);
        copy.put(normalize(key), stringify(value));
        return new Placeholders(Collections.unmodifiableMap(copy));
    }

    public Placeholders merge(Placeholders other) {
        if (other == null || other.values.isEmpty()) {
            return this;
        }
        if (values.isEmpty()) {
            return other;
        }
        Map<String, String> copy = new LinkedHashMap<>(values);
        copy.putAll(other.values);
        return new Placeholders(Collections.unmodifiableMap(copy));
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public String apply(String input) {
        if (input == null || input.isEmpty() || values.isEmpty()) {
            return input == null ? "" : input;
        }
        Matcher matcher = TOKEN.matcher(input);
        StringBuilder out = new StringBuilder(input.length());
        while (matcher.find()) {
            String replacement = values.get(matcher.group(1).toUpperCase(Locale.ROOT));
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement != null ? replacement : matcher.group(0)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String normalize(String key) {
        String trimmed = key.trim();
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '%' && trimmed.charAt(trimmed.length() - 1) == '%') {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private static String stringify(Object value) {
        if (value == null) {
            return "";
        }
        return ESCAPE.escapeTags(String.valueOf(value));
    }
}
