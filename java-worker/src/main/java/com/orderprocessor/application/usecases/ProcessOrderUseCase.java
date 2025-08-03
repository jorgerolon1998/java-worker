package com.orderprocessor.application.usecases;

import com.orderprocessor.application.dtos.OrderMessage;
import com.orderprocessor.domain.entities.Order;
import com.orderprocessor.domain.entities.OrderProduct;
import com.orderprocessor.domain.entities.CustomerDetails;
import com.orderprocessor.domain.repositories.OrderRepository;
import com.orderprocessor.infrastructure.external.ProductServiceClient;
import com.orderprocessor.infrastructure.external.CustomerServiceClient;
import com.orderprocessor.infrastructure.redis.DistributedLockService;
import com.orderprocessor.infrastructure.redis.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main use case for processing orders
 */
@Service
public class ProcessOrderUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ProcessOrderUseCase.class);
    private static final String LOCK_PREFIX = "order:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final CustomerServiceClient customerServiceClient;
    private final DistributedLockService lockService;
    private final CacheService cacheService;

    public ProcessOrderUseCase(OrderRepository orderRepository,
                             ProductServiceClient productServiceClient,
                             CustomerServiceClient customerServiceClient,
                             DistributedLockService lockService,
                             CacheService cacheService) {
        this.orderRepository = orderRepository;
        this.productServiceClient = productServiceClient;
        this.customerServiceClient = customerServiceClient;
        this.lockService = lockService;
        this.cacheService = cacheService;
    }

    /**
     * Process an order message from Kafka
     * @param orderMessage the order message to process
     * @return the processed order
     */
    public Mono<Order> processOrder(OrderMessage orderMessage) {
        String orderId = orderMessage.getOrderId();
        String lockKey = LOCK_PREFIX + orderId;

        logger.info("Processing order: {}", orderId);

        return lockService.acquireLock(lockKey, LOCK_TTL)
                .flatMap(lockAcquired -> {
                    if (!lockAcquired) {
                        logger.warn("Order {} is already being processed", orderId);
                        return Mono.empty();
                    }

                    return processOrderWithLock(orderMessage)
                            .doFinally(signalType -> lockService.releaseLock(lockKey));
                })
                .doOnError(error -> logger.error("Error processing order {}: {}", orderId, error.getMessage()));
    }

    private Mono<Order> processOrderWithLock(OrderMessage orderMessage) {
        String orderId = orderMessage.getOrderId();

        return orderRepository.existsByOrderId(orderId)
                .flatMap(exists -> {
                    if (exists) {
                        logger.info("Order {} already exists, skipping", orderId);
                        return Mono.empty();
                    }

                    return enrichAndSaveOrder(orderMessage);
                });
    }

    private Mono<Order> enrichAndSaveOrder(OrderMessage orderMessage) {
        String orderId = orderMessage.getOrderId();
        String customerId = orderMessage.getCustomerId();

        logger.info("Enriching order {} with external data", orderId);

        return Mono.zip(
                enrichProducts(orderMessage.getProductIds()),
                enrichCustomer(customerId)
        ).flatMap(tuple -> {
            List<OrderProduct> products = tuple.getT1();
            CustomerDetails customerDetails = tuple.getT2();

            // Validate business rules
            return validateOrder(orderMessage, products, customerDetails)
                    .flatMap(valid -> {
                        if (!valid) {
                            logger.warn("Order {} validation failed", orderId);
                            return Mono.empty();
                        }

                        // Validate that we have products
                        if (products == null || products.isEmpty()) {
                            logger.warn("Order {} has no products, skipping", orderId);
                            return Mono.empty();
                        }

                        // Create and save order
                        Order order = new Order(orderId, customerId, products);
                        order.enrichWithCustomerDetails(customerDetails);
                        order.markAsCompleted();

                        return orderRepository.save(order)
                                .doOnSuccess(savedOrder -> logger.info("Order {} processed successfully", orderId))
                                .doOnError(error -> logger.error("Error saving order {}: {}", orderId, error.getMessage()));
                    });
        });
    }

    private Mono<List<OrderProduct>> enrichProducts(List<String> productIds) {
        logger.info("Starting product enrichment for {} products: {}", productIds.size(), productIds);
        return Flux.fromIterable(productIds)
                .flatMap(productId -> getProductWithCache(productId))
                .collectList()
                .doOnSuccess(products -> logger.info("Product enrichment completed. Found {} products: {}", 
                    products.size(), products.stream().map(p -> p.getProductId()).toList()))
                .doOnError(error -> logger.error("Error enriching products: {}", error.getMessage()));
    }

    private Mono<OrderProduct> getProductWithCache(String productId) {
        String cacheKey = "product:" + productId;
        logger.info("Getting product {} from cache or API", productId);

        return cacheService.get(cacheKey, OrderProduct.class)
                .doOnSuccess(cachedProduct -> {
                    if (cachedProduct != null) {
                        logger.info("Found product {} in cache", productId);
                    }
                })
                .switchIfEmpty(
                        productServiceClient.getProduct(productId)
                                .doOnSuccess(product -> {
                                    logger.info("Retrieved product {} from API: {}", productId, product);
                                    cacheService.set(cacheKey, product, CACHE_TTL);
                                })
                                .doOnError(error -> logger.error("Error getting product {} from API: {}", productId, error.getMessage()))
                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                                        .filter(throwable -> !(throwable instanceof IllegalArgumentException)))
                )
                .doOnError(error -> logger.error("Error getting product {}: {}", productId, error.getMessage()));
    }

    private Mono<CustomerDetails> enrichCustomer(String customerId) {
        String cacheKey = "customer:" + customerId;

        return cacheService.get(cacheKey, CustomerDetails.class)
                .switchIfEmpty(
                        customerServiceClient.getCustomer(customerId)
                                .doOnSuccess(customer -> cacheService.set(cacheKey, customer, CACHE_TTL))
                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                                        .filter(throwable -> !(throwable instanceof IllegalArgumentException)))
                );
    }

    private Mono<Boolean> validateOrder(OrderMessage orderMessage, List<OrderProduct> products, CustomerDetails customerDetails) {
        // Validate customer is active
        if (!customerDetails.isActive()) {
            logger.warn("Customer {} is not active", customerDetails.getCustomerId());
            return Mono.just(false);
        }

        // Validate all products are active
        boolean allProductsActive = products.stream().allMatch(OrderProduct::isActive);
        if (!allProductsActive) {
            logger.warn("Some products in order {} are not active", orderMessage.getOrderId());
            return Mono.just(false);
        }

        // Validate credit limit
        double totalAmount = products.stream()
                .mapToDouble(product -> product.getPrice().doubleValue())
                .sum();

        if (!customerDetails.hasAvailableCredit(java.math.BigDecimal.valueOf(totalAmount))) {
            logger.warn("Customer {} does not have sufficient credit for order {}", 
                    customerDetails.getCustomerId(), orderMessage.getOrderId());
            return Mono.just(false);
        }

        return Mono.just(true);
    }
} 