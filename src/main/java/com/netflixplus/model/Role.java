package com.netflixplus.model;

public enum Role {
    USER(1),
    ADMIN(2),
    MASTER(3);

    public final int level;

    Role(int level) {
        this.level = level;
    }

    public static Role fromString(String role) {
        return Role.valueOf(role);
    }
}
