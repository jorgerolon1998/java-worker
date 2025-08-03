package com.orderprocessor.infrastructure.external;

import com.orderprocessor.domain.entities.CustomerDetails;
import com.orderprocessor.domain.entities.CustomerStatus;
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
 * Client for Customer Service API
 */
@Service
public class CustomerServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceClient.class);

    private final WebClient webClient;
    private final String baseUrl;

    public CustomerServiceClient(@Value("${app.external.customer-service.url:http://localhost:8082}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    /**
     * Get customer details by ID
     * @param customerId the customer ID
     * @return the customer details
     */
    @CircuitBreaker(name = "customerService", fallbackMethod = "getCustomerFallback")
    @Retry(name = "customerService")
    public Mono<CustomerDetails> getCustomer(String customerId) {
        logger.debug("Fetching customer details for ID: {}", customerId);

        return webClient.get()
                .uri("/api/customers/{customerId}", customerId)
                .retrieve()
                .bodyToMono(CustomerResponse.class)
                .timeout(Duration.ofSeconds(10))
                .map(this::mapToCustomerDetails)
                .doOnSuccess(customer -> logger.debug("Successfully fetched customer: {}", customerId))
                .doOnError(error -> logger.error("Error fetching customer {}: {}", customerId, error.getMessage()));
    }

    private CustomerDetails mapToCustomerDetails(CustomerResponse response) {
        CustomerDetails customer = new CustomerDetails();
        customer.setCustomerId(response.getId());
        customer.setName(response.getName());
        customer.setEmail(response.getEmail());
        customer.setStatus(CustomerStatus.fromString(response.getStatus()));
        customer.setCreditLimit(response.getCreditLimit());
        customer.setCurrentBalance(response.getCurrentBalance());
        return customer;
    }

    /**
     * Fallback method for circuit breaker
     */
    public Mono<CustomerDetails> getCustomerFallback(String customerId, Exception exception) {
        logger.warn("Customer service fallback triggered for customer {}: {}", customerId, exception.getMessage());
        return Mono.error(new RuntimeException("Customer service unavailable for customer: " + customerId));
    }

    /**
     * Customer response DTO
     */
    public static class CustomerResponse {
        private String id;
        private String name;
        private String email;
        private String status;
        private java.math.BigDecimal creditLimit;
        private java.math.BigDecimal currentBalance;

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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public java.math.BigDecimal getCreditLimit() {
            return creditLimit;
        }

        public void setCreditLimit(java.math.BigDecimal creditLimit) {
            this.creditLimit = creditLimit;
        }

        public java.math.BigDecimal getCurrentBalance() {
            return currentBalance;
        }

        public void setCurrentBalance(java.math.BigDecimal currentBalance) {
            this.currentBalance = currentBalance;
        }
    }
} 