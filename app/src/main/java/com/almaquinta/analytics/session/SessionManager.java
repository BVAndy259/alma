package com.almaquinta.analytics.session;

import com.almaquinta.analytics.data.model.AppUser;
import com.almaquinta.analytics.data.model.UserRole;

public class SessionManager {
    private static SessionManager instance;
    private AppUser currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void setCurrentUser(AppUser user) { this.currentUser = user; }
    public AppUser getCurrentUser() { return currentUser; }
    public void logout() { currentUser = null; }

    public boolean isAdmin() {
        return currentUser != null && (currentUser.getRole() == UserRole.ADMIN
                || currentUser.getRole() == UserRole.COORDINATOR);
    }

    public boolean isAdminOnly() {
        return currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }
}
