package com.puntocoma.back.service;

import com.puntocoma.back.dto.RegistroRequest;
import com.puntocoma.back.dto.UsuarioResponse;
import com.puntocoma.back.entity.Usuario;
import com.puntocoma.back.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public UsuarioResponse registrarUsuario(RegistroRequest request) {
        // 1. Validar que el email no exista
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("El correo ya está registrado");
        }

        // 2. Crear el usuario y asignar valores
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.getNombre());
        nuevoUsuario.setEmail(request.getEmail());

        // NOTA: Aquí luego aplicaremos BCrypt para encriptar, por ahora lo dejamos plano para probar
        nuevoUsuario.setPassword(request.getPassword());
        nuevoUsuario.setRol("USUARIO"); // Rol por defecto según tu documento

        // 3. Guardar en la base de datos
        Usuario guardado = usuarioRepository.save(nuevoUsuario);

        // 4. Retornar respuesta segura (Sin contraseña)
        return new UsuarioResponse(guardado.getId(), guardado.getNombre(), guardado.getEmail(), guardado.getRol());
    }
}