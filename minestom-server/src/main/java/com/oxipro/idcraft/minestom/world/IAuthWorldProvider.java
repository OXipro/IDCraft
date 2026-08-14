package com.oxipro.idcraft.minestom.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;

// Loads and owns the auth instance for a given world type
public interface IAuthWorldProvider {

    String type();

    InstanceContainer getInstance();

    Pos getSpawn();

    void applyTo(Player player);

    void shutdown();
}
