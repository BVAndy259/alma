package com.almaquinta.controlusuarios.security;

import com.almaquinta.controlusuarios.session.SessionManager;

public class AuthorizationService {
    public void requireAdmin() {
        if (!SessionManager.getInstance().isAdmin()) {
            throw new SecurityException("Acceso denegado: solo administradores o coordinadores.");
        }
    }
}
