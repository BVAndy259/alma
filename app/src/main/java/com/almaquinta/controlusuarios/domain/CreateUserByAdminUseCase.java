package com.almaquinta.controlusuarios.domain;

import com.almaquinta.controlusuarios.data.model.AppUser;
import com.almaquinta.controlusuarios.data.model.UserRole;
import com.almaquinta.controlusuarios.security.AuthorizationService;

public class CreateUserByAdminUseCase {
    private final AuthorizationService authorizationService;

    public CreateUserByAdminUseCase(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public AppUser execute(String id, String name, String lastName, String email) {
        authorizationService.requireAdmin();
        return new AppUser(lastName, id, name, email, UserRole.USER, true);
    }
}
