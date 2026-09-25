package com.dollarapi.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    @Hidden
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "service", "dollar-api-java",
                "version", "1.0.0",
                "status", "UP",
                "documentation", "/swagger-ui.html",
                "author", Map.of(
                        "name", "Federico Gabriel Osorio",
                        "portfolio", "https://portfolio-fedeosorio.vercel.app/",
                        "github", "https://github.com/FedeOsorio",
                        "linkedin", "https://www.linkedin.com/in/fedeosorio/"
                ),
                "timestamp", Instant.now().toString()
        ));
    }
}
