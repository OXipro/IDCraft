package com.oxipro.idcraft.minestom.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

// Loads and owns the auth instance for a given world type
public interface IAuthWorldProvider {

    String type();

    Instance getInstance();

    Pos getSpawn();

    void applyTo(Player player);

    void shutdown();
}
