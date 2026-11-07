package com.puntocoma.back.repository;

import com.puntocoma.back.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Esto servirá para verificar si un correo ya está registrado
    Optional<Usuario> findByEmail(String email);
}