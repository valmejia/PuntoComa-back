package com.puntocoma.back.security;

import com.puntocoma.back.entity.Rol;

import java.security.Principal;

/** Principal guardado en el SecurityContext; se inyecta en controladores con @AuthenticationPrincipal. */
public record UsuarioAutenticado(Long id, String email, Rol rol) implements Principal {
    @Override
    public String getName() { return email; }

    public boolean esStaff() { return rol == Rol.VENDEDOR || rol == Rol.ADMIN; }
}
