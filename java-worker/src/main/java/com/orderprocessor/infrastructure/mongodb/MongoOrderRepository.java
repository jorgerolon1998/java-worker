package com.orderprocessor.infrastructure.mongodb;

import com.orderprocessor.domain.entities.Order;
import com.orderprocessor.domain.repositories.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * MongoDB implementation of OrderRepository
 */
@Repository
public class MongoOrderRepository implements OrderRepository {

    private static final Logger logger = LoggerFactory.getLogger(MongoOrderRepository.class);

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoOrderRepository(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Order> save(Order order) {
        return mongoTemplate.save(order)
                .doOnSuccess(savedOrder -> logger.info("Order saved successfully: {}", savedOrder.getOrderId()))
                .doOnError(error -> logger.error("Error saving order {}: {}", order.getOrderId(), error.getMessage()));
    }

    @Override
    public Mono<Order> findByOrderId(String orderId) {
        Query query = new Query(Criteria.where("orderId").is(orderId));
        
        return mongoTemplate.findOne(query, Order.class)
                .doOnSuccess(order -> {
                    if (order != null) {
                        logger.debug("Order found: {}", orderId);
                    } else {
                        logger.debug("Order not found: {}", orderId);
                    }
                })
                .doOnError(error -> logger.error("Error finding order {}: {}", orderId, error.getMessage()));
    }

    @Override
    public Mono<Boolean> existsByOrderId(String orderId) {
        Query query = new Query(Criteria.where("orderId").is(orderId));
        
        return mongoTemplate.exists(query, Order.class)
                .doOnSuccess(exists -> logger.debug("Order exists check for {}: {}", orderId, exists))
                .doOnError(error -> logger.error("Error checking order existence for {}: {}", orderId, error.getMessage()));
    }
} 