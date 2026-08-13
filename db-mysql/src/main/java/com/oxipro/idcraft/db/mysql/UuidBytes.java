package com.oxipro.idcraft.db.mysql;

import java.nio.ByteBuffer;
import java.util.UUID;

// utils class: never exposed outside db-mysql
final class UuidBytes {

    private UuidBytes() {
    }

    static byte[] toBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    static UUID fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
