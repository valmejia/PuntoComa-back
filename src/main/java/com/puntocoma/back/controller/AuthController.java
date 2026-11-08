package com.puntocoma.back.controller;

import com.puntocoma.back.dto.AuthResponse;
import com.puntocoma.back.dto.LoginRequest;
import com.puntocoma.back.dto.RegistroRequest;
import com.puntocoma.back.dto.UsuarioResponse;
import com.puntocoma.back.security.UsuarioAutenticado;
import com.puntocoma.back.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/api/salud")
    public Map<String, String> salud() {
        return Map.of("estado", "ok", "mensaje", "¡El backend de Punto&Coma está funcionando correctamente!");
    }

    /** Se conserva /api/usuarios/registro por compatibilidad con peticiones anteriores. */
    @PostMapping({"/api/auth/registro", "/api/usuarios/registro"})
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/api/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping({"/api/auth/me", "/api/usuarios/me"})
    public UsuarioResponse me(@AuthenticationPrincipal UsuarioAutenticado actual) {
        return authService.perfil(actual.id());
    }
}
