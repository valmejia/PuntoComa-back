package com.puntocoma.back.controller;

import com.puntocoma.back.dto.PrestamoRequest;
import com.puntocoma.back.dto.PrestamoResponse;
import com.puntocoma.back.entity.EstadoPrestamo;
import com.puntocoma.back.security.UsuarioAutenticado;
import com.puntocoma.back.service.PrestamoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    private final PrestamoService prestamoService;

    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrestamoResponse solicitar(@Valid @RequestBody PrestamoRequest req, @AuthenticationPrincipal UsuarioAutenticado actual) {
        return prestamoService.solicitar(req, actual);
    }

    @GetMapping("/mios")
    public List<PrestamoResponse> mios(@AuthenticationPrincipal UsuarioAutenticado actual) {
        return prestamoService.misPrestamos(actual.id());
    }

    @GetMapping("/recibidos")
    public List<PrestamoResponse> recibidos(@AuthenticationPrincipal UsuarioAutenticado actual) {
        return prestamoService.prestamosDeMisLibros(actual.id());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMIN')")
    public List<PrestamoResponse> todos(@RequestParam(required = false) EstadoPrestamo estado) {
        return prestamoService.todos(estado);
    }

    @GetMapping("/{id}")
    public PrestamoResponse obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actual) {
        return prestamoService.obtener(id, actual);
    }

    @PutMapping("/{id}/devolucion")
    public PrestamoResponse devolver(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actual) {
        return prestamoService.devolver(id, actual);
    }
}
