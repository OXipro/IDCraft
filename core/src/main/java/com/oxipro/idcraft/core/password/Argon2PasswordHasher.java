package com.oxipro.idcraft.core.password;

import com.oxipro.idcraft.api.password.IPasswordHasher;
import com.password4j.Hash;
import com.password4j.Password;

public class Argon2PasswordHasher implements IPasswordHasher {

    @Override
    public String hash(String plainPassword) {
        Hash hashed = Password.hash(plainPassword).addRandomSalt().withArgon2();
        return hashed.getResult();
    }

    @Override
    public boolean matches(String plainPassword, String hash) {
        try {
            return Password.check(plainPassword, hash).withArgon2();
        } catch (RuntimeException invalidHash) {
            return false;
        }
    }
}
