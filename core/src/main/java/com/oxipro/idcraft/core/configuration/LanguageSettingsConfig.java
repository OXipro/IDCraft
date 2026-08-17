package com.oxipro.idcraft.core.configuration;

import com.oxipro.cmu.configlang.api.config.IConfigFile;
import com.oxipro.cmu.configlang.api.language.LanguageSettings;
import com.oxipro.cmu.configlang.api.language.Locales;
import com.oxipro.idcraft.core.configuration.paths.CommonMainConfigPaths;

import java.util.Locale;

public final class LanguageSettingsConfig {

    private LanguageSettingsConfig() {
    }

    public static LanguageSettings fromConfig(IConfigFile config) {
        boolean clientLocale = true;
        boolean ipLanguage = true;
        Locale fallback = Locale.US;
        try {
            clientLocale = config.getBoolean(CommonMainConfigPaths.LANGUAGE_CLIENT_LOCALE);
        } catch (Exception ignored) {
        }
        try {
            ipLanguage = config.getBoolean(CommonMainConfigPaths.LANGUAGE_IP);
        } catch (Exception ignored) {
        }
        try {
            String raw = config.getString(CommonMainConfigPaths.LANGUAGE_FALLBACK);
            if (raw != null && !raw.isBlank()) {
                fallback = Locales.parse(raw);
            }
        } catch (Exception ignored) {
        }
        return LanguageSettings.builder()
                .clientLocale(clientLocale)
                .ipLanguage(ipLanguage)
                .fallbackLocale(fallback)
                .build();
    }
}
