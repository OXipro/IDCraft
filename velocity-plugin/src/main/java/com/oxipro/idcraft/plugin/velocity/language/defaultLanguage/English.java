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
    }

    private static File languageFile(IDCraftVelocityPlugin plugin) {
        return new File(plugin.getPluginData().toFile(), ConfigsType.LANG_EN.filePath);
    }

    public void saveDefaults() {
        addDefault(LanguagePaths.LANGUAGE_FANCY_NAME, "English");
        addDefault(LanguagePaths.ERROR_NO_AUTH_SERVER,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>No authentication server is available.</red>\n<gray>Please try again later.</gray>");
        addDefault(LanguagePaths.ERROR_AUTH_SERVER_UNAVAILABLE,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>The authentication server is unavailable.</red>\n<gray>Please try again later.</gray>");
        addDefault(LanguagePaths.ERROR_SERVER_SWITCH_NOT_ALLOWED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>You must authenticate before switching servers.</red>");
        addDefault(LanguagePaths.ERROR_NO_AVAILABLE_SERVER,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>No available server found.</red>\n<gray>Please try again later.</gray>");
        addDefault(LanguagePaths.ERROR_AUTH_DENIED_PREMIUM_USERNAME_RESERVED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient>\n\n<red>This username is reserved for a premium account.</red>");
        addDefault(LanguagePaths.ERROR_AUTH_DENIED_ACCOUNT_LOCKED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient>\n\n<red>This account is temporarily locked.</red>\n<gray>Please try again later.</gray>");
        addDefault(LanguagePaths.ERROR_AUTH_DENIED_WIRED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient>\n\n<red>Connection rejected.</red>");
        addDefault(LanguagePaths.ERROR_AUTH_DENIED_IDENTITY_PROVIDER_UNAVAILABLE,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient>\n\n<red>Authentication services are unavailable.</red>\n<gray>Please try again later.</gray>");
        addDefault(LanguagePaths.ACCOUNT_DESK_DISABLED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Account management is currently disabled.</red>");
        addDefault(LanguagePaths.ACCOUNT_DESK_MUST_LOGIN,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <yellow>Finish login</yellow> <gray>before managing your account.</gray>");
        addDefault(LanguagePaths.ACCOUNT_DESK_ALREADY,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>You are already in the account desk.</gray>");
        addDefault(LanguagePaths.ACCOUNT_DESK_COOLDOWN,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>Please wait</gray> <yellow>%SECONDS%s</yellow> <gray>before using</gray> <aqua>/auth</aqua> <gray>again.</gray>");
        addDefault(LanguagePaths.ACCOUNT_DESK_TRANSFERRING,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>Opening your</gray> <yellow>account desk</yellow><gray>...</gray>");
        addDefault(LanguagePaths.ACCOUNT_DESK_LOCKED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <yellow>Finish or leave</yellow> <gray>the account desk first.</gray>");
        save(true);
    }
}
