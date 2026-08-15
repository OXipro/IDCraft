package com.oxipro.idcraft.api.account;

import com.oxipro.idcraft.api.auth.AuthFactor;

import java.util.Map;
import java.util.UUID;

public interface IAccountFactorRepository {

    Map<AuthFactor, String> findAll(UUID uuid);

    String find(UUID uuid, AuthFactor factor);

    UUID findUuidByEmail(String email);

    void save(UUID uuid, AuthFactor factor, String value);

    void delete(UUID uuid, AuthFactor factor);

    void deleteAll(UUID uuid);
}
