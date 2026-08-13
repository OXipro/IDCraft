package com.oxipro.idcraft.plugin.velocity.authservers;

public enum LoadBalancerType {
    NONE,
    ROUND_ROBIN,
    LEAST_PLAYERS,
    OPTIMAL_CHOICE;

    public static LoadBalancerType fromString(String value) {
        if (value == null) return NONE;
        try {
            return LoadBalancerType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
