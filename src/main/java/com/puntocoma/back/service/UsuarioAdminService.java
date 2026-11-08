package com.puntocoma.back.service;

import com.puntocoma.back.dto.UsuarioResponse;
import com.puntocoma.back.entity.Rol;
import com.puntocoma.back.entity.Usuario;
import com.puntocoma.back.exception.ApiException;
import com.puntocoma.back.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/** Gestión de roles y bloqueo de cuentas (sólo ADMIN). */
@Service
public class UsuarioAdminService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioAdminService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll(Sort.by("id")).stream().map(UsuarioResponse::de).toList();
    }

    @Transactional
    public UsuarioResponse cambiarRol(Long id, Rol rol, Long adminId) {
        if (Objects.equals(id, adminId)) {
            throw ApiException.badRequest("No puedes cambiar tu propio rol");
        }
        Usuario u = buscar(id);
        u.setRol(rol);
        return UsuarioResponse.de(u);
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo, Long adminId) {
        if (Objects.equals(id, adminId)) {
            throw ApiException.badRequest("No puedes bloquear tu propia cuenta");
        }
        Usuario u = buscar(id);
        u.setActivo(activo);
        return UsuarioResponse.de(u);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> ApiException.noEncontrado("Usuario no encontrado"));
    }
}
