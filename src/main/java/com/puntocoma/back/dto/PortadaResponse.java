package com.puntocoma.back.dto;

import java.util.List;

/** Datos públicos para la pantalla de bienvenida (antes de iniciar sesión). */
public record PortadaResponse(long titulos, long autores, long lectores, List<LibroResponse> destacados) {}
