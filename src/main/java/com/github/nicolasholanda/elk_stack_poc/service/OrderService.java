package com.github.nicolasholanda.elk_stack_poc.service;

import com.github.nicolasholanda.elk_stack_poc.model.Order;
import com.github.nicolasholanda.elk_stack_poc.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public Order createOrder(Order order) {
        log.info("Creating new order for user: {}", order.getUserId());
        order.setOrderNumber(generateOrderNumber());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        try {
            Order savedOrder = orderRepository.save(order);
            log.info("Order created successfully with id: {}, orderNumber: {}", savedOrder.getId(), savedOrder.getOrderNumber());
            return savedOrder;
        } catch (Exception e) {
            log.error("Error creating order for user: {}", order.getUserId(), e);
            throw new RuntimeException("Failed to create order", e);
        }
    }

    public Optional<Order> getOrderById(Long id) {
        log.debug("Fetching order with id: {}", id);
        return orderRepository.findById(id);
    }

    public List<Order> getAllOrders() {
        log.debug("Fetching all orders");
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderByOrderNumber(String orderNumber) {
        log.debug("Fetching order with orderNumber: {}", orderNumber);
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public List<Order> getOrdersByUserId(Long userId) {
        log.debug("Fetching orders for user: {}", userId);
        List<Order> orders = orderRepository.findByUserId(userId);
        log.debug("Found {} orders for user: {}", orders.size(), userId);
        return orders;
    }

    public Order updateOrderStatus(Long id, Order.OrderStatus newStatus) {
        log.info("Updating order status for id: {} to status: {}", id, newStatus);

        return orderRepository.findById(id)
                .map(order -> {
                    Order.OrderStatus oldStatus = order.getStatus();
                    log.debug("Order found, updating status from {} to {}", oldStatus, newStatus);
                    order.setStatus(newStatus);
                    order.setUpdatedAt(LocalDateTime.now());
                    Order updated = orderRepository.save(order);
                    log.info("Order status updated successfully. Id: {}, oldStatus: {}, newStatus: {}", id, oldStatus, newStatus);
                    return updated;
                })
                .orElseThrow(() -> {
                    log.warn("Order not found with id: {}", id);
                    return new RuntimeException("Order not found with id: " + id);
                });
    }

    public Order updateOrder(Long id, Order orderDetails) {
        log.info("Updating order with id: {}", id);

        return orderRepository.findById(id)
                .map(order -> {
                    log.debug("Order found, updating details for id: {}", id);
                    order.setDescription(orderDetails.getDescription());
                    order.setTotalAmount(orderDetails.getTotalAmount());
                    order.setUpdatedAt(LocalDateTime.now());
                    Order updated = orderRepository.save(order);
                    log.info("Order updated successfully with id: {}", id);
                    return updated;
                })
                .orElseThrow(() -> {
                    log.warn("Order not found with id: {}", id);
                    return new RuntimeException("Order not found with id: " + id);
                });
    }

    public void deleteOrder(Long id) {
        log.info("Deleting order with id: {}", id);

        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
            log.info("Order deleted successfully with id: {}", id);
        } else {
            log.warn("Order not found for deletion with id: {}", id);
            throw new RuntimeException("Order not found with id: " + id);
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

