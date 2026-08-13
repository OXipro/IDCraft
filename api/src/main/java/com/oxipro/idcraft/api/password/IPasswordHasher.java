package com.oxipro.idcraft.api.password;

public interface IPasswordHasher {

    String hash(String plainPassword);

    boolean matches(String plainPassword, String hash);
}
