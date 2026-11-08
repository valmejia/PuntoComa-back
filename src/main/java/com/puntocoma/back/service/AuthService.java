package com.puntocoma.back.service;

import com.puntocoma.back.dto.AuthResponse;
import com.puntocoma.back.dto.LoginRequest;
import com.puntocoma.back.dto.RegistroRequest;
import com.puntocoma.back.dto.UsuarioResponse;
import com.puntocoma.back.entity.Rol;
import com.puntocoma.back.entity.Usuario;
import com.puntocoma.back.exception.ApiException;
import com.puntocoma.back.repository.UsuarioRepository;
import com.puntocoma.back.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** HU-01 / CU-01: registro con rol USUARIO por defecto y contraseña cifrada con BCrypt. */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflicto("El correo ya está registrado");
        }
        Usuario u = new Usuario();
        u.setNombre(request.nombre().trim());
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(request.password()));
        u.setRol(Rol.USUARIO);
        u.setActivo(true);
        return UsuarioResponse.de(usuarioRepository.save(u));
    }

    /** HU-02 / CU-02: valida credenciales y devuelve un JWT con el rol del usuario. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Usuario u = usuarioRepository.findByEmailIgnoreCase(normalizarEmail(request.email()))
                .filter(x -> passwordEncoder.matches(request.password(), x.getPassword()))
                .orElseThrow(() -> ApiException.noAutorizado("Correo o contraseña incorrectos"));
        if (!u.isActivo()) {
            throw ApiException.prohibido("Tu cuenta está bloqueada. Contacta al administrador");
        }
        return new AuthResponse(jwtService.generarToken(u), "Bearer", jwtService.getExpiracionMs(), UsuarioResponse.de(u));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(Long usuarioId) {
        return usuarioRepository.findById(usuarioId).map(UsuarioResponse::de)
                .orElseThrow(() -> ApiException.noEncontrado("Usuario no encontrado"));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
