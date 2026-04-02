package com.almaquinta.analytics.domain;

import com.almaquinta.analytics.data.model.AppUser;
import com.almaquinta.analytics.data.model.UserRole;
import com.almaquinta.analytics.security.AuthorizationService;

public class CreateUserByAdminUseCase {
    private final AuthorizationService authorizationService;

    public CreateUserByAdminUseCase(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public AppUser execute(String id, String name, String lastName, String email) {
        authorizationService.requireAdmin();
        return new AppUser(lastName, id, name, email, UserRole.EMPLOYEE, true);
    }
}
