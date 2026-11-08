package com.puntocoma.back.controller;

import com.puntocoma.back.dto.CambiarEstadoUsuarioRequest;
import com.puntocoma.back.dto.CambiarRolRequest;
import com.puntocoma.back.dto.UsuarioResponse;
import com.puntocoma.back.security.UsuarioAutenticado;
import com.puntocoma.back.service.UsuarioAdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Protegido por SecurityConfig: /api/admin/** requiere ROLE_ADMIN. */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioAdminService service;

    public AdminUsuarioController(UsuarioAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    @PutMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@PathVariable Long id, @Valid @RequestBody CambiarRolRequest req,
                                      @AuthenticationPrincipal UsuarioAutenticado actual) {
        return service.cambiarRol(id, req.rol(), actual.id());
    }

    @PutMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoUsuarioRequest req,
                                         @AuthenticationPrincipal UsuarioAutenticado actual) {
        return service.cambiarEstado(id, req.activo(), actual.id());
    }
}
