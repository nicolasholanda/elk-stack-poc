package com.github.nicolasholanda.elk_stack_poc.controller;

import com.github.nicolasholanda.elk_stack_poc.model.User;
import com.github.nicolasholanda.elk_stack_poc.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testCreateUser() throws Exception {
        User user = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .phone("123456789")
                .build();

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", equalTo("John Doe")))
                .andExpect(jsonPath("$.email", equalTo("john@example.com")))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void testGetUserById() throws Exception {
        User user = User.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .phone("987654321")
                .build();

        User savedUser = userRepository.save(user);

        mockMvc.perform(get("/api/users/{id}", savedUser.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(savedUser.getId().intValue())))
                .andExpect(jsonPath("$.name", equalTo("Jane Doe")))
                .andExpect(jsonPath("$.email", equalTo("jane@example.com")));
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllUsers() throws Exception {
        User user1 = User.builder()
                .name("User One")
                .email("user1@example.com")
                .phone("111111111")
                .build();

        User user2 = User.builder()
                .name("User Two")
                .email("user2@example.com")
                .phone("222222222")
                .build();

        userRepository.save(user1);
        userRepository.save(user2);

        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", equalTo("User One")))
                .andExpect(jsonPath("$[1].name", equalTo("User Two")));
    }
}

