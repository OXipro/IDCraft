package com.oxipro.idcraft.minestom;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.dialog.*;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerConfigCustomClickEvent;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class IDCraftMinestomServer {

    private static final Key SUBMIT_KEY = Key.key("dialog_demo:submit");
    private static final Key LANG_FR_KEY = Key.key("dialog_demo:lang_fr");
    private static final Key LANG_EN_KEY = Key.key("dialog_demo:lang_en");

    private static final Map<UUID, CompletableFuture<Void>> pendingAuth = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerLang = new ConcurrentHashMap<>();

    static void main() {
        MinecraftServer minecraftServer = MinecraftServer.init();

        var eventHandler = MinecraftServer.getGlobalEventHandler();

        eventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();

            // Auto-détection basique via le locale client, fallback "en"
            String detected = "en";
            try {
                String locale = player.getSettings() != null ? String.valueOf(player.getSettings().locale()) : null;
                if (locale != null && locale.toLowerCase().startsWith("fr")) {
                    detected = "fr";
                }
            } catch (Exception ignored) {}
            playerLang.put(player.getUuid(), detected);

            player.sendPacket(new ShowDialogPacket(buildAuthDialog(player, null)));

            CompletableFuture<Void> future = new CompletableFuture<>();
            pendingAuth.put(player.getUuid(), future);
            future.join();
        });

        eventHandler.addListener(PlayerConfigCustomClickEvent.class, event -> {
            Player player = event.getPlayer();
            Key key = event.getKey();

            if (key.equals(LANG_FR_KEY) || key.equals(LANG_EN_KEY)) {
                playerLang.put(player.getUuid(), key.equals(LANG_FR_KEY) ? "fr" : "en");
                // On renvoie juste le dialog traduit, sans toucher au CompletableFuture
                player.sendPacket(new ShowDialogPacket(buildAuthDialog(player, null)));
                return;
            }

            if (!key.equals(SUBMIT_KEY)) return;

            String lang = playerLang.getOrDefault(player.getUuid(), "en");

            String password = "";
            String confirm = "";
            String email = "";
            if (event.getPayload() instanceof CompoundBinaryTag compound) {
                password = compound.getString("password", "");
                confirm = compound.getString("confirm_password", "");
                email = compound.getString("email", "");
            }

            if (password.isEmpty()) {
                player.sendPacket(new ShowDialogPacket(buildAuthDialog(player, t(lang, "err_empty"))));
                return;
            }

            if (!password.equals(confirm)) {
                player.sendPacket(new ShowDialogPacket(buildAuthDialog(player, t(lang, "err_mismatch"))));
                return;
            }

            System.out.println("=== Nouvelle inscription ===");
            System.out.println("Joueur       : " + player.getUsername());
            System.out.println("Mot de passe : " + "*".repeat(password.length()));
            System.out.println("Email        : " + (email.isBlank() ? "(non fourni)" : email));

            CompletableFuture<Void> future = pendingAuth.remove(player.getUuid());
            if (future != null) future.complete(null);
        });

        minecraftServer.start("0.0.0.0", 25565);
    }

    // --- Mini-dictionnaire de traduction (à remplacer par un vrai système i18n plus tard) ---
    private static String t(String lang, String key) {
        Map<String, String> fr = Map.of(
                "title", "Bienvenue, %s !",
                "intro", "Crée un compte pour continuer à jouer.",
                "pwd", "Mot de passe",
                "confirm", "Confirmer le mot de passe",
                "email", "Email (optionnel)",
                "register", "S'inscrire",
                "lang_button", "EN",
                "err_empty", "Le mot de passe ne peut pas être vide.",
                "err_mismatch", "Les mots de passe ne correspondent pas."
        );
        Map<String, String> en = Map.of(
                "title", "Welcome, %s!",
                "intro", "Create an account to continue playing.",
                "pwd", "Password",
                "confirm", "Confirm Password",
                "email", "Email (optional)",
                "register", "Register",
                "lang_button", "FR",
                "err_empty", "Password cannot be empty.",
                "err_mismatch", "Passwords do not match."
        );
        return lang.equals("fr") ? fr.get(key) : en.get(key);
    }

    private static Dialog buildAuthDialog(Player player, String errorMessage) {
        String lang = playerLang.getOrDefault(player.getUuid(), "en");

        List<DialogBody> body = new ArrayList<>();
        body.add(new DialogBody.PlainMessage(
                Component.text(t(lang, "intro")),
                DialogBody.PlainMessage.DEFAULT_WIDTH
        ));
        if (errorMessage != null) {
            body.add(new DialogBody.PlainMessage(
                    Component.text(errorMessage, NamedTextColor.RED),
                    DialogBody.PlainMessage.DEFAULT_WIDTH
            ));
        }

        DialogMetadata metadata = new DialogMetadata(
                Component.text(String.format(t(lang, "title"), player.getUsername())),
                null,
                false,
                false,
                DialogAfterAction.CLOSE,
                body,
                List.of(
                        new DialogInput.Text(
                                "password", DialogInput.DEFAULT_WIDTH,
                                Component.text(t(lang, "pwd")),
                                true, "", 64, null
                        ),
                        new DialogInput.Text(
                                "confirm_password", DialogInput.DEFAULT_WIDTH,
                                Component.text(t(lang, "confirm")),
                                true, "", 64, null
                        ),
                        new DialogInput.Text(
                                "email", DialogInput.DEFAULT_WIDTH,
                                Component.text(t(lang, "email")),
                                true, "", 128, null
                        )
                )
        );

        // Le bouton affiché propose de basculer vers l'AUTRE langue
        Key toggleKey = lang.equals("fr") ? LANG_EN_KEY : LANG_FR_KEY;

        return new Dialog.MultiAction(
                metadata,
                List.of(
                        new DialogActionButton(
                                Component.text(t(lang, "register")), null, DialogActionButton.DEFAULT_WIDTH,
                                new DialogAction.DynamicCustom(SUBMIT_KEY, null)
                        ),
                        new DialogActionButton(
                                Component.text(t(lang, "lang_button")), null, DialogActionButton.DEFAULT_WIDTH,
                                new DialogAction.DynamicCustom(toggleKey, null)
                        )
                ),
                null,
                1
        );
    }

}
