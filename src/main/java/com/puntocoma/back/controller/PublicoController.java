package com.puntocoma.back.controller;

import com.puntocoma.back.dto.PortadaResponse;
import com.puntocoma.back.service.LibroService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints públicos (no requieren token). */
@RestController
@RequestMapping("/api/publico")
public class PublicoController {

    private final LibroService libroService;

    public PublicoController(LibroService libroService) {
        this.libroService = libroService;
    }

    /** Contadores y 3 libros destacados para la pantalla de bienvenida. */
    @GetMapping("/portada")
    public PortadaResponse portada() {
        return libroService.portada();
    }
}
