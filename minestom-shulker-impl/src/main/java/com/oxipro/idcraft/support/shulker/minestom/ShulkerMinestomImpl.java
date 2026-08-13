package com.oxipro.idcraft.support.shulker.minestom;

import io.shulkermc.server.minestom.ShulkerServerAgentMinestom;

import java.util.logging.Logger;

public class ShulkerMinestomImpl {

    public void initServer() {
        ShulkerServerAgentMinestom.init(Logger.getLogger("ShulkerMinestom"));
    }

    public void start() {
        ShulkerServerAgentMinestom.start();
    }
}
