package com.puntocoma.back;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test")
    public String test() {
        return "¡El backend de Punto&Coma está funcionando correctamente!";
    }
}