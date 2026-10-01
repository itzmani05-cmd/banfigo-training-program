package com.example.week1_backend_assesment.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {
    private final DataSource dataSource;
    private final String applicationName;

    public HealthController(DataSource dataSource, @Value("${spring.application.name}") String applicationName) {
        this.dataSource = dataSource;
        this.applicationName = applicationName;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean databaseUp = isDatabaseUp();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", databaseUp ? "UP" : "DOWN");
        response.put("database", databaseUp ? "UP" : "DOWN");
        response.put("timestamp", LocalDateTime.now());
        HttpStatus status = databaseUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return new ResponseEntity<>(response, status);
    }

    @GetMapping("/api/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", applicationName);
        response.put("description", "Mini Banking / Open Banking Consent Management System");
        response.put("version", "0.0.1-SNAPSHOT");
        response.put("javaVersion", System.getProperty("java.version"));
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    private boolean isDatabaseUp() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception ex) {
            return false;
        }
    }
}
