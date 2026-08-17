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

        addDefault(LanguagePaths.LOGIN_TITLE, "Login");
        addDefault(LanguagePaths.LOGIN_INTRO, "Enter your password");
        addDefault(LanguagePaths.LOGIN_PASSWORD_FIELD, "Password");
        addDefault(LanguagePaths.LOGIN_SUBMIT_BUTTON, "Login");
        addDefault(LanguagePaths.LOGIN_COMMAND_HINT, "Use /login \\<password> to continue.");
        addDefault(LanguagePaths.LOGIN_COMMAND_HINT_2FA, "Use /login \\<password> \\<2fa-code> to continue.");

        addDefault(LanguagePaths.REGISTER_TITLE, "Register");
        addDefault(LanguagePaths.REGISTER_INTRO, "Create a password to secure your account");
        addDefault(LanguagePaths.REGISTER_PASSWORD_FIELD, "Password");
        addDefault(LanguagePaths.REGISTER_CONFIRM_FIELD, "Confirm password");
        addDefault(LanguagePaths.REGISTER_SUBMIT_BUTTON, "Register");
        addDefault(LanguagePaths.REGISTER_COMMAND_HINT, "Use /register \\<password> \\<confirm> [email] to create your account.");
        addDefault(LanguagePaths.REGISTER_EMAIL_FIELD, "Email (optional)");
        addDefault(LanguagePaths.REGISTER_PASSWORD_HINT, "Password must be at least %MIN_LENGTH% characters.");
        addDefault(LanguagePaths.REGISTER_PASSWORD_REGEX_HINT, "It must also match the server password policy.");
        addDefault(LanguagePaths.REGISTER_PASSWORD_NO_SPACES_HINT, "Spaces are not allowed.");

        addDefault(LanguagePaths.ERROR_PASSWORD_TOO_SHORT, "Your password is too short.");
        addDefault(LanguagePaths.ERROR_PASSWORD_CONTAINS_SPACES, "Password must not contain spaces.");
        addDefault(LanguagePaths.ERROR_PASSWORD_REGEX_MISMATCH, "Your password does not meet the required format.");
        addDefault(LanguagePaths.ERROR_PASSWORDS_DO_NOT_MATCH, "Passwords don't match.");
        addDefault(LanguagePaths.ERROR_RATE_LIMITED, "Too many attempts, try again later.");
        addDefault(LanguagePaths.ERROR_ACCOUNT_ALREADY_EXISTS, "This account already exists, use /login.");
        addDefault(LanguagePaths.ERROR_ACCOUNT_NOT_FOUND, "No account found for this username, use /register.");
        addDefault(LanguagePaths.ERROR_ACCOUNT_LOCKED, "This account is temporarily locked, try again later.");
        addDefault(LanguagePaths.ERROR_WRONG_PASSWORD, "Wrong password.");
        addDefault(LanguagePaths.ERROR_FACTOR_INVALID, "Invalid authentication code or value.");
        addDefault(LanguagePaths.ERROR_FACTOR_REQUIRED, "A required authentication factor is missing.");
        addDefault(LanguagePaths.ERROR_PROVIDER_UNAVAILABLE, "Authentication services are unavailable, try again later.");
        addDefault(LanguagePaths.ERROR_CONNECT_NOT_ALLOWED, "You are not allowed to join this authentication server.");

        addDefault(LanguagePaths.TWO_FACTOR_TITLE, "Two-factor setup");
        addDefault(LanguagePaths.TWO_FACTOR_INTRO, "Enter a secret or TOTP setup value (temporary stub).");
        addDefault(LanguagePaths.TWO_FACTOR_CODE_FIELD, "2FA code");
        addDefault(LanguagePaths.TWO_FACTOR_BUTTON, "Setup 2FA");
        addDefault(LanguagePaths.TWO_FACTOR_COMMAND_HINT, "Optional: /2fa setup then /2fa confirm \\<code>");
        addDefault(LanguagePaths.TWO_FACTOR_COMMAND_SETUP_HINT, "Use /2fa confirm \\<code> to finish 2FA setup.");

        addDefault(LanguagePaths.FACTOR_GENERIC_TITLE, "Factor setup");
        addDefault(LanguagePaths.FACTOR_GENERIC_INTRO, "Enter the required value.");
        addDefault(LanguagePaths.FACTOR_GENERIC_FIELD, "Value");
        addDefault(LanguagePaths.FACTOR_GENERIC_BUTTON, "Setup");
        addDefault(LanguagePaths.FACTOR_SUBMIT_BUTTON, "Confirm");
        addDefault(LanguagePaths.FACTOR_CANCEL_BUTTON, "Cancel");
        addDefault(LanguagePaths.FACTOR_COMPLETED_HINT, "Factor setup completed. Finish registration when ready.");

        addDefault(LanguagePaths.FACTOR_EMAIL_TITLE, "Email");
        addDefault(LanguagePaths.FACTOR_EMAIL_INTRO, "Link an email address to your account.");
        addDefault(LanguagePaths.FACTOR_EMAIL_FIELD, "Email");
        addDefault(LanguagePaths.FACTOR_EMAIL_BUTTON, "Email");

        addDefault(LanguagePaths.DESK_HUB_TITLE, "Account");
        addDefault(LanguagePaths.DESK_HUB_INTRO, "Manage your login methods.");
        addDefault(LanguagePaths.DESK_HUB_INTRO_NO_ACCOUNT, "Create a password to enable extra login methods.");
        addDefault(LanguagePaths.DESK_HUB_PASSWORD, "Change password");
        addDefault(LanguagePaths.DESK_HUB_PASSWORD_CREATE, "Create password");
        addDefault(LanguagePaths.DESK_HUB_DONE, "Done");
        addDefault(LanguagePaths.DESK_HUB_FACTOR_LINKED, "linked");
        addDefault(LanguagePaths.DESK_HUB_FACTOR_OPEN, "not linked");
        addDefault(LanguagePaths.DESK_PASSWORD_TITLE, "Password");
        addDefault(LanguagePaths.DESK_PASSWORD_INTRO, "Enter your current password and a new one.");
        addDefault(LanguagePaths.DESK_PASSWORD_CREATE_INTRO, "Choose a password for this account.");
        addDefault(LanguagePaths.DESK_PASSWORD_CURRENT, "Current password");
        addDefault(LanguagePaths.DESK_PASSWORD_NEW, "New password");
        addDefault(LanguagePaths.DESK_PASSWORD_CONFIRM, "Confirm password");
        addDefault(LanguagePaths.DESK_PASSWORD_SAVE, "Save");
        addDefault(LanguagePaths.DESK_PASSWORD_BACK, "Back");
        addDefault(LanguagePaths.DESK_PASSWORD_CHANGED, "Your password has been updated.");
        addDefault(LanguagePaths.DESK_PASSWORD_CREATED, "Your password has been created.");
        addDefault(LanguagePaths.DESK_FACTOR_ENROLL, "Save");
        addDefault(LanguagePaths.DESK_FACTOR_REMOVE, "Remove");
        addDefault(LanguagePaths.DESK_FACTOR_BACK, "Back");
        addDefault(LanguagePaths.DESK_FACTOR_CURRENT, "Current password");
        addDefault(LanguagePaths.DESK_FACTOR_ENROLLED, "Factor linked.");
        addDefault(LanguagePaths.DESK_FACTOR_REMOVED, "Factor removed.");
        addDefault(LanguagePaths.DESK_NEED_ACCOUNT, "Create a password before adding extra factors.");
        addDefault(LanguagePaths.DESK_COMMAND_HINT, "Account desk commands:");
        addDefault(LanguagePaths.DESK_COMMAND_PASSWORD_HINT, "/changepassword <current> <new> <confirm>");
        addDefault(LanguagePaths.DESK_COMMAND_CREATE_HINT, "/changepassword <new> <confirm>");
        addDefault(LanguagePaths.DESK_COMMAND_EMAIL_HINT, "/account email set <addr> | /account email remove");
        addDefault(LanguagePaths.DESK_COMMAND_TOTP_HINT, "/account totp setup | /account totp confirm <code> | /account totp disable");
        addDefault(LanguagePaths.DESK_COMMAND_DONE_HINT, "/account done");
        addDefault(LanguagePaths.ERROR_FACTOR_NOT_ENROLLED, "This factor is not linked.");
        addDefault(LanguagePaths.ERROR_FACTOR_ALREADY_ENROLLED, "This factor is already linked.");

        save(true);
    }
}