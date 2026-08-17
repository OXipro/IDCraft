package com.oxipro.idcraft.minestom.language.defaultLanguage;

import com.oxipro.cmu.configlang.standalone.config.ConfigFile;
import com.oxipro.cmu.configlang.standalone.language.Language;
import com.oxipro.idcraft.api.configuration.ConfigsType;
import com.oxipro.idcraft.minestom.IDCraftMinestomServer;
import com.oxipro.idcraft.minestom.language.LanguagePaths;

import java.io.File;

public class English extends Language {

    public English(IDCraftMinestomServer server) {
        super(new ConfigFile(
                new File(IDCraftMinestomServer.CONFIG_DIR, ConfigsType.LANG_EN.filePath),
                server.getResourceAsStream(ConfigsType.LANG_EN.filePath)
        ));
    }

    @Override
    protected void saveDefaults() {
        addDefault(LanguagePaths.LANGUAGE_FANCY_NAME, "English");

        addDefault(LanguagePaths.LOGIN_TITLE,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Login</white>");
        addDefault(LanguagePaths.LOGIN_INTRO,
                "<gray>Welcome back,</gray> <white>%PLAYER%</white><gray>. Enter your password to continue.</gray>");
        addDefault(LanguagePaths.LOGIN_PASSWORD_FIELD, "<white>Password</white>");
        addDefault(LanguagePaths.LOGIN_SUBMIT_BUTTON, "<green><bold>Login</bold></green>");
        addDefault(LanguagePaths.LOGIN_COMMAND_HINT,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>Use</gray> <yellow>/login \\<password></yellow> <gray>to continue.</gray>");
        addDefault(LanguagePaths.LOGIN_COMMAND_HINT_2FA,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>Use</gray> <yellow>/login \\<password> \\<2fa-code></yellow> <gray>to continue.</gray>");

        addDefault(LanguagePaths.REGISTER_TITLE,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Register</white>");
        addDefault(LanguagePaths.REGISTER_INTRO,
                "<gray>Create a password to secure your</gray> <white>%PLAYER%</white> <gray>account.</gray>");
        addDefault(LanguagePaths.REGISTER_PASSWORD_FIELD, "<white>Password</white>");
        addDefault(LanguagePaths.REGISTER_CONFIRM_FIELD, "<white>Confirm password</white>");
        addDefault(LanguagePaths.REGISTER_SUBMIT_BUTTON, "<green><bold>Register</bold></green>");
        addDefault(LanguagePaths.REGISTER_COMMAND_HINT,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>Use</gray> <yellow>/register \\<password> \\<confirm> [email]</yellow> <gray>to create your account.</gray>");
        addDefault(LanguagePaths.REGISTER_EMAIL_FIELD, "<white>Email</white> <dark_gray>(optional)</dark_gray>");
        addDefault(LanguagePaths.REGISTER_EMAIL_DEFAULT, "example@email.com");
        addDefault(LanguagePaths.COMMAND_EMAIL_EXAMPLE, "example@email.com");
        addDefault(LanguagePaths.COMMAND_TOTP_EXAMPLE, "123456");
        addDefault(LanguagePaths.REGISTER_PASSWORD_HINT,
                "<gray>Password must be at least</gray> <yellow>%MIN_LENGTH%</yellow> <gray>characters.</gray>");
        addDefault(LanguagePaths.REGISTER_PASSWORD_REGEX_HINT,
                " <gray>It must also match the server password policy.</gray>");
        addDefault(LanguagePaths.REGISTER_PASSWORD_NO_SPACES_HINT,
                " <gray>Spaces are not allowed.</gray>");
        addDefault(LanguagePaths.REGISTER_LANGUAGE_BUTTON,
                "<aqua>Language:</aqua> <white>%LANGUAGE%</white>");
        addDefault(LanguagePaths.LANGUAGE_PICK_TITLE,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Language</white>");
        addDefault(LanguagePaths.LANGUAGE_PICK_BACK, "<gray>Back</gray>");

        addDefault(LanguagePaths.ERROR_PASSWORD_TOO_SHORT,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Your password is too short.</red>");
        addDefault(LanguagePaths.ERROR_PASSWORD_CONTAINS_SPACES,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Password must not contain spaces.</red>");
        addDefault(LanguagePaths.ERROR_PASSWORD_REGEX_MISMATCH,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Your password does not meet the required format.</red>");
        addDefault(LanguagePaths.ERROR_PASSWORDS_DO_NOT_MATCH,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Passwords don't match.</red>");
        addDefault(LanguagePaths.ERROR_RATE_LIMITED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Too many attempts.</red> <gray>Try again later.</gray>");
        addDefault(LanguagePaths.ERROR_ACCOUNT_ALREADY_EXISTS,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>This account already exists.</red> <gray>Use</gray> <yellow>/login</yellow><gray>.</gray>");
        addDefault(LanguagePaths.ERROR_ACCOUNT_NOT_FOUND,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>No account found for this username.</red> <gray>Use</gray> <yellow>/register</yellow><gray>.</gray>");
        addDefault(LanguagePaths.ERROR_ACCOUNT_LOCKED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>This account is temporarily locked.</red> <gray>Try again later.</gray>");
        addDefault(LanguagePaths.ERROR_WRONG_PASSWORD,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Wrong password.</red>");
        addDefault(LanguagePaths.ERROR_FACTOR_INVALID,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Invalid authentication code or value.</red>");
        addDefault(LanguagePaths.ERROR_FACTOR_REQUIRED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>A required authentication factor is missing.</red>");
        addDefault(LanguagePaths.ERROR_PROVIDER_UNAVAILABLE,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Authentication services are unavailable.</red> <gray>Try again later.</gray>");
        addDefault(LanguagePaths.ERROR_CONNECT_NOT_ALLOWED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient>\n\n<red>You are not allowed to join this authentication server.</red>");

        addDefault(LanguagePaths.TWO_FACTOR_TITLE,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Two-factor setup</white>");
        addDefault(LanguagePaths.TWO_FACTOR_INTRO,
                "<gray>Enter a secret or TOTP setup value to protect your account.</gray>");
        addDefault(LanguagePaths.TWO_FACTOR_CODE_FIELD, "<white>2FA code</white>");
        addDefault(LanguagePaths.TWO_FACTOR_BUTTON, "<aqua><bold>Setup 2FA</bold></aqua>");
        addDefault(LanguagePaths.TWO_FACTOR_COMMAND_HINT,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>Optional:</gray> <yellow>/2fa setup</yellow> <gray>then</gray> <yellow>/2fa confirm \\<code></yellow>");
        addDefault(LanguagePaths.TWO_FACTOR_COMMAND_SETUP_HINT,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>Use</gray> <yellow>/2fa confirm \\<code></yellow> <gray>to finish 2FA setup.</gray>");

        addDefault(LanguagePaths.FACTOR_GENERIC_TITLE,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Factor setup</white>");
        addDefault(LanguagePaths.FACTOR_GENERIC_INTRO, "<gray>Enter the required value to continue.</gray>");
        addDefault(LanguagePaths.FACTOR_GENERIC_FIELD, "<white>Value</white>");
        addDefault(LanguagePaths.FACTOR_GENERIC_BUTTON, "<aqua><bold>Setup</bold></aqua>");
        addDefault(LanguagePaths.FACTOR_SUBMIT_BUTTON, "<green><bold>Confirm</bold></green>");
        addDefault(LanguagePaths.FACTOR_CANCEL_BUTTON, "<gray>Cancel</gray>");
        addDefault(LanguagePaths.FACTOR_COMPLETED_HINT,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <green>Factor setup completed.</green> <gray>Finish registration when ready.</gray>");

        addDefault(LanguagePaths.FACTOR_EMAIL_TITLE,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Email</white>");
        addDefault(LanguagePaths.FACTOR_EMAIL_INTRO, "<gray>Link an email address to your account.</gray>");
        addDefault(LanguagePaths.FACTOR_EMAIL_FIELD, "<white>Email</white>");
        addDefault(LanguagePaths.FACTOR_EMAIL_BUTTON, "<aqua><bold>Email</bold></aqua>");

        addDefault(LanguagePaths.DESK_HUB_TITLE,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Account</white>");
        addDefault(LanguagePaths.DESK_HUB_INTRO,
                "<gray>Manage your login methods for</gray> <white>%PLAYER%</white><gray>.</gray>");
        addDefault(LanguagePaths.DESK_HUB_INTRO_NO_ACCOUNT,
                "<gray>Create a password to enable extra login methods.</gray>");
        addDefault(LanguagePaths.DESK_HUB_PASSWORD, "<yellow>Change password</yellow>");
        addDefault(LanguagePaths.DESK_HUB_PASSWORD_CREATE, "<yellow>Create password</yellow>");
        addDefault(LanguagePaths.DESK_HUB_DONE, "<green><bold>Done</bold></green>");
        addDefault(LanguagePaths.DESK_HUB_LANGUAGE, "<aqua>Language:</aqua> <white>%LANGUAGE%</white>");
        addDefault(LanguagePaths.DESK_HUB_FACTOR_LINKED, "<green>linked</green>");
        addDefault(LanguagePaths.DESK_HUB_FACTOR_OPEN, "<dark_gray>not linked</dark_gray>");
        addDefault(LanguagePaths.DESK_PASSWORD_TITLE,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Password</white>");
        addDefault(LanguagePaths.DESK_PASSWORD_INTRO,
                "<gray>Enter your current password and a new one.</gray>");
        addDefault(LanguagePaths.DESK_PASSWORD_CREATE_INTRO,
                "<gray>Choose a password for this account.</gray>");
        addDefault(LanguagePaths.DESK_PASSWORD_CURRENT, "<white>Current password</white>");
        addDefault(LanguagePaths.DESK_PASSWORD_NEW, "<white>New password</white>");
        addDefault(LanguagePaths.DESK_PASSWORD_CONFIRM, "<white>Confirm password</white>");
        addDefault(LanguagePaths.DESK_PASSWORD_SAVE, "<green><bold>Save</bold></green>");
        addDefault(LanguagePaths.DESK_PASSWORD_BACK, "<gray>Back</gray>");
        addDefault(LanguagePaths.DESK_PASSWORD_CHANGED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <green>Your password has been updated.</green>");
        addDefault(LanguagePaths.DESK_PASSWORD_CREATED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <green>Your password has been created.</green>");
        addDefault(LanguagePaths.DESK_FACTOR_ENROLL, "<green><bold>Save</bold></green>");
        addDefault(LanguagePaths.DESK_FACTOR_REMOVE, "<red><bold>Remove</bold></red>");
        addDefault(LanguagePaths.DESK_FACTOR_BACK, "<gray>Back</gray>");
        addDefault(LanguagePaths.DESK_FACTOR_CURRENT, "<white>Current password</white>");
        addDefault(LanguagePaths.DESK_FACTOR_ENROLLED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <green>Factor linked.</green>");
        addDefault(LanguagePaths.DESK_FACTOR_REMOVED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <yellow>Factor removed.</yellow>");
        addDefault(LanguagePaths.DESK_NEED_ACCOUNT,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <yellow>Create a password</yellow> <gray>before adding extra factors.</gray>");
        addDefault(LanguagePaths.DESK_COMMAND_HINT,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>Account desk commands:</gray>");
        addDefault(LanguagePaths.DESK_COMMAND_PASSWORD_HINT,
                "<dark_gray>›</dark_gray> <yellow>/changepassword \\<current> \\<new> \\<confirm></yellow>");
        addDefault(LanguagePaths.DESK_COMMAND_CREATE_HINT,
                "<dark_gray>›</dark_gray> <yellow>/changepassword \\<new> \\<confirm></yellow>");
        addDefault(LanguagePaths.DESK_COMMAND_EMAIL_HINT,
                "<dark_gray>›</dark_gray> <yellow>/account email set \\<addr></yellow> <dark_gray>|</dark_gray> <yellow>/account email remove</yellow>");
        addDefault(LanguagePaths.DESK_COMMAND_TOTP_HINT,
                "<dark_gray>›</dark_gray> <yellow>/account totp setup</yellow> <dark_gray>|</dark_gray> <yellow>/account totp confirm \\<code></yellow> <dark_gray>|</dark_gray> <yellow>/account totp disable</yellow>");
        addDefault(LanguagePaths.DESK_COMMAND_DONE_HINT,
                "<dark_gray>›</dark_gray> <yellow>/account done</yellow>");
        addDefault(LanguagePaths.ERROR_FACTOR_NOT_ENROLLED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>This factor is not linked.</red>");
        addDefault(LanguagePaths.ERROR_FACTOR_ALREADY_ENROLLED,
                "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>This factor is already linked.</red>");

        save(true);
    }
}
