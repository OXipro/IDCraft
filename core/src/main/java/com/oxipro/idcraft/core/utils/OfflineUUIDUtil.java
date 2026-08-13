package com.oxipro.idcraft.core.utils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class OfflineUUIDUtil {

    public static UUID generate(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }

}
