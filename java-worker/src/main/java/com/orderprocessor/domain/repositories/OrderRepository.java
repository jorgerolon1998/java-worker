package com.orderprocessor.domain.repositories;

import com.orderprocessor.domain.entities.Order;
import reactor.core.publisher.Mono;

/**
 * Repository interface for Order domain entity
 */
public interface OrderRepository {

    /**
     * Save an order
     * @param order the order to save
     * @return the saved order
     */
    Mono<Order> save(Order order);

    /**
     * Find order by order ID
     * @param orderId the order ID
     * @return the order if found, empty if not
     */
    Mono<Order> findByOrderId(String orderId);

    /**
     * Check if order exists by order ID
     * @param orderId the order ID
     * @return true if exists, false otherwise
     */
    Mono<Boolean> existsByOrderId(String orderId);
} 