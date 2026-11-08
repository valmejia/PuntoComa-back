package com.puntocoma.back.api;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mvc;

    protected static String emailUnico() {
        return "u" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    protected ResultActions registrar(String nombre, String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(nombre, email, password)));
    }

    protected ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    protected String token(String email, String password) throws Exception {
        String body = login(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    /** Registra un usuario nuevo y devuelve su token. */
    protected String nuevoUsuario() throws Exception {
        String email = emailUnico();
        registrar("Usuario Prueba", email, "password123").andExpect(status().isCreated());
        return token(email, "password123");
    }

    protected String tokenAdmin() throws Exception {
        return token("admin@puntocoma.com", "Admin12345");
    }

    protected ResultActions get(String url, String token) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .header("Authorization", "Bearer " + token));
    }

    protected ResultActions postJson(String url, String token, String json) throws Exception {
        return mvc.perform(post(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions putJson(String url, String token, String json) throws Exception {
        return mvc.perform(put(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json == null ? "" : json));
    }

    protected ResultActions delete(String url, String token) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url)
                .header("Authorization", "Bearer " + token));
    }

    protected static String libroJson(String titulo, String autor, String genero, String formato, String tipo,
                                      String precio, int stock) {
        return """
                {"titulo":"%s","autor":"%s","genero":"%s","formato":"%s","tipo":"%s","precio":%s,"stock":%d,
                 "descripcion":"Descripción de prueba"}
                """.formatted(titulo, autor, genero, formato, tipo, precio == null ? "null" : precio, stock);
    }

    protected long publicar(String token, String json) throws Exception {
        String body = postJson("/api/libros", token, json).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected static long id(ResultActions r) throws Exception {
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
