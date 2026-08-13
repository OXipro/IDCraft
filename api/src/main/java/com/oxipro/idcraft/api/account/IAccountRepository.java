package com.oxipro.idcraft.api.account;

import java.util.UUID;

public interface IAccountRepository {

    // returns null if not found
    Account findByUuid(UUID uuid);

    // returns null if not found, lookup is case-insensitive (username_lower)
    Account findByUsername(String username);

    boolean existsByUsername(String username);

    void save(Account account);

    void delete(UUID uuid);
}
