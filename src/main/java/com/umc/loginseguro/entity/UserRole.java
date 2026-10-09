package com.umc.loginseguro.entity;

public enum UserRole {
    USER("USER"),
    MANAGER("MANAGER"),
    ADMIN("ADMIN");

    private String name;

    private UserRole(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    public boolean compare(UserRole role) {
        return this.name.equalsIgnoreCase(role.getName());
    }

}
