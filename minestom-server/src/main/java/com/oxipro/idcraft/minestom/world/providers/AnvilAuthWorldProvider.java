package com.oxipro.idcraft.minestom.world.providers;

import com.oxipro.idcraft.minestom.world.AuthWorldConfig;
import com.oxipro.idcraft.minestom.world.IAuthWorldProvider;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.anvil.AnvilLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class AnvilAuthWorldProvider implements IAuthWorldProvider {

    private final InstanceContainer instance;
    private final Pos spawn;

    public AnvilAuthWorldProvider(AuthWorldConfig config) {
        this.spawn = config.getSpawn();
        Path path = Path.of(config.getAnvilPath());
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException(
                    "auth.world.anvil.path does not exist or is not a directory: " + path.toAbsolutePath());
        }

        InstanceContainer container = MinecraftServer.getInstanceManager().createInstanceContainer();
        container.setChunkLoader(new AnvilLoader(path));
        this.instance = container;
    }

    @Override
    public String type() {
        return "ANVIL";
    }

    @Override
    public InstanceContainer getInstance() {
        return instance;
    }

    @Override
    public Pos getSpawn() {
        return spawn;
    }

    @Override
    public void applyTo(Player player) {
        player.setInstance(instance, spawn);
        player.setRespawnPoint(spawn);
    }

    @Override
    public void shutdown() {
    }
}
