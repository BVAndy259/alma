package com.almaquinta.analytics.security;

import com.almaquinta.analytics.session.SessionManager;

public class AuthorizationService {
    public void requireRegistrationAccess() {
        if (!SessionManager.getInstance().isAdmin()) {
            throw new SecurityException("Acceso denegado: solo administradores o coordinadores.");
        }
    }

    public void requireAdminOnly() {
        if (!SessionManager.getInstance().isAdminOnly()) {
            throw new SecurityException("Acceso denegado: solo administradores.");
        }
    }

    public void requireAdmin() {
        if (!SessionManager.getInstance().isAdmin()) {
            throw new SecurityException("Acceso denegado: solo administradores o coordinadores.");
        }
    }
}
