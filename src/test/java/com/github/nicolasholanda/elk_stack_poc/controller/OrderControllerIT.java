package com.github.nicolasholanda.elk_stack_poc.controller;

import com.github.nicolasholanda.elk_stack_poc.model.Order;
import com.github.nicolasholanda.elk_stack_poc.model.User;
import com.github.nicolasholanda.elk_stack_poc.repository.OrderRepository;
import com.github.nicolasholanda.elk_stack_poc.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testCreateOrder() throws Exception {
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .phone("1234567890")
                .build();
        User savedUser = userRepository.save(user);

        Order order = Order.builder()
                .userId(savedUser.getId())
                .orderNumber("ORD-001")
                .status(Order.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("99.99"))
                .description("Test order")
                .build();

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.orderNumber", matchesPattern("ORD-[A-F0-9]{8}")))
                .andExpect(jsonPath("$.userId", equalTo(savedUser.getId().intValue())))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void testGetOrderById() throws Exception {
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .phone("1234567890")
                .build();
        User savedUser = userRepository.save(user);

        Order order = Order.builder()
                .userId(savedUser.getId())
                .orderNumber("ORD-002")
                .status(Order.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("49.99"))
                .description("Another test order")
                .build();

        Order savedOrder = orderRepository.save(order);

        mockMvc.perform(get("/api/orders/{id}", savedOrder.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(savedOrder.getId().intValue())))
                .andExpect(jsonPath("$.orderNumber", equalTo("ORD-002")))
                .andExpect(jsonPath("$.userId", equalTo(savedUser.getId().intValue())));
    }

    @Test
    void testGetOrderById_NotFound() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllOrders() throws Exception {
        User user1 = User.builder()
                .name("User One")
                .email("user1@example.com")
                .phone("111111111")
                .build();
        User savedUser1 = userRepository.save(user1);

        User user2 = User.builder()
                .name("User Two")
                .email("user2@example.com")
                .phone("222222222")
                .build();
        User savedUser2 = userRepository.save(user2);

        Order order1 = Order.builder()
                .userId(savedUser1.getId())
                .orderNumber("ORD-003")
                .status(Order.OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal("199.99"))
                .description("First order")
                .build();

        Order order2 = Order.builder()
                .userId(savedUser2.getId())
                .orderNumber("ORD-004")
                .status(Order.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("299.99"))
                .description("Second order")
                .build();

        orderRepository.save(order1);
        orderRepository.save(order2);

        mockMvc.perform(get("/api/orders")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].orderNumber", equalTo("ORD-003")))
                .andExpect(jsonPath("$[1].orderNumber", equalTo("ORD-004")));
    }
}

