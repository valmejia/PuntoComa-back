package com.puntocoma.back.config;

import com.puntocoma.back.entity.*;
import com.puntocoma.back.repository.LibroRepository;
import com.puntocoma.back.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Crea el administrador inicial (el registro público sólo crea USUARIO)
 * y, opcionalmente, un vendedor y libros de ejemplo para probar el catálogo.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final LibroRepository libroRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}") private String adminEmail;
    @Value("${app.admin.password}") private String adminPassword;
    @Value("${app.seed.libros:false}") private boolean sembrarLibros;

    public DataInitializer(UsuarioRepository usuarioRepository, LibroRepository libroRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.libroRepository = libroRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        cifrarPasswordsLegadas();
        if (!usuarioRepository.existsByRol(Rol.ADMIN) && !usuarioRepository.existsByEmailIgnoreCase(adminEmail)) {
            crearUsuario("Administrador", adminEmail, adminPassword, Rol.ADMIN);
            log.info("Administrador inicial creado: {}", adminEmail);
        }
        if (sembrarLibros && libroRepository.count() == 0) {
            Usuario vendedor = usuarioRepository.findByEmailIgnoreCase("vendedor@puntocoma.com")
                    .orElseGet(() -> crearUsuario("Librería Punto&Coma", "vendedor@puntocoma.com", "Vendedor123", Rol.VENDEDOR));
            libro(vendedor, "Cien años de soledad", "Gabriel García Márquez", "Novela", FormatoLibro.FISICO, TipoPublicacion.AMBOS, "349.00", 3);
            libro(vendedor, "Pedro Páramo", "Juan Rulfo", "Novela", FormatoLibro.FISICO, TipoPublicacion.PRESTAMO, null, 2);
            libro(vendedor, "El Aleph", "Jorge Luis Borges", "Cuento", FormatoLibro.DIGITAL, TipoPublicacion.VENTA, "129.00", 100);
            libro(vendedor, "Fundación", "Isaac Asimov", "Ciencia ficción", FormatoLibro.FISICO, TipoPublicacion.AMBOS, "289.00", 1);
            libro(vendedor, "El principito", "Antoine de Saint-Exupéry", "Infantil", FormatoLibro.DIGITAL, TipoPublicacion.AMBOS, "99.00", 50);
            libro(vendedor, "Clean Code", "Robert C. Martin", "Tecnología", FormatoLibro.FISICO, TipoPublicacion.VENTA, "799.00", 4);
            log.info("Libros de ejemplo creados (vendedor@puntocoma.com / Vendedor123)");
        }
    }

    /**
     * El registro del Sprint 0 guardaba contraseñas en texto plano. Las cifra con BCrypt
     * para que esos usuarios puedan seguir iniciando sesión con su misma contraseña.
     */
    private void cifrarPasswordsLegadas() {
        var legados = usuarioRepository.findAll().stream()
                .filter(u -> u.getPassword() != null && !u.getPassword().startsWith("$2"))
                .toList();
        legados.forEach(u -> u.setPassword(passwordEncoder.encode(u.getPassword())));
        if (!legados.isEmpty()) {
            usuarioRepository.saveAll(legados);
            log.info("Contraseñas en texto plano cifradas con BCrypt: {}", legados.size());
        }
    }

    private Usuario crearUsuario(String nombre, String email, String password, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email.toLowerCase());
        u.setPassword(passwordEncoder.encode(password));
        u.setRol(rol);
        u.setActivo(true);
        return usuarioRepository.save(u);
    }

    private void libro(Usuario dueno, String titulo, String autor, String genero, FormatoLibro formato,
                       TipoPublicacion tipo, String precio, int stock) {
        Libro l = new Libro();
        l.setTitulo(titulo);
        l.setAutor(autor);
        l.setGenero(genero);
        l.setFormato(formato);
        l.setTipo(tipo);
        l.setPrecio(precio == null ? null : new BigDecimal(precio));
        l.setStock(stock);
        l.setEstado(stock > 0 ? EstadoLibro.DISPONIBLE : EstadoLibro.AGOTADO);
        l.setPublicadoPor(dueno);
        libroRepository.save(l);
    }
}
