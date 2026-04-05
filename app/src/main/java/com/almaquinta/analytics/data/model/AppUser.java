package com.almaquinta.analytics.data.model;

public class AppUser {
    private final String id, name, lastName, email;
    private final UserRole role;
    private final boolean active;

    public AppUser(String lastName, String id, String name, String email, UserRole role, boolean active) {
        this.lastName = lastName;
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
