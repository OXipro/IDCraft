package com.oxipro.idcraft.minestom.auth.prompt;

import com.oxipro.idcraft.api.auth.AuthFactor;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class LoginSubmission {

    private final Map<AuthFactor, String> values;

    public LoginSubmission(Map<AuthFactor, String> values) {
        EnumMap<AuthFactor, String> copy = new EnumMap<>(AuthFactor.class);
        if (values != null) {
            copy.putAll(values);
        }
        this.values = Collections.unmodifiableMap(copy);
    }

    public Map<AuthFactor, String> getValues() {
        return values;
    }

    public String get(AuthFactor factor) {
        return values.get(factor);
    }
}
