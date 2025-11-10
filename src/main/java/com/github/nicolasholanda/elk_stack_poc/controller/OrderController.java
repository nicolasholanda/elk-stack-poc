package com.github.nicolasholanda.elk_stack_poc.controller;

import com.github.nicolasholanda.elk_stack_poc.model.Order;
import com.github.nicolasholanda.elk_stack_poc.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        log.info("Received request to create order for user: {}", order.getUserId());
        try {
            Order createdOrder = orderService.createOrder(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (Exception e) {
            log.error("Failed to create order", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        log.info("Received request to get order with id: {}", id);
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Order not found with id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        log.info("Received request to get all orders");
        List<Order> orders = orderService.getAllOrders();
        log.info("Retrieved {} orders", orders.size());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<Order> getOrderByOrderNumber(@PathVariable String orderNumber) {
        log.info("Received request to get order with orderNumber: {}", orderNumber);
        return orderService.getOrderByOrderNumber(orderNumber)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Order not found with orderNumber: {}", orderNumber);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable Long userId) {
        log.info("Received request to get orders for user: {}", userId);
        List<Order> orders = orderService.getOrdersByUserId(userId);
        log.info("Retrieved {} orders for user: {}", orders.size(), userId);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestParam Order.OrderStatus status) {
        log.info("Received request to update order status for id: {} to: {}", id, status);
        try {
            Order updatedOrder = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            log.error("Failed to update order status for id: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order orderDetails) {
        log.info("Received request to update order with id: {}", id);
        try {
            Order updatedOrder = orderService.updateOrder(id, orderDetails);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            log.error("Failed to update order with id: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        log.info("Received request to delete order with id: {}", id);
        try {
            orderService.deleteOrder(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete order with id: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }
}

