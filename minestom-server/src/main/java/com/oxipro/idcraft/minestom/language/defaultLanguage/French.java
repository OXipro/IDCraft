package com.oxipro.idcraft.minestom.language.defaultLanguage;

import com.oxipro.cmu.configlang.standalone.config.ConfigFile;
import com.oxipro.cmu.configlang.standalone.language.Language;
import com.oxipro.idcraft.api.configuration.ConfigsType;
import com.oxipro.idcraft.minestom.IDCraftMinestomServer;
import com.oxipro.idcraft.minestom.language.LanguagePaths;

import java.io.File;

public class French extends Language {

    public French(IDCraftMinestomServer server) {
        super(new ConfigFile(
                new File(IDCraftMinestomServer.CONFIG_DIR, ConfigsType.LANG_FR.filePath),
                server.getResourceAsStream(ConfigsType.LANG_FR.filePath)
        ));
    }

    @Override
    protected void saveDefaults() {
        addDefault(LanguagePaths.LANGUAGE_FANCY_NAME, "Francais");
        addDefault(LanguagePaths.LOGIN_TITLE, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Connexion</white>");
        addDefault(LanguagePaths.LOGIN_INTRO, "<gray>Bon retour,</gray> <white>%PLAYER%</white><gray>. Entre ton mot de passe pour continuer.</gray>");
        addDefault(LanguagePaths.LOGIN_PASSWORD_FIELD, "<white>Mot de passe</white>");
        addDefault(LanguagePaths.LOGIN_SUBMIT_BUTTON, "<green><bold>Connexion</bold></green>");
        addDefault(LanguagePaths.LOGIN_COMMAND_HINT, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>Utilise</gray> <yellow>/login \\<password></yellow> <gray>pour continuer.</gray>");
        addDefault(LanguagePaths.LOGIN_COMMAND_HINT_2FA, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>Utilise</gray> <yellow>/login \\<password> \\<2fa-code></yellow> <gray>pour continuer.</gray>");
        addDefault(LanguagePaths.REGISTER_TITLE, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Inscription</white>");
        addDefault(LanguagePaths.REGISTER_INTRO, "<gray>Cree un mot de passe pour securiser le compte</gray> <white>%PLAYER%</white><gray>.</gray>");
        addDefault(LanguagePaths.REGISTER_PASSWORD_FIELD, "<white>Mot de passe</white>");
        addDefault(LanguagePaths.REGISTER_CONFIRM_FIELD, "<white>Confirmer le mot de passe</white>");
        addDefault(LanguagePaths.REGISTER_SUBMIT_BUTTON, "<green><bold>S'inscrire</bold></green>");
        addDefault(LanguagePaths.REGISTER_COMMAND_HINT, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>Utilise</gray> <yellow>/register \\<password> \\<confirm> [email]</yellow> <gray>pour creer ton compte.</gray>");
        addDefault(LanguagePaths.REGISTER_EMAIL_FIELD, "<white>Email</white> <dark_gray>(optionnel)</dark_gray>");
        addDefault(LanguagePaths.REGISTER_EMAIL_DEFAULT, "exemple@email.com");
        addDefault(LanguagePaths.COMMAND_EMAIL_EXAMPLE, "exemple@email.com");
        addDefault(LanguagePaths.COMMAND_TOTP_EXAMPLE, "123456");
        addDefault(LanguagePaths.REGISTER_PASSWORD_HINT, "<gray>Le mot de passe doit faire au moins</gray> <yellow>%MIN_LENGTH%</yellow> <gray>caracteres.</gray>");
        addDefault(LanguagePaths.REGISTER_PASSWORD_REGEX_HINT, " <gray>Il doit aussi respecter la politique du serveur.</gray>");
        addDefault(LanguagePaths.REGISTER_PASSWORD_NO_SPACES_HINT, " <gray>Les espaces ne sont pas autorises.</gray>");
        addDefault(LanguagePaths.REGISTER_LANGUAGE_BUTTON, "<aqua>Langue :</aqua> <white>%LANGUAGE%</white>");
        addDefault(LanguagePaths.LANGUAGE_PICK_TITLE, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Langue</white>");
        addDefault(LanguagePaths.LANGUAGE_PICK_BACK, "<gray>Retour</gray>");
        addDefault(LanguagePaths.ERROR_PASSWORD_TOO_SHORT, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Ton mot de passe est trop court.</red>");
        addDefault(LanguagePaths.ERROR_PASSWORD_CONTAINS_SPACES, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Le mot de passe ne doit pas contenir d'espaces.</red>");
        addDefault(LanguagePaths.ERROR_PASSWORD_REGEX_MISMATCH, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Ton mot de passe ne respecte pas le format requis.</red>");
        addDefault(LanguagePaths.ERROR_PASSWORDS_DO_NOT_MATCH, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Les mots de passe ne correspondent pas.</red>");
        addDefault(LanguagePaths.ERROR_RATE_LIMITED, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Trop de tentatives.</red> <gray>Reessaie plus tard.</gray>");
        addDefault(LanguagePaths.ERROR_ACCOUNT_ALREADY_EXISTS, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Ce compte existe deja.</red> <gray>Utilise</gray> <yellow>/login</yellow><gray>.</gray>");
        addDefault(LanguagePaths.ERROR_ACCOUNT_NOT_FOUND, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Aucun compte pour ce pseudo.</red> <gray>Utilise</gray> <yellow>/register</yellow><gray>.</gray>");
        addDefault(LanguagePaths.ERROR_ACCOUNT_LOCKED, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Ce compte est temporairement verrouille.</red> <gray>Reessaie plus tard.</gray>");
        addDefault(LanguagePaths.ERROR_WRONG_PASSWORD, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Mauvais mot de passe.</red>");
        addDefault(LanguagePaths.ERROR_FACTOR_INVALID, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Code ou valeur d'authentification invalide.</red>");
        addDefault(LanguagePaths.ERROR_FACTOR_REQUIRED, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Un facteur d'authentification requis est manquant.</red>");
        addDefault(LanguagePaths.ERROR_PROVIDER_UNAVAILABLE, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Les services d'authentification sont indisponibles.</red> <gray>Reessaie plus tard.</gray>");
        addDefault(LanguagePaths.ERROR_CONNECT_NOT_ALLOWED, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient>\n\n<red>Tu n'es pas autorise a rejoindre ce serveur d'authentification.</red>");
        addDefault(LanguagePaths.TWO_FACTOR_TITLE, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Double authentification</white>");
        addDefault(LanguagePaths.TWO_FACTOR_INTRO, "<gray>Entre un secret ou une valeur TOTP pour proteger ton compte.</gray>");
        addDefault(LanguagePaths.TWO_FACTOR_CODE_FIELD, "<white>Code 2FA</white>");
        addDefault(LanguagePaths.TWO_FACTOR_BUTTON, "<aqua><bold>Configurer 2FA</bold></aqua>");
        addDefault(LanguagePaths.TWO_FACTOR_COMMAND_HINT, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>Optionnel :</gray> <yellow>/2fa setup</yellow> <gray>puis</gray> <yellow>/2fa confirm \\<code></yellow>");
        addDefault(LanguagePaths.TWO_FACTOR_COMMAND_SETUP_HINT, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>Utilise</gray> <yellow>/2fa confirm \\<code></yellow> <gray>pour terminer la 2FA.</gray>");
        addDefault(LanguagePaths.FACTOR_GENERIC_TITLE, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Configuration du facteur</white>");
        addDefault(LanguagePaths.FACTOR_GENERIC_INTRO, "<gray>Entre la valeur requise pour continuer.</gray>");
        addDefault(LanguagePaths.FACTOR_GENERIC_FIELD, "<white>Valeur</white>");
        addDefault(LanguagePaths.FACTOR_GENERIC_BUTTON, "<aqua><bold>Configurer</bold></aqua>");
        addDefault(LanguagePaths.FACTOR_SUBMIT_BUTTON, "<green><bold>Confirmer</bold></green>");
        addDefault(LanguagePaths.FACTOR_CANCEL_BUTTON, "<gray>Annuler</gray>");
        addDefault(LanguagePaths.FACTOR_COMPLETED_HINT, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <green>Facteur configure.</green> <gray>Termine l'inscription quand tu es pret.</gray>");
        addDefault(LanguagePaths.FACTOR_EMAIL_TITLE, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Email</white>");
        addDefault(LanguagePaths.FACTOR_EMAIL_INTRO, "<gray>Lie une adresse email a ton compte.</gray>");
        addDefault(LanguagePaths.FACTOR_EMAIL_FIELD, "<white>Email</white>");
        addDefault(LanguagePaths.FACTOR_EMAIL_BUTTON, "<aqua><bold>Email</bold></aqua>");
        addDefault(LanguagePaths.DESK_HUB_TITLE, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Compte</white>");
        addDefault(LanguagePaths.DESK_HUB_INTRO, "<gray>Gere tes methodes de connexion pour</gray> <white>%PLAYER%</white><gray>.</gray>");
        addDefault(LanguagePaths.DESK_HUB_INTRO_NO_ACCOUNT, "<gray>Cree un mot de passe pour activer d'autres methodes.</gray>");
        addDefault(LanguagePaths.DESK_HUB_PASSWORD, "<yellow>Changer le mot de passe</yellow>");
        addDefault(LanguagePaths.DESK_HUB_PASSWORD_CREATE, "<yellow>Creer un mot de passe</yellow>");
        addDefault(LanguagePaths.DESK_HUB_DONE, "<green><bold>Termine</bold></green>");
        addDefault(LanguagePaths.DESK_HUB_LANGUAGE, "<aqua>Langue :</aqua> <white>%LANGUAGE%</white>");
        addDefault(LanguagePaths.DESK_HUB_FACTOR_LINKED, "<green>lie</green>");
        addDefault(LanguagePaths.DESK_HUB_FACTOR_OPEN, "<dark_gray>non lie</dark_gray>");
        addDefault(LanguagePaths.DESK_PASSWORD_TITLE, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>·</dark_gray> <white>Mot de passe</white>");
        addDefault(LanguagePaths.DESK_PASSWORD_INTRO, "<gray>Entre ton mot de passe actuel et un nouveau.</gray>");
        addDefault(LanguagePaths.DESK_PASSWORD_CREATE_INTRO, "<gray>Choisis un mot de passe pour ce compte.</gray>");
        addDefault(LanguagePaths.DESK_PASSWORD_CURRENT, "<white>Mot de passe actuel</white>");
        addDefault(LanguagePaths.DESK_PASSWORD_NEW, "<white>Nouveau mot de passe</white>");
        addDefault(LanguagePaths.DESK_PASSWORD_CONFIRM, "<white>Confirmer le mot de passe</white>");
        addDefault(LanguagePaths.DESK_PASSWORD_SAVE, "<green><bold>Enregistrer</bold></green>");
        addDefault(LanguagePaths.DESK_PASSWORD_BACK, "<gray>Retour</gray>");
        addDefault(LanguagePaths.DESK_PASSWORD_CHANGED, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <green>Ton mot de passe a ete mis a jour.</green>");
        addDefault(LanguagePaths.DESK_PASSWORD_CREATED, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <green>Ton mot de passe a ete cree.</green>");
        addDefault(LanguagePaths.DESK_FACTOR_ENROLL, "<green><bold>Enregistrer</bold></green>");
        addDefault(LanguagePaths.DESK_FACTOR_REMOVE, "<red><bold>Retirer</bold></red>");
        addDefault(LanguagePaths.DESK_FACTOR_BACK, "<gray>Retour</gray>");
        addDefault(LanguagePaths.DESK_FACTOR_CURRENT, "<white>Mot de passe actuel</white>");
        addDefault(LanguagePaths.DESK_FACTOR_ENROLLED, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <green>Facteur lie.</green>");
        addDefault(LanguagePaths.DESK_FACTOR_REMOVED, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <yellow>Facteur retire.</yellow>");
        addDefault(LanguagePaths.DESK_NEED_ACCOUNT, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <yellow>Cree un mot de passe</yellow> <gray>avant d'ajouter d'autres facteurs.</gray>");
        addDefault(LanguagePaths.DESK_COMMAND_HINT, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <gray>Commandes du bureau compte :</gray>");
        addDefault(LanguagePaths.DESK_COMMAND_PASSWORD_HINT, "<dark_gray>›</dark_gray> <yellow>/changepassword \\<actuel> \\<nouveau> \\<confirm></yellow>");
        addDefault(LanguagePaths.DESK_COMMAND_CREATE_HINT, "<dark_gray>›</dark_gray> <yellow>/changepassword \\<nouveau> \\<confirm></yellow>");
        addDefault(LanguagePaths.DESK_COMMAND_EMAIL_HINT, "<dark_gray>›</dark_gray> <yellow>/account email set \\<addr></yellow> <dark_gray>|</dark_gray> <yellow>/account email remove</yellow>");
        addDefault(LanguagePaths.DESK_COMMAND_TOTP_HINT, "<dark_gray>›</dark_gray> <yellow>/account totp setup</yellow> <dark_gray>|</dark_gray> <yellow>/account totp confirm \\<code></yellow> <dark_gray>|</dark_gray> <yellow>/account totp disable</yellow>");
        addDefault(LanguagePaths.DESK_COMMAND_DONE_HINT, "<dark_gray>›</dark_gray> <yellow>/account done</yellow>");
        addDefault(LanguagePaths.ERROR_FACTOR_NOT_ENROLLED, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Ce facteur n'est pas lie.</red>");
        addDefault(LanguagePaths.ERROR_FACTOR_ALREADY_ENROLLED, "<gradient:#FFE082:#FFB300><bold>IDCraft</bold></gradient> <dark_gray>»</dark_gray> <red>Ce facteur est deja lie.</red>");
        save(true);
    }
}
