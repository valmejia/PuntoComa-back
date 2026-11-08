package com.puntocoma.back.api;

import com.jayway.jsonpath.JsonPath;
import com.puntocoma.back.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sprint 1: HU-01 registro, HU-02 login con roles (JWT), HU-03 catálogo con filtros, CA-10 BCrypt. */
@SpringBootTest
@AutoConfigureMockMvc
class Sprint1AuthCatalogoTest extends ApiTestSupport {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Test
    void registroCreaUsuarioConRolUsuarioYPasswordCifrada() throws Exception {
        String email = emailUnico();
        registrar("Valeria Mejia", email, "miPassword123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("USUARIO"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist());

        String hash = usuarioRepository.findByEmailIgnoreCase(email).orElseThrow().getPassword();
        assertThat(hash).startsWith("$2").isNotEqualTo("miPassword123");
    }

    @Test
    void registroRechazaEmailDuplicadoInvalidoYPasswordCorta() throws Exception {
        String email = emailUnico();
        registrar("A", email, "password123").andExpect(status().isCreated());
        registrar("B", email.toUpperCase(), "password123").andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El correo ya está registrado"));
        registrar("C", "no-es-email", "password123").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").exists());
        registrar("D", emailUnico(), "corta").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    void endpointAntiguoDeRegistroSigueFuncionando() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/usuarios/registro")
                        .contentType("application/json")
                        .content("{\"nombre\":\"X\",\"email\":\"%s\",\"password\":\"password123\"}".formatted(emailUnico())))
                .andExpect(status().isCreated());
    }

    @Test
    void loginDevuelveJwtConRolYMeLoUsa() throws Exception {
        String email = emailUnico();
        registrar("Jesus", email, "password123");
        String body = login(email, "password123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol").value("USUARIO"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");
        get("/api/auth/me", token).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void loginConCredencialesIncorrectasDa401() throws Exception {
        String email = emailUnico();
        registrar("X", email, "password123");
        login(email, "otraPassword").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
        login("noexiste@test.com", "password123").andExpect(status().isUnauthorized());
    }

    @Test
    void rutasProtegidasSinTokenOTokenInvalidoDan401() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/libros"))
                .andExpect(status().isUnauthorized());
        get("/api/libros", "token.falso.123").andExpect(status().isUnauthorized());
    }

    @Test
    void adminGestionaRolesYBloqueaCuentas() throws Exception {
        String admin = tokenAdmin();
        String email = emailUnico();
        long id = id(registrar("Futuro vendedor", email, "password123"));

        // Un usuario normal no puede entrar al panel de administración
        get("/api/admin/usuarios", token(email, "password123")).andExpect(status().isForbidden());

        putJson("/api/admin/usuarios/" + id + "/rol", admin, "{\"rol\":\"VENDEDOR\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("VENDEDOR"));
        login(email, "password123").andExpect(jsonPath("$.usuario.rol").value("VENDEDOR"));

        putJson("/api/admin/usuarios/" + id + "/estado", admin, "{\"activo\":false}").andExpect(status().isOk());
        login(email, "password123").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje", containsString("bloqueada")));

        get("/api/admin/usuarios", admin).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(greaterThan(1))));
    }

    @Test
    void catalogoFiltraPorTituloAutorGeneroFormatoYTipo() throws Exception {
        String t = nuevoUsuario();
        String sufijo = Long.toString(System.nanoTime());
        String autor = "Autor " + sufijo;
        publicar(t, libroJson("Rayuela " + sufijo, autor, "Novela", "FISICO", "VENTA", "200.00", 2));
        publicar(t, libroJson("Ficciones " + sufijo, autor, "Cuento", "DIGITAL", "PRESTAMO", null, 1));
        publicar(t, libroJson("Aura " + sufijo, autor, "Novela", "DIGITAL", "AMBOS", "80.00", 0));

        get("/api/libros?autor=" + sufijo, t).andExpect(jsonPath("$", hasSize(3)));
        get("/api/libros?q=rayuela " + sufijo, t).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].titulo").value("Rayuela " + sufijo));
        get("/api/libros?autor=" + sufijo + "&genero=novela", t).andExpect(jsonPath("$", hasSize(2)));
        get("/api/libros?autor=" + sufijo + "&formato=DIGITAL", t).andExpect(jsonPath("$", hasSize(2)));
        // AMBOS aparece en VENTA y en PRESTAMO
        get("/api/libros?autor=" + sufijo + "&tipo=PRESTAMO", t).andExpect(jsonPath("$", hasSize(2)));
        get("/api/libros?autor=" + sufijo + "&disponible=true", t).andExpect(jsonPath("$", hasSize(2)));
        get("/api/libros?q=" + sufijo + "-nada", t).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        get("/api/libros?formato=PAPIRO", t).andExpect(status().isBadRequest());
        get("/api/libros/generos", t).andExpect(jsonPath("$", hasItems("Novela", "Cuento")));
    }
}
