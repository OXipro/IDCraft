package com.oxipro.idcraft.support.shulker.velocity;

import com.oxipro.idcraft.api.support.authservers.IAuthServersProvider;
import io.shulkermc.proxy.api.ShulkerProxyAPI;

import java.util.Collections;
import java.util.List;

// Stub until Shulker cluster API is wired
public class ShulkerVelocityAuthServersProvider implements IAuthServersProvider {

    private final ShulkerProxyAPI shulkerProxyAPI;

    private String tag;

    public ShulkerVelocityAuthServersProvider(String tag) {
        this.shulkerProxyAPI = ShulkerProxyAPI.instance();
        if (shulkerProxyAPI == null) return;
        this.tag = tag;
    }

    @Override
    public List<String> get() {
        if (shulkerProxyAPI == null) return Collections.emptyList();
        return shulkerProxyAPI.getServersByTag(tag).stream().toList();
    }
}
