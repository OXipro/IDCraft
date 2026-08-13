package com.oxipro.idcraft.plugin.velocity.language.defaultLanguage;

import com.oxipro.cmu.configlang.standalone.config.ConfigFile;
import com.oxipro.cmu.configlang.standalone.language.Language;
import com.oxipro.idcraft.api.configuration.ConfigsType;
import com.oxipro.idcraft.plugin.velocity.IDCraftVelocityPlugin;
import com.oxipro.idcraft.plugin.velocity.language.LanguagePaths;

import java.io.File;

public class English extends Language {

    public English(IDCraftVelocityPlugin plugin) {
        super(new ConfigFile(
                languageFile(plugin),
                plugin.getResourceAsStream(ConfigsType.LANG_EN.filePath)
        ));
        saveDefaults();
    }

    private static File languageFile(IDCraftVelocityPlugin plugin) {
        return new File(plugin.getPluginData().toFile(), ConfigsType.LANG_EN.filePath);
    }

    private void saveDefaults() {
        addDefault(LanguagePaths.LANGUAGE_FANCY_NAME, "English");
        addDefault(LanguagePaths.ERROR_NO_AUTH_SERVER, "No authentication server is available. Try again later.");
        addDefault(LanguagePaths.ERROR_AUTH_SERVER_UNAVAILABLE, "The authentication server is unavailable. Try again later.");
        addDefault(LanguagePaths.ERROR_SERVER_SWITCH_NOT_ALLOWED, "You must authenticate before switching servers.");
        addDefault(LanguagePaths.ERROR_AUTH_DENIED_PREMIUM_USERNAME_RESERVED, "This username is reserved for a premium account.");
        addDefault(LanguagePaths.ERROR_AUTH_DENIED_ACCOUNT_LOCKED, "This account is temporarily locked. Try again later.");
        addDefault(LanguagePaths.ERROR_AUTH_DENIED_WIRED, "Connection rejected.");
        addDefault(LanguagePaths.ERROR_AUTH_DENIED_IDENTITY_PROVIDER_UNAVAILABLE, "Authentication services are unavailable. Try again later.");
    }
}
