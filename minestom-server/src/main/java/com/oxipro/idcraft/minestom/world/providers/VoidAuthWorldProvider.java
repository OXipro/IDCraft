package com.oxipro.idcraft.minestom.world.providers;

import com.oxipro.idcraft.minestom.world.AuthWorldConfig;
import com.oxipro.idcraft.minestom.world.IAuthWorldProvider;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;

public class VoidAuthWorldProvider implements IAuthWorldProvider {

    private final Instance instance;
    private final Pos spawn;

    public VoidAuthWorldProvider(AuthWorldConfig config) {
        this.spawn = config.getSpawn();
        InstanceContainer container = MinecraftServer.getInstanceManager().createInstanceContainer();
        container.setGenerator(unit -> unit.modifier().fillHeight(0, 41, Block.BEDROCK));
        this.instance = container;
    }

    @Override
    public String type() {
        return "VOID";
    }

    @Override
    public Instance getInstance() {
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
