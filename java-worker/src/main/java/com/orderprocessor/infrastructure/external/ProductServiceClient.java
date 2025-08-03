package com.orderprocessor.infrastructure.external;

import com.orderprocessor.domain.entities.OrderProduct;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Client for Product Service API
 */
@Service
public class ProductServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceClient.class);

    private final WebClient webClient;
    private final String baseUrl;

    public ProductServiceClient(@Value("${app.external.product-service.url:http://localhost:8081}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    /**
     * Get product details by ID
     * @param productId the product ID
     * @return the product details
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    @Retry(name = "productService")
    public Mono<OrderProduct> getProduct(String productId) {
        logger.debug("Fetching product details for ID: {}", productId);

        return webClient.get()
                .uri("/api/products/{productId}", productId)
                .retrieve()
                .bodyToMono(ProductResponse.class)
                .timeout(Duration.ofSeconds(10))
                .map(this::mapToOrderProduct)
                .doOnSuccess(product -> logger.debug("Successfully fetched product: {}", productId))
                .doOnError(error -> logger.error("Error fetching product {}: {}", productId, error.getMessage()));
    }

    private OrderProduct mapToOrderProduct(ProductResponse response) {
        OrderProduct product = new OrderProduct();
        product.setProductId(response.getId());
        product.setName(response.getName());
        product.setDescription(response.getDescription());
        product.setPrice(response.getPrice());
        product.setActive(response.isActive());
        return product;
    }

    /**
     * Fallback method for circuit breaker
     */
    public Mono<OrderProduct> getProductFallback(String productId, Exception exception) {
        logger.warn("Product service fallback triggered for product {}: {}", productId, exception.getMessage());
        return Mono.error(new RuntimeException("Product service unavailable for product: " + productId));
    }

    /**
     * Product response DTO
     */
    public static class ProductResponse {
        private String id;
        private String name;
        private String description;
        private java.math.BigDecimal price;
        private boolean active;

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public java.math.BigDecimal getPrice() {
            return price;
        }

        public void setPrice(java.math.BigDecimal price) {
            this.price = price;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
} 