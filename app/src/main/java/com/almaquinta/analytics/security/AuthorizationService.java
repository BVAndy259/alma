package com.almaquinta.analytics.security;

import com.almaquinta.analytics.session.SessionManager;

public class AuthorizationService {
    public void requireAdmin() {
        if (!SessionManager.getInstance().isAdmin()) {
            throw new SecurityException("Acceso denegado: solo administradores o coordinadores.");
        }
    }
}
