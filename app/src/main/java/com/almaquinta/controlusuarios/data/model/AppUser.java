package com.almaquinta.controlusuarios.data.model;

public class AppUser {
    private final String id;
    private final String name;
    private final String lastName;
    private final String email;
    private UserRole role;
    private boolean active;

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

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
