package com.puntocoma.back.dto;

import com.puntocoma.back.entity.Rol;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequest(@NotNull(message = "El rol es obligatorio") Rol rol) {}
