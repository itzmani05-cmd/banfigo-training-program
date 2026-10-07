package com.example.banfigo.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Runs a tiny controller through the real handler to check each error comes back with the right status and shape
class GlobalExceptionHandlerTest {

    @RestController
    static class TestController {
        @GetMapping("/items/{id}")
        ResponseEntity<String> get(@PathVariable Long id) {
            if (id == 1) throw new IllegalStateException("Item 1 is locked");
            if (id == 2) throw new RuntimeException("database password is hunter2");
            return ResponseEntity.ok("ok");
        }

        @PostMapping("/items")
        ResponseEntity<String> create(@RequestBody Map<String, Object> body) {
            return ResponseEntity.ok("ok");
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void wrongStateIsConflict() throws Exception {
        mvc.perform(get("/items/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Item 1 is locked"));
    }

    @Test
    void wrongPathTypeIsBadRequest() throws Exception {
        mvc.perform(get("/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mvc.perform(post("/items").contentType(APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON for this endpoint"));
    }

    @Test
    void wrongHttpMethodKeepsItsStatus() throws Exception {
        mvc.perform(put("/items/5"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unexpectedErrorIsGeneric500WithoutInternals() throws Exception {
        mvc.perform(get("/items/2"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."));
    }
}
