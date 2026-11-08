package com.puntocoma.back.dto;

import com.puntocoma.back.entity.Rol;
import com.puntocoma.back.entity.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(Long id, String nombre, String email, Rol rol, boolean activo, LocalDateTime fechaRegistro) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol(), u.isActivo(), u.getFechaRegistro());
    }
}
