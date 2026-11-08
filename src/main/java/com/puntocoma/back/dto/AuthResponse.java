package com.puntocoma.back.dto;

/** Respuesta de login: token JWT + datos del usuario para que la app redirija según el rol. */
public record AuthResponse(String token, String tipo, long expiraEnMs, UsuarioResponse usuario) {}
